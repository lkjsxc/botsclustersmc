//! Linux foreground supervisor. No shell commands are constructed from metadata.
mod json;mod install;mod config;mod probe;mod logs;mod console;mod audit;mod academy_audit;mod campus;
#[allow(dead_code)]
#[path="../learning/src/lib.rs"] mod learning;
use std::{env,fs,io::Write,net::TcpListener,path::Path,process::{Child,Command,Stdio},sync::atomic::{AtomicBool,Ordering},thread,time::{Duration,Instant}};
use install::Result;
use std::os::unix::process::CommandExt;
static STOP:AtomicBool=AtomicBool::new(false);
static SERVER_READY:AtomicBool=AtomicBool::new(false);
static LOGGER_FAILED:AtomicBool=AtomicBool::new(false);
#[cfg(unix)]extern "C" fn on_signal(_:i32){STOP.store(true,Ordering::Relaxed);}
fn signals(){#[cfg(unix)]unsafe{unsafe extern "C"{fn signal(sig:i32,handler:usize)->usize;}signal(2,on_signal as *const () as usize);signal(15,on_signal as *const () as usize);}}
fn disk_free(root:&Path)->Result<u64>{let out=Command::new("df").arg("-Pk").arg(root).output().map_err(|e|e.to_string())?;if !out.status.success(){return Err("df failed".into());}let text=String::from_utf8_lossy(&out.stdout);let last=text.lines().last().ok_or("df output empty")?;let available=last.split_whitespace().nth(3).ok_or("df output format")?.parse::<u64>().map_err(|_|"df numeric output")?;Ok(available*1024)}
fn launch_logs(cmd:&mut Command,root:&Path,label:&str)->Result<Child>{
    cmd.process_group(0);
    cmd.stdout(Stdio::piped()).stderr(Stdio::piped());let mut child=cmd.spawn().map_err(|e|format!("launch {label}: {e}"))?;
    logs::pipe(child.stdout.take().unwrap(),root.join(format!("logs/{label}.out.log")));
    logs::pipe(child.stderr.take().unwrap(),root.join(format!("logs/{label}.err.log")));
    Ok(child)
}
fn wait_or_kill(child:&mut Child,seconds:u64,label:&str)->Result<()>{let start=Instant::now();loop{if let Some(status)=child.try_wait().map_err(|e|e.to_string())?{return if status.success(){Ok(())}else{Err(format!("{label} exited unsuccessfully: {status}"))};}if start.elapsed().as_secs()>=seconds{eprintln!("{label} did not exit gracefully; forced termination. Check world/state backups.");child.kill().map_err(|e|e.to_string())?;child.wait().map_err(|e|e.to_string())?;return Err(format!("{label} required forced termination"));}thread::sleep(Duration::from_millis(250));}}
fn prepare(root:&Path,cfg:&config::Config)->Result<(std::path::PathBuf,std::path::PathBuf)>{
    cfg.write(root)?;let java=install::java(root)?;let jar=install::folia(root)?;
    if cfg.curriculum {
        install::command(Command::new("bash").arg(root.join("scripts/build-bridge.sh"))
            .env("BCMC_ROOT",root).env("BCMC_JAVA",&java).env("BCMC_FOLIA_JAR",&jar).env("BCMC_CURRICULUM","true"))?;
    }
    Ok((java,jar))
}
fn run(root:&Path,cfg:&config::Config)->Result<()>{
    for dir in ["logs","state",".runtime"]{fs::create_dir_all(root.join(dir)).map_err(|e|e.to_string())?;}
    if disk_free(root)?<cfg.disk*1024*1024*1024{return Err(format!("need at least {} GiB free before start",cfg.disk));}
    // Bind test BEFORE spawning Java; never attach bots to an unrelated server.
    let check=TcpListener::bind((cfg.bind,cfg.port)).map_err(|e|format!("port {} unavailable: {e}",cfg.port))?;drop(check);
    let (java,jar)=prepare(root,cfg)?;
    if !root.join("bin/botsclustersmc-bots").is_file(){return Err("bot executable missing; run ./scripts/build.sh".into());}
    let stop=root.join(".runtime/stop");if stop.exists(){fs::remove_file(&stop).map_err(|e|e.to_string())?;}
    install::atomic(&root.join(".runtime/supervisor.pid"),format!("{}\n",std::process::id()).as_bytes())?;
    let clock=std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).map_err(|e|e.to_string())?;
    let run_id=format!("{}-{}",clock.as_nanos(),std::process::id());
    install::atomic(&root.join(".runtime/run.json"),format!("{{\"run_id\":\"{}\",\"started\":{},\"bots\":{}}}\n",run_id,clock.as_secs(),cfg.bots).as_bytes())?;
    let _=fs::remove_file(root.join(".runtime/server-ready.json"));
    if cfg.curriculum {
        let lab=root.join(".runtime/lab");fs::create_dir_all(&lab).map_err(|e|e.to_string())?;
        // Only transient protocol files are removed, never world/checkpoint data.
        for id in 0..64 {for stem in ["request","frame"] {let path=lab.join(format!("{stem}-{id}.txt"));if path.exists(){fs::remove_file(path).map_err(|e|e.to_string())?;}}}
        for file in ["bridge.ready","campus.ready","fatal.txt"] {let path=lab.join(file);if path.exists(){fs::remove_file(path).map_err(|e|e.to_string())?;}}
    }
    let mut server_cmd=Command::new(&java);
    server_cmd.env("BCMC_ROOT",root).env("BCMC_RUN_ID",&run_id);
    server_cmd.current_dir(root.join("server")).args(["-Xms2G",&format!("-Xmx{}G",cfg.heap),"-XX:+UseG1GC","-XX:ActiveProcessorCount=10","-XX:ParallelGCThreads=2","-XX:ConcGCThreads=1","-XX:+ExitOnOutOfMemoryError","-XX:MaxDirectMemorySize=512M","-XX:MaxMetaspaceSize=768M","-Dio.netty.eventLoopThreads=2","-Dterminal.jline=false","-Dterminal.ansi=false","-jar"]).arg(jar).arg("--nogui").stdin(Stdio::piped());
    eprintln!("Starting Folia {} on {}:{}; bots={}; mode={}; heap={} GiB",install::MC,cfg.bind,cfg.port,cfg.bots,cfg.mode,cfg.heap);
    let mut server=launch_logs(&mut server_cmd,root,"folia")?;
    let _=install::atomic(&root.join(".runtime/java.pid"),server.id().to_string().as_bytes());
    let _=install::atomic(&root.join(".runtime/last-exit.txt"),b"running: no clean shutdown recorded yet\n");
    let mut brain:Option<Child>=None;
    let console_listener=match console::listen(root){Ok(l)=>l,Err(e)=>{if let Some(input)=server.stdin.as_mut(){let _=writeln!(input,"stop");}let _=wait_or_kill(&mut server,180,"Folia");return Err(e);}};
    let result=(||->Result<()>{
        let began=Instant::now();let address=cfg.local_address();
        loop{
            if STOP.load(Ordering::Relaxed)||stop.exists(){return Ok(());}
            if let Some(s)=server.try_wait().map_err(|e|e.to_string())?{return Err(format!("Folia exited during startup: {s}; inspect logs/folia.*.log"));}
            if SERVER_READY.load(Ordering::Relaxed){if let Ok(j)=probe::status(&address){let version=j.get("version")?.get("name")?.text()?;
                if !version.contains(install::MC)||j.get("version")?.get("protocol")?.integer()?!=774{return Err(format!("protocol version mismatch: expected {} / 774, got {version}",install::MC));}
                install::atomic(&root.join(".runtime/server-ready.json"),format!("{{\"run_id\":\"{run_id}\",\"protocol\":774}}\n").as_bytes())?;break;
            }}
            if began.elapsed().as_secs()>900{return Err("Folia startup timeout; no bot was started".into());}
            thread::sleep(Duration::from_secs(2));
        }
        let diameter=config::number("WORLD_DIAMETER",2048,512,8192)?;
        if let Some(input)=server.stdin.as_mut(){writeln!(input,"worldborder set {diameter}").map_err(|e|e.to_string())?;input.flush().map_err(|e|e.to_string())?;}
        if cfg.curriculum {
            let began=Instant::now();
            loop {
                if campus::ready(root,&run_id,cfg.bots as usize)?{break;}
                if server.try_wait().map_err(|e|e.to_string())?.is_some(){return Err("Folia exited while building the campus".into());}
                if began.elapsed().as_secs()>900{return Err("Complete campus readiness timeout; no bots started".into());}
                if STOP.load(Ordering::Relaxed)||stop.exists(){return Ok(());}
                thread::sleep(Duration::from_millis(250));
            }
            eprintln!("All {} training cells verified for this run. Connecting the requested population.",cfg.bots);
        }
        // Do not inherit any old stop/status indication as proof of a new run.
        let _=fs::remove_file(root.join("state/status.json"));
        let mut cmd=Command::new(root.join("bin/botsclustersmc-bots"));cmd.current_dir(root).env("BCMC_ROOT",root).env("BCMC_RUN_ID",&run_id).env("BOT_SERVER",&address).env("RUN_SECONDS","0").stdin(Stdio::null());
        brain=Some(launch_logs(&mut cmd,root,"bots")?);
        let _=install::atomic(&root.join(".runtime/bots.pid"),brain.as_ref().unwrap().id().to_string().as_bytes());
        eprintln!("Folia answered the status handshake. RL process started; check ./status.sh for real joins and PPO updates.");
        let began=Instant::now();let mut check_at=Instant::now();let mut has_acted=false;
        loop{
            if STOP.load(Ordering::Relaxed)||stop.exists(){break;}
            if let Some(input)=server.stdin.as_mut(){console::poll(&console_listener,input);}
            if cfg.seconds>0&&began.elapsed().as_secs()>=cfg.seconds{break;}
            if let Some(s)=server.try_wait().map_err(|e|e.to_string())?{return Err(format!("Folia exited: {s}"));}
            if let Some(s)=brain.as_mut().unwrap().try_wait().map_err(|e|e.to_string())?{return Err(format!("bot/learner exited: {s}; inspect logs/bots.*.log and state/status.json"));}
            if !has_acted {
                if let Ok(text)=fs::read_to_string(root.join("state/status.json")) {
                    let j=json::parse(&text)?;
                    let now=std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).map_err(|e|e.to_string())?.as_secs();
                    has_acted=campus::population(&j,&run_id,cfg.bots as usize,now,120)?;
                    if has_acted{eprintln!("All {} bots spawned and recorded decisions; movement is not proof of learned skills.",cfg.bots);}
                }
                if began.elapsed().as_secs()>360&&!has_acted{return Err("Requested population did not fully spawn and act; no silent population reduction".into());}
            }
            if check_at.elapsed().as_secs()>=30{
                let now=std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).map_err(|e|e.to_string())?.as_secs();
                if let Ok(text)=fs::read_to_string(root.join("state/status.json")){let state=json::parse(&text)?;
                    let updated=state.get("updated")?.integer()?;
                    if now.saturating_sub(updated)>120{return Err("learner heartbeat stale; shutting down".into());}
                    if has_acted && !campus::population(&state,&run_id,cfg.bots as usize,now,240)? {
                        // A disconnected member is allowed a bounded reconnect grace period.
                        let stale=state.get("agents")?.array()?.iter().any(|a|now.saturating_sub(a.get("last_seen").and_then(json::Json::integer).unwrap_or(0))>240);
                        if stale{return Err("An individual bot is stale; refusing to train with a silently reduced population".into());}
                    }
                    let rss=|child:&Child|fs::read_to_string(format!("/proc/{}/status",child.id())).ok().and_then(|s|s.lines().find(|l|l.starts_with("VmRSS:")).and_then(|l|l.split_whitespace().nth(1)).and_then(|x|x.parse::<u64>().ok())).unwrap_or(0);
                    install::atomic(&root.join(".runtime/resources.json"),format!("{{\"run_id\":\"{run_id}\",\"updated\":{now},\"java_rss_kib\":{},\"bots_rss_kib\":{}}}\n",rss(&server),rss(brain.as_ref().unwrap())).as_bytes())?;
                }else if began.elapsed().as_secs()>120{return Err("learner status missing; shutting down".into());}
                if disk_free(root)?<cfg.disk*1024*1024*1024{return Err("disk free-space guard activated; shutting down, never deleting worlds".into());}logs::prune_old_folia_logs(root);check_at=Instant::now();}
            thread::sleep(Duration::from_millis(250));
        }Ok(())
    })();
    eprintln!("Stopping learning and saving state, then stopping Folia...");
    let _=install::atomic(&stop,b"stop\n");
    let brain_stop=if let Some(child)=brain.as_mut(){wait_or_kill(child,90,"learner")}else{Ok(())};
    if server.try_wait().ok().flatten().is_none(){if let Some(input)=server.stdin.as_mut(){let _=writeln!(input,"stop");let _=input.flush();}}
    let server_stop=wait_or_kill(&mut server,180,"Folia");
    let _=fs::remove_file(root.join(".runtime/supervisor.pid"));
    let _=fs::remove_file(root.join(".runtime/console.sock"));
    let log_result=if LOGGER_FAILED.load(Ordering::Relaxed){Err("a log writer failed; inspect available disk and permissions".into())}else{Ok(())};
    let result=result.and(brain_stop).and(server_stop).and(log_result);
    let message=match &result{Ok(())=>"clean shutdown\n".to_string(),Err(e)=>format!("abnormal: {e}\n")};
    let _=install::atomic(&root.join(".runtime/last-exit.txt"),message.as_bytes());
    let _=fs::remove_file(root.join(".runtime/java.pid"));let _=fs::remove_file(root.join(".runtime/bots.pid"));
    result
}
fn main(){if let Err(e)=main_inner(){eprintln!("botsclustersmc ERROR: {e}");std::process::exit(1);}}
fn main_inner()->Result<()>{
    if env::consts::OS!="linux"{return Err("Linux is required".into());}
    let root=env::var("BCMC_ROOT").map(std::path::PathBuf::from).unwrap_or(env::current_dir().map_err(|e|e.to_string())?).canonicalize().map_err(|e|e.to_string())?;
    let action=env::args().nth(1).unwrap_or("run".into());
    if action=="verify-run" || action=="verify-academy" {
        let previous=env::args().nth(2).map(std::path::PathBuf::from);
        return if action=="verify-academy"{academy_audit::verify(&root,previous.as_deref())}else{audit::verify(&root,previous.as_deref())};
    }
    if action=="check-bridge" {
        let java=install::java(&root)?;let jar=install::folia(&root)?;
        return install::command(Command::new("bash").arg(root.join("scripts/build-bridge.sh"))
            .env("BCMC_ROOT",&root).env("BCMC_JAVA",java).env("BCMC_FOLIA_JAR",jar).env("BCMC_CURRICULUM","true"));
    }
    if action=="console"{return console::send(&root,&env::args().skip(2).collect::<Vec<_>>().join(" "));}
    if action=="status"{
        let status=fs::read_to_string(root.join("state/status.json")).map_err(|_|"No status file yet. Inspect logs/ or finish the initial build.")?;
        if env::args().any(|a|a=="--json"){println!("{status}");return Ok(());}
        let j=json::parse(&status)?;
        println!("mode={}  policy_version={}  trained_samples={}  discarded_rollouts={}",j.get("mode")?.text()?,j.get("version")?.integer()?,j.get("trained_samples")?.integer()?,j.get("dropped_rollouts")?.integer()?);
        println!("run_id={}  optimizer_steps={}  resumed={:?}",j.get("run_id")?.text()?,j.get("optimizer_steps")?.integer()?,j.get("resumed")?);
        println!("updated_unix={}  last_PPO_update_unix={}  update_ms={}",j.get("updated")?.integer()?,j.get("last_update")?.integer()?,j.get("update_ms")?.integer()?);
        if j.get("version")?.integer()?==0{println!("No completed PPO update yet; movement alone is NOT proof of learning.");}
        let error=j.get("error")?.text()?;if !error.is_empty(){println!("ERROR: {error}");}
        for a in j.get("agents")?.array()?{println!("{} online={:?} steps={} explored_cells={} deaths={} inventory_kinds={} stats_packets={}",a.get("name")?.text()?,a.get("online")?,a.get("steps")?.integer()?,a.get("cells")?.integer()?,a.get("deaths")?.integer()?,a.get("inventory_kinds")?.integer()?,a.get("stats_packets")?.integer()?);}
        if let Ok(text)=fs::read_to_string(root.join("state/academy-status.json")) {
            let c=json::parse(&text)?;
            print!("{}",course_summary(&c,j.get("run_id")?.text()?)?);
        }
        println!("Raw status: state/status.json; learning history: logs/learning.csv");return Ok(());
    }
    if action=="check-json"{let path=env::args().nth(2).ok_or("check-json requires a path")?;json::parse(&fs::read_to_string(path).map_err(|e|e.to_string())?)?;println!("JSON valid");return Ok(());}
    let cfg=config::Config::load()?;signals();
    match action.as_str(){"run"=>run(&root,&cfg),"prepare"=>{prepare(&root,&cfg)?;Ok(())},"probe"=>{println!("{:#?}",probe::status(&cfg.local_address())?);Ok(())},_=>Err("usage: botsclustersmc-run [run|prepare|status|probe|check-json path|verify-run [previous-status.json]|verify-academy [previous-status.json]]".into())}
}

