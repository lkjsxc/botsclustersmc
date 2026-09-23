//! A strict, bounded on-policy collection barrier.
//!
//! Actors keep the same behaviour policy until their quota has been reached AND
//! a real episode has ended. They then stop before starting another episode.
//! Inference does not pause halfway through an episode. A learner may train only
//! after all configured actors have sealed their contribution. A missing actor
//! is an infrastructure failure, not permission to train on an accidental subset.
//!
//! This module does not claim synchronous collection is always faster. Its
//! throughput/straggler tradeoff must be measured in Folia before adoption.
use super::{self as shared_root};
use shared_root::Result;

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct Round { pub policy_version: u64, pub generation: u64 }

#[derive(Debug)]
pub struct Packet<T> {
    pub round: Round,
    pub actor: usize,
    pub sequence: u64,
    pub evaluation: bool,
    pub samples: Vec<T>,
    pub ends_episode: bool,
    pub seal: bool,
}

#[derive(Clone, Copy, Debug, Default, PartialEq, Eq)]
pub struct ActorProgress {
    pub samples: usize,
    pub next_sequence: u64,
    pub sealed: bool,
    pub at_episode_boundary: bool,
}

#[derive(Debug)]
pub struct Batch<T> {
    pub round: Round,
    /// Kept in actor/temporal order; never run GAE across actor boundaries.
    pub trajectories: Vec<Vec<T>>,
    pub fragments: u64,
}
impl<T> Batch<T> {
    pub fn len(&self) -> usize { self.trajectories.iter().map(Vec::len).sum() }
    pub fn is_empty(&self) -> bool { self.len() == 0 }
}

#[derive(Debug)]
pub struct Cohort<T> {
    round: Round,
    quota: usize,
    max_per_actor: usize,
    actors: Vec<ActorProgress>,
    data: Vec<Vec<T>>,
    fragments: u64,
    taken: bool,
}
impl<T> Cohort<T> {
    pub fn new(round: Round, actors: usize, quota: usize, max_episode_samples: usize) -> Result<Self> {
        if !(1..=64).contains(&actors) || quota == 0 || max_episode_samples == 0 {
            return Err("invalid cohort dimensions");
        }
        let cap = quota.checked_add(max_episode_samples).ok_or("cohort capacity overflow")?;
        if cap > 100_000 || cap.checked_mul(actors).is_none() { return Err("cohort capacity too large"); }
        Ok(Self { round, quota, max_per_actor: cap,
            actors: vec![ActorProgress::default(); actors],
            data: (0..actors).map(|_| Vec::new()).collect(), fragments: 0, taken: false })
    }
    pub fn round(&self) -> Round { self.round }
    pub fn progress(&self) -> &[ActorProgress] { &self.actors }
    pub fn quota(&self) -> usize { self.quota }
    pub fn ready(&self) -> bool { self.actors.iter().all(|a| a.sealed) }
    pub fn outstanding_actors(&self) -> Vec<usize> {
        self.actors.iter().enumerate().filter_map(|(i, a)| (!a.sealed).then_some(i)).collect()
    }
    pub fn len(&self) -> usize { self.actors.iter().map(|a| a.samples).sum() }
    pub fn is_empty(&self) -> bool { self.len() == 0 }

