"""One-shot, reviewable source migration. Development branch only; not a runtime dependency."""
from pathlib import Path
import subprocess
import re

BASE = '720d989b3999d07437185fc407609d1c90d9fbc9'
def original(path):
    return subprocess.check_output(['git', 'show', f'{BASE}:{path}'], text=True)
def write(path, text):
    p = Path(path); p.parent.mkdir(parents=True, exist_ok=True); p.write_text(text)
def replace(text, before, after):
    assert before in text, before
    return text.replace(before, after)

# The runtime owns one canonical implementation. The experimental crate becomes
# a compatibility/test frontend to these exact same modules, not a second learner.
paths = subprocess.check_output(['git','ls-tree','-r','--name-only',BASE,'experimental/rl-next/src'],text=True).splitlines()
for path in paths:
    rel = Path(path).relative_to('experimental/rl-next/src')
    if rel.name == 'main.rs': continue
    dest = Path('learning/src/next') / ('mod.rs' if rel.name == 'lib.rs' else rel)
    text = original(path)
    depth = len(rel.parts) if rel.name != 'mod.rs' else len(rel.parts)-1
    if rel.name == 'lib.rs': depth = 0
    prefix = 'super::' * depth if depth else 'self::'
    text = text.replace('crate::', prefix)
    text = text.replace('(1..=32)', '(1..=64)').replace('one to 32 actors','one to 64 actors')
    text = text.replace('baseline.session.actor>=32','baseline.session.actor>=64')
    text = text.replace('bytes.len()>64*1024','bytes.len()>128*1024')
    text = text.replace('b"BCMCLP02"','b"BCMCLP03"')
    if rel.name == 'lib.rs':
        text = text.replace('//! Experimental components, NOT wired into the v0.3.1 Minecraft executable.\n//! See INTEGRATION.md before attempting to use these in a live learner.','//! Canonical RL-next mechanisms used by the actual Minecraft runtime.')
    if rel.as_posix() == 'curriculum/mod.rs':
        text = text.replace('    pub fn frontier(&self)', '''    pub fn training_counts(&self,id:usize)->Result<(u64,u64)>{
        let a=self.actors.get(id).ok_or("unknown actor")?;
        Ok((a.trained_since_exam,a.full_since_exam))
    }
    pub fn has_pending(&self)->bool{self.actors.iter().any(|a|a.pending.is_some())}
    /// Explicit evaluation mode only; it grants no training qualification.
    pub fn begin_evaluation(&mut self,policy:PolicyId)->Result<()> {
        if self.has_pending(){return Err("evaluation requires an episode boundary");}
        self.generation=self.generation.checked_add(1).ok_or("generation exhausted")?;
        self.phase=Phase::Exam{frozen:policy};
        for a in &mut self.actors{a.exam_cursor=0;a.scores.fill(ExamScore::default());}
        Ok(())
    }
    pub fn frontier(&self)''')
        text += '''
#[cfg(test)] mod population_tests {
 use super::*;
 #[test] fn sixty_four_actor_state_roundtrips_without_a_64k_limit(){
  let p=PolicyId{version:0,signature:1};let mut c=Curriculum::new(9,64,11).unwrap();
  for id in 0..64{let l=c.issue(id,p).unwrap().unwrap();c.record(&l,p,false).unwrap();}
  let bytes=c.checkpoint(p).unwrap();assert!(bytes.len()>65536);
  let r=Curriculum::from_checkpoint(&bytes,p,12).unwrap();assert_eq!(r.bots(),64);
  assert_eq!(r.stats(63).unwrap()[0].attempts,1);assert!(Curriculum::new(1,65,2).is_err());
 }
}
'''
    write(dest,text)
write('experimental/rl-next/src/lib.rs','//! Test/benchmark frontend to the same RL implementation used in Minecraft.\n#[path="../../../learning/src/next/mod.rs"] mod integrated;\npub use integrated::*;\n')
for path in paths:
    if Path(path).name not in ('lib.rs','main.rs'): Path(path).unlink(missing_ok=True)

