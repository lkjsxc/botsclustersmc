use std::{fs::{self,OpenOptions},io::{self,Write},path::{Path,PathBuf},time::{SystemTime,UNIX_EPOCH}};
use crate::learning::{checkpoint::atomic_write,ppo::Report};
pub fn now()->u64{SystemTime::now().duration_since(UNIX_EPOCH).unwrap_or_default().as_secs()}
pub fn hash(s:&str)->u64{crate::learning::checkpoint::checksum(s.as_bytes())}
pub fn escape(s:&str)->String{let mut out=String::new();for c in s.chars(){match c{'"'=>out.push_str("\\\""),'\\'=>out.push_str("\\\\"),'\n'=>out.push_str("\\n"),'\r'=>out.push_str("\\r"),'\t'=>out.push_str("\\t"),c if c<' '=>{},c=>out.push(c)}}out}
#[derive(Clone,Default)]
pub struct AgentView {pub id:usize,pub name:String,pub online:bool,pub dimension:u64,pub position:[f64;3],pub health:f32,pub food:u32,pub steps:u64,pub cells:u64,pub deaths:u64,pub last_seen:u64,pub last_reward:f32,pub inventory_kinds:usize,pub stats_packets:u64,pub spawns:u64,pub disconnects:u64,pub policy_version:u64}
impl AgentView {
    pub fn json(&self)->String {
        format!(concat!("{{\"id\":{},\"name\":\"{}\",\"online\":{},",
            "\"dimension\":{},\"position\":[{:.3},{:.3},{:.3}],\"health\":{:.2},",
            "\"food\":{},\"steps\":{},\"cells\":{},\"deaths\":{},\"last_seen\":{},",
            "\"reward\":{:.5},\"inventory_kinds\":{},\"stats_packets\":{},",
            "\"spawns\":{},\"disconnects\":{},\"policy_version\":{}}}"),
            self.id,escape(&self.name),self.online,self.dimension,
            self.position[0],self.position[1],self.position[2],self.health,self.food,
            self.steps,self.cells,self.deaths,self.last_seen,self.last_reward,
            self.inventory_kinds,self.stats_packets,self.spawns,self.disconnects,self.policy_version)
    }
}
#[derive(Default,Clone)]
pub struct TrainingView {
    pub version:u64,pub samples:u64,pub dropped:u64,pub report:Report,
    pub last_update:u64,pub update_ms:u128,pub error:String,
    pub initial_version:u64,pub initial_samples:u64,pub optimizer_steps:u64,
    pub initial_optimizer_steps:u64,pub initial_fingerprint:u64,pub fingerprint:u64,
    pub resumed:bool,
}
pub fn write_status(root:&Path,mode:&str,run_id:&str,training:&TrainingView,agents:&[AgentView],frontier:usize)->io::Result<()> {
    let t=training;let r=&t.report;let a=agents.iter().map(AgentView::json).collect::<Vec<_>>().join(",");
    let json=format!(concat!("{{\"schema\":2,\"run_id\":\"{}\",\"updated\":{},\"mode\":\"{}\",",
        "\"version\":{},\"trained_samples\":{},\"dropped_rollouts\":{},",
        "\"last_update\":{},\"update_ms\":{},\"reward_mean\":{:.6},",
        "\"policy_loss\":{:.6},\"value_loss\":{:.6},\"entropy\":{:.6},",
        "\"kl\":{:.6},\"grad_norm\":{:.6},\"frontier_kinds\":{},\"error\":\"{}\",",
        "\"initial_version\":{},\"initial_samples\":{},\"optimizer_steps\":{},",
        "\"initial_optimizer_steps\":{},\"initial_fingerprint\":\"{:016x}\",",
        "\"fingerprint\":\"{:016x}\",\"resumed\":{},\"agents\":[{}]}}\n"),
        escape(run_id),now(),escape(mode),t.version,t.samples,t.dropped,t.last_update,t.update_ms,
        r.mean_reward,r.policy_loss,r.value_loss,r.entropy,r.kl,r.grad_norm,frontier,escape(&t.error),
        t.initial_version,t.initial_samples,t.optimizer_steps,t.initial_optimizer_steps,
        t.initial_fingerprint,t.fingerprint,t.resumed,a);
    atomic_write(&root.join("state/status.json"),json.as_bytes(),0)
}
pub struct CsvLog{path:PathBuf}
impl CsvLog{
    pub fn new(root:&Path)->io::Result<Self>{fs::create_dir_all(root.join("logs"))?;Ok(Self{path:root.join("logs/learning.csv")})}
    pub fn append(&mut self,t:&TrainingView)->io::Result<()>{
        if fs::metadata(&self.path).map(|m|m.len()>8*1024*1024).unwrap_or(false){
            for i in (1..4).rev(){let p=self.path.with_extension(format!("csv.{i}"));if p.exists(){fs::rename(p,self.path.with_extension(format!("csv.{}",i+1)))?;}}
            fs::rename(&self.path,self.path.with_extension("csv.1"))?;
        }
        let empty=fs::metadata(&self.path).map(|m|m.len()==0).unwrap_or(true);let mut f=OpenOptions::new().create(true).append(true).open(&self.path)?;
        if empty{writeln!(f,"unix_time,version,trained_samples,batch_samples,mean_reward,policy_loss,value_loss,entropy,kl,clip_fraction,gradient_norm,update_ms,dropped_rollouts")?;}
        let r=&t.report;writeln!(f,"{},{},{},{},{},{},{},{},{},{},{},{},{}",now(),t.version,t.samples,r.samples,r.mean_reward,r.policy_loss,r.value_loss,r.entropy,r.kl,r.clip_fraction,r.grad_norm,t.update_ms,t.dropped)?;f.flush()
    }
}
