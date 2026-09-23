"""Reviewed, bounded source transformations on the isolated integration branch.
This maintenance program is not used during ordinary clone/build/start.
"""
from pathlib import Path

# A parent module cannot be imported anonymously. Resolve its named `next`
# child from the enclosing learning module; this also works in the test crate.
for path in Path('learning/src/next').rglob('*.rs'):
    text = path.read_text()
    text = text.replace('use super::{self as shared_root};', 'use super::super::next as shared_root;')
    text = text.replace('use super::super::{self as shared_root};', 'use super::super::super::next as shared_root;')
    path.write_text(text)
Path('experimental/rl-next/src/lib.rs').write_text('//! Standalone checks use the canonical native implementation.\n#[path = "../../../learning/src/next/mod.rs"]\nmod next;\npub use next::*;\n')

p = Path('learning/src/lib.rs')
s = p.read_text().replace('FRAME: usize = 640', 'FRAME: usize = 704').replace('SCHEMA: u32 = 1', 'SCHEMA: u32 = 2')
if 'pub mod bundle;' not in s:
    s += '\npub mod bundle;\n'
p.write_text(s)

p = Path('learning/src/network.rs')
s = p.read_text()
if 'pub distribution:' not in s:
    s = s.replace('use super::rng::Rng;', 'use super::rng::Rng;\nuse super::next::math::{Distribution,Gate};')
    s = s.replace('pub h1: Vec<f32>, pub h2: Vec<f32>, pub out: Vec<f32>, pub probs: Vec<f32>,', 'pub h1: Vec<f32>, pub h2: Vec<f32>, pub out: Vec<f32>, pub probs: Vec<f32>,\n    pub distribution: Distribution,')
    s = s.replace('self.heads.len() <= 32', 'self.heads.len() <= 16').replace('n <= 1024', 'n <= 128')
    old = 'let probs = masked_softmax(&out[..out.len() - 1], &self.heads, mask);\n        Forward { h1, h2, out, probs }'
    assert old in s
    s = s.replace(old, '''let mut logits=Vec::with_capacity(self.heads.len());
        let mut masks=Vec::with_capacity(self.heads.len());let mut offset=0;
        for &n in &self.heads {
            logits.push(out[offset..offset+n].iter().map(|&v|v as f64).collect());
            masks.push(mask[offset..offset+n].to_vec());offset+=n;
        }
        let gate=(self.heads.as_slice()==super::HEADS).then_some(Gate{parent:6,child:7,active_bits:0b1110,neutral:0});
        let distribution=Distribution::new(&logits,&masks,gate).expect("invalid neural categorical distribution");
        let probs=distribution.probabilities().iter().flatten().map(|&v|v as f32).collect();
        Forward { h1, h2, out, probs, distribution }''')
    start = s.index('    pub fn decide(')
    end = s.index('\n}\nfn dot', start)
    s = s[:start] + '''    pub fn decide(&self, x: &[f32], mask: &[bool], rng: &mut Rng, greedy: bool) -> Decision {
        let f=self.forward(x,mask);
        let mut shared=super::next::Rng(rng.0);
        let chosen=f.distribution.sample(&mut shared,greedy).expect("invalid policy distribution");
        rng.0=shared.0;
        Decision{actions:chosen.actions,logp:chosen.log_probability as f32,value:*f.out.last().unwrap()}
    }''' + s[end:]
    old='for (&n, &a) in heads.iter().zip(actions) { assert!(a < n); logp += p[offset+a].max(1e-30).ln(); offset += n; }'
    assert old in s
    s=s.replace(old,'''for (h,(&n,&a)) in heads.iter().zip(actions).enumerate() {
        assert!(a<n);
        if heads==super::HEADS && h==7 && !(1..=3).contains(&actions[6]) { assert_eq!(a,0); }
        else {logp+=p[offset+a].max(1e-30).ln();}
        offset+=n;
    }''')
    p.write_text(s)

