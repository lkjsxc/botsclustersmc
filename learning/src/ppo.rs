use super::{network::{Model, log_probability}, rng::Rng};

#[derive(Clone)]
pub struct Transition {
    pub obs: Vec<f32>, pub mask: Vec<bool>, pub actions: Vec<usize>,
    pub old_logp: f32, pub value: f32, pub next_value: f32,
    pub reward: f32, pub terminal: bool,
}
#[derive(Clone)]
pub struct Rollout { pub version: u64, pub steps: Vec<Transition> }
#[derive(Clone)]
pub struct Sample { pub t: Transition, pub advantage: f32, pub target: f32 }
#[derive(Clone, Debug)]
pub struct Params {
    pub gamma: f32, pub lambda: f32, pub clip: f32, pub entropy: f32,
    pub value_coef: f32, pub lr: f32, pub grad_clip: f32,
    pub epochs: usize, pub minibatch: usize, pub target_kl: f32,
}
impl Default for Params {
    fn default() -> Self {
        Self { gamma: 0.997, lambda: 0.95, clip: 0.2, entropy: 0.002,
            value_coef: 0.5, lr: 0.0003, grad_clip: 0.5, epochs: 3, minibatch: 64, target_kl: 0.03 }
    }
}
impl Params {
    pub fn validate(&self) -> Result<(),String> {
        if ![self.gamma,self.lambda,self.clip,self.entropy,self.value_coef,self.lr,
            self.grad_clip,self.target_kl].iter().all(|x|x.is_finite())
            || !(0.0..=1.0).contains(&self.gamma) || !(0.0..=1.0).contains(&self.lambda)
            || self.clip<=0.0 || self.clip>=1.0 || self.entropy<0.0 || self.value_coef<0.0
            || self.lr<=0.0 || self.grad_clip<=0.0 || self.target_kl<=0.0
            || self.epochs==0 || self.minibatch==0 {
            return Err("invalid PPO parameters".into());
        }
        Ok(())
    }
}
fn validate_transition(t:&Transition,m:&Model)->Result<(),String> {
    if t.obs.len()!=m.input || t.mask.len()+1!=m.layout().outputs || t.actions.len()!=m.heads.len()
        || !t.obs.iter().all(|x|x.is_finite())
        || ![t.value,t.next_value,t.reward,t.old_logp].iter().all(|x|x.is_finite()) {
        return Err("invalid transition shape or non-finite input".into());
    }
    let mut off=0;
    for (&n,&a) in m.heads.iter().zip(&t.actions) {
        if a>=n || !t.mask[off+a] || !t.mask[off..off+n].iter().any(|v|*v) {
            return Err("illegal recorded action".into());
        }
        off+=n;
    }
    Ok(())
}
#[derive(Clone, Debug)]
pub struct Adam { pub m: Vec<f32>, pub v: Vec<f32>, pub step: u64 }
impl Adam {
    pub fn new(n: usize) -> Self { Self { m: vec![0.0;n], v: vec![0.0;n], step: 0 } }
    pub fn update(&mut self, w: &mut [f32], grad: &[f32], lr: f32, clip: f32) -> Result<f32,String> {
        if w.len()!=grad.len() || self.m.len()!=w.len() || self.v.len()!=w.len() { return Err("Adam shape mismatch".into()); }
        if !grad.iter().all(|g| g.is_finite()) { return Err("non-finite gradient; update rejected".into()); }
        let norm = grad.iter().map(|&g| (g as f64).powi(2)).sum::<f64>().sqrt() as f32;
        if !norm.is_finite() || !lr.is_finite() || lr<=0.0 || !clip.is_finite() || clip<=0.0 { return Err("invalid optimizer scale or overflowed gradient norm".into()); }
        let scale = (clip / norm.max(1e-12)).min(1.0); self.step += 1;
        let bc1 = 1.0-0.9_f64.powf(self.step as f64);
        let bc2 = 1.0-0.999_f64.powf(self.step as f64);
        for i in 0..w.len() {
            let g = grad[i] * scale;
            self.m[i] = 0.9*self.m[i]+0.1*g; self.v[i] = 0.999*self.v[i]+0.001*g*g;
            let update = (self.m[i] as f64 / bc1) / ((self.v[i] as f64 / bc2).sqrt()+1e-8);
            w[i] -= lr * update as f32;
        }
        if !w.iter().all(|x| x.is_finite()) { return Err("non-finite weights".into()); }
        Ok(norm)
    }
}
#[derive(Clone, Default, Debug)]
pub struct Report {
    pub samples: usize, pub minibatches: usize, pub mean_reward: f32,
    pub policy_loss: f32, pub value_loss: f32, pub entropy: f32,
    pub kl: f32, pub clip_fraction: f32, pub grad_norm: f32,
}