# Explicit semantic generation: old models/worlds are never silently reinterpreted.
for folder in ['app','launcher','scripts','tests','bridge']:
    for p in Path(folder).rglob('*'):
        if p.is_file() and p.suffix in ('.rs','.sh','.java','.py','.yml'):
            text=p.read_text(); text=text.replace('BCMCLAB2','BCMCLAB3').replace('botsclustersmc-academy-v1','botsclustersmc-academy-v2').replace('bcmc_academy_v1','bcmc_academy_v2')
            p.write_text(text)
for name in ['academy.sh','start.sh','stop.sh','status.sh','console.sh','smoke.sh']:
    p=Path(name);text=p.read_text().replace('botsclustersmc-academy-v1','botsclustersmc-academy-v2');p.write_text(text)

s=original('learning/src/lib.rs').replace('FRAME: usize = 640','FRAME: usize = 704').replace('SCHEMA: u32 = 1','SCHEMA: u32 = 2')
write('learning/src/lib.rs',s+'\npub mod next;\npub mod academy;\n')
s=original('learning/src/curriculum.rs').replace('id < 32 && stage','id < 64 && stage')
write('learning/src/curriculum.rs',s)  # legacy foundation geometry/tests; coordinator is no longer used by the runtime
s=original('scripts/build.sh').replace('cp learning/src/*.rs "$example/core/"','cp -a learning/src/. "$example/core/"')
write('scripts/build.sh',s)
s=Path('app/settings.rs').read_text().replace('number("BOTS",32,1,32)','number("BOTS",64,1,64)')
write('app/settings.rs',s)
s=Path('launcher/config.rs').read_text().replace('number("BOTS",32,1,32)','number("BOTS",64,1,64)').replace('number("VIEW_DISTANCE",3,3,8)','number("VIEW_DISTANCE",10,3,16)').replace('max-players=48','max-players=96').replace('entity-broadcast-range-percentage=75','entity-broadcast-range-percentage=100')
write('launcher/config.rs',s)
for name in ['launcher/main.rs','launcher/campus.rs','launcher/academy_audit.rs']:
    s=Path(name).read_text().replace('0..32','0..64').replace('(1..=32)','(1..=64)')
    if name.endswith('academy_audit.rs'):
        s=s.replace('curriculum::Curriculum','next::curriculum::{Curriculum,PolicyId}').replace('stage>=6','stage>=18')
        s=s.replace('Curriculum::decode(&fs::read(root.join("state/academy.bcmc")).map_err(|e|e.to_string())?,bots,cp.model.version,cp.model.fingerprint())','Curriculum::from_checkpoint(&fs::read(root.join("state/academy.bcmc")).map_err(|e|e.to_string())?,PolicyId{version:cp.model.version,signature:cp.model.fingerprint()},0)')
        s=s.replace('course.stage as u64','course.frontier() as u64')
        s=s.replace('    if course.frontier()', '    if course.bots()!=bots{return Err("curriculum population mismatch".into());}\n    if course.frontier()')
    write(name,s)
for name in ['academy.sh','scripts/runtime-config.sh','bridge/src/org/botsclustersmc/lab/EnvironmentConfig.java']:
    s=Path(name).read_text().replace('<= 32','<= 64').replace('1..32','1..64').replace('bots>32','bots>64')
    if name=='academy.sh':
        s=s.replace('VIEW_DISTANCE SIMULATION_DISTANCE','VIEW_DISTANCE BOT_VIEW_DISTANCE OBSERVER_VIEW_DISTANCE OBSERVER_TOUR_SECONDS SIMULATION_DISTANCE')
    write(name,s)
