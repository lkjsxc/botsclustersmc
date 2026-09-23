use std::{fs,path::Path,sync::{Arc,Mutex,OnceLock,RwLock,atomic::{AtomicBool,AtomicU64,Ordering}},thread,time::{Duration,Instant}};
use crate::{learning::{checkpoint::{Checkpoint,atomic_write},network::Model,ppo::{self,Rollout,Transition},reward::RewardBook,
    next::{cohort::{Cohort,Round,Packet},curriculum::{Curriculum,Phase,PolicyId,Lesson}},OBS,HEADS},metrics::{self,AgentView,TrainingView,CsvLog},settings::Settings};

pub static STOP:AtomicBool=AtomicBool::new(false);
static RUNTIME:OnceLock<Arc<Runtime>>=OnceLock::new();
/// All issue/complete/publish operations use this one coordination boundary.
/// Lock order: coordination -> policy. Never acquire these while holding metrics.
pub struct Coordination {pub course:Curriculum,pub cohort:Cohort<Transition>}
pub struct Runtime {
    pub cfg:Settings,pub policy:RwLock<Arc<Model>>,pub rewards:Mutex<RewardBook>,
    pub coordination:Mutex<Coordination>,pub academy_log:Mutex<()>,
    pub peers:Mutex<Vec<AgentView>>,pub training:Mutex<TrainingView>,
    pub submitted_samples:AtomicU64,pub shutdown_discarded_samples:AtomicU64,
}
pub fn runtime()->&'static Arc<Runtime>{RUNTIME.get().expect("runtime initialized before joining")}
pub fn policy_id(m:&Model)->PolicyId{PolicyId{version:m.version,signature:m.fingerprint()}}
fn same_schema(m:&Model)->bool{m.input==OBS&&m.hidden==64&&m.heads.as_slice()==HEADS.as_slice()}
fn cohort(cfg:&Settings,version:u64,generation:u64)->Result<Cohort<Transition>,String>{
    let quota=cfg.rollout.max(cfg.batch.div_ceil(cfg.bots));
    Cohort::new(Round{policy_version:version,generation},cfg.bots,quota,1024).map_err(str::to_string)
}
pub fn initialize(cfg:Settings)->Result<(Arc<Runtime>,thread::JoinHandle<Result<(),String>>),String>{
    if !cfg.curriculum{return Err("Only the isolated Academy runtime is supported; use ./start.sh".into());}
    fs::create_dir_all(cfg.root.join("state")).map_err(|e|e.to_string())?;
    let context_path=cfg.root.join("state/environment.txt");let context="botsclustersmc-academy-v2-rl-next18-timed-conditional-704\n";
    if context_path.exists(){if fs::read_to_string(&context_path).map_err(|e|e.to_string())?!=context{return Err("Incompatible training semantics. Preserve the old installation and create a fresh clone; no automatic reset or checkpoint conversion.".into());}}
    else {if cfg.root.join("state/policy.bcmc").exists(){return Err("Unversioned policy: preserve the full old Academy; refuse implicit migration".into());}atomic_write(&context_path,context.as_bytes(),0).map_err(|e|e.to_string())?;}
    let cp_path=cfg.root.join("state/policy.bcmc");let rp_path=cfg.root.join("state/rewards.bcmc");let course_path=cfg.root.join("state/academy.bcmc");
    if cfg.mode!="random" && (cp_path.exists()!=rp_path.exists()||cp_path.exists()!=course_path.exists()) {return Err("Incomplete policy/curriculum/reward checkpoint set: restore a full stopped backup; no implicit reset".into());}
    let resumed=cp_path.exists()&&cfg.mode!="random";
    let cp=if resumed{Checkpoint::load(&cp_path).map_err(|e|e.to_string())?}
        else if cfg.mode=="eval"{return Err("evaluation requires a saved policy".into());}
        else{Checkpoint::new(Model::new(OBS,64,HEADS.to_vec(),cfg.seed),cfg.seed^0x5123)};
    if !same_schema(&cp.model){return Err("policy observation/action/network schema differs".into());}
    let identity=policy_id(&cp.model);
    let mut course=if resumed{Curriculum::from_checkpoint(&fs::read(course_path).map_err(|e|e.to_string())?,identity,metrics::hash(&cfg.run_id)).map_err(str::to_string)?}
        else{Curriculum::new(cfg.seed,cfg.bots,metrics::hash(&cfg.run_id)).map_err(str::to_string)?};
    if course.bots()!=cfg.bots{return Err("saved actor population differs; preserve data and use a separate Academy".into());}
    if cfg.mode=="eval"{course.begin_evaluation(identity).map_err(str::to_string)?;}
    let collection=cohort(&cfg,cp.model.version,course.generation())?;
    let mut rewards=if resumed{RewardBook::load(&rp_path).map_err(|e|e.to_string())?}else{RewardBook::new(cfg.bots)};rewards.resize(cfg.bots);
    let peers=(0..cfg.bots).map(|i|AgentView{id:i,name:format!("{}{:02}",cfg.prefix,i),..Default::default()}).collect();
    let rt=Arc::new(Runtime{coordination:Mutex::new(Coordination{course,cohort:collection}),academy_log:Mutex::new(()),policy:RwLock::new(Arc::new(cp.model.clone())),
        training:Mutex::new(TrainingView{version:cp.model.version,samples:cp.samples,initial_version:cp.model.version,initial_samples:cp.samples,
            optimizer_steps:cp.adam.step,initial_optimizer_steps:cp.adam.step,fingerprint:cp.model.fingerprint(),initial_fingerprint:cp.model.fingerprint(),resumed,..Default::default()}),
        cfg,rewards:Mutex::new(rewards),peers:Mutex::new(peers),submitted_samples:AtomicU64::new(0),shutdown_discarded_samples:AtomicU64::new(0)});
    save(&rt,&cp)?;
    RUNTIME.set(rt.clone()).map_err(|_|"runtime initialized twice")?;
    let worker=rt.clone();let handle=thread::Builder::new().name("botsclustersmc-ppo".into()).spawn(move||{
        let result=std::panic::catch_unwind(std::panic::AssertUnwindSafe(||learn(&worker,cp))).unwrap_or_else(|_|Err("learner panicked; no fallback policy".into()));
        if let Err(ref error)=result{worker.training.lock().unwrap().error=error.clone();eprintln!("TRAINER FATAL: {error}");STOP.store(true,Ordering::Relaxed);let _=publish_status(&worker);}result
    }).map_err(|e|e.to_string())?;
    Ok((rt,handle))
}
pub fn begin(id:usize)->Result<Option<(Lesson,Arc<Model>)>,String>{
    let rt=runtime();let mut c=rt.coordination.lock().unwrap();
    if stop_requested(){return Ok(None);}
    if rt.cfg.mode=="train" && c.course.phase()==Phase::Training && c.cohort.progress().get(id).ok_or("unknown actor")?.sealed{return Ok(None);}
    let model=rt.policy.read().unwrap().clone();let lesson=c.course.issue(id,policy_id(&model)).map_err(str::to_string)?;
    Ok(lesson.map(|l|(l,model)))
}
/// An episode completion and its experience enter together. No queue overflow,
/// old-policy replay, partial population update, or silently discarded packet.
pub fn complete(l:&Lesson,model:&Model,steps:Vec<Transition>,success:bool)->Result<(),String>{
    let rt=runtime();let mut c=rt.coordination.lock().unwrap();
    if stop_requested(){rt.shutdown_discarded_samples.fetch_add(steps.len() as u64,Ordering::Relaxed);return Ok(());}
    let identity=policy_id(model);let mut next_course=c.course.clone();
    next_course.record(l,identity,success).map_err(str::to_string)?;
    if l.evaluation || rt.cfg.mode!="train" {if !steps.is_empty(){return Err("evaluation experience must never enter PPO".into());}}
    else{
        if l.session.generation!=c.cohort.round().generation||l.policy.version!=c.cohort.round().policy_version{return Err("episode/cohort generation or policy mismatch".into());}
        if steps.is_empty()||!steps.last().unwrap().terminal{return Err("cohort completion requires a real terminal transition".into());}
        let id=l.session.actor as usize;let progress=c.cohort.progress()[id];let n=steps.len();
        let packet=Packet{round:c.cohort.round(),actor:id,sequence:progress.next_sequence,evaluation:false,
            seal:progress.samples+n>=c.cohort.quota(),ends_episode:true,samples:steps};
        c.cohort.push(packet,|ts|{
            for (i,t) in ts.iter().enumerate(){
                if t.obs.len()!=OBS||t.mask.len()!=HEADS.iter().sum::<usize>()||t.actions.len()!=HEADS.len()||t.ticks==0||t.ticks>1_000_000||
                    t.obs.iter().any(|x|!x.is_finite())||[t.old_logp,t.value,t.next_value,t.reward].iter().any(|x|!x.is_finite())||
                    t.actions.iter().zip(HEADS).any(|(a,n)|*a>=n)||t.terminal!=(i+1==ts.len()) {return Err("invalid episode trajectory");}
            }Ok(())
        }).map_err(str::to_string)?;
        rt.submitted_samples.fetch_add(n as u64,Ordering::Relaxed);
    }
    c.course=next_course;Ok(())
}
fn publish_status(rt:&Runtime)->Result<(),String>{
    let t=rt.training.lock().unwrap().clone();let a=rt.peers.lock().unwrap().clone();
    metrics::write_status(&rt.cfg.root,&rt.cfg.mode,&rt.cfg.run_id,&t,&a,0).map_err(|e|e.to_string())?;
    crate::academy::status(rt)
}
fn save_course(rt:&Runtime,cp:&Checkpoint,c:&Curriculum)->Result<(),String>{
    if rt.cfg.mode!="train"{return Ok(());}
    // These files are a checked pair, not a distributed transaction with the world.
    // A crash between writes fails closed on restart, never silently rewinds one half.
    let bytes=c.checkpoint(policy_id(&cp.model)).map_err(str::to_string)?;
    rt.rewards.lock().unwrap().save(&rt.cfg.root.join("state/rewards.bcmc")).map_err(|e|e.to_string())?;
    cp.save(&rt.cfg.root.join("state/policy.bcmc")).map_err(|e|e.to_string())?;
    atomic_write(&rt.cfg.root.join("state/academy.bcmc"),&bytes,3).map_err(|e|e.to_string())?;Ok(())
}
fn save(rt:&Runtime,cp:&Checkpoint)->Result<(),String>{let c=rt.coordination.lock().unwrap();save_course(rt,cp,&c.course)}
fn learn(rt:&Runtime,cp:Checkpoint)->Result<(),String>{
    publish_status(rt)?;
    let finished=AtomicBool::new(false);
    let result=thread::scope(|scope|{
        let heartbeat=scope.spawn(||{
            while !finished.load(Ordering::Relaxed){thread::sleep(Duration::from_millis(500));if finished.load(Ordering::Relaxed){break;}
                if let Err(e)=publish_status(rt){let mut t=rt.training.lock().unwrap();if t.error.is_empty(){t.error=format!("status publisher: {e}");}STOP.store(true,Ordering::Relaxed);break;}}
        });
        let result=std::panic::catch_unwind(std::panic::AssertUnwindSafe(||learn_loop(rt,cp))).unwrap_or_else(|_|Err("learner loop panicked".into()));
        finished.store(true,Ordering::Relaxed);heartbeat.join().map_err(|_|"status publisher panicked".to_string())?;result
    });
    publish_status(rt)?;result
}
fn learn_loop(rt:&Runtime,mut cp:Checkpoint)->Result<(),String>{
    let p=ppo::Params::default();let began=Instant::now();let mut last_save=Instant::now();let mut csv=CsvLog::new(&rt.cfg.root).map_err(|e|e.to_string())?;
    while !stop_requested(){
        if rt.cfg.root.join(".runtime/stop").exists()||(rt.cfg.seconds>0&&began.elapsed().as_secs()>=rt.cfg.seconds){request_stop();break;}
        let batch={
            let mut c=rt.coordination.lock().unwrap();
            if c.course.exam_finished(){
                if rt.cfg.mode=="train"{let passed=c.course.finish_exam(policy_id(&cp.model)).map_err(str::to_string)?;
                    c.cohort=cohort(&rt.cfg,cp.model.version,c.course.generation())?;save_course(rt,&cp,&c.course)?;
                    eprintln!("ACADEMY exam-finished passed={passed} frontier={} policy={}",c.course.frontier(),cp.model.version);
                }else{eprintln!("ACADEMY external evaluation finished; weights unchanged");request_stop();}
            }
            if rt.cfg.mode=="train"&&c.course.phase()==Phase::Training&&c.cohort.ready(){Some(c.cohort.take_ready().map_err(str::to_string)?)}else{None}
        };
        if let Some(batch)=batch{
            let before=Instant::now();let generation=batch.round.generation;let submitted=batch.len();
            let rollouts=batch.trajectories.into_iter().map(|steps|Rollout{version:batch.round.policy_version,steps}).collect();
            let samples=ppo::prepare(rollouts,&cp.model,&p)?;
            if samples.len()!=submitted{return Err("cohort sample accounting mismatch".into());}
            let report=ppo::train(&mut cp.model,&mut cp.adam,&mut cp.rng,&samples,&p)?;
            cp.samples=cp.samples.checked_add(report.samples as u64).ok_or("sample count exhausted")?;
            {
                let mut c=rt.coordination.lock().unwrap();
                c.cohort.commit(Round{policy_version:cp.model.version,generation}).map_err(str::to_string)?;
                if c.course.exam_ready(){c.course.begin_exam(policy_id(&cp.model)).map_err(str::to_string)?;
                    c.cohort=cohort(&rt.cfg,cp.model.version,c.course.generation())?;
                    eprintln!("ACADEMY frozen-exam frontier={} policy={}",c.course.frontier(),cp.model.version);
                }
                save_course(rt,&cp,&c.course)?;
                *rt.policy.write().unwrap()=Arc::new(cp.model.clone());
            }
            {let mut t=rt.training.lock().unwrap();t.version=cp.model.version;t.samples=cp.samples;t.optimizer_steps=cp.adam.step;t.fingerprint=cp.model.fingerprint();
                t.report=report;t.last_update=metrics::now();t.update_ms=before.elapsed().as_millis();csv.append(&t).map_err(|e|e.to_string())?;
                eprintln!("PPO cohort actors={} version={} samples={} batch={} KL={:.6} update_ms={}",rt.cfg.bots,t.version,t.samples,submitted,t.report.kl,t.update_ms);}
            last_save=Instant::now();
        }
        if last_save.elapsed().as_secs()>=60{save(rt,&cp)?;last_save=Instant::now();}
        thread::sleep(Duration::from_millis(50));
    }
    // A bounded stop does not invent terminal observations or train an incomplete cohort.
    let buffered=rt.coordination.lock().unwrap().cohort.len();
    rt.shutdown_discarded_samples.fetch_add(buffered as u64,Ordering::Relaxed);
    save(rt,&cp)?;let error=rt.training.lock().unwrap().error.clone();if error.is_empty(){Ok(())}else{Err(error)}
}
#[cfg(unix)] extern "C" fn signal_handler(_:i32){STOP.store(true,Ordering::Relaxed);}
pub fn signals(){#[cfg(unix)] unsafe{unsafe extern "C"{fn signal(sig:i32,handler:usize)->usize;}signal(2,signal_handler as *const () as usize);signal(15,signal_handler as *const () as usize);}}
pub fn stop_requested()->bool{STOP.load(Ordering::Relaxed)}
pub fn request_stop(){STOP.store(true,Ordering::Relaxed);}
#[allow(dead_code)]pub fn stopped_file(root:&Path)->bool{root.join(".runtime/stop").exists()}
pub fn fail(error:&str){STOP.store(true,Ordering::Relaxed);eprintln!("AGENT FATAL: {error}");if let Ok(mut t)=runtime().training.lock(){if t.error.is_empty(){t.error=error.to_owned();}}}
