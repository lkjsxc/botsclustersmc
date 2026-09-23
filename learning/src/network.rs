use super::rng::Rng;
use super::next::math::{Distribution,Gate};

#[derive(Clone, Debug)]
pub struct Model {
    pub input: usize,
    pub hidden: usize,
    pub heads: Vec<usize>,
    /// Flat parameters: W1, b1, W2, b2, policy/value W3, b3. Output-major matrices.
    pub weights: Vec<f32>,
    pub version: u64,
}
#[derive(Clone)]
pub struct Forward {
    pub h1: Vec<f32>, pub h2: Vec<f32>, pub out: Vec<f32>, pub probs: Vec<f32>,
    pub distribution: Distribution,
}
#[derive(Clone, Debug)]
pub struct Decision {
    pub actions: Vec<usize>, pub logp: f32, pub value: f32,
}
#[derive(Clone, Debug)]
pub struct Layout {
    pub w1: usize, pub b1: usize, pub w2: usize, pub b2: usize,
    pub w3: usize, pub b3: usize, pub len: usize, pub outputs: usize,
}
impl Model {
    /// Stable non-cryptographic diagnostic, not a security/authenticity hash.
    pub fn fingerprint(&self) -> u64 {
        self.weights.iter().flat_map(|w| w.to_bits().to_le_bytes()).fold(
            0xcbf29ce484222325u64, |h, b| (h ^ u64::from(b)).wrapping_mul(0x100000001b3))
    }
    pub fn layout(&self) -> Layout {
        let w1 = 0;
        let b1 = self.hidden * self.input;
        let w2 = b1 + self.hidden;
        let b2 = w2 + self.hidden * self.hidden;
        let w3 = b2 + self.hidden;
        let outputs = self.heads.iter().sum::<usize>() + 1;
        let b3 = w3 + outputs * self.hidden;
        Layout { w1, b1, w2, b2, w3, b3, len: b3 + outputs, outputs }
    }
    pub fn new(input: usize, hidden: usize, heads: Vec<usize>, seed: u64) -> Self {
        assert!(input > 0 && hidden > 0 && !heads.is_empty() && heads.iter().all(|&x| x > 0));
        let mut m = Self { input, hidden, heads, weights: vec![], version: 0 };
        let l = m.layout(); m.weights = vec![0.0; l.len];
        let mut rng = Rng(seed);
        for row in 0..hidden { for col in 0..input {
            m.weights[l.w1 + row * input + col] = rng.symmetric((6.0 / (input + hidden) as f32).sqrt());
        }}
        for row in 0..hidden { for col in 0..hidden {
            m.weights[l.w2 + row * hidden + col] = rng.symmetric((3.0 / hidden as f32).sqrt());
        }}
        for row in 0..l.outputs { for col in 0..hidden {
            // Near-uniform actor, normally scaled critic. No action preferences.
            let scale = if row + 1 == l.outputs { 1.0 } else { 0.01 };
            m.weights[l.w3 + row * hidden + col] = rng.symmetric(scale * (3.0 / hidden as f32).sqrt());
        }}
        m
    }
    pub fn validate(&self) -> bool {
        self.input > 0 && self.input <= 4096 && self.hidden > 0 && self.hidden <= 512
            && !self.heads.is_empty() && self.heads.len() <= 16
            && self.heads.iter().all(|&n| n > 0 && n <= 128)
            && self.weights.len() == self.layout().len
            && self.weights.iter().all(|v| v.is_finite())
    }
    pub fn forward(&self, x: &[f32], mask: &[bool]) -> Forward {
        assert_eq!(x.len(), self.input);
        let l = self.layout(); assert_eq!(mask.len() + 1, l.outputs);
        let mut h1 = vec![0.0; self.hidden]; let mut h2 = vec![0.0; self.hidden];
        for (row, y) in h1.iter_mut().enumerate() {
            let w = &self.weights[l.w1 + row * self.input..l.w1 + (row + 1) * self.input];
            *y = (dot(w, x) + self.weights[l.b1 + row]).tanh();
        }
        for (row, y) in h2.iter_mut().enumerate() {
            let w = &self.weights[l.w2 + row * self.hidden..l.w2 + (row + 1) * self.hidden];
            *y = (dot(w, &h1) + self.weights[l.b2 + row]).tanh();
        }
        let mut out = vec![0.0; l.outputs];
        for (row, y) in out.iter_mut().enumerate() {
            let w = &self.weights[l.w3 + row * self.hidden..l.w3 + (row + 1) * self.hidden];
            *y = dot(w, &h2) + self.weights[l.b3 + row];
        }
        let mut logits=Vec::with_capacity(self.heads.len());
        let mut masks=Vec::with_capacity(self.heads.len());let mut offset=0;
        for &n in &self.heads {
            logits.push(out[offset..offset+n].iter().map(|&v|v as f64).collect());
            masks.push(mask[offset..offset+n].to_vec());offset+=n;
        }
        let gate=(self.heads.as_slice()==super::HEADS).then_some(Gate{parent:6,child:7,active_bits:0b1110,neutral:0});
        let distribution=Distribution::new(&logits,&masks,gate).expect("invalid neural categorical distribution");
        let probs=distribution.probabilities().iter().flatten().map(|&v|v as f32).collect();
        Forward { h1, h2, out, probs, distribution }
    }
    /// Backpropagate an externally computed derivative with respect to each raw output.
    pub fn backward(&self, x: &[f32], f: &Forward, dout: &[f32], grad: &mut [f32]) {
        let l = self.layout(); assert_eq!(dout.len(), l.outputs); assert_eq!(grad.len(), l.len);
        let mut dh2 = vec![0.0; self.hidden];
        for (row, &g) in dout.iter().enumerate() {
            grad[l.b3 + row] += g;
            for col in 0..self.hidden {
                let k = l.w3 + row * self.hidden + col;
                grad[k] += g * f.h2[col]; dh2[col] += g * self.weights[k];
            }
        }
        let mut dh1 = vec![0.0; self.hidden];
        for row in 0..self.hidden {
            let g = dh2[row] * (1.0 - f.h2[row] * f.h2[row]);
            grad[l.b2 + row] += g;
            for col in 0..self.hidden {
                let k = l.w2 + row * self.hidden + col;
                grad[k] += g * f.h1[col]; dh1[col] += g * self.weights[k];
            }
        }
        for row in 0..self.hidden {
            let g = dh1[row] * (1.0 - f.h1[row] * f.h1[row]);
            grad[l.b1 + row] += g;
            for (col, &v) in x.iter().enumerate() { grad[l.w1 + row * self.input + col] += g * v; }
        }
    }
    pub fn decide(&self, x: &[f32], mask: &[bool], rng: &mut Rng, greedy: bool) -> Decision {
        let f=self.forward(x,mask);
        let mut shared=super::next::Rng(rng.0);
        let chosen=f.distribution.sample(&mut shared,greedy).expect("invalid policy distribution");
        rng.0=shared.0;
        Decision{actions:chosen.actions,logp:chosen.log_probability as f32,value:*f.out.last().unwrap()}
    }
}
fn dot(a: &[f32], b: &[f32]) -> f32 { a.iter().zip(b).map(|(x,y)| x*y).sum() }
pub fn masked_softmax(logits: &[f32], heads: &[usize], mask: &[bool]) -> Vec<f32> {
    assert_eq!(logits.len(), mask.len()); assert_eq!(heads.iter().sum::<usize>(), logits.len());
    let mut p = vec![0.0; logits.len()]; let mut offset = 0;
    for &n in heads {
        assert!(mask[offset..offset+n].iter().any(|m| *m), "each head needs at least one legal action");
        let max = (offset..offset+n).filter(|&i| mask[i]).map(|i| logits[i]).fold(f32::NEG_INFINITY, f32::max);
        let mut sum = 0.0;
        for i in offset..offset+n { if mask[i] { p[i] = (logits[i] - max).exp(); sum += p[i]; } }
        for v in &mut p[offset..offset+n] { *v /= sum; }
        offset += n;
    }
    p
}
pub fn log_probability(p: &[f32], heads: &[usize], actions: &[usize]) -> f32 {
    assert_eq!(heads.len(), actions.len()); let mut offset = 0; let mut logp = 0.0;
    for (h,(&n,&a)) in heads.iter().zip(actions).enumerate() {
        assert!(a<n);
        if heads==super::HEADS && h==7 && !(1..=3).contains(&actions[6]) { assert_eq!(a,0); }
        else {logp+=p[offset+a].max(1e-30).ln();}
        offset+=n;
    }
    logp
}
#[cfg(test)] mod tests {
    use super::*;
    #[test] fn masks_and_probabilities() {
        let p = masked_softmax(&[1000.0, 1001.0, -99.0, 4.0], &[2,2], &[true,true,false,true]);
        assert!((p[0]+p[1]-1.0).abs() < 1e-6); assert_eq!(p[2],0.0); assert_eq!(p[3],1.0);
    }
    #[test] fn backprop_matches_finite_difference() {
        let mut m = Model::new(3,4,vec![2,3],42); let x = [0.2,-0.7,0.5]; let mask = [true;5];
        let f = m.forward(&x, &mask); let d = [0.2, -0.4, 0.1, -0.3, 0.6, 0.7];
        let mut g = vec![0.0; m.weights.len()]; m.backward(&x,&f,&d,&mut g);
        for i in 0..m.weights.len() {
            let old = m.weights[i]; let eps = 0.001;
            m.weights[i] = old + eps; let plus = dot(&m.forward(&x,&mask).out,&d);
            m.weights[i] = old - eps; let minus = dot(&m.forward(&x,&mask).out,&d);
            m.weights[i] = old;
            assert!(((plus-minus)/(2.0*eps)-g[i]).abs() < 0.0002, "parameter {i}");
        }
    }
    #[test] fn sample_never_picks_masked_action() {
        let m = Model::new(2,3,vec![3],2); let mut rng=Rng(4);
        for _ in 0..1000 { assert_eq!(m.decide(&[0.0,1.0],&[false,true,false],&mut rng,false).actions,[1]); }
    }
}
