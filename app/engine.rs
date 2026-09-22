use std::{fs,path::Path,sync::{Arc,Mutex,OnceLock,RwLock,atomic::{AtomicBool,Ordering},mpsc::{self,Receiver,SyncSender}},thread,time::{Duration,Instant}};
use crate::{learning::{checkpoint::Checkpoint,network::Model,ppo::{self,Rollout},reward::RewardBook,curriculum::{Curriculum,Phase},checkpoint::atomic_write,OBS,HEADS},metrics::{self,AgentView,TrainingView,CsvLog},settings::Settings};

pub static STOP:AtomicBool=AtomicBool::new(false);
static RUNTIME:OnceLock<Arc<Runtime>>=OnceLock::new();
pub struct Runtime {
    pub cfg:Settings,pub policy:RwLock<Arc<Model>>,pub rewards:Mutex<RewardBook>,
    pub curriculum:Option<Mutex<Curriculum>>,pub academy_log:Mutex<()>,
    pub peers:Mutex<Vec<AgentView>>,pub training:Mutex<TrainingView>,pub tx:SyncSender<Rollout>,
}
pub fn runtime()->&'static Arc<Runtime>{RUNTIME.get().expect("runtime initialized before joining")}
fn same_schema(m:&Model)->bool{m.input==OBS&&m.hidden==64&&m.heads.as_slice()==HEADS.as_slice()}
pub fn initialize(cfg:Settings)->Result<(Arc<Runtime>,thread::JoinHandle<Result<(),String>>),String>{
    fs::create_dir_all(cfg.root.join("state")).map_err(|e|e.to_string())?;
    let context_path=cfg.root.join("state/environment.txt");
    let context=if cfg.curriculum {"botsclustersmc-academy-v1-campus8x16\n"}else{"wilderness-v2-angular-rate\n"};
    if context_path.exists() {
        if fs::read_to_string(&context_path).map_err(|e|e.to_string())?!=context {return Err("training environment/input semantics differ; use a separate experiment, never reinterpret existing weights".into());}
    } else {
        if cfg.root.join("state/policy.bcmc").exists() {return Err("legacy checkpoint uses instant camera actions; preserve the old installation and start academy.sh with new independent state".into());}
        atomic_write(&context_path,context.as_bytes(),0).map_err(|e|e.to_string())?;
    }
    let cp_path=cfg.root.join("state/policy.bcmc");let rp_path=cfg.root.join("state/rewards.bcmc");
    if cp_path.exists()!=rp_path.exists() && cfg.mode!="random"{return Err("policy/reward pair incomplete: restore a stopped full backup; refusing implicit reset".into());}
    let resumed=cp_path.exists() && cfg.mode!="random";
    let cp=if cfg.mode=="random"{Checkpoint::new(Model::new(OBS,64,HEADS.to_vec(),cfg.seed),cfg.seed^0x5123)}
        else if cp_path.exists(){Checkpoint::load(&cp_path).map_err(|e|e.to_string())?}
        else if cfg.mode=="eval"{return Err("evaluation requires an existing state/policy.bcmc checkpoint".into());}
        else{Checkpoint::new(Model::new(OBS,64,HEADS.to_vec(),cfg.seed),cfg.seed^0x5123)};
    if !same_schema(&cp.model){return Err("policy observation/action/network schema differs from this build".into());}
    let curriculum_path=cfg.root.join("state/academy.bcmc");
    let curriculum=if cfg.curriculum {
        let mut c=if resumed {
            let bytes=fs::read(&curriculum_path).map_err(|e|format!("academy state missing: {e}"))?;
            Curriculum::decode(&bytes,cfg.bots,cp.model.version,cp.model.fingerprint()).map_err(|e|e.to_string())?
        }else{Curriculum::new(cfg.bots)};
        if cfg.mode=="eval"{c.phase=Phase::Exam;c.generation+=1;}
        Some(Mutex::new(c))
    }else{None};
    let mut rewards=if rp_path.exists()&&cfg.mode!="random"{RewardBook::load(&rp_path).map_err(|e|e.to_string())?}else{RewardBook::new(cfg.bots)};
    rewards.resize(cfg.bots);
    // Persist the initial pair before any agent can change the world.
    if cfg.mode=="train"{rewards.save(&rp_path).map_err(|e|e.to_string())?;cp.save(&cp_path).map_err(|e|e.to_string())?;}
    let (tx,rx)=mpsc::sync_channel(32);
    let peers=(0..cfg.bots).map(|i|AgentView{id:i,name:format!("{}{:02}",cfg.prefix,i),..Default::default()}).collect();
    let rt=Arc::new(Runtime{curriculum,academy_log:Mutex::new(()),policy:RwLock::new(Arc::new(cp.model.clone())),training:Mutex::new(TrainingView{version:cp.model.version,samples:cp.samples,
        initial_version:cp.model.version,initial_samples:cp.samples,
        optimizer_steps:cp.adam.step,initial_optimizer_steps:cp.adam.step,
        fingerprint:cp.model.fingerprint(),initial_fingerprint:cp.model.fingerprint(),
        resumed,..Default::default()}),cfg,rewards:Mutex::new(rewards),peers:Mutex::new(peers),tx});
    if rt.cfg.mode=="train"{save(&rt,&cp)?;}
    RUNTIME.set(rt.clone()).map_err(|_|"runtime initialized twice")?;
    let worker_rt=rt.clone();let handle=thread::Builder::new().name("botsclustersmc-ppo".into()).spawn(move||{
        let result=std::panic::catch_unwind(std::panic::AssertUnwindSafe(||learn(&worker_rt,rx,cp))).unwrap_or_else(|_|Err("learner thread panicked; no silent continuation".into()));
        if let Err(ref error)=result{worker_rt.training.lock().unwrap().error=error.clone();eprintln!("TRAINER FATAL: {error}");STOP.store(true,Ordering::Relaxed);let _=publish_status(&worker_rt);}
        result
    }).map_err(|e|e.to_string())?;
    Ok((rt,handle))
}
pub fn send(rt:&Runtime,r:Rollout){if r.steps.is_empty()||rt.cfg.mode!="train"{return;}if rt.tx.try_send(r).is_err(){rt.training.lock().unwrap().dropped+=1;}}
fn publish_status(rt:&Runtime)->Result<(),String>{
    let t=rt.training.lock().unwrap().clone();let a=rt.peers.lock().unwrap().clone();let frontier=rt.rewards.lock().unwrap().frontier.len();
    metrics::write_status(&rt.cfg.root,&rt.cfg.mode,&rt.cfg.run_id,&t,&a,frontier).map_err(|e|e.to_string())?;
    crate::academy::status(rt)
}
fn save(rt:&Runtime,cp:&Checkpoint)->Result<(),String>{
    if rt.cfg.mode!="train"{return Ok(());}
    // Ledger first: in a partial checkpoint failure, do not grant earned rewards
    // again. World + agent state are NOT a distributed atomic transaction.
    rt.rewards.lock().unwrap().save(&rt.cfg.root.join("state/rewards.bcmc")).map_err(|e|e.to_string())?;
    cp.save(&rt.cfg.root.join("state/policy.bcmc")).map_err(|e|e.to_string())?;
    if let Some(c)=&rt.curriculum {
        let data=c.lock().unwrap().encode(cp.model.version,cp.model.fingerprint());
        atomic_write(&rt.cfg.root.join("state/academy.bcmc"),&data,3).map_err(|e|e.to_string())?;
    }
    Ok(())
}
fn learn(rt:&Runtime,rx:Receiver<Rollout>,mut cp:Checkpoint)->Result<(),String>{
    let p=ppo::Params::default();let mut batches=Vec::new();let mut n=0usize;let mut batch_began:Option<Instant>=None;let mut last_status=Instant::now();let mut last_save=Instant::now();let began=Instant::now();let mut csv=CsvLog::new(&rt.cfg.root).map_err(|e|e.to_string())?;
    publish_status(rt)?;
    while !STOP.load(Ordering::Relaxed){
        if rt.cfg.root.join(".runtime/stop").exists() || (rt.cfg.seconds>0&&began.elapsed().as_secs()>=rt.cfg.seconds){STOP.store(true,Ordering::Relaxed);break;}
        let mut examining=false;
        let mut phase_change=None;
        if let Some(c)=&rt.curriculum {
            let mut c=c.lock().unwrap();
            if rt.cfg.mode=="train" {phase_change=c.advance();}
            examining=c.phase==Phase::Exam;
        }
        if let Some(event)=phase_change {
            eprintln!("ACADEMY {event}; policy_version={}",cp.model.version);
            batches.clear();n=0;batch_began=None;
            while rx.try_recv().is_ok(){rt.training.lock().unwrap().dropped+=1;}
            save(rt,&cp)?;publish_status(rt)?;
        }
        if let Ok(r)=rx.recv_timeout(Duration::from_millis(100)){
            if examining || r.version!=cp.model.version{rt.training.lock().unwrap().dropped+=1;}else{if batch_began.is_none(){batch_began=Some(Instant::now());}n+=r.steps.len();batches.push(r);}
        }
        if rt.cfg.mode=="train"&&!examining&&(n>=rt.cfg.batch||(n>=64&&batch_began.is_some_and(|t|t.elapsed().as_secs()>=20))){
            let before=Instant::now();let samples=ppo::prepare(std::mem::take(&mut batches),&cp.model,&p)?;
            let report=ppo::train(&mut cp.model,&mut cp.adam,&mut cp.rng,&samples,&p)?;
            cp.samples+=report.samples as u64;*rt.policy.write().unwrap()=Arc::new(cp.model.clone());
            {let mut t=rt.training.lock().unwrap();t.version=cp.model.version;t.samples=cp.samples;t.optimizer_steps=cp.adam.step;t.fingerprint=cp.model.fingerprint();t.report=report;t.last_update=metrics::now();t.update_ms=before.elapsed().as_millis();csv.append(&t).map_err(|e|e.to_string())?;eprintln!("PPO version={} samples={} reward={:.5} KL={:.5} update_ms={}",t.version,t.samples,t.report.mean_reward,t.report.kl,t.update_ms);}
            save(rt,&cp)?;last_save=Instant::now();batch_began=None;n=0;
        }
        if last_save.elapsed().as_secs()>=60 {save(rt,&cp)?;last_save=Instant::now();}
        if last_status.elapsed().as_secs()>=2 {publish_status(rt)?;last_status=Instant::now();}
    }
    // In-flight fragments are deliberately discarded rather than inventing a
    // final reward/state or mixing a stale behavior policy into a new update.
    save(rt,&cp)?;publish_status(rt)?;
    let error=rt.training.lock().unwrap().error.clone();
    if error.is_empty(){Ok(())}else{Err(error)}
}
#[cfg(unix)] extern "C" fn signal_handler(_:i32){STOP.store(true,Ordering::Relaxed);}
pub fn signals(){#[cfg(unix)] unsafe{unsafe extern "C"{fn signal(sig:i32,handler:usize)->usize;}signal(2,signal_handler as *const () as usize);signal(15,signal_handler as *const () as usize);}}
pub fn stop_requested()->bool{STOP.load(Ordering::Relaxed)}
pub fn request_stop(){STOP.store(true,Ordering::Relaxed);}
#[allow(dead_code)]pub fn stopped_file(root:&Path)->bool{root.join(".runtime/stop").exists()}

/// Handler futures are spawned by Azalea, so returning an error alone does not
/// stop the swarm. Explicitly fail the entire run instead of silently losing an agent.
pub fn fail(error: &str) {
    STOP.store(true, Ordering::Relaxed);
    eprintln!("AGENT FATAL: {error}");
    if let Ok(mut training) = runtime().training.lock() {
        if training.error.is_empty() { training.error=error.to_owned(); }
    }
}
