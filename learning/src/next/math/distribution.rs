//! Factorized categorical policies with one optional conditional child head.
//! For Minecraft, GUI slot is meaningful only for left/right/shift click. An
//! unused sampled slot must not change PPO's likelihood ratio or entropy gradient.
use super::super as shared_root;
use shared_root::{Result,Rng};

#[derive(Clone,Copy,Debug,PartialEq,Eq)]
pub struct Gate {
    pub parent:usize,
    pub child:usize,
    pub active_bits:u128,
    pub neutral:usize,
}
impl Gate {pub fn active(self,a:usize)->bool {a<128 && self.active_bits & (1u128<<a)!=0}}
#[derive(Clone,Debug)]
pub struct Distribution {
    probabilities:Vec<Vec<f64>>,
    log_probabilities:Vec<Vec<f64>>,
    masks:Vec<Vec<bool>>,
    gate:Option<Gate>,
}
#[derive(Clone,Debug)]
pub struct SampledAction {pub actions:Vec<usize>,pub log_probability:f64}
impl Distribution {
    pub fn new(logits:&[Vec<f64>],masks:&[Vec<bool>],gate:Option<Gate>)->Result<Self> {
        if logits.is_empty() || logits.len()!=masks.len() || logits.len()>16 {return Err("invalid categorical head count");}
        for (xs,mask) in logits.iter().zip(masks) {
            if xs.is_empty() || xs.len()>128 || xs.len()!=mask.len() || !mask.iter().any(|x|*x) || xs.iter().any(|x|!x.is_finite()) {
                return Err("invalid categorical logits or mask");
            }
        }
        if let Some(g)=gate {
            if g.parent>=g.child || g.child>=logits.len() || g.neutral>=logits[g.child].len() ||
                (logits[g.parent].len()<128 && g.active_bits>>logits[g.parent].len()!=0) {
                return Err("invalid conditional gate");
            }
        }
        let mut probabilities=Vec::new();let mut log_probabilities=Vec::new();
        for (xs,mask) in logits.iter().zip(masks) {
            let max=xs.iter().zip(mask).filter(|(_,m)|**m).map(|(x,_)|*x).fold(f64::NEG_INFINITY,f64::max);
            let mut ps=Vec::with_capacity(xs.len());
            for (&x,&m) in xs.iter().zip(mask) {
                if m && !(x-max).is_finite() {return Err("logit range overflow");}
                ps.push(if m {(x-max).exp()} else {0.0});
            }
            let sum=ps.iter().sum::<f64>();if !sum.is_finite() || sum<=0.0 {return Err("softmax normalization failed");}
            let log_sum=sum.ln();
            let ls=xs.iter().zip(mask).map(|(&x,&m)|if m {x-max-log_sum} else {f64::NEG_INFINITY}).collect();
            for p in &mut ps {*p/=sum;}
            probabilities.push(ps);log_probabilities.push(ls);
        }
        Ok(Self {probabilities,log_probabilities,masks:masks.to_vec(),gate})
    }
    pub fn probabilities(&self)->&[Vec<f64>]{&self.probabilities}
    pub fn masks(&self)->&[Vec<bool>]{&self.masks}
    pub fn gate(&self)->Option<Gate>{self.gate}
    fn ignored(&self,head:usize,actions:&[usize])->bool {
        self.gate.is_some_and(|g|g.child==head && !g.active(actions[g.parent]))
    }
    pub fn log_probability(&self,actions:&[usize])->Result<f64> {
        if actions.len()!=self.probabilities.len() {return Err("action head mismatch");}
        let mut total=0.0;
        for (h,&a) in actions.iter().enumerate() {
            if a>=self.probabilities[h].len() {return Err("action index out of range");}
            if self.ignored(h,actions) {
                if Some(a)!=self.gate.map(|g|g.neutral) {return Err("inactive child must have canonical neutral action");}
                continue;
            }
            if !self.masks[h][a] {return Err("masked action");}
            total+=self.log_probabilities[h][a];
        }
        if !total.is_finite() {return Err("log probability overflow");} Ok(total)
    }
    pub fn sample(&self,rng:&mut Rng,greedy:bool)->Result<SampledAction> {
        let mut actions=Vec::with_capacity(self.probabilities.len());
        for (h,ps) in self.probabilities.iter().enumerate() {
            if self.ignored(h,&actions) {actions.push(self.gate.unwrap().neutral);continue;}
            let a=if greedy {
                let mut best=0;let mut value=f64::NEG_INFINITY;
                for (i,&p) in ps.iter().enumerate(){if self.masks[h][i] && p>value {best=i;value=p;}}
                best
            } else {rng.weighted(ps)?};
            actions.push(a);
        }
        let log_probability=self.log_probability(&actions)?;Ok(SampledAction{actions,log_probability})
    }
    /// Chain-rule gradient through logits for a scalar derivative dL/d(log pi).
    pub fn score_gradient(&self,actions:&[usize],d_log_probability:f64)->Result<Vec<Vec<f64>>> {
        self.log_probability(actions)?;
        if !d_log_probability.is_finite(){return Err("nonfinite policy derivative");}
        let mut grad=self.probabilities.iter().map(|p|vec![0.0;p.len()]).collect::<Vec<_>>();
        for (h,ps) in self.probabilities.iter().enumerate(){
            if self.ignored(h,actions){continue;}
            for (i,&p) in ps.iter().enumerate(){grad[h][i]=d_log_probability*((i==actions[h]) as u8 as f64-p);}
        }
        if grad.iter().flatten().any(|x|!x.is_finite()){return Err("policy derivative overflow");} Ok(grad)
    }
    /// Entropy and its exact logit gradient. With normalize_heads=true this is
    /// a bounded regularizer: average H(head)/ln(number of legal choices), with
    /// the child multiplied by the parent's probability of activating it.
    /// Its parent gradient includes dP(active)/dlogit. Omitting that term is wrong.
    /// With false, this returns the Shannon entropy of the actual joint policy.
    pub fn entropy(&self,normalize_heads:bool)->(f64,Vec<Vec<f64>>) {
        let heads=self.probabilities.len();let mut hs=vec![0.0;heads];let mut scales=vec![1.0;heads];
        for h in 0..heads {
            for (i,&p) in self.probabilities[h].iter().enumerate(){if p>0.0{hs[h]-=p*self.log_probabilities[h][i];}}
            if normalize_heads {
                let n=self.masks[h].iter().filter(|x|**x).count();scales[h]=if n>1{1.0/(n as f64).ln()}else{0.0};
            }
        }
        let mut q=1.0;
        if let Some(g)=self.gate {
            q=self.probabilities[g.parent].iter().enumerate().filter(|(i,_)|g.active(*i)).map(|(_,p)|*p).sum();
            let reachable=self.masks[g.parent].iter().enumerate().any(|(i,m)|*m && g.active(i));
            if normalize_heads && !reachable {scales[g.child]=0.0;}
        }
        if normalize_heads {
            let count=scales.iter().filter(|s|**s>0.0).count().max(1) as f64;
            for s in &mut scales{*s/=count;}
        }
        let mut value=0.0;let mut grad=self.probabilities.iter().map(|p|vec![0.0;p.len()]).collect::<Vec<_>>();
        for h in 0..heads {
            let weight=if self.gate.is_some_and(|g|g.child==h){q}else{1.0};
            value+=scales[h]*weight*hs[h];
            for (i,&p) in self.probabilities[h].iter().enumerate(){
                if p>0.0 {grad[h][i]=-scales[h]*weight*p*(self.log_probabilities[h][i]+hs[h]);}
            }
        }
        if let Some(g)=self.gate {
            for (i,&p) in self.probabilities[g.parent].iter().enumerate(){
                grad[g.parent][i]+=scales[g.child]*hs[g.child]*p*((g.active(i) as u8 as f64)-q);
            }
        }
        (value,grad)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    fn gate()->Gate {Gate{parent:0,child:1,active_bits:0b0110,neutral:0}}
    fn setup()->(Vec<Vec<f64>>,Vec<Vec<bool>>){(vec![vec![0.1,0.7,-0.4,0.2],vec![1.1,-0.2,0.3]],vec![vec![true;4],vec![true;3]])}
    #[test] fn stable_masked_softmax_and_invalid_inputs() {
        let d=Distribution::new(&[vec![1000.0,-1000.0,999.0]],&[vec![true,false,true]],None).unwrap();
        assert_eq!(d.probabilities()[0][1],0.0);assert!((d.probabilities()[0].iter().sum::<f64>()-1.0).abs()<1e-12);
        assert!(d.log_probability(&[1]).is_err());
        assert!(Distribution::new(&[vec![0.0]],&[vec![false]],None).is_err());
        assert!(Distribution::new(&[vec![f64::NAN]],&[vec![true]],None).is_err());
        assert!(Distribution::new(&[vec![0.0]],&[vec![true]],Some(gate())).is_err());
    }
    #[test] fn exact_conditional_joint_distribution_is_normalized() {
        let (x,m)=setup();let d=Distribution::new(&x,&m,Some(gate())).unwrap();let mut sum=0.0;
        for a in 0..4{for b in 0..if gate().active(a){3}else{1}{sum+=d.log_probability(&[a,b]).unwrap().exp();}}
        assert!((sum-1.0).abs()<1e-12);assert!(d.log_probability(&[0,2]).is_err());
    }
    #[test] fn inactive_slot_contributes_no_log_probability_or_gradient() {
        let (x,m)=setup();let d=Distribution::new(&x,&m,Some(gate())).unwrap();
        let lp=d.log_probability(&[0,0]).unwrap();assert!((lp-d.probabilities()[0][0].ln()).abs()<1e-12);
        let g=d.score_gradient(&[0,0],1.0).unwrap();assert_eq!(g[1],vec![0.0;3]);
        let e=d.score_gradient(&[1,2],1.0).unwrap();assert!(e[1].iter().any(|x|*x!=0.0));
    }
    #[test] fn sampling_uses_canonical_neutral_even_when_that_slot_is_masked() {
        let d=Distribution::new(&[vec![0.0;4],vec![0.0;3]],&[vec![true,false,false,false],vec![false,true,true]],Some(gate())).unwrap();
        let mut r=Rng(42);for _ in 0..100{let a=d.sample(&mut r,false).unwrap();assert_eq!(a.actions,[0,0]);assert_eq!(a.log_probability,0.0);}
        assert_eq!(d.entropy(true).0,0.0);
    }
    #[test] fn score_gradient_matches_finite_difference_for_active_and_inactive_branches() {
        let (x,m)=setup();let d=Distribution::new(&x,&m,Some(gate())).unwrap();let h=1e-6;
        for action in [[0,0],[1,2],[2,1],[3,0]] {
            let grad=d.score_gradient(&action,1.0).unwrap();
            for head in 0..x.len(){for i in 0..x[head].len(){
                let mut plus=x.clone();plus[head][i]+=h;let mut minus=x.clone();minus[head][i]-=h;
                let fd=(Distribution::new(&plus,&m,Some(gate())).unwrap().log_probability(&action).unwrap()-Distribution::new(&minus,&m,Some(gate())).unwrap().log_probability(&action).unwrap())/(2.0*h);
                assert!((fd-grad[head][i]).abs()<1e-8,"head {head} slot {i}");
            }}
        }
    }
    #[test] fn entropy_gradient_includes_conditional_parent_derivative() {
        let (x,m)=setup();let d=Distribution::new(&x,&m,Some(gate())).unwrap();let h=1e-6;
        for normalized in [false,true]{let (entropy,grad)=d.entropy(normalized);assert!(entropy>=0.0);if normalized{assert!(entropy<=1.0);}
            for head in 0..x.len(){for i in 0..x[head].len(){
                let mut plus=x.clone();plus[head][i]+=h;let mut minus=x.clone();minus[head][i]-=h;
                let fd=(Distribution::new(&plus,&m,Some(gate())).unwrap().entropy(normalized).0-Distribution::new(&minus,&m,Some(gate())).unwrap().entropy(normalized).0)/(2.0*h);
                assert!((fd-grad[head][i]).abs()<1e-8,"conditional entropy head {head} slot {i}: {fd} != {}",grad[head][i]);
            }}
        }
    }
    #[test] fn shannon_entropy_matches_enumerated_joint_actions() {
        let (x,m)=setup();let d=Distribution::new(&x,&m,Some(gate())).unwrap();let mut h=0.0;
        for a in 0..4{for b in 0..if gate().active(a){3}else{1}{let lp=d.log_probability(&[a,b]).unwrap();h-=lp.exp()*lp;}}
        assert!((h-d.entropy(false).0).abs()<1e-12);
    }
    #[test] fn large_slot_head_does_not_dominate_normalized_entropy() {
        let d=Distribution::new(&[vec![0.0;2],vec![0.0;90]],&[vec![true;2],vec![true;90]],None).unwrap();
        assert!((d.entropy(true).0-1.0).abs()<1e-12);
        assert!((d.entropy(false).0-(180.0_f64).ln()).abs()<1e-12);
    }
    #[test] fn sampling_matches_the_executed_joint_likelihood() {
        let (x,m)=setup();let d=Distribution::new(&x,&m,Some(gate())).unwrap();let mut r=Rng(481);let mut counts=[0usize;4];
        for _ in 0..20000 {let a=d.sample(&mut r,false).unwrap();counts[a.actions[0]]+=1;
            assert_eq!(a.log_probability,d.log_probability(&a.actions).unwrap());if !gate().active(a.actions[0]){assert_eq!(a.actions[1],0);}}
        for (i,&n) in counts.iter().enumerate(){assert!((n as f64/20000.0-d.probabilities()[0][i]).abs()<0.02);}
    }
}