p=Path('learning/src/ppo.rs')
s=p.read_text()
if 'pub elapsed_ticks:' not in s:
    s=s.replace('use super::{network::{Model, log_probability}, rng::Rng};', 'use super::{network::Model,rng::Rng,next::math};')
    s=s.replace('#[derive(Clone)]\npub struct Transition', '#[derive(Clone,Debug)]\npub struct Transition')
    s=s.replace('pub reward: f32, pub terminal: bool,','''pub reward: f32, pub terminal: bool,
    /// Duration and end tick belong to the authoritative server episode.
    pub elapsed_ticks:u32,pub truncated:bool,pub episode:u64,pub tick:u64,''')
    s=s.replace('fn validate_transition(', 'pub fn validate_transition(')
    s=s.replace('let mut off=0;\n    for (&n,&a)', 'if t.elapsed_ticks==0 || t.elapsed_ticks>1_000_000 || t.tick<u64::from(t.elapsed_ticks) || (t.terminal&&t.truncated) {return Err("invalid server transition duration/boundary".into());}\n    let mut off=0;\n    for (&n,&a)')
    s=s.replace('let scale = (clip / norm.max(1e-12)).min(1.0); self.step += 1;', 'let scale = (clip / norm.max(1e-12)).min(1.0); self.step = self.step.checked_add(1).ok_or("optimizer step overflow")?;')
    start=s.index('pub fn advantages(')
    end=s.index('\npub fn prepare(',start)
    s=s[:start]+'''pub fn advantages(steps: Vec<Transition>, p: &Params) -> Vec<Sample> {
    let timed:Vec<_>=steps.iter().map(|t|math::TimedValue{
        reward:t.reward as f64,value:t.value as f64,next_value:t.next_value as f64,ticks:t.elapsed_ticks,
        boundary:if t.terminal{math::Boundary::Terminated}else if t.truncated{math::Boundary::Truncated}else{math::Boundary::Continuing}
    }).collect();
    let targets=math::gae(&timed,p.gamma as f64,p.lambda as f64,4).expect("validated timed trajectory");
    steps.into_iter().zip(targets).map(|(t,a)|Sample{t,advantage:a.advantage as f32,target:a.target as f32}).collect()
}''' +s[end:]
    s=s.replace('log_probability(&f.probs,&m.heads,&t.actions)', 'f.distribution.log_probability(&t.actions).map_err(str::to_string)? as f32')
    start=s.index('    let mean = out.iter()')
    end=s.index('\n    Ok(out)',start)
    s=s[:start]+'''    let mut normalized:Vec<_>=out.iter().map(|s|s.advantage as f64).collect();
    math::normalize_advantages(&mut normalized).map_err(str::to_string)?;
    for (sample,advantage) in out.iter_mut().zip(normalized) {sample.advantage=advantage as f32;}''' +s[end:]
    start=s.index('    let saved_model=model.clone();')
    end=s.index('\n#[cfg(test)] mod tests',start)
    s=s[:start]+'''    // All mutated optimizer state, including shuffle RNG, is retried together.
    let mut state=(model.clone(),adam.clone(),rng.clone(),Report::default());
    let cfg=math::TrustConfig{learning_rate:p.lr as f64,maximum_kl:p.target_kl as f64,retries:8,shrink:0.5};
    let mut detail=None;
    let guard=math::guarded_update(&mut state,cfg,|state,lr|{
        let mut params=p.clone();params.lr=lr as f32;
        match train_inner(&mut state.0,&mut state.1,&mut state.2,batch,&params) {
            Ok(report)=>{state.3=report;Ok(())},
            Err(error)=>{detail=Some(error);Err("native optimizer update failed")}
        }
    },|state|{
        let old:Vec<_>=batch.iter().map(|s|s.t.old_logp as f64).collect();
        let mut new=Vec::with_capacity(batch.len());
        for s in batch {new.push(state.0.forward(&s.t.obs,&s.t.mask).distribution.log_probability(&s.t.actions)?);}
        math::sampled_kl(&old,&new)
    }).map_err(|e|detail.unwrap_or_else(||e.to_string()))?;
    state.0.version=model.version.checked_add(1).ok_or("policy version overflow")?;
    state.3.kl=guard.final_kl as f32;
    state.3.attempts=guard.attempts;
    state.3.learning_rate=guard.learning_rate as f32;
    *model=state.0;*adam=state.1;*rng=state.2;Ok(state.3)
}
fn train_inner(model:&mut Model,adam:&mut Adam,rng:&mut Rng,batch:&[Sample],p:&Params)->Result<Report,String>{
    let mut report=Report{samples:batch.len(),mean_reward:batch.iter().map(|s|s.t.reward as f64).sum::<f64>() as f32/batch.len() as f32,..Report::default()};
    let mut order:Vec<usize>=(0..batch.len()).collect();let mut count=0usize;
    for _ in 0..p.epochs {
        rng.shuffle(&mut order);
        for ids in order.chunks(p.minibatch) {
            let mut grad=vec![0.0;model.weights.len()];
            for &id in ids {
                let s=&batch[id];let f=model.forward(&s.t.obs,&s.t.mask);
                let lp=f.distribution.log_probability(&s.t.actions).map_err(str::to_string)?;
                let objective=math::clipped_objective(s.t.old_logp as f64,lp,s.advantage as f64,p.clip as f64).map_err(str::to_string)?;
                let score=f.distribution.score_gradient(&s.t.actions,objective.d_log_probability).map_err(str::to_string)?;
                let (entropy,entropy_grad)=f.distribution.entropy(true);
                let mut dout=Vec::with_capacity(f.out.len());
                for (gs,ge) in score.iter().zip(&entropy_grad) {
                    for (&a,&e) in gs.iter().zip(ge) {dout.push((a-p.entropy as f64*e) as f32);}
                }
                let err=*f.out.last().unwrap()-s.target;
                dout.push(p.value_coef*err);
                model.backward(&s.t.obs,&f,&dout,&mut grad);
                report.policy_loss+=objective.loss as f32;report.value_loss+=0.5*err*err;
                report.entropy+=entropy as f32;report.clip_fraction+=objective.clipped as u8 as f32;count+=1;
            }
            for g in &mut grad {*g/=ids.len() as f32;}
            report.grad_norm+=adam.update(&mut model.weights,&grad,p.lr,p.grad_clip)?;
            report.minibatches+=1;
        }
    }
    if !model.validate(){return Err("model validation failed".into());}
    let n=count.max(1) as f32;
    report.policy_loss/=n;report.value_loss/=n;report.entropy/=n;report.clip_fraction/=n;
    report.grad_norm/=report.minibatches.max(1) as f32;
    if ![report.mean_reward,report.policy_loss,report.value_loss,report.entropy,report.grad_norm].iter().all(|v|v.is_finite()){return Err("nonfinite optimizer report".into());}
    Ok(report)
}
''' +s[end:]
    s=s.replace('pub kl: f32, pub clip_fraction: f32, pub grad_norm: f32,', 'pub kl: f32, pub clip_fraction: f32, pub grad_norm: f32,\n    pub attempts:usize,pub learning_rate:f32,')
    # Existing synthetic fixtures use one nominal decision interval; they do not
    # claim chronological Minecraft streams. Runtime continuity is checked separately.
    s=s.replace('reward:r,terminal }','reward:r,terminal,elapsed_ticks:4,truncated:false,episode:0,tick:4 }')
    s=s.replace('terminal:true}', 'terminal:true,elapsed_ticks:4,truncated:false,episode:0,tick:4}')
    s=s.replace('terminal:true};', 'terminal:true,elapsed_ticks:4,truncated:false,episode:0,tick:4};')
    p.write_text(s)

# 64 actors serialize more than 64 KiB of curriculum state.
p=Path('learning/src/next/curriculum/checkpoint.rs')
s=p.read_text().replace('bytes.len()>64*1024','bytes.len()>256*1024')
p.write_text(s)

# Only explicit evaluation mode may force an exam; normal training still uses
# the shared readiness gate. Evaluation never promotes or writes training state.
p=Path('learning/src/next/curriculum/mod.rs')
s=p.read_text()
if 'pub fn begin_evaluation(' not in s:
    at=s.index('    pub fn issue(')
    s=s[:at]+'''    pub fn begin_evaluation(&mut self,policy:PolicyId)->Result<()> {
        if self.actors.iter().any(|a|a.pending.is_some()){return Err("evaluation has pending lessons");}
        self.generation=self.generation.checked_add(1).ok_or("generation exhausted")?;
        self.phase=Phase::Exam{frozen:policy};
        for a in &mut self.actors{a.exam_cursor=0;a.scores.fill(ExamScore::default());}
        Ok(())
    }
''' +s[at:]
    p.write_text(s)
print('Canonical math, native conditional PPO and atomic bundle sources prepared.')
