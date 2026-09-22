//! Rewards evaluate outcomes; they NEVER choose an action or supply demonstrations.
//! The progress frontier is the greatest single-agent cumulative Mined/Crafted
//! counter received from the server. Pickup/drop/used counters are excluded.
//! Repeated receipts and counter resets cannot mint the same progress twice.
use std::{collections::BTreeMap,io,path::Path};
use super::checkpoint::{Encoder,Decoder,atomic_write,checksum};
const BLOOM_WORDS:usize=4096; // 32 KiB per agent; bounded, no eviction farming.
#[derive(Clone,Debug)]
pub struct AgentLedger { pub bloom:Vec<u64>,pub cells:u64,pub deaths:u64,pub last_team:f32 }
impl Default for AgentLedger {fn default()->Self{Self{bloom:vec![0;BLOOM_WORDS],cells:0,deaths:0,last_team:0.0}}}
#[derive(Clone,Debug)]
pub struct RewardBook {
    pub agents:Vec<AgentLedger>,pub frontier:BTreeMap<u32,u32>,pub team_total:f32,pub active:usize,
}
#[derive(Default,Clone,Copy,Debug)]
pub struct RewardParts {pub exploration:f32,pub progress:f32,pub team:f32,pub damage:f32,pub death:f32}
impl RewardParts {pub fn total(self)->f32{self.exploration+self.progress+self.team+self.damage+self.death}}
fn mix(mut n:u64)->u64{n=(n^(n>>30)).wrapping_mul(0xbf58476d1ce4e5b9);n=(n^(n>>27)).wrapping_mul(0x94d049bb133111eb);n^(n>>31)}
impl AgentLedger {
    pub fn visit(&mut self,dimension:u64,x:i32,y:i32,z:i32)->bool {
        let key=mix(dimension)^mix(x.div_euclid(8) as u64)^mix((y.div_euclid(4) as u64).wrapping_add(1234567))^mix((z.div_euclid(8) as u64).wrapping_add(987654321));
        let mut seen=true;
        for salt in [17u64,117,1117]{let bit=mix(key.wrapping_add(salt)) as usize%(BLOOM_WORDS*64);let word=&mut self.bloom[bit/64];let mask=1u64<<(bit%64);seen &= *word&mask!=0;*word|=mask;}
        if !seen{self.cells+=1;}!seen
    }
}
impl RewardBook {
    pub fn new(n:usize)->Self{Self{agents:vec![AgentLedger::default();n],frontier:BTreeMap::new(),team_total:0.0,active:n}}
    pub fn resize(&mut self,n:usize){
        assert!((1..=32).contains(&n));
        let mut a=AgentLedger::default();a.last_team=self.team_total;
        if n>self.agents.len(){self.agents.resize(n,a);}
        // Retain retired agents' exploration/death history, but distribute new
        // team credit only among the configured population. Never award an
        // inactive agent historical team credit for rejoining.
        self.active=n;
        for a in &mut self.agents{a.last_team=self.team_total;}
    }
    #[allow(clippy::too_many_arguments)]
    pub fn observe(&mut self,id:usize,dim:u64,pos:[f64;3],progress:&[(u32,u32)],lost_health:f32,died:bool)->RewardParts{
        assert!(id<self.active);let mut r=RewardParts::default();
        if self.agents[id].visit(dim,pos[0].floor() as i32,pos[1].floor() as i32,pos[2].floor() as i32){r.exploration=0.01;}
        let mut gain=0.0;
        for &(kind,count) in progress {
            if kind==0{continue;}let count=count.min(64);
            let old=self.frontier.entry(kind).or_insert(0);
            if count>*old {gain+=0.06*((count as f32+1.0).ln()-(*old as f32+1.0).ln());*old=count;}
        }
        gain=gain.min(0.5);r.progress=gain*0.7;self.team_total+=gain*0.3;
        r.team=(self.team_total-self.agents[id].last_team).max(0.0)/self.active as f32;
        self.agents[id].last_team=self.team_total;
        r.damage=-0.03*lost_health.clamp(0.0,20.0);
        if died{r.death=-1.0;self.agents[id].deaths+=1;}
        r
    }
    pub fn save(&self,path:&Path)->io::Result<()>{
        let mut e=Encoder(b"BCREW001".to_vec());e.f32(self.team_total);e.u64(self.frontier.len() as u64);
        for (&k,&v) in &self.frontier{e.u64(k as u64);e.u64(v as u64);}
        e.u64(self.agents.len() as u64);
        for a in &self.agents{e.u64(a.cells);e.u64(a.deaths);e.f32(a.last_team);for &w in &a.bloom{e.u64(w);}}
        let h=checksum(&e.0);e.u64(h);atomic_write(path,&e.0,3)
    }
    pub fn load(path:&Path)->io::Result<Self>{
        use std::io::Read;
        let mut b=vec![];std::fs::File::open(path)?.take(4*1024*1024+1).read_to_end(&mut b)?;
        let bad=||io::Error::new(io::ErrorKind::InvalidData,"reward ledger corrupted or incompatible; no automatic reset");
        if b.len()<24||b.len()>4*1024*1024{return Err(bad());}
        let body=&b[..b.len()-8];if checksum(body)!=u64::from_le_bytes(b[b.len()-8..].try_into().unwrap()){return Err(bad());}
        let mut d=Decoder{bytes:body,pos:0};if d.take(8)?!=b"BCREW001"{return Err(bad());}
        let team_total=d.f32()?;let len=d.size(65536)?;let mut frontier=BTreeMap::new();
        for _ in 0..len{let k=d.size(u32::MAX as usize)? as u32;let v=d.size(64)? as u32;frontier.insert(k,v);}
        let n=d.size(32)?;if n==0{return Err(bad());}let mut agents=vec![];
        for _ in 0..n{let cells=d.u64()?;let deaths=d.u64()?;let last_team=d.f32()?;let mut bloom=vec![];for _ in 0..BLOOM_WORDS{bloom.push(d.u64()?);}agents.push(AgentLedger{bloom,cells,deaths,last_team});}
        if d.pos!=body.len(){return Err(bad());}Ok(Self{agents,frontier,team_total,active:n})
    }
}
#[cfg(test)]mod tests{
    use super::*;
    #[test]fn repeated_progress_receipts_and_resets_do_not_reward_again(){let mut b=RewardBook::new(2);let p=[0.0,64.0,0.0];assert!(b.observe(0,1,p,&[(17,8)],0.0,false).progress>0.0);assert_eq!(b.observe(0,1,p,&[],0.0,false).progress,0.0);assert_eq!(b.observe(1,1,p,&[(17,8)],0.0,false).progress,0.0);assert_eq!(b.observe(0,1,p,&[(17,8)],0.0,false).progress,0.0);}
    #[test]fn repeat_visits_and_negative_coordinates(){let mut a=AgentLedger::default();assert!(a.visit(1,-1,64,0));assert!(!a.visit(1,-1,64,0));assert!(a.visit(1,1,64,0));assert_eq!(a.cells,2);}
    #[test]fn death_does_not_clear_progress_frontier(){let mut b=RewardBook::new(1);let p=[0.0,64.0,0.0];b.observe(0,1,p,&[(17,8)],0.0,false);let r=b.observe(0,1,p,&[],20.0,true);assert!(r.total()<0.0);assert_eq!(b.observe(0,1,p,&[(17,8)],0.0,false).progress,0.0);assert_eq!(b.agents[0].deaths,1);}

    #[test]fn shrinking_population_does_not_dilute_credit_with_retired_agents(){
        let mut b=RewardBook::new(4);let pos=[0.0,64.0,0.0];
        b.observe(0,1,pos,&[(17,4)],0.0,false);b.resize(2);
        assert_eq!(b.agents.len(),4);assert_eq!(b.active,2);
        assert_eq!(b.observe(1,1,pos,&[],0.0,false).team,0.0);
        let before=b.team_total;let r=b.observe(0,1,pos,&[(18,4)],0.0,false);
        assert!((r.team-(b.team_total-before)/2.0).abs()<1e-7);
    }
}
