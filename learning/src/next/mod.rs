//! Shared RL mechanisms. The runtime and standalone checks use this implementation.
pub mod cohort;
pub mod curriculum;
pub mod math;
pub mod tasks;

pub type Result<T> = std::result::Result<T, &'static str>;

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct Rng(pub u64);
impl Rng {
    pub fn next_u64(&mut self) -> u64 {
        self.0 = self.0.wrapping_add(0x9e3779b97f4a7c15);
        let mut z = self.0;
        z = (z ^ (z >> 30)).wrapping_mul(0xbf58476d1ce4e5b9);
        z = (z ^ (z >> 27)).wrapping_mul(0x94d049bb133111eb);
        z ^ (z >> 31)
    }
    pub fn unit(&mut self) -> f64 {
        (self.next_u64() >> 11) as f64 * (1.0 / ((1u64 << 53) as f64))
    }
    pub fn index(&mut self, n: usize) -> Result<usize> {
        if n == 0 { return Err("empty random range"); }
        let n = u64::try_from(n).map_err(|_| "random range too large")?;
        let threshold = n.wrapping_neg() % n;
        loop { let x = self.next_u64(); if x >= threshold { return Ok((x % n) as usize); } }
    }
    pub fn weighted(&mut self, weights: &[f64]) -> Result<usize> {
        if weights.is_empty() || weights.iter().any(|x| !x.is_finite() || *x < 0.0) {
            return Err("invalid sampling weights");
        }
        let sum: f64 = weights.iter().sum();
        if !sum.is_finite() || sum <= 0.0 { return Err("zero or overflowing sampling weights"); }
        let mut at = self.unit() * sum;
        let mut last = 0;
        for (i, &w) in weights.iter().enumerate() {
            if w > 0.0 { last = i; }
            if at < w { return Ok(i); }
            at -= w;
        }
        Ok(last)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test] fn deterministic_and_bounded_random() {
        let mut a = Rng(44); let mut b = a;
        for _ in 0..10000 { assert_eq!(a.next_u64(), b.next_u64()); }
        for _ in 0..10000 { let x = a.unit(); assert!((0.0..1.0).contains(&x)); assert!(a.index(32).unwrap() < 32); }
        assert!(a.index(0).is_err());
    }
    #[test] fn invalid_weights_do_not_advance_rng() {
        let mut r = Rng(11); let before = r;
        for w in [&[][..], &[0.0][..], &[f64::NAN][..], &[-1.0, 2.0][..], &[f64::MAX, f64::MAX][..]] {
            assert!(r.weighted(w).is_err()); assert_eq!(r, before);
        }
        for _ in 0..100 { assert_eq!(r.weighted(&[0.0, 1.0, 0.0]).unwrap(), 1); }
    }
}
