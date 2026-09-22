//! Pure numerical components. They require adapter integration and live ablations.
mod distribution;
mod time;
mod trust;
pub use distribution::*;
pub use time::*;
pub use trust::*;
use crate::Result;

#[derive(Clone, Copy, Debug)]
pub struct ClippedObjective { pub loss: f64, pub d_log_probability: f64, pub ratio: f64, pub clipped: bool }
/// Gradient of the negative clipped PPO surrogate, with the behaviour logp
/// fixed. Extreme/nonfinite inputs fail rather than being silently clamped.
pub fn clipped_objective(old_logp: f64, logp: f64, advantage: f64, epsilon: f64) -> Result<ClippedObjective> {
    if [old_logp, logp, advantage, epsilon].iter().any(|x| !x.is_finite()) || !(0.0..1.0).contains(&epsilon) {
        return Err("invalid PPO objective inputs");
    }
    let d = logp - old_logp;
    if !d.is_finite() || d.abs() > 60.0 { return Err("unsafe importance ratio"); }
    let ratio = d.exp();
    let clipped = (advantage > 0.0 && ratio > 1.0 + epsilon) || (advantage < 0.0 && ratio < 1.0 - epsilon);
    let objective = (ratio * advantage).min(ratio.clamp(1.0 - epsilon, 1.0 + epsilon) * advantage);
    let loss = -objective;
    let grad = if clipped { 0.0 } else { -ratio * advantage };
    if !loss.is_finite() || !grad.is_finite() { return Err("PPO objective overflow"); }
    Ok(ClippedObjective { loss, d_log_probability: grad, ratio, clipped })
}
/// Huber value loss and derivative with respect to the predicted value.
/// The delta is a tunable scale, not a universal replacement for squared loss.
pub fn huber_value(value: f64, target: f64, delta: f64) -> Result<(f64, f64)> {
    if !value.is_finite() || !target.is_finite() || !delta.is_finite() || delta <= 0.0 {
        return Err("invalid Huber inputs");
    }
    let e = value - target;
    if !e.is_finite() { return Err("value residual overflow"); }
    let pair = if e.abs() <= delta { (0.5 * e * e, e) }
        else { (delta * (e.abs() - 0.5 * delta), delta * e.signum()) };
    if !pair.0.is_finite() { return Err("value loss overflow"); } Ok(pair)
}
/// Population normalization using f64 Welford statistics; does not mutate on error.
pub fn normalize_advantages(values: &mut [f64]) -> Result<(f64, f64)> {
    if values.is_empty() || values.iter().any(|x| !x.is_finite()) { return Err("invalid advantages"); }
    let (mut mean, mut m2) = (0.0, 0.0);
    for (i, &x) in values.iter().enumerate() {
        let delta = x - mean; mean += delta / (i + 1) as f64; m2 += delta * (x - mean);
    }
    let variance = m2 / values.len() as f64;
    if !mean.is_finite() || !variance.is_finite() || variance < -1e-12 { return Err("advantage statistics overflow"); }
    let std = variance.max(0.0).sqrt();
    let divisor = std.max(1e-8);
    let normalized: Vec<_> = values.iter().map(|x| (*x - mean) / divisor).collect();
    if normalized.iter().any(|x| !x.is_finite()) { return Err("advantage normalization overflow"); }
    values.copy_from_slice(&normalized); Ok((mean, std))
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test] fn both_signs_of_ppo_clipping() {
        for (ratio, adv, clipped) in [(1.3, 1.0, true), (0.7, -1.0, true), (0.7, 1.0, false), (1.3, -1.0, false)] {
            let o = clipped_objective(-2.0, -2.0 + f64::ln(ratio), adv, 0.2).unwrap();
            assert_eq!(o.clipped, clipped);
            assert!((o.d_log_probability - if clipped {0.0} else {-ratio * adv}).abs() < 1e-12);
        }
    }
    #[test] fn clipped_gradient_matches_finite_difference() {
        for a in [-3.0, -0.2, 0.0, 0.3, 4.0] { for r in [0.5_f64, 0.9, 1.1, 1.5] {
            let l = -2.0 + r.ln(); let h = 1e-6;
            let fd = (clipped_objective(-2.0,l+h,a,0.2).unwrap().loss - clipped_objective(-2.0,l-h,a,0.2).unwrap().loss)/(2.0*h);
            assert!((fd-clipped_objective(-2.0,l,a,0.2).unwrap().d_log_probability).abs()<1e-7);
        }}
    }
    #[test] fn objectives_reject_poisoned_values() {
        assert!(clipped_objective(0.0, f64::NAN, 1.0, 0.2).is_err());
        assert!(clipped_objective(-100.0, 0.0, 1.0, 0.2).is_err());
        assert!(clipped_objective(0.0, 0.0, 1.0, -0.1).is_err());
        assert!(huber_value(f64::MAX, -f64::MAX, 1.0).is_err());
    }
    #[test] fn huber_is_continuous_and_has_bounded_gradient() {
        for e in [-100.0_f64, -1.0, -0.25, 0.0, 0.25, 1.0, 100.0] {
            let (l,g) = huber_value(e, 0.0, 1.0).unwrap(); assert!(g.abs() <= 1.0); assert!(l >= 0.0);
            let h = 1e-6; let fd = (huber_value(e+h,0.0,1.0).unwrap().0-huber_value(e-h,0.0,1.0).unwrap().0)/(2.0*h);
            assert!((fd-g).abs() < 1e-6);
        }
    }
    #[test] fn normalization_handles_constant_and_large_offset_values() {
        let mut x = [1e10+1.0,1e10+2.0,1e10+3.0]; let (_,std) = normalize_advantages(&mut x).unwrap();
        assert!((std-(2.0_f64/3.0).sqrt()).abs()<1e-9); assert!(x.iter().sum::<f64>().abs()<1e-9);
        let mut same=[4.0;32]; normalize_advantages(&mut same).unwrap(); assert_eq!(same,[0.0;32]);
        let mut bad=[1.0,f64::NAN]; assert!(normalize_advantages(&mut bad).is_err()); assert_eq!(bad[0],1.0);
    }
}