s=Path('smoke.sh').read_text().replace('^([1-9]|[12][0-9]|3[0-2])$','^([1-9]|[1-5][0-9]|6[0-4])$').replace('1..32','1..64')
write('smoke.sh',s)
s=original('.env.example').replace('BOTS=32','BOTS=64').replace('VIEW_DISTANCE=3','VIEW_DISTANCE=10').replace('# BATCH_SAMPLES=2048','# BATCH_SAMPLES=4096')
s+='\n# Rendering is distinct from simulation. Bots receive a small local view.\nBOT_VIEW_DISTANCE=3\nOBSERVER_VIEW_DISTANCE=10\nOBSERVER_TOUR_SECONDS=15\n'
write('.env.example',s)
write('VERSION','0.5.0\n')
s=Path('tests/package_checks.py').read_text().replace('bots=32','bots=64').replace('test_default_32','test_default_64').replace("'32 0.0.0.0 true'","'64 0.0.0.0 true'").replace('/academy true 32 0.0.0.0','/academy true 64 0.0.0.0')
write('tests/package_checks.py',s)
s=Path('tests/recovery_checks.py').read_text().replace("'bcmc 32 25565 0.0.0.0'","'bcmc 64 25565 0.0.0.0'")
write('tests/recovery_checks.py',s)
s=Path('bridge/EnvironmentConfigTest.java').read_text().replace(',33,',',65,')
write('bridge/EnvironmentConfigTest.java',s)

# Executed conditional action probabilities, not probabilities of ignored GUI slots.
s=original('learning/src/network.rs')
pos=s.index('    pub fn decide('); end=s.index('\n}\nfn dot',pos)
s=s[:pos]+'''    pub fn distribution(&self,f:&Forward,mask:&[bool])->super::next::Result<super::next::math::Distribution>{
        use super::next::math::{Distribution,Gate};
        let mut off=0;let mut logits=Vec::new();let mut masks=Vec::new();
        for &n in &self.heads{logits.push(f.out[off..off+n].iter().map(|x|*x as f64).collect());masks.push(mask[off..off+n].to_vec());off+=n;}
        let gate=(self.heads.as_slice()==super::HEADS).then_some(Gate{parent:6,child:7,active_bits:0b1110,neutral:0});
        Distribution::new(&logits,&masks,gate)
    }
    pub fn decide(&self,x:&[f32],mask:&[bool],rng:&mut Rng,greedy:bool)->Decision{
        let f=self.forward(x,mask);let d=self.distribution(&f,mask).expect("validated policy distribution");
        let mut random=super::next::Rng(rng.0);let a=d.sample(&mut random,greedy).expect("valid sampled action");rng.0=random.0;
        Decision{actions:a.actions,logp:a.log_probability as f32,value:*f.out.last().unwrap()}
    }'''+s[end:]
