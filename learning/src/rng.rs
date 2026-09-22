/// SplitMix64. Its state is checkpointed. Never used for security tokens.
#[derive(Clone, Debug)]
pub struct Rng(pub u64);
impl Rng {
    pub fn next(&mut self) -> u64 {
        self.0 = self.0.wrapping_add(0x9e3779b97f4a7c15);
        let mut z = self.0;
        z = (z ^ (z >> 30)).wrapping_mul(0xbf58476d1ce4e5b9);
        z = (z ^ (z >> 27)).wrapping_mul(0x94d049bb133111eb);
        z ^ (z >> 31)
    }
    pub fn uniform(&mut self) -> f32 {
        ((self.next() >> 41) as f32 + 0.5) / 8_388_608.0
    }
    pub fn symmetric(&mut self, scale: f32) -> f32 { (self.uniform() * 2.0 - 1.0) * scale }
    pub fn index(&mut self, n: usize) -> usize {
        assert!(n > 0);
        ((self.next() as u128 * n as u128) >> 64) as usize
    }
    pub fn shuffle<T>(&mut self, xs: &mut [T]) {
        for i in (1..xs.len()).rev() { let j = self.index(i + 1); xs.swap(i, j); }
    }
}
#[cfg(test)] mod tests {
    use super::*;
    #[test] fn deterministic_and_bounded() {
        let (mut a, mut b) = (Rng(7), Rng(7));
        for _ in 0..10000 { assert_eq!(a.next(), b.next()); }
        for _ in 0..10000 { let x = a.uniform(); assert!(x > 0.0 && x < 1.0); }
    }
}