/// Format the actual v2 runtime schema without consulting obsolete v1 fields.
fn course_summary(c:&json::Json,run:&str)->Result<String>{
    if c.get("schema")?.integer()?!=3 || c.get("run_id")?.text()?!=run{return Err("incompatible or stale course status".into());}
    let stage=c.get("stage")?.integer()?;
    if stage>=18{return Err("invalid course stage".into());}
    let mut out=format!("Academy stage={}/18 ({}) phase={}\n",stage+1,c.get("name")?.text()?,c.get("phase")?.text()?);
    out+=&format!("course_completed={:?}; a historical pass does NOT certify cooperative living\n",c.get("course_completed")?);
    out+=&format!("cohort_samples={} sealed_actors={} local_untrained_samples={} unfinished_actions={}\n",c.get("cohort_samples")?.integer()?,c.get("sealed_actors")?.integer()?,c.get("local_untrained_samples")?.integer()?,c.get("unfinished_actions")?.integer()?);
    let scores=c.get("scores")?.array()?;
    if scores.is_empty()||scores.len()>64{return Err("invalid course population".into());}
    for (id,row) in scores.iter().enumerate(){
        if row.get("id")?.integer()?!=id as u64||row.get("task")?.integer()?>=18{return Err("invalid course row identity".into());}
        let number=|name:&str|->Result<f64>{match row.get(name)?{json::Json::Number(s)=>{let n=s.parse::<f64>().map_err(|_|"invalid course ratio")?;if n.is_finite()&&(0.0..=1.0).contains(&n){Ok(n)}else{Err("invalid course ratio".into())}},_=>Err("expected numeric course ratio".into())}};
        out+=&format!("agent {}: task={} state={} attempts={} success_EMA={:.1}% difficulty={:.2} current_exam={}/{}\n",id,row.get("task")?.integer()?,row.get("state")?.text()?,row.get("attempts")?.integer()?,number("success_rate")?*100.,number("difficulty")?,row.get("exam_successes")?.integer()?,row.get("exam_trials")?.integer()?);
    }
    out+="Promotion requires every actor: 14/16 current trials and 3/4 for EACH earlier skill. EMA is not an exam score.\n";
    Ok(out)
}
#[cfg(test)]mod course_status_tests{
    use super::*;
    fn data()->String{r#"{"schema":3,"run_id":"run","stage":0,"name":"forward-stop","phase":"training","course_completed":false,"cohort_samples":123,"sealed_actors":1,"local_untrained_samples":4,"unfinished_actions":1,"scores":[{"id":0,"task":0,"state":"practice","attempts":42,"success_rate":0.75,"difficulty":0.4,"exam_trials":0,"exam_successes":0}]}"#.to_string()}
    #[test]fn public_status_formats_actual_v2_fields(){let text=course_summary(&json::parse(&data()).unwrap(),"run").unwrap();assert!(text.contains("stage=1/18"));assert!(text.contains("success_EMA=75.0%"));assert!(text.contains("cohort_samples=123"));assert!(!text.contains("foundations_passed"));}
    #[test]fn rejects_foreign_and_malformed_status(){assert!(course_summary(&json::parse(&data()).unwrap(),"other").is_err());let bad=data().replace("0.75","1.75");assert!(course_summary(&json::parse(&bad).unwrap(),"run").is_err());}
}