s=s.replace('for (&n, &a) in heads.iter().zip(actions) { assert!(a < n); logp += p[offset+a].max(1e-30).ln(); offset += n; }','for (h,(&n, &a)) in heads.iter().zip(actions).enumerate() { assert!(a < n); if !(heads==super::HEADS && h==7 && !matches!(actions[6],1..=3)){logp += p[offset+a].max(1e-30).ln();} offset += n; }')
write('learning/src/network.rs',s)
s=original('learning/src/ppo.rs')
s=s.replace('pub reward: f32, pub terminal: bool,','pub reward: f32, pub terminal: bool, pub ticks:u32,')
s=s.replace('|| !t.obs.iter()', '|| t.ticks==0 || t.ticks>1_000_000\n        || !t.obs.iter()')
s=s.replace('    let mut off=0;\n    for (&n,&a) in m.heads.iter().zip(&t.actions) {','    let mut off=0;\n    for (h,(&n,&a)) in m.heads.iter().zip(&t.actions).enumerate() {\n        if m.heads.as_slice()==super::HEADS && h==7 && !matches!(t.actions[6],1..=3){if a!=0{return Err("noncanonical inactive slot".into());}off+=n;continue;}')
a=s.index('pub fn advantages('); b=s.index('\npub fn prepare',a)
s=s[:a]+'''pub fn advantages(steps:Vec<Transition>,p:&Params)->Vec<Sample>{
    use super::next::math::{gae,TimedValue,Boundary};
    let timed=steps.iter().map(|t|TimedValue{reward:t.reward as f64,value:t.value as f64,next_value:t.next_value as f64,ticks:t.ticks,
        boundary:if t.terminal{Boundary::Terminated}else{Boundary::Continuing}}).collect::<Vec<_>>();
    let values=gae(&timed,p.gamma as f64,p.lambda as f64,4).expect("validated timed transitions");
    steps.into_iter().zip(values).map(|(t,a)|Sample{t,advantage:a.advantage as f32,target:a.target as f32}).collect()
}'''+s[b:]
s=s.replace('log_probability(&f.probs,&m.heads,&t.actions)','m.distribution(&f,&t.mask)?.log_probability(&t.actions)? as f32')
s=s.replace('log_probability(&f.probs,&model.heads,&s.t.actions)','model.distribution(&f,&s.t.mask)?.log_probability(&s.t.actions)? as f32')
a=s.index('    let saved_model=model.clone();');b=s.index('\nfn train_inner',a)
s=s[:a]+'''    use super::next::math::{guarded_update,TrustConfig,sampled_kl};
    let mut state=(model.clone(),adam.clone(),rng.clone());let mut report=Report::default();
    let mut detailed_error=None;
    let guard=guarded_update(&mut state,TrustConfig{learning_rate:p.lr as f64,maximum_kl:p.target_kl as f64,retries:8,shrink:0.5},
      |s,lr|{let mut tuned=p.clone();tuned.lr=lr as f32;
        match train_inner(&mut s.0,&mut s.1,&mut s.2,batch,&tuned){Ok(r)=>{report=r;Ok(())},Err(e)=>{detailed_error=Some(e);Err("PPO update failed")}}},
      |s|{let mut old=Vec::with_capacity(batch.len());let mut new=Vec::with_capacity(batch.len());
        for item in batch{let f=s.0.forward(&item.t.obs,&item.t.mask);old.push(item.t.old_logp as f64);new.push(s.0.distribution(&f,&item.t.mask)?.log_probability(&item.t.actions)?);}
        sampled_kl(&old,&new) });
    let guard=guard.map_err(|e|detailed_error.unwrap_or_else(||e.to_string()))?;
    report.kl=guard.final_kl as f32;
    *model=state.0;*adam=state.1;*rng=state.2;Ok(report)
}
'''+s[b:]
a=s.index('                let mut dout=');b=s.index('                let err=',a)
s=s[:a]+'''                let distribution=model.distribution(&f,&s.t.mask)?;
                let score=distribution.score_gradient(&s.t.actions,dlp as f64)?;
                let (entropy,entropy_gradient)=distribution.entropy(true);
                let mut dout=vec![0.0;f.out.len()];let mut off=0;
                for (head,&n) in model.heads.iter().enumerate(){for k in 0..n{
                    dout[off+k]=(score[head][k]-p.entropy as f64*entropy_gradient[head][k]) as f32;
                }off+=n;}
                let entropy=entropy as f32;
'''+s[b:]
# Add a time duration to test transitions. Production transitions are in the new actor.
s=re.sub(r'Transition\s*\{(?!\s*pub obs)', 'Transition{ticks:4,',s)
write('learning/src/ppo.rs',s)
s=Path('app/sensor.rs').read_text().replace('other.id as f32/31.0','other.id as f32/63.0').replace('features:&[f32;23]','features:&[f32;87]').replace('[617..640]','[617..704]')
write('app/sensor.rs',s)
s=Path('app/act.rs').read_text().replace('if slot<slot_count {match a[6]{','if slot<slot_count || a[6]==4 {match a[6]{')
write('app/act.rs',s)
print('Canonical RL modules, population, semantic-generation guards and timed conditional PPO prepared.')