    /// Validate BEFORE mutating. The caller must validate payload dimensions,
    /// finiteness, behaviour log-probabilities and transition chronology too.
    /// The packet's ends_episode flag must match its final transition boundary.
    /// Any returned error must be surfaced; this API never silently drops it.
    pub fn push(&mut self, p: Packet<T>, validate: impl FnOnce(&[T]) -> Result<()>) -> Result<()> {
        if self.taken { return Err("batch already taken; policy publication required"); }
        if p.evaluation { return Err("evaluation experience cannot enter PPO"); }
        if p.round != self.round { return Err("wrong policy version or curriculum generation"); }
        let a = self.actors.get(p.actor).ok_or("unknown actor")?;
        if a.sealed { return Err("actor already sealed"); }
        if p.sequence != a.next_sequence { return Err("missing, duplicated or reordered fragment"); }
        if p.samples.is_empty() && (!p.seal || !a.at_episode_boundary) {
            return Err("empty seal cannot invent an unobserved episode terminal");
        }
        if !p.samples.is_empty() && a.at_episode_boundary && a.samples >= self.quota {
            return Err("actor started another episode instead of waiting at the cohort barrier");
        }
        let count = a.samples.checked_add(p.samples.len()).ok_or("sample count overflow")?;
        if count > self.max_per_actor { return Err("actor exceeded bounded episode capacity"); }
        if p.seal && (!p.ends_episode || count < self.quota) {
            return Err("seal requires quota and a genuine episode boundary");
        }
        let sequence = a.next_sequence.checked_add(1).ok_or("sequence exhausted")?;
        let fragments = self.fragments.checked_add(1).ok_or("fragment counter exhausted")?;
        validate(&p.samples)?;
        self.data[p.actor].extend(p.samples);
        self.actors[p.actor] = ActorProgress { samples: count, next_sequence: sequence,
            sealed: p.seal, at_episode_boundary: p.ends_episode };
        self.fragments = fragments;
        Ok(())
    }
    /// Taking does NOT release actors or increment a policy version. Keep this
    /// batch until the guarded optimizer transaction has succeeded.
    pub fn take_ready(&mut self) -> Result<Batch<T>> {
        if !self.ready() { return Err("not every actor has sealed its episode"); }
        if self.taken { return Err("batch already taken"); }
        self.taken = true;
        let data = std::mem::replace(&mut self.data, (0..self.actors.len()).map(|_| Vec::new()).collect());
        Ok(Batch { round: self.round, trajectories: data, fragments: self.fragments })
    }
    /// Call only AFTER successful optimization/persistence. Publish the model and
    /// this release under the adapter's coordination lock; actors must obtain the
    /// model matching the released round, not an independently cached pointer.
    pub fn commit(&mut self, next: Round) -> Result<()> {
        if !self.taken || !self.ready() { return Err("no completed optimizer transaction to commit"); }
        if next.generation != self.round.generation ||
            Some(next.policy_version) != self.round.policy_version.checked_add(1) {
            return Err("commit must advance exactly one policy version in the same generation");
        }
        self.round = next;
        self.actors.fill(ActorProgress::default());
        self.fragments = 0;
        self.taken = false;
        Ok(())
    }
    /// An explicit phase/reset cancellation returns every buffered sample for an
    /// auditable discarded-sample count. Do not use this as a timed partial update.
    /// An already-taken batch belongs to the optimizer and cannot be cancelled here.
    pub fn cancel_for_generation(&mut self, next: Round) -> Result<Batch<T>> {
        if self.taken { return Err("optimizer owns batch; cancellation must be coordinated"); }
        if next.generation <= self.round.generation || next.policy_version != self.round.policy_version {
            return Err("phase cancellation changes generation, not behaviour policy");
        }
        let old = self.round;
        let data = std::mem::replace(&mut self.data, (0..self.actors.len()).map(|_| Vec::new()).collect());
        let fragments = self.fragments;
        self.round = next;
        self.actors.fill(ActorProgress::default()); self.fragments = 0;
        Ok(Batch { round: old, trajectories: data, fragments })
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    fn r() -> Round { Round { policy_version: 7, generation: 2 } }
    fn packet(actor: usize, seq: u64, n: usize, seal: bool) -> Packet<u32> {
        Packet { round: r(), actor, sequence: seq, evaluation: false,
            samples: vec![actor as u32; n], ends_episode: seal, seal }
    }
    #[test] fn all_32_actors_contribute_and_no_samples_vanish() {
        let mut c = Cohort::new(r(), 32, 64, 300).unwrap();
        for i in (0..32).rev() { c.push(packet(i, 0, 32, false), |_| Ok(())).unwrap(); }
        assert_eq!(c.len(), 1024); assert!(!c.ready());
        for i in 0..31 { c.push(packet(i, 1, 40, true), |_| Ok(())).unwrap(); }
        assert_eq!(c.outstanding_actors(), [31]); assert!(c.take_ready().is_err());
        c.push(packet(31, 1, 40, true), |_| Ok(())).unwrap();
        let b = c.take_ready().unwrap(); assert_eq!(b.len(), 2304); assert_eq!(b.fragments, 64);
        for (i, ts) in b.trajectories.iter().enumerate() { assert_eq!(ts.len(), 72); assert!(ts.iter().all(|x| *x == i as u32)); }
        assert!(c.take_ready().is_err());
        c.commit(Round { policy_version: 8, generation: 2 }).unwrap();
        assert_eq!(c.len(), 0); assert_eq!(c.outstanding_actors().len(), 32);
    }
    #[test] fn rejects_wrong_version_generation_and_exam_without_mutation() {
        let mut c = Cohort::new(r(), 1, 4, 20).unwrap();
        for kind in 0..5 {
            let mut p = packet(0, 0, 4, true);
            match kind { 0 => p.round.policy_version -= 1, 1 => p.round.policy_version += 1,
                2 => p.round.generation += 1, 3 => p.evaluation = true, _ => p.actor = 32 }
            assert!(c.push(p, |_| Ok(())).is_err()); assert!(c.is_empty());
            assert_eq!(c.progress()[0], ActorProgress::default());
        }
    }
    #[test] fn requires_real_terminal_after_quota() {
        let mut c = Cohort::new(r(), 1, 4, 20).unwrap();
        let mut p = packet(0, 0, 4, true); p.ends_episode = false;
        assert!(c.push(p, |_| Ok(())).is_err());
        assert!(c.push(packet(0, 0, 3, true), |_| Ok(())).is_err());
        let mut ended = packet(0, 0, 4, false); ended.ends_episode = true;
        c.push(ended, |_| Ok(())).unwrap(); assert!(!c.ready());
        c.push(packet(0, 1, 0, true), |_| Ok(())).unwrap(); assert!(c.ready());
    }
    #[test] fn empty_seal_cannot_invent_a_terminal_reward_transition() {
        let mut c = Cohort::new(r(), 1, 4, 20).unwrap();
        c.push(packet(0, 0, 4, false), |_| Ok(())).unwrap();
        assert!(c.push(packet(0, 1, 0, true), |_| Ok(())).is_err());
        assert_eq!(c.len(), 4); assert_eq!(c.progress()[0].next_sequence, 1);
        c.push(packet(0, 1, 1, true), |_| Ok(())).unwrap(); assert!(c.ready());
    }
    #[test] fn completed_quota_requires_a_barrier_before_another_episode() {
        let mut c = Cohort::new(r(), 1, 4, 20).unwrap();
        let mut ended = packet(0, 0, 4, false); ended.ends_episode = true;
        c.push(ended, |_| Ok(())).unwrap();
        assert!(c.push(packet(0, 1, 1, false), |_| Ok(())).is_err());
        c.push(packet(0, 1, 0, true), |_| Ok(())).unwrap(); assert!(c.ready());
    }
    #[test] fn duplicate_gap_payload_and_capacity_errors_are_atomic() {
        let mut c = Cohort::new(r(), 1, 4, 20).unwrap();
        c.push(packet(0, 0, 3, false), |_| Ok(())).unwrap();
        for seq in [0, 2] { assert!(c.push(packet(0, seq, 1, false), |_| Ok(())).is_err()); }
        assert!(c.push(packet(0, 1, 1, false), |_| Err("bad float")).is_err());
        assert!(c.push(packet(0, 1, 22, false), |_| Ok(())).is_err());
        assert_eq!(c.len(), 3); assert_eq!(c.progress()[0].next_sequence, 1);
        c.push(packet(0, 1, 1, true), |_| Ok(())).unwrap();
        assert!(c.push(packet(0, 2, 1, true), |_| Ok(())).is_err());
    }
    #[test] fn phase_cancellation_is_explicit_and_preserves_accounting() {
        let mut c = Cohort::new(r(), 2, 4, 20).unwrap();
        c.push(packet(0, 0, 4, true), |_| Ok(())).unwrap();
        c.push(packet(1, 0, 2, false), |_| Ok(())).unwrap();
        let b = c.cancel_for_generation(Round { policy_version: 7, generation: 3 }).unwrap();
        assert_eq!(b.len(), 6); assert_eq!(b.fragments, 2); assert!(c.is_empty());
        assert!(c.cancel_for_generation(Round { policy_version: 8, generation: 4 }).is_err());
    }
    #[test] fn cannot_publish_untrained_or_skip_versions() {
        let mut c = Cohort::new(r(), 1, 1, 2).unwrap();
        assert!(c.commit(Round { policy_version: 8, generation: 2 }).is_err());
        c.push(packet(0, 0, 1, true), |_| Ok(())).unwrap();
        assert!(c.commit(Round { policy_version: 8, generation: 2 }).is_err());
        let _batch = c.take_ready().unwrap();
        assert!(c.commit(Round { policy_version: 9, generation: 2 }).is_err());
        assert!(c.commit(Round { policy_version: 8, generation: 3 }).is_err());
        assert!(c.cancel_for_generation(Round { policy_version: 7, generation: 3 }).is_err());
        c.commit(Round { policy_version: 8, generation: 2 }).unwrap();
    }
    #[test] fn invalid_dimensions_and_missing_actors_never_pass() {
        for n in [0, 33] { assert!(Cohort::<u8>::new(r(), n, 1, 1).is_err()); }
        assert!(Cohort::<u8>::new(r(), 1, usize::MAX, 2).is_err());
        let mut c = Cohort::new(r(), 2, 1, 1).unwrap();
        c.push(packet(0, 0, 1, true), |_| Ok(())).unwrap();
        for _ in 0..100 { assert!(c.take_ready().is_err()); }
    }
}
