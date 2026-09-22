use super::*;
const MAGIC:&[u8;8]=b"BCMCLP02";
fn put(out:&mut Vec<u8>,x:u64){out.extend_from_slice(&x.to_le_bytes());}
fn read(bytes:&[u8],at:&mut usize)->Result<u64>{let end=at.checked_add(8).ok_or("checkpoint offset overflow")?;
    let data=bytes.get(*at..end).ok_or("truncated curriculum checkpoint")?;let x=u64::from_le_bytes(data.try_into().map_err(|_|"bad checkpoint word")?);*at=end;Ok(x)}
/// Small accidental-corruption check, not authentication, not a security hash.
fn checksum(bytes:&[u8])->u64{bytes.iter().fold(0xcbf29ce484222325,|h,b|(h^*b as u64).wrapping_mul(0x100000001b3))}
fn flag(x:u64)->Result<bool>{match x{0=>Ok(false),1=>Ok(true),_=>Err("invalid checkpoint flag")}}
impl Curriculum {
    /// Pair these bytes atomically with the exact model/optimizer checkpoint.
    /// In-flight lessons are NOT replayable transitions and are not serialized.
    /// Their issued serials ARE persisted, so restart cannot reuse old task tokens.
    pub fn checkpoint(&self,policy:PolicyId)->Result<Vec<u8>>{
        if let Phase::Exam{frozen}=self.phase{if frozen!=policy{return Err("checkpoint is not the frozen exam policy");}}
        let mut out=MAGIC.to_vec();
        for x in [policy.version,policy.signature,self.seed,self.frontier as u64,self.generation,self.actors.len() as u64,
            self.examinations,self.ever_completed as u64,self.last_exam_passed as u64,matches!(self.phase,Phase::Exam{..}) as u64]{put(&mut out,x);}
        for a in &self.actors {
            for x in [a.serial,a.training_choices,a.frontier_choices,a.trained_since_exam,a.full_since_exam]{put(&mut out,x);}
            for s in a.skills {
                if !s.valid(){return Err("invalid skill statistics at checkpoint");}
                for x in [s.attempts,s.full_attempts,s.window_attempts,s.window_successes]{put(&mut out,x);}
                for x in [s.fast_success,s.slow_success,s.difficulty,s.full_success,s.validation_weakness]{put(&mut out,x.to_bits());}
            }
        }
        let sum=checksum(&out);put(&mut out,sum);Ok(out)
    }
    /// Restore weights/Adam/RNG separately BEFORE calling this. The caller supplies
    /// the actual restored policy ID, not metadata copied from this checkpoint.
    /// Every restart gets a fresh run and generation. A partial exam restarts from
    /// zero using the SAME frozen policy and new evaluation seeds, never partial
    /// cherry-picked successes. Completed training statistics remain intact.
    pub fn from_checkpoint(bytes:&[u8],expected:PolicyId,new_run:u64)->Result<Self>{
        if bytes.len()<96 || bytes.len()>64*1024 || bytes.get(..8)!=Some(MAGIC.as_slice()){return Err("wrong curriculum checkpoint schema or size");}
        let mut tail=bytes.len()-8;let claimed=read(bytes,&mut tail)?;
        if checksum(&bytes[..bytes.len()-8])!=claimed{return Err("curriculum checkpoint is corrupted");}
        let mut at=8;let policy=PolicyId{version:read(bytes,&mut at)?,signature:read(bytes,&mut at)?};
        if policy!=expected{return Err("curriculum/model checkpoint pair mismatch");}
        let seed=read(bytes,&mut at)?;let frontier=usize::try_from(read(bytes,&mut at)?).map_err(|_|"frontier overflow")?;
        let generation=read(bytes,&mut at)?;let bots=usize::try_from(read(bytes,&mut at)?).map_err(|_|"actor count overflow")?;
        let examinations=read(bytes,&mut at)?;let ever_completed=flag(read(bytes,&mut at)?)?;
        let last_exam_passed=flag(read(bytes,&mut at)?)?;let exam=flag(read(bytes,&mut at)?)?;
        if !(1..=32).contains(&bots) || frontier>=TASK_COUNT || generation==0 ||
            frontier as u64>examinations || (ever_completed && frontier+1!=TASK_COUNT){return Err("invalid curriculum checkpoint metadata");}
        let expected_len=8+10*8+bots*(5+TASK_COUNT*9)*8+8;
        if bytes.len()!=expected_len{return Err("curriculum checkpoint length mismatch");}
        let mut c=Self::new(seed,bots,new_run)?;c.frontier=frontier;
        c.generation=generation.checked_add(1).ok_or("generation exhausted")?;
        c.examinations=examinations;c.ever_completed=ever_completed;c.last_exam_passed=last_exam_passed;
        c.phase=if exam{Phase::Exam{frozen:expected}}else{Phase::Training};
        for a in &mut c.actors {
            a.serial=read(bytes,&mut at)?;a.training_choices=read(bytes,&mut at)?;a.frontier_choices=read(bytes,&mut at)?;
            a.trained_since_exam=read(bytes,&mut at)?;a.full_since_exam=read(bytes,&mut at)?;
            for s in &mut a.skills {
                s.attempts=read(bytes,&mut at)?;s.full_attempts=read(bytes,&mut at)?;
                s.window_attempts=read(bytes,&mut at)?;s.window_successes=read(bytes,&mut at)?;
                s.fast_success=f64::from_bits(read(bytes,&mut at)?);s.slow_success=f64::from_bits(read(bytes,&mut at)?);
                s.difficulty=f64::from_bits(read(bytes,&mut at)?);s.full_success=f64::from_bits(read(bytes,&mut at)?);
                s.validation_weakness=f64::from_bits(read(bytes,&mut at)?);
                if !s.valid(){return Err("invalid restored skill statistics");}
            }
            if a.serial<a.training_choices || a.training_choices<a.frontier_choices || a.full_since_exam>a.trained_since_exam ||
                a.trained_since_exam>a.skills[frontier].attempts || a.full_since_exam>a.skills[frontier].full_attempts {
                return Err("inconsistent curriculum checkpoint counters");
            }
        }
        if at!=bytes.len()-8{return Err("unexpected curriculum checkpoint data");}Ok(c)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    fn p()->PolicyId{PolicyId{version:5,signature:321}}
    fn prepare()->Curriculum{let mut c=Curriculum::new(78,2,100).unwrap();for _ in 0..40{for id in 0..2{let l=c.issue(id,p()).unwrap().unwrap();c.record(&l,p(),true).unwrap();}}c}
    #[test] fn restore_preserves_adaptation_and_uses_fresh_context(){
        let mut c=prepare();let pending=c.issue(0,p()).unwrap().unwrap();let bytes=c.checkpoint(p()).unwrap();
        let mut restored=Curriculum::from_checkpoint(&bytes,p(),200).unwrap();assert_eq!(c.stats(0).unwrap(),restored.stats(0).unwrap());
        assert_eq!(restored.generation(),c.generation()+1);assert!(restored.pending(0).unwrap().is_none());
        let new=restored.issue(0,p()).unwrap().unwrap();assert!(new.session.lesson>pending.session.lesson);assert_eq!(new.session.run,200);
        assert!(restored.record(&pending,p(),true).is_err());
    }
    #[test] fn partial_exam_restarts_at_zero_with_same_policy(){
        let mut c=prepare();assert!(c.exam_ready());c.begin_exam(p()).unwrap();let l=c.issue(0,p()).unwrap().unwrap();c.record(&l,p(),true).unwrap();
        assert_eq!(c.scores(0).unwrap()[0].trials,1);let bytes=c.checkpoint(p()).unwrap();
        let mut r=Curriculum::from_checkpoint(&bytes,p(),201).unwrap();assert_eq!(r.phase(),Phase::Exam{frozen:p()});
        assert_eq!(r.scores(0).unwrap()[0].trials,0);let n=r.issue(0,p()).unwrap().unwrap();assert!(n.evaluation);assert_ne!(n.seed,l.seed);assert_eq!(n.policy,l.policy);
    }
    #[test] fn wrong_policy_truncation_corruption_and_trailing_data_are_rejected(){
        let c=prepare();let bytes=c.checkpoint(p()).unwrap();assert!(Curriculum::from_checkpoint(&bytes,PolicyId{version:6,..p()},3).is_err());
        assert!(Curriculum::from_checkpoint(&bytes,PolicyId{signature:322,..p()},3).is_err());
        for cut in [0,7,80,bytes.len()-1]{assert!(Curriculum::from_checkpoint(&bytes[..cut],p(),3).is_err());}
        let mut bad=bytes.clone();bad[150]^=1;assert!(Curriculum::from_checkpoint(&bad,p(),3).is_err());
        let mut extra=bytes;extra.push(0);assert!(Curriculum::from_checkpoint(&extra,p(),3).is_err());
    }
    #[test] fn oversized_actor_count_cannot_allocate_before_validation(){
        let mut bytes=prepare().checkpoint(p()).unwrap();bytes[48..56].copy_from_slice(&u64::MAX.to_le_bytes());
        let n=bytes.len();let sum=checksum(&bytes[..n-8]);bytes[n-8..].copy_from_slice(&sum.to_le_bytes());
        assert!(Curriculum::from_checkpoint(&bytes,p(),3).is_err());
    }
}
