//! One atomic checkpoint binds neural weights, Adam, trainer/actor RNG and course.
//! A corrupt or incompatible file is an error, never a request to restart training.
use std::{fs::File,io::{self,Read},path::Path};
use super::{checkpoint::{Checkpoint,Encoder,Decoder,checksum,atomic_write},next::curriculum::{Curriculum,PolicyId}};
const MAGIC:&[u8;8]=b"BCBNDL02";
const MAX_BYTES:u64=65*1024*1024;
fn bad(s:&str)->io::Error{io::Error::new(io::ErrorKind::InvalidData,s)}
#[derive(Clone)]
pub struct Bundle{pub checkpoint:Checkpoint,pub curriculum:Curriculum,pub actor_rngs:Vec<u64>}
pub fn policy_id(cp:&Checkpoint)->PolicyId{PolicyId{version:cp.model.version,signature:cp.model.fingerprint()}}
impl Bundle {
    pub fn encode(&self)->io::Result<Vec<u8>> {
        if self.actor_rngs.len()!=self.curriculum.bots() || !(1..=64).contains(&self.actor_rngs.len()){return Err(bad("bundle actor count mismatch"));}
        let cp=self.checkpoint.encode();
        let course=self.curriculum.checkpoint(policy_id(&self.checkpoint)).map_err(bad)?;
        let mut e=Encoder(MAGIC.to_vec());e.u64(cp.len() as u64);e.u64(course.len() as u64);e.u64(self.actor_rngs.len() as u64);
        e.0.extend(cp);e.0.extend(course);for &r in &self.actor_rngs{e.u64(r);}
        let sum=checksum(&e.0);e.u64(sum);
        if e.0.len() as u64>MAX_BYTES{return Err(bad("bundle too large"));}Ok(e.0)
    }
    pub fn decode(bytes:&[u8],bots:usize,new_run:u64)->io::Result<Self>{
        if bytes.len()<40 || bytes.len() as u64>MAX_BYTES || !(1..=64).contains(&bots){return Err(bad("bundle size/population"));}
        let body=&bytes[..bytes.len()-8];let sum=u64::from_le_bytes(bytes[bytes.len()-8..].try_into().unwrap());
        if checksum(body)!=sum{return Err(bad("bundle corruption; restore a complete backup explicitly"));}
        let mut d=Decoder{bytes:body,pos:0};
        if d.take(8)?!=MAGIC{return Err(bad("bundle schema"));}
        let cp_len=d.size(64*1024*1024)?;let course_len=d.size(256*1024)?;let count=d.size(64)?;
        if count!=bots{return Err(bad("bundle population differs; no implicit weight reuse"));}
        let checkpoint=Checkpoint::decode(d.take(cp_len)?)?;
        let curriculum=Curriculum::from_checkpoint(d.take(course_len)?,policy_id(&checkpoint),new_run).map_err(bad)?;
        if curriculum.bots()!=bots{return Err(bad("course/actor mismatch"));}
        let mut actor_rngs=Vec::with_capacity(bots);for _ in 0..bots{actor_rngs.push(d.u64()?);}
        if d.pos!=body.len(){return Err(bad("trailing bundle data"));}
        Ok(Self{checkpoint,curriculum,actor_rngs})
    }
    pub fn load(path:&Path,bots:usize,new_run:u64)->io::Result<Self>{
        let mut bytes=Vec::new();File::open(path)?.take(MAX_BYTES+1).read_to_end(&mut bytes)?;
        Self::decode(&bytes,bots,new_run)
    }
    pub fn save(&self,path:&Path)->io::Result<()>{atomic_write(path,&self.encode()?,3)}
}
#[cfg(test)] mod tests {
    use super::*;use super::super::network::Model;
    fn fixture()->Bundle{Bundle{checkpoint:Checkpoint::new(Model::new(2,4,vec![2],9),88),curriculum:Curriculum::new(7,64,100).unwrap(),actor_rngs:(0..64).collect()}}
    #[test]fn all_64_actor_states_round_trip_in_one_file(){let b=fixture();let bytes=b.encode().unwrap();assert!(bytes.len()>64*1024);let r=Bundle::decode(&bytes,64,200).unwrap();assert_eq!(r.actor_rngs,b.actor_rngs);assert_eq!(r.checkpoint.encode(),b.checkpoint.encode());assert_eq!(r.curriculum.bots(),64);assert_eq!(r.curriculum.generation(),b.curriculum.generation()+1);}
    #[test]fn no_partial_or_wrong_population_restore(){let bytes=fixture().encode().unwrap();for end in [0,7,32,1000,bytes.len()-1]{assert!(Bundle::decode(&bytes[..end],64,1).is_err());}assert!(Bundle::decode(&bytes,32,1).is_err());let mut corrupt=bytes;corrupt[99]^=1;assert!(Bundle::decode(&corrupt,64,1).is_err());}
    #[test]fn a_model_course_mismatch_is_not_saved(){let mut b=fixture();b.curriculum.begin_evaluation(policy_id(&b.checkpoint)).unwrap();b.checkpoint.model.version+=1;assert!(b.encode().is_err());}
}
