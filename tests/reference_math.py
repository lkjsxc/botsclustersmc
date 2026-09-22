#!/usr/bin/env python3
"""Independent mathematical checks, NOT compilation/execution of the Rust code.
Python/NumPy/PyTorch are development-only; production uses none of them.
"""
from __future__ import annotations
import json
import math
from pathlib import Path
import unittest
import numpy as np
import torch

ROOT = Path(__file__).resolve().parents[1]

class Rng:
    def __init__(self, seed: int): self.state = seed
    def next(self) -> int:
        mask=(1<<64)-1
        self.state=(self.state+0x9e3779b97f4a7c15)&mask
        z=self.state
        z=((z^(z>>30))*0xbf58476d1ce4e5b9)&mask
        z=((z^(z>>27))*0x94d049bb133111eb)&mask
        return z^(z>>31)
    def uniform(self) -> float: return ((self.next()>>41)+0.5)/8388608.0
    def index(self, n: int) -> int: return (self.next()*n)>>64
    def shuffle(self, values: list[int]) -> None:
        for i in range(len(values)-1, 0, -1):
            j=self.index(i+1); values[i],values[j]=values[j],values[i]

def initial(input_size: int, hidden: int, outputs: int, seed: int):
    rng=Rng(seed)
    def matrix(n,m,scale):
        return np.array([(rng.uniform()*2-1)*scale for _ in range(n*m)]).reshape(n,m)
    return [matrix(hidden,input_size,math.sqrt(6/(input_size+hidden))),np.zeros(hidden),
            matrix(hidden,hidden,math.sqrt(3/hidden)),np.zeros(hidden),
            np.vstack([matrix(1,hidden,(1 if i==outputs-1 else .01)*math.sqrt(3/hidden)) for i in range(outputs)]),np.zeros(outputs)]

def forward(w, x):
    h1=np.tanh(x@w[0].T+w[1]); h2=np.tanh(h1@w[2].T+w[3]); out=h2@w[4].T+w[5]
    return h1,h2,out

def probabilities(logits, heads, mask):
    p=np.zeros_like(logits); off=0
    for n in heads:
        z=np.where(mask[:,off:off+n],logits[:,off:off+n],-np.inf)
        e=np.exp(z-z.max(axis=1,keepdims=True));p[:,off:off+n]=e/e.sum(axis=1,keepdims=True);off+=n
    return p

def manual(w,x,heads,mask,actions,old_logp,adv,target,entropy=.002,value_coef=.5):
    h1,h2,out=forward(w,x);p=probabilities(out[:,:-1],heads,mask);rows=np.arange(len(x));off=0;lp=np.zeros(len(x))
    for h,n in enumerate(heads):lp+=np.log(np.maximum(p[rows,off+actions[:,h]],1e-30));off+=n
    diff=np.clip(lp-old_logp,-20,20);ratio=np.exp(diff);raw=ratio*adv;capped=np.clip(ratio,.8,1.2)*adv
    dlp=np.where(raw<=capped,-adv*ratio,0.0);dout=np.zeros_like(out);off=0;total_entropy=np.zeros(len(x))
    for h,n in enumerate(heads):
        ps=p[:,off:off+n];logs=np.log(np.maximum(ps,1e-30));H=-(ps*logs).sum(axis=1);total_entropy+=H
        one=np.zeros_like(ps);one[rows,actions[:,h]]=1
        dout[:,off:off+n]=dlp[:,None]*(one-ps)+entropy*ps*(logs+H[:,None]);off+=n
    err=out[:,-1]-target;dout[:,-1]=value_coef*err
    d2=(dout@w[4])*(1-h2*h2);d1=(d2@w[2])*(1-h1*h1)
    grads=[d1.T@x,d1.sum(axis=0),d2.T@h1,d2.sum(axis=0),dout.T@h2,dout.sum(axis=0)]
    loss=np.sum(-np.minimum(raw,capped)-entropy*total_entropy+.5*value_coef*err*err)
    kl=np.mean((ratio-1)-diff)
    return grads,loss,kl

