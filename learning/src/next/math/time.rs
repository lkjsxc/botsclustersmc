use super::super::{self as shared_root};
use shared_root::Result;

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum Boundary {
    Continuing,
    /// Administrative cutoff: bootstrap the final visible state, but do not
    /// propagate the next episode's advantage backwards through this transition.
    Truncated,
    /// Genuine terminal state, including a finite-horizon task's stated deadline.
    Terminated,
}
#[derive(Clone, Copy, Debug)]
pub struct TimedValue {
    pub reward: f64,
    pub value: f64,
    pub next_value: f64,
    pub ticks: u32,
    pub boundary: Boundary,
}
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct AdvantageTarget { pub advantage: f64, pub target: f64 }

/// `gamma` and `lambda` are specified per `base_ticks` (4 for a 5 Hz policy).
/// This is an explicit semi-Markov convention: both discount and trace decay are
/// raised to the elapsed-duration ratio. Do not use wall-clock time or mix a
/// different discount into potential shaping.
pub fn duration_discount(gamma: f64, ticks: u32, base_ticks: u32) -> Result<f64> {
    if !gamma.is_finite() || !(0.0..=1.0).contains(&gamma) || ticks == 0 || base_ticks == 0 || ticks > 1_000_000 {
        return Err("invalid discount duration");
    }
    Ok(gamma.powf(ticks as f64 / base_ticks as f64))
}

pub fn gae(trajectory: &[TimedValue], gamma: f64, lambda: f64, base_ticks: u32) -> Result<Vec<AdvantageTarget>> {
    // Validate parameters even for an empty trajectory.
    duration_discount(gamma, 1, base_ticks)?;
    duration_discount(lambda, 1, base_ticks)?;
    for t in trajectory {
        if [t.reward, t.value, t.next_value].iter().any(|x| !x.is_finite()) { return Err("nonfinite transition"); }
        duration_discount(gamma, t.ticks, base_ticks)?;
    }
    let mut result = vec![AdvantageTarget { advantage: 0.0, target: 0.0 }; trajectory.len()];
    let mut tail = 0.0;
    for (i, t) in trajectory.iter().enumerate().rev() {
        let g = duration_discount(gamma, t.ticks, base_ticks)?;
        let l = duration_discount(lambda, t.ticks, base_ticks)?;
        let bootstrap = if t.boundary == Boundary::Terminated { 0.0 } else { g * t.next_value };
        let carry = if t.boundary == Boundary::Continuing { g * l * tail } else { 0.0 };
        let advantage = t.reward + bootstrap - t.value + carry;
        let target = advantage + t.value;
        if !advantage.is_finite() || !target.is_finite() { return Err("GAE overflow"); }
        result[i] = AdvantageTarget { advantage, target }; tail = advantage;
    }
    Ok(result)
}

pub fn potential_shaping(previous: f64, next: f64, gamma: f64, ticks: u32, base_ticks: u32, boundary: Boundary) -> Result<f64> {
    if !previous.is_finite() || !next.is_finite() { return Err("nonfinite potential"); }
    let g = duration_discount(gamma, ticks, base_ticks)?;
    let next = if boundary == Boundary::Terminated { 0.0 } else { next };
    let reward = g * next - previous;
    if !reward.is_finite() { return Err("potential overflow"); } Ok(reward)
}

#[cfg(test)]
mod tests {
    use super::*;
    fn t(r: f64, v: f64, nv: f64, ticks: u32, boundary: Boundary) -> TimedValue {
        TimedValue { reward:r, value:v, next_value:nv, ticks, boundary }
    }
    fn close(a:f64,b:f64) { assert!((a-b).abs()<1e-10,"{a} != {b}"); }
    #[test] fn ordinary_gae_is_recovered_at_four_ticks() {
        let ts=[t(1.0,0.3,0.4,4,Boundary::Continuing),t(2.0,0.4,99.0,4,Boundary::Terminated)];
        let a=gae(&ts,0.9,0.8,4).unwrap();
        close(a[1].advantage,1.6); close(a[0].advantage,1.0+0.9*0.4-0.3+0.9*0.8*1.6);
        close(a[0].target,a[0].advantage+0.3);
    }
    #[test] fn eight_ticks_decay_twice_not_once() {
        let ts=[t(1.0,0.3,0.4,8,Boundary::Continuing),t(2.0,0.4,0.0,4,Boundary::Terminated)];
        let a=gae(&ts,0.9,0.8,4).unwrap();
        close(a[0].advantage,1.0+0.81*0.4-0.3+0.81*0.64*1.6);
    }
    #[test] fn truncation_bootstraps_but_never_leaks_the_next_episode() {
        let ts=[t(1.0,0.3,0.4,4,Boundary::Truncated),t(1000.0,0.0,0.0,4,Boundary::Terminated)];
        let a=gae(&ts,0.9,0.8,4).unwrap(); close(a[0].advantage,1.0+0.9*0.4-0.3);
        let end=gae(&[t(1.0,0.3,1e8,4,Boundary::Terminated)],0.9,0.8,4).unwrap(); close(end[0].advantage,0.7);
    }
    #[test] fn potential_telescopes_with_variable_duration() {
        let phis=[-3.0,-2.0,-0.5,0.0]; let ticks=[4,8,12]; let mut discount=1.0;let mut total=0.0;
        for i in 0..3 {
            let boundary=if i==2 {Boundary::Terminated} else {Boundary::Continuing};
            total+=discount*potential_shaping(phis[i],phis[i+1],0.99,ticks[i],4,boundary).unwrap();
            discount*=duration_discount(0.99,ticks[i],4).unwrap();
        }
        close(total,-phis[0]);
        close(potential_shaping(-2.0,-10.0,0.9,4,4,Boundary::Terminated).unwrap(),2.0);
    }
    #[test] fn lambda_zero_is_one_step_td() {
        let ts=[t(1.0,0.3,0.4,4,Boundary::Continuing),t(100.0,0.4,0.0,4,Boundary::Terminated)];
        close(gae(&ts,0.9,0.0,4).unwrap()[0].advantage,1.06);
    }
    #[test] fn invalid_and_overflowing_trajectories_are_rejected() {
        for ticks in [0,1_000_001] { assert!(gae(&[t(0.0,0.0,0.0,ticks,Boundary::Continuing)],0.9,0.9,4).is_err()); }
        assert!(gae(&[],1.1,0.9,4).is_err()); assert!(gae(&[],0.9,0.9,0).is_err());
        assert!(gae(&[t(f64::NAN,0.0,0.0,4,Boundary::Terminated)],0.9,0.9,4).is_err());
        assert!(gae(&[t(f64::MAX,-f64::MAX,0.0,4,Boundary::Terminated)],0.9,0.9,4).is_err());
    }
}
