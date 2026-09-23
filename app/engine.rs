use std::{fs,path::Path,sync::{Arc,Mutex,OnceLock,atomic::{AtomicBool,Ordering}},thread,time::{Duration,Instant}};
use crate::{learning::{bundle::{Bundle,policy_id},checkpoint::{Checkpoint,atomic_write},control::{Coordinator,Mode},next::curriculum::Curriculum,network::Model,ppo,OBS,HEADS},metrics::{self,AgentView,TrainingView,CsvLog},settings::Settings};
pub static STOP:AtomicBool=AtomicBool::new(false);
static RUNTIME:OnceLock<Arc<Runtime>>=OnceLock::new();
pub struct Runtime{pub cfg:Settings,pub control:Mutex<Coordinator>,pub academy_log:Mutex<()>,pub peers:Mutex<Vec<AgentView>>,pub training:Mutex<TrainingView>}
pub fn runtime()->&'static Arc<Runtime>{RUNTIME.get().expect("runtime initialized before joining")}
fn same_schema(m:&Model)->bool{m.input==OBS&&m.hidden==64&&m.heads.as_slice()==HEADS.as_slice()}
pub fn initialize(cfg:Settings)->Result<(Arc<Runtime>,thread::JoinHandle<Result<(),String>>),String>{
    if !cfg.curriculum{return Err("this integrated runtime requires the owned Academy v2; use start.sh".into());}
    fs::create_dir_all(cfg.root.join("state")).map_err(|e|e.to_string())?;
    fs::create_dir_all(cfg.root.join("logs")).map_err(|e|e.to_string())?;
    let context_path=cfg.root.join("state/environment.txt");
    let context="botsclustersmc-academy-v2-18tasks-conditional-gui-timed-gae\n";
    if context_path.exists(){
        if fs::read_to_string(&context_path).map_err(|e|e.to_string())?!=context{return Err("incompatible observations/actions: preserve the old Academy and use a fresh academy-v2 directory".into());}
    }else{
        if cfg.root.join("state/policy.bcmc").exists()||cfg.root.join("state/academy.bcmc").exists(){return Err("legacy split checkpoints cannot be reinterpreted as integrated state".into());}
        if cfg.mode=="train"{atomic_write(&context_path,context.as_bytes(),0).map_err(|e|e.to_string())?;}
    }
    let path=cfg.root.join("state/training.bcmc");let mode=Mode::parse(&cfg.mode)?;
    let resumed=path.exists()&&mode!=Mode::Random;let run=metrics::hash(&cfg.run_id);
    let bundle=if resumed{Bundle::load(&path,cfg.bots,run).map_err(|e|e.to_string())?}
        else if mode==Mode::Eval{return Err("evaluation requires an existing state/training.bcmc checkpoint".into());}
        else{Bundle{checkpoint:Checkpoint::new(Model::new(OBS,64,HEADS.to_vec(),cfg.seed),cfg.seed^0x5123),curriculum:Curriculum::new(cfg.seed,cfg.bots,run).map_err(str::to_string)?,actor_rngs:(0..cfg.bots).map(|i|cfg.seed.wrapping_add(i as u64*73_187)).collect()}};
    if !same_schema(&bundle.checkpoint.model){return Err("neural observation/action schema does not match this build".into());}
    let cp=bundle.checkpoint.clone();
    let control=Coordinator::new(bundle,cfg.batch.div_ceil(cfg.bots),mode)?;
    let peers=(0..cfg.bots).map(|i|AgentView{id:i,name:format!("{}{:02}",cfg.prefix,i),..Default::default()}).collect();
    let training=TrainingView{version:cp.model.version,samples:cp.samples,initial_version:cp.model.version,initial_samples:cp.samples,
        optimizer_steps:cp.adam.step,initial_optimizer_steps:cp.adam.step,fingerprint:cp.model.fingerprint(),initial_fingerprint:cp.model.fingerprint(),resumed,..Default::default()};
    let rt=Arc::new(Runtime{cfg,control:Mutex::new(control),academy_log:Mutex::new(()),peers:Mutex::new(peers),training:Mutex::new(training)});
    save(&rt,&cp)?;
    RUNTIME.set(rt.clone()).map_err(|_|"runtime initialized twice")?;
    let worker=rt.clone();let handle=thread::Builder::new().name("botsclustersmc-ppo".into()).spawn(move||{
        let result=std::panic::catch_unwind(std::panic::AssertUnwindSafe(||learn(&worker,cp))).unwrap_or_else(|_|Err("learner thread panicked; no fallback update".into()));
        if let Err(ref e)=result{
            worker.training.lock().unwrap().error=e.clone();eprintln!("TRAINER FATAL: {e}");STOP.store(true,Ordering::Relaxed);let _=publish_status(&worker);
        }
        result
    }).map_err(|e|e.to_string())?;
    Ok((rt,handle))
}
fn publish_status(rt:&Runtime)->Result<(),String>{
    let t=rt.training.lock().unwrap().clone();let agents=rt.peers.lock().unwrap().clone();
    let frontier=rt.control.lock().unwrap().course.frontier();
    metrics::write_status(&rt.cfg.root,&rt.cfg.mode,&rt.cfg.run_id,&t,&agents,frontier).map_err(|e|e.to_string())?;
    crate::academy::status(rt)
}
fn save(rt:&Runtime,cp:&Checkpoint)->Result<(),String>{
    if rt.cfg.mode!="train"{return Ok(());}
    rt.control.lock().unwrap().snapshot(cp)?.save(&rt.cfg.root.join("state/training.bcmc")).map_err(|e|e.to_string())
}
fn learn(rt:&Runtime,mut cp:Checkpoint)->Result<(),String>{
    let p=ppo::Params::default();let began=Instant::now();let mut last_status=Instant::now();let mut last_save=Instant::now();let mut last_round=Instant::now();
    let mut csv=CsvLog::new(&rt.cfg.root).map_err(|e|e.to_string())?;publish_status(rt)?;
    while !stop_requested(){
        if rt.cfg.root.join(".runtime/stop").exists()||(rt.cfg.seconds>0&&began.elapsed().as_secs()>=rt.cfg.seconds){request_stop();break;}
        let update=rt.control.lock().unwrap().take_update()?;
        if let Some(update)=update{
            if update.policy!=policy_id(&cp){return Err("optimizer acquired the wrong behavior policy".into());}
            let before=Instant::now();let samples=ppo::prepare(update.rollouts,&cp.model,&p)?;
            if samples.len()!=update.samples{return Err("collected/trained sample accounting mismatch".into());}
            let report=ppo::train(&mut cp.model,&mut cp.adam,&mut cp.rng,&samples,&p)?;
            cp.samples=cp.samples.checked_add(report.samples as u64).ok_or("sample count overflow")?;
            {
                let mut c=rt.control.lock().unwrap();let candidate=c.candidate(&cp)?;
                candidate.save(&rt.cfg.root.join("state/training.bcmc")).map_err(|e|e.to_string())?;
                c.publish_update(&candidate)?;
                if c.examining(){eprintln!("ACADEMY frozen exam started; stage={} policy_version={}",c.course.frontier(),cp.model.version);}
            }
            {
                let mut t=rt.training.lock().unwrap();t.version=cp.model.version;t.samples=cp.samples;t.optimizer_steps=cp.adam.step;t.fingerprint=cp.model.fingerprint();
                t.report=report;t.last_update=metrics::now();t.update_ms=before.elapsed().as_millis();csv.append(&t).map_err(|e|e.to_string())?;
                eprintln!("PPO version={} samples={} cohort_samples={} KL={:.6} attempts={} lr={:.8} update_ms={}",t.version,t.samples,t.report.samples,t.report.kl,t.report.attempts,t.report.learning_rate,t.update_ms);
            }
            last_round=Instant::now();last_save=Instant::now();publish_status(rt)?;
        }
        {
            let mut c=rt.control.lock().unwrap();
            if c.mode==Mode::Eval&&c.course.exam_finished(){eprintln!("Frozen evaluation completed without changing training state");request_stop();}
            if let Some((candidate,passed))=c.exam_candidate(&cp)?{
                candidate.save(&rt.cfg.root.join("state/training.bcmc")).map_err(|e|e.to_string())?;
                c.publish_exam(&candidate)?;last_round=Instant::now();
                eprintln!("ACADEMY exam passed={passed}; next stage={}; no elapsed-time promotion",c.course.frontier());
            }
            if c.mode==Mode::Train&&!c.examining()&&last_round.elapsed().as_secs()>rt.cfg.cohort_timeout{
                return Err(format!("cohort made no complete update for {}s; outstanding actors {:?}; no partial or reduced-population update was substituted",rt.cfg.cohort_timeout,c.outstanding()));
            }
        }
        if last_save.elapsed().as_secs()>=60{save(rt,&cp)?;last_save=Instant::now();}
        if last_status.elapsed().as_secs()>=2{publish_status(rt)?;last_status=Instant::now();}
        thread::sleep(Duration::from_millis(50));
    }
    // In-flight episodes restart from new environment resets, never synthetic
    // terminal rewards. Exactly counted completed-but-untrained samples remain
    // visible in academy-status.json and are not smuggled into a later policy.
    save(rt,&cp)?;publish_status(rt)?;
    let error=rt.training.lock().unwrap().error.clone();if error.is_empty(){Ok(())}else{Err(error)}
}
#[cfg(unix)] extern "C" fn signal_handler(_:i32){STOP.store(true,Ordering::Relaxed);}
pub fn signals(){#[cfg(unix)] unsafe{unsafe extern "C"{fn signal(sig:i32,handler:usize)->usize;}signal(2,signal_handler as *const () as usize);signal(15,signal_handler as *const () as usize);}}
pub fn stop_requested()->bool{STOP.load(Ordering::Relaxed)}
pub fn request_stop(){STOP.store(true,Ordering::Relaxed);}
#[allow(dead_code)]pub fn stopped_file(root:&Path)->bool{root.join(".runtime/stop").exists()}
pub fn fail(error:&str){STOP.store(true,Ordering::Relaxed);eprintln!("AGENT FATAL: {error}");if let Ok(mut t)=runtime().training.lock(){if t.error.is_empty(){t.error=error.into();}}}