class ReferenceChecks(unittest.TestCase):
    def test_rng_f32_endpoint_fix(self):
        for bits in [0,1,8388606,8388607]:
            x=np.float32(np.float32(bits)+np.float32(.5))/np.float32(8388608)
            self.assertGreater(x,0);self.assertLess(x,1)
        r=Rng(7);self.assertTrue(all(0<r.uniform()<1 for _ in range(10000)))

    def test_ppo_gradient_matches_independent_autograd(self):
        rng=np.random.default_rng(9);x=rng.normal(size=(4,3));heads=[2,3];w=initial(3,4,6,31)
        mask=np.array([[1,1,1,0,1]]*4,dtype=bool);a=np.array([[0,0],[1,2],[0,2],[1,0]])
        p=probabilities(forward(w,x)[2][:,:-1],heads,mask)
        lp=np.log(p[np.arange(4),a[:,0]])+np.log(p[np.arange(4),2+a[:,1]])
        # Exercise both clipped regions and both signs, not just ratio==1.
        old=lp-np.log(np.array([1.4,1.4,.6,.6]));adv=np.array([1.,-1.,-1.,1.]);target=rng.normal(size=4)
        grads,loss,_=manual(w,x,heads,mask,a,old,adv,target)
        ws=[torch.tensor(v,dtype=torch.float64,requires_grad=True) for v in w];xt=torch.tensor(x)
        h1=torch.tanh(xt@ws[0].T+ws[1]);h2=torch.tanh(h1@ws[2].T+ws[3]);out=h2@ws[4].T+ws[5]
        probs=[];off=0
        for n in heads:
            probs.append(torch.softmax(out[:,off:off+n].masked_fill(~torch.tensor(mask[:,off:off+n]),-torch.inf),dim=1));off+=n
        logp=sum(torch.log(q[torch.arange(4),torch.tensor(a[:,h])]) for h,q in enumerate(probs))
        ratio=torch.exp(torch.clamp(logp-torch.tensor(old),-20,20));at=torch.tensor(adv)
        H=sum(-(q*torch.log(torch.clamp(q,min=1e-30))).sum(dim=1) for q in probs)
        objective=(-torch.minimum(ratio*at,torch.clamp(ratio,.8,1.2)*at)-.002*H+.25*(out[:,-1]-torch.tensor(target))**2).sum()
        objective.backward();self.assertAlmostEqual(loss,objective.item(),places=10)
        for expected,tensor in zip(grads,ws):np.testing.assert_allclose(expected,tensor.grad.numpy(),atol=1e-10,rtol=1e-10)

    def test_gae_terminal_and_truncation(self):
        def gae(rows):
            carry=0;out=[]
            for reward,value,next_value,terminal in reversed(rows):
                live=not terminal;carry=reward+.9*next_value*live-value+.9*1.0*live*carry;out.append(value+carry)
            return out[::-1]
        np.testing.assert_allclose(gae([(1,.5,999,True)]),[1])
        np.testing.assert_allclose(gae([(1,0,0,False),(2,0,10,False)]),[10.9,11])
        np.testing.assert_allclose(gae([(1,0,0,True),(20,0,10,False)]),[1,29])

    def test_contextual_bandit_reward_only_reference(self):
        w=initial(2,12,3,31);rng=Rng(123);m=[np.zeros_like(v) for v in w];v=[np.zeros_like(v) for v in w];step=0
        for update in range(45):
            x=np.eye(2)[np.arange(128)%2];out=forward(w,x)[2];mask=np.ones((128,2),dtype=bool);p=probabilities(out[:,:2],[2],mask)
            acts=np.array([[int(rng.uniform()>=q[0])] for q in p]);r=np.where(acts[:,0]==np.arange(128)%2,1.,-1.)
            old=np.log(p[np.arange(128),acts[:,0]]);adv=r-out[:,-1];adv=(adv-adv.mean())/np.sqrt(adv.var()+1e-8)
            order=list(range(128));stop=False;applied=0
            for epoch in range(3):
                rng.shuffle(order)
                for start in range(0,128,32):
                    ids=np.array(order[start:start+32]);g,_,kl=manual(w,x[ids],[2],mask[ids],acts[ids],old[ids],adv[ids],r[ids],entropy=.001)
                    if kl>.03 and applied:stop=True;break
                    g=[a/len(ids) for a in g];norm=math.sqrt(sum((a*a).sum() for a in g));scale=min(1,.5/max(norm,1e-12));step+=1;applied+=1
                    for i,a in enumerate(g):
                        a=a*scale;m[i]=.9*m[i]+.1*a;v[i]=.999*v[i]+.001*a*a
                        w[i]-=.003*(m[i]/(1-.9**step))/(np.sqrt(v[i]/(1-.999**step))+1e-8)
                if stop:break
        p=probabilities(forward(w,np.eye(2))[2][:,:2],[2],np.ones((2,2),bool))
        print("Independent Python reference bandit probabilities:",p.diagonal().tolist())
        self.assertTrue(np.all(p.diagonal()>.9))

    def test_progress_frontier_blocks_duplicate_receipts_and_counter_resets(self):
        frontier={};rewards=[]
        for counters in [{17:8},{},{17:8},{17:8},{17:16}]:
            gain=0
            for item,count in counters.items():
                old=frontier.get(item,0);count=min(count,64)
                if count>old:gain+=.06*(math.log1p(count)-math.log1p(old));frontier[item]=count
            rewards.append(gain)
        self.assertGreater(rewards[0],0);self.assertEqual(rewards[1:4],[0,0,0]);self.assertGreater(rewards[4],0)

    def test_declared_dimensions_and_no_production_python(self):
        lib=(ROOT/'learning/src/lib.rs').read_text();self.assertIn('640',lib);self.assertIn('[9,7,5,3,6,10,6,90]',lib.replace(' ',''))
        self.assertEqual(sum([9,7,5,3,6,10,6,90]),136)
        self.assertEqual(640*2+12,1292)
        for folder in ['app','launcher','learning/src']:
            for f in (ROOT/folder).glob('*.rs'):
                self.assertNotIn('Command::new("python',f.read_text())
        sensor=(ROOT/"app/sensor.rs").read_text();self.assertIn("matches!(stat,Stat::Mined(_)|Stat::Crafted(_))",sensor)
        app='\n'.join(p.read_text() for p in (ROOT/'app').glob('*.rs'))
        for forbidden in ['bot.goto(', 'bot.find_path(', 'bot.start_mining(', 'bot.block_interact(', 'bot.open_container_at(', 'bot.entity_interact(']:
            self.assertNotIn(forbidden,app)

if __name__=='__main__':unittest.main(verbosity=2)
