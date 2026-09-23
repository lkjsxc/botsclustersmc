use super::super::super::next as shared_root;
use shared_root::Result;

/// Non-negative sampled KL estimator for actions drawn from the OLD policy:
/// mean(exp(new_logp-old_logp)-1-(new_logp-old_logp)).
/// Use the entire behaviour batch after an update, not only the last minibatch.
pub fn sampled_kl(old_logp: &[f64], new_logp: &[f64]) -> Result<f64> {
    if old_logp.is_empty() || old_logp.len() != new_logp.len() { return Err("KL shape mismatch"); }
    let mut sum = 0.0;
    for (&a,&b) in old_logp.iter().zip(new_logp) {
        if !a.is_finite() || !b.is_finite() { return Err("nonfinite log probability"); }
        let d = b-a;
        if !d.is_finite() || d.abs()>60.0 { return Err("unsafe KL ratio"); }
        // exp_m1 avoids cancellation for tiny policy updates.
        sum += (d.exp_m1()-d).max(0.0);
    }
    let kl=sum/old_logp.len() as f64;
    if !kl.is_finite() {return Err("KL overflow");} Ok(kl)
}

#[derive(Clone, Copy, Debug)]
pub struct TrustConfig {
    pub learning_rate: f64,
    pub maximum_kl: f64,
    pub retries: usize,
    pub shrink: f64,
}
impl Default for TrustConfig {
    fn default()->Self {Self {learning_rate:3e-4,maximum_kl:0.03,retries:3,shrink:0.5}}
}
#[derive(Clone, Copy, Debug)]
pub struct TrustReport {pub attempts:usize,pub learning_rate:f64,pub final_kl:f64}

/// `state` MUST include model weights, optimizer moments/step count and ALL RNG
/// streams mutated by `update`. Do not write checkpoints, increment curriculum
/// generations, or publish models inside either closure. Such side effects cannot
/// be rolled back by a value snapshot.
///
/// Large but finite KL retries from the exact same initial state at a lower rate.
/// NaNs, update errors and measurement errors fail closed and restore everything.
/// This is an engineering guard around PPO, not RePPO, TRPO or a new RL algorithm.
pub fn guarded_update<S:Clone>(state:&mut S, cfg:TrustConfig,
    mut update:impl FnMut(&mut S,f64)->Result<()>,
    mut measure:impl FnMut(&S)->Result<f64>) -> Result<TrustReport> {
    if !cfg.learning_rate.is_finite() || cfg.learning_rate<=0.0 || !cfg.maximum_kl.is_finite() || cfg.maximum_kl<=0.0 ||
        !(1..=8).contains(&cfg.retries) || !cfg.shrink.is_finite() || !(0.0..1.0).contains(&cfg.shrink) || cfg.shrink==0.0 {
        return Err("invalid trust-region guard parameters");
    }
    let original=state.clone(); let mut lr=cfg.learning_rate;
    for attempt in 1..=cfg.retries {
        *state=original.clone();
        if let Err(e)=update(state,lr) { *state=original; return Err(e); }
        let kl=match measure(state) {Ok(k) if k.is_finite() && k>=0.0=>k,
            Ok(_)=>{*state=original;return Err("invalid final KL");},
            Err(e)=>{*state=original;return Err(e);}};
        if kl<=cfg.maximum_kl {return Ok(TrustReport {attempts:attempt,learning_rate:lr,final_kl:kl});}
        lr*=cfg.shrink;
        if !lr.is_finite() || lr<=0.0 { *state=original;return Err("learning rate underflow"); }
    }
    *state=original; Err("every guarded PPO attempt exceeded final KL limit")
}

#[cfg(test)]
mod tests {
    use super::*;
    use shared_root::Rng;
    #[derive(Clone,Debug,PartialEq)] struct State {weight:f64,moment:f64,step:u64,rng:Rng}
    fn state()->State {State{weight:1.0,moment:0.0,step:5,rng:Rng(7)}}
    #[test] fn identity_and_small_kl_are_stable() {
        assert_eq!(sampled_kl(&[-1.0,-2.0],&[-1.0,-2.0]).unwrap(),0.0);
        let k=sampled_kl(&[-1.0],&[-1.0+1e-8]).unwrap(); assert!(k>0.0 && k<1e-15);
        assert!(sampled_kl(&[],&[]).is_err()); assert!(sampled_kl(&[-1.0],&[f64::NAN]).is_err());
    }
    #[test] fn retries_restore_weights_optimizer_step_and_rng() {
        let mut s=state();let old=s.clone();let mut draws=Vec::new();
        let r=guarded_update(&mut s,TrustConfig{learning_rate:1.0,maximum_kl:0.3,retries:4,shrink:0.5},
            |s,lr|{draws.push(s.rng.next_u64());s.weight+=lr;s.moment+=lr;s.step+=1;Ok(())},
            |s|Ok(s.weight-1.0)).unwrap();
        assert_eq!(r.attempts,3);assert_eq!(r.learning_rate,0.25);assert_eq!(s.step,6);
        assert_eq!(s.weight,1.25);assert_eq!(s.moment,0.25);assert_eq!(draws.len(),3);assert!(draws.iter().all(|x|*x==draws[0]));
        let mut rng=old.rng;rng.next_u64();assert_eq!(s.rng,rng);
    }
    #[test] fn all_rejected_updates_leave_no_side_effects() {
        let mut s=state();let old=s.clone();
        let e=guarded_update(&mut s,TrustConfig::default(),|s,lr|{s.weight+=lr;s.step+=1;s.rng.next_u64();Ok(())},|_|Ok(100.0));
        assert!(e.is_err());assert_eq!(s,old);
    }
    #[test] fn update_and_measure_errors_restore_state() {
        let mut s=state();let old=s.clone();
        assert!(guarded_update(&mut s,TrustConfig::default(),|s,_|{s.weight=9.0;Err("broken gradient")},|_|Ok(0.0)).is_err());assert_eq!(s,old);
        assert!(guarded_update(&mut s,TrustConfig::default(),|s,_|{s.step+=9;Ok(())},|_|Err("bad model")).is_err());assert_eq!(s,old);
        assert!(guarded_update(&mut s,TrustConfig::default(),|s,_|{s.moment=9.0;Ok(())},|_|Ok(f64::NAN)).is_err());assert_eq!(s,old);
    }
    #[test] fn invalid_parameters_never_invoke_optimizer() {
        let mut s=state();let mut called=false;
        let c=TrustConfig{learning_rate:f64::NAN,..Default::default()};
        assert!(guarded_update(&mut s,c,|_,_|{called=true;Ok(())},|_|Ok(0.0)).is_err());assert!(!called);
    }
}
