use std::{fs::{self,File},io::{self,Read,Write},path::Path};
use super::{network::Model,ppo::Adam,rng::Rng,SCHEMA};
const MAGIC:&[u8;8]=b"BCRL0001";
const MAX_BYTES:u64=64*1024*1024;
#[derive(Clone)]
pub struct Checkpoint { pub model:Model,pub adam:Adam,pub rng:Rng,pub samples:u64 }
pub fn checksum(bytes:&[u8])->u64 { bytes.iter().fold(0xcbf29ce484222325u64,|h,b|(h^*b as u64).wrapping_mul(0x100000001b3)) }
fn invalid(message:&str)->io::Error {io::Error::new(io::ErrorKind::InvalidData,message)}
pub struct Encoder(pub Vec<u8>);
impl Encoder {
    pub fn u64(&mut self,v:u64){self.0.extend_from_slice(&v.to_le_bytes());}
    pub fn f32(&mut self,v:f32){self.0.extend_from_slice(&v.to_le_bytes());}
    pub fn floats(&mut self,v:&[f32]){self.u64(v.len() as u64);for &x in v {self.f32(x);}}
}
pub struct Decoder<'a>{pub bytes:&'a [u8],pub pos:usize}
impl<'a> Decoder<'a>{
    pub fn take(&mut self,n:usize)->io::Result<&'a [u8]>{
        if n>self.bytes.len().saturating_sub(self.pos){return Err(invalid("truncated checkpoint"));}
        let out=&self.bytes[self.pos..self.pos+n];self.pos+=n;Ok(out)
    }
    pub fn u64(&mut self)->io::Result<u64>{Ok(u64::from_le_bytes(self.take(8)?.try_into().unwrap()))}
    pub fn size(&mut self,max:usize)->io::Result<usize>{let n=self.u64()?;if n>max as u64{return Err(invalid("checkpoint allocation limit"));}Ok(n as usize)}
    pub fn f32(&mut self)->io::Result<f32>{let f=f32::from_le_bytes(self.take(4)?.try_into().unwrap());if !f.is_finite(){return Err(invalid("non-finite checkpoint"));}Ok(f)}
    pub fn floats(&mut self,max:usize)->io::Result<Vec<f32>>{let n=self.size(max)?;let mut out=Vec::with_capacity(n);for _ in 0..n{out.push(self.f32()?);}Ok(out)}
}
impl Checkpoint {
    pub fn new(model:Model,seed:u64)->Self{let adam=Adam::new(model.weights.len());Self{model,adam,rng:Rng(seed),samples:0}}
    pub fn encode(&self)->Vec<u8>{
        let mut e=Encoder(MAGIC.to_vec());e.u64(SCHEMA as u64);
        e.u64(self.model.input as u64);e.u64(self.model.hidden as u64);e.u64(self.model.heads.len() as u64);
        for &n in &self.model.heads{e.u64(n as u64);}
        e.u64(self.model.version);e.u64(self.samples);e.u64(self.rng.0);e.u64(self.adam.step);
        e.floats(&self.model.weights);e.floats(&self.adam.m);e.floats(&self.adam.v);
        let hash=checksum(&e.0);e.u64(hash);e.0
    }
    pub fn decode(bytes:&[u8])->io::Result<Self>{
        if bytes.len()<24||bytes.len() as u64>MAX_BYTES{return Err(invalid("checkpoint size"));}
        let body=&bytes[..bytes.len()-8];let hash=u64::from_le_bytes(bytes[bytes.len()-8..].try_into().unwrap());
        if checksum(body)!=hash{return Err(invalid("checkpoint checksum mismatch; restore backup explicitly"));}
        let mut d=Decoder{bytes:body,pos:0};if d.take(8)?!=MAGIC{return Err(invalid("checkpoint magic"));}
        if d.u64()?!=SCHEMA as u64{return Err(invalid("checkpoint schema mismatch"));}
        let input=d.size(4096)?;let hidden=d.size(512)?;let count=d.size(32)?;let mut heads=vec![];
        for _ in 0..count{heads.push(d.size(1024)?);}
        let version=d.u64()?;let samples=d.u64()?;let rng=Rng(d.u64()?);let step=d.u64()?;
        let weights=d.floats(4_000_000)?;let m=d.floats(4_000_000)?;let v=d.floats(4_000_000)?;
        let model=Model{input,hidden,heads,weights,version};
        if !model.validate()||m.len()!=model.weights.len()||v.len()!=m.len()||v.iter().any(|x|*x<0.0)||d.pos!=body.len(){return Err(invalid("checkpoint structural validation"));}
        Ok(Self{model,adam:Adam{m,v,step},rng,samples})
    }
    pub fn load(path:&Path)->io::Result<Self>{let f=File::open(path)?;let mut bytes=vec![];f.take(MAX_BYTES+1).read_to_end(&mut bytes)?;Self::decode(&bytes)}
    pub fn save(&self,path:&Path)->io::Result<()>{atomic_write(path,&self.encode(),3)}
}
/// File and directory fsync, atomic rename, bounded backup retention. No silent reset.
pub fn atomic_write(path:&Path,bytes:&[u8],backups:usize)->io::Result<()>{
    let parent=path.parent().unwrap_or(Path::new("."));fs::create_dir_all(parent)?;
    let tmp=path.with_extension("tmp");
    {let mut f=File::create(&tmp)?;f.write_all(bytes)?;f.sync_all()?;}
    if backups>0 {
        for i in (1..backups).rev(){let a=path.with_extension(format!("bak{i}"));let b=path.with_extension(format!("bak{}",i+1));if a.exists(){fs::rename(a,b)?;}}
        if path.exists(){fs::copy(path,path.with_extension("bak1"))?;}
    }
    fs::rename(tmp,path)?;File::open(parent)?.sync_all()?;Ok(())
}
#[cfg(test)] mod tests{
    use super::*;
    #[test] fn checkpoint_round_trip_includes_optimizer_and_rng(){
        let mut c=Checkpoint::new(Model::new(3,4,vec![2,3],19),123);c.adam.m[0]=0.4;c.adam.v[1]=0.7;c.adam.step=9;c.samples=1234;
        let d=Checkpoint::decode(&c.encode()).unwrap();assert_eq!(c.model.weights,d.model.weights);assert_eq!(c.adam.m,d.adam.m);assert_eq!(d.rng.0,123);assert_eq!(d.samples,1234);assert_eq!(d.adam.step,9);
    }
    #[test] fn truncation_and_corruption_fail_closed(){let c=Checkpoint::new(Model::new(2,2,vec![2],1),1);let b=c.encode();for len in 0..b.len(){assert!(Checkpoint::decode(&b[..len]).is_err());}let mut b=b;b[30]^=1;assert!(Checkpoint::decode(&b).is_err());}
}