/// Tail bootstraps from next_value. terminal kills both bootstrap and GAE carry.
/// Every call is ONE bot's contiguous rollout, never interleaved bot trajectories.
pub fn advantages(steps: Vec<Transition>, p: &Params) -> Vec<Sample> {
    let mut out = Vec::with_capacity(steps.len()); let mut carry = 0.0;
    for t in steps.into_iter().rev() {
        let live = if t.terminal { 0.0 } else { 1.0 };
        let delta = t.reward + p.gamma * t.next_value * live - t.value;
        carry = delta + p.gamma * p.lambda * live * carry;
        let target = t.value + carry;
        out.push(Sample { t, advantage: carry, target });
    }
    out.reverse(); out
}
pub fn prepare(rollouts: Vec<Rollout>, m: &Model, p: &Params) -> Result<Vec<Sample>,String> {
    p.validate()?;
    if !m.validate(){return Err("invalid behavior model".into());}
    let mut out = vec![];
    for r in rollouts {
        if r.version != m.version { return Err("off-policy rollout rejected".into()); }
        for t in &r.steps {
            validate_transition(t,m)?;
            // A matching version number is necessary but not sufficient. Prove
            // the recorded behavior likelihood and critic match THIS snapshot.
            let f=m.forward(&t.obs,&t.mask);
            let lp=log_probability(&f.probs,&m.heads,&t.actions);
            let value=*f.out.last().unwrap();
            let agrees=|a:f32,b:f32| (a-b).abs()<=2e-4*(1.0+a.abs().max(b.abs()));
            if !agrees(lp,t.old_logp)||!agrees(value,t.value) {
                return Err("behavior policy likelihood/value mismatch".into());
            }
        }
        out.extend(advantages(r.steps,p));
    }
    if out.is_empty() { return Err("empty batch".into()); }
    if !out.iter().all(|s|s.advantage.is_finite()&&s.target.is_finite()) {return Err("non-finite GAE/target".into());}
    let mean = out.iter().map(|s| s.advantage as f64).sum::<f64>() / out.len() as f64;
    let var = out.iter().map(|s| (s.advantage as f64-mean).powi(2)).sum::<f64>() / out.len() as f64;
    let sd = (var + 1e-8).sqrt();
    for s in &mut out { s.advantage=((s.advantage as f64-mean)/sd) as f32; }
    Ok(out)
}
/// Exact derivative of the clipped PPO objective with respect to log pi(a|s).
/// Negative advantages clip on the LOWER ratio boundary; positive on the upper.
pub fn clipped_surrogate(adv: f32, ratio: f32, clip: f32) -> (f32, f32) {
    let raw = ratio * adv; let capped=ratio.clamp(1.0-clip,1.0+clip)*adv;
    let active = raw <= capped;
    (-raw.min(capped), if active { -adv*ratio } else { 0.0 })
}
pub fn train(model: &mut Model, adam: &mut Adam, rng: &mut Rng, batch: &[Sample], p: &Params) -> Result<Report,String> {
    p.validate()?;
    if !model.validate(){return Err("invalid update model".into());}
    if batch.is_empty(){return Err("empty update".into());}
    for s in batch {
        validate_transition(&s.t,model)?;
        if !s.advantage.is_finite()||!s.target.is_finite(){return Err("non-finite training target".into());}
    }
    let saved_model=model.clone(); let saved_adam=adam.clone(); let saved_rng=rng.clone();
    let result=train_inner(model,adam,rng,batch,p);
    if result.is_err() { *model=saved_model; *adam=saved_adam; *rng=saved_rng; }
    result
}
fn train_inner(model: &mut Model, adam: &mut Adam, rng: &mut Rng, batch: &[Sample], p: &Params) -> Result<Report,String> {
    let mut report=Report { samples: batch.len(), mean_reward: batch.iter().map(|s| s.t.reward).sum::<f32>()/batch.len() as f32, ..Report::default() };
    let mut order: Vec<usize>=(0..batch.len()).collect(); let mut count=0usize;
    'epochs: for _ in 0..p.epochs {
        rng.shuffle(&mut order);
        for ids in order.chunks(p.minibatch) {
            let mut grad=vec![0.0;model.weights.len()]; let mut mb_kl=0.0;
            for &id in ids {
                let s=&batch[id]; let f=model.forward(&s.t.obs,&s.t.mask);
                let lp=log_probability(&f.probs,&model.heads,&s.t.actions);
                let diff=(lp-s.t.old_logp).clamp(-20.0,20.0); let ratio=diff.exp();
                let kl=(ratio-1.0)-diff; mb_kl+=kl;
                let (actor_loss,dlp)=clipped_surrogate(s.advantage,ratio,p.clip);
                let mut dout=vec![0.0;f.out.len()]; let mut off=0; let mut entropy=0.0;
                for (&n,&action) in model.heads.iter().zip(&s.t.actions) {
                    let ps=&f.probs[off..off+n];
                    let h=-ps.iter().filter(|&&v| v>0.0).map(|v| v*v.ln()).sum::<f32>(); entropy+=h;
                    for k in 0..n {
                        // d(-entropy)/dz_j = p_j (log p_j + H); illegal p=0 contributes 0.
                        dout[off+k]=dlp*((if k==action {1.0} else {0.0})-ps[k]);
                        if ps[k]>0.0 { dout[off+k]+=p.entropy*ps[k]*(ps[k].ln()+h); }
                    }
                    off+=n;
                }
                let err=f.out[off]-s.target; dout[off]=p.value_coef*err;
                model.backward(&s.t.obs,&f,&dout,&mut grad);
                report.policy_loss+=actor_loss; report.value_loss+=0.5*err*err;
                report.entropy+=entropy; report.kl+=kl;
                report.clip_fraction+=if (ratio-1.0).abs()>p.clip {1.0} else {0.0}; count+=1;
            }
            // Do not apply a further optimizer step after divergence is observed.
            if mb_kl/ids.len() as f32>p.target_kl && report.minibatches>0 { break 'epochs; }
            for g in &mut grad { *g/=ids.len() as f32; }
            report.grad_norm+=adam.update(&mut model.weights,&grad,p.lr,p.grad_clip)?;
            report.minibatches+=1;
        }
    }
    if !model.validate() { return Err("model validation failed; entire PPO update rolled back".into()); }
    if report.minibatches==0 { return Err("no optimizer steps".into()); }
    let d=count.max(1) as f32;
    report.policy_loss/=d; report.value_loss/=d; report.entropy/=d; report.kl/=d; report.clip_fraction/=d;
    report.grad_norm/=report.minibatches as f32; model.version+=1;
    Ok(report)
}
#[cfg(test)] mod tests {
    use super::*;
    fn step(r:f32, value:f32, next:f32, terminal:bool)->Transition {
        Transition { obs:vec![0.0],mask:vec![true,true],actions:vec![0],old_logp:-2.0_f32.ln(),value,next_value:next,reward:r,terminal }
    }
    #[test] fn terminal_does_not_bootstrap() {
        let p=Params{gamma:0.9,lambda:1.0,..Params::default()};
        let a=advantages(vec![step(1.0,0.5,999.0,true)],&p);
        assert!((a[0].target-1.0).abs()<1e-6);
    }
    #[test] fn truncation_bootstraps_and_boundaries_cut_carry() {
        let p=Params{gamma:0.9,lambda:1.0,..Params::default()};
        let a=advantages(vec![step(1.0,0.0,0.0,false),step(2.0,0.0,10.0,false)],&p);
        assert!((a[0].target-10.9).abs()<1e-5); assert!((a[1].target-11.0).abs()<1e-5);
        let a=advantages(vec![step(1.0,0.0,0.0,true),step(20.0,0.0,10.0,false)],&p);
        assert_eq!(a[0].target,1.0);
    }
    #[test] fn both_ppo_clip_directions() {
        assert_eq!(clipped_surrogate(1.0,1.4,0.2).1,0.0);
        assert!(clipped_surrogate(-1.0,1.4,0.2).1>0.0);
        assert_eq!(clipped_surrogate(-1.0,0.6,0.2).1,0.0);
        assert!(clipped_surrogate(1.0,0.6,0.2).1<0.0);
    }
    #[test] fn stale_policy_is_rejected() {
        let m=Model::new(1,4,vec![2],1);
        assert!(prepare(vec![Rollout{version:1,steps:vec![step(1.0,0.0,0.0,true)]}],&m,&Params::default()).is_err());
    }
    #[test] fn invalid_gradient_is_not_applied() {
        let mut a=Adam::new(2); let mut w=vec![1.0,2.0];
        assert!(a.update(&mut w,&[f32::NAN,0.0],0.1,0.5).is_err()); assert_eq!(w,[1.0,2.0]); assert_eq!(a.step,0);
    }
    #[test] fn learns_a_contextual_bandit_from_reward_only() {
        let mut m=Model::new(2,12,vec![2],31); let mut adam=Adam::new(m.weights.len()); let mut rng=Rng(123);
        let p=Params{lr:0.003,entropy:0.001,epochs:3,minibatch:32,..Params::default()};
        for _ in 0..45 {
            let mut steps=vec![];
            for k in 0..128 {
                let obs=if k%2==0 {vec![1.0,0.0]} else {vec![0.0,1.0]};
                let d=m.decide(&obs,&[true,true],&mut rng,false);
                steps.push(Transition{obs,mask:vec![true,true],actions:d.actions.clone(),old_logp:d.logp,value:d.value,next_value:0.0,reward:if d.actions[0]==k%2 {1.0} else {-1.0},terminal:true});
            }
            let batch=prepare(vec![Rollout{version:m.version,steps}],&m,&p).unwrap();
            train(&mut m,&mut adam,&mut rng,&batch,&p).unwrap();
        }
        assert!(m.forward(&[1.0,0.0],&[true,true]).probs[0]>0.90);
        assert!(m.forward(&[0.0,1.0],&[true,true]).probs[1]>0.90);
    }

    #[test] fn behavior_snapshot_is_checked_not_just_version() {
        let m=Model::new(1,4,vec![2],9); let mut rng=Rng(14);
        let d=m.decide(&[0.5],&[true,true],&mut rng,false);
        let mut t=Transition{obs:vec![0.5],mask:vec![true,true],actions:d.actions,
            old_logp:d.logp,value:d.value,next_value:0.0,reward:1.0,terminal:true};
        let check=|t:Transition|prepare(vec![Rollout{version:m.version,steps:vec![t]}],&m,&Params::default());
        assert!(check(t.clone()).is_ok());
        t.old_logp+=0.1;assert!(check(t.clone()).is_err());
        t.old_logp=d.logp;t.value+=1.0;assert!(check(t).is_err());
    }
    #[test] fn invalid_parameters_fail_without_mutation() {
        let mut m=Model::new(1,4,vec![2],1);let before=m.weights.clone();
        let mut adam=Adam::new(m.weights.len());let mut rng=Rng(7);
        let p=Params{entropy:f32::NAN,..Params::default()};
        assert!(train(&mut m,&mut adam,&mut rng,&[],&p).is_err());
        assert_eq!(m.weights,before);assert_eq!(adam.step,0);assert_eq!(rng.0,7);
    }
}
