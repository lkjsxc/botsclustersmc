use std::{env,path::PathBuf};
#[derive(Clone,Debug)]
pub struct Settings { pub root:PathBuf,pub bots:usize,pub hz:u64,pub rollout:usize,pub batch:usize,pub seed:u64,pub mode:String,pub prefix:String,pub server:String,pub seconds:u64,pub radius:f32, pub run_id:String, pub curriculum:bool }
fn number(name:&str,default:u64,min:u64,max:u64)->Result<u64,String>{let v=env::var(name).unwrap_or_else(|_|default.to_string()).parse::<u64>().map_err(|_|format!("{name} must be an integer"))?;if v<min||v>max{return Err(format!("{name} must be {min}..={max}"));}Ok(v)}
impl Settings {pub fn load()->Result<Self,String>{
    let mode=env::var("BCMC_MODE").unwrap_or("train".into());if !["train","eval","random"].contains(&mode.as_str()){return Err("BCMC_MODE must be train, eval, or random".into());}
    let prefix=env::var("BOT_PREFIX").unwrap_or("bcmc".into());if prefix.is_empty()||prefix.len()>13||!prefix.bytes().all(|b|b.is_ascii_alphanumeric()||b==b'_'){return Err("BOT_PREFIX must contain 1..13 ASCII letters, digits, underscores".into());}
    let curriculum=env::var("BCMC_CURRICULUM").as_deref()==Ok("true");
    if curriculum && !env::var("BCMC_ROOT").map(PathBuf::from).unwrap_or_default().join(".botsclustersmc-academy-v1").is_file(){return Err("curriculum requires the isolated academy.sh directory".into());}
    let hz=number("ACTION_HZ",if curriculum{5}else{2},1,5)?;
    if curriculum && hz!=5 {return Err("academy requires ACTION_HZ=5 (4 client ticks)".into());}
    // An integer number of client ticks; actual duration is measured in metrics.
    if 20%hz!=0{return Err("ACTION_HZ must divide 20".into());}
    let bots=number("BOTS",32,1,32)? as usize;let rollout=number("ROLLOUT_STEPS",128,16,512)? as usize;
    let run_id = env::var("BCMC_RUN_ID").unwrap_or_else(|_| format!("standalone-{}-{}", crate::metrics::now(), std::process::id()));
    if run_id.is_empty() || run_id.len()>80 || !run_id.bytes().all(|b|b.is_ascii_alphanumeric()||b==b'-') {return Err("invalid run id".into());}
    Ok(Self{run_id,curriculum,root:env::var("BCMC_ROOT").map(PathBuf::from).unwrap_or(env::current_dir().map_err(|e|e.to_string())?),bots,hz,rollout,batch:number("BATCH_SAMPLES",(bots*rollout).clamp(64,8192) as u64,64,8192)? as usize,seed:number("SEED",71683,0,u64::MAX)?,mode,prefix,server:env::var("BOT_SERVER").unwrap_or("127.0.0.1:25565".into()),seconds:number("RUN_SECONDS",0,0,864000)?,radius:number("WORLD_DIAMETER",2048,512,8192)? as f32/2.0})
}}
