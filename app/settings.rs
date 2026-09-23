use std::{env,path::PathBuf};
#[derive(Clone,Debug)]
pub struct Settings{pub root:PathBuf,pub bots:usize,pub hz:u64,pub rollout:usize,pub batch:usize,pub seed:u64,pub mode:String,pub prefix:String,pub server:String,pub seconds:u64,pub radius:f32,pub run_id:String,pub curriculum:bool,pub cohort_timeout:u64,pub join_delay_ms:u64}
fn number(name:&str,default:u64,min:u64,max:u64)->Result<u64,String>{let v=env::var(name).unwrap_or_else(|_|default.to_string()).parse::<u64>().map_err(|_|format!("{name} must be an integer"))?;if v<min||v>max{return Err(format!("{name} must be {min}..={max}"));}Ok(v)}
impl Settings{pub fn load()->Result<Self,String>{
    let mode=env::var("BCMC_MODE").unwrap_or("train".into());if !["train","eval","random"].contains(&mode.as_str()){return Err("BCMC_MODE must be train, eval, or random".into());}
    let prefix=env::var("BOT_PREFIX").unwrap_or("bcmc".into());if prefix.is_empty()||prefix.len()>13||!prefix.bytes().all(|b|b.is_ascii_alphanumeric()||b==b'_'){return Err("BOT_PREFIX must contain 1..13 ASCII letters, digits, underscores".into());}
    let root=env::var("BCMC_ROOT").map(PathBuf::from).unwrap_or(env::current_dir().map_err(|e|e.to_string())?);
    let curriculum=env::var("BCMC_CURRICULUM").as_deref()==Ok("true");
    if !curriculum||!root.join(".botsclustersmc-academy-v2").is_file(){return Err("integrated learning requires start.sh and an owned Academy v2".into());}
    let hz=number("ACTION_HZ",5,5,5)?;let bots=number("BOTS",64,1,64)? as usize;let rollout=number("ROLLOUT_STEPS",64,16,512)? as usize;
    let run_id=env::var("BCMC_RUN_ID").unwrap_or_else(|_|format!("standalone-{}-{}",crate::metrics::now(),std::process::id()));
    if run_id.is_empty()||run_id.len()>80||!run_id.bytes().all(|b|b.is_ascii_alphanumeric()||b==b'-'){return Err("invalid run id".into());}
    Ok(Self{root,bots,hz,rollout,batch:number("BATCH_SAMPLES",(bots*rollout).clamp(64,8192) as u64,64,8192)? as usize,seed:number("SEED",71683,0,u64::MAX)?,mode,prefix,
        server:env::var("BOT_SERVER").unwrap_or("127.0.0.1:25565".into()),seconds:number("RUN_SECONDS",0,0,864000)?,radius:number("WORLD_DIAMETER",512,512,8192)? as f32/2.,run_id,curriculum,
        cohort_timeout:number("COHORT_TIMEOUT_SECONDS",600,180,3600)?,join_delay_ms:number("BOT_JOIN_DELAY_MS",400,100,3000)?})
}}
