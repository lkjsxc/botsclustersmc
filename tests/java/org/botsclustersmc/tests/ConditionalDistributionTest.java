package org.botsclustersmc.tests;

import java.util.*;
import org.botsclustersmc.core.*;
import org.botsclustersmc.training.Exploration;

/** Exact finite joint enumeration is an oracle, not another conditional softmax implementation. */
public final class ConditionalDistributionTest {
    private static int checks;
    private static final int PARENT=Task.offset(6),CHILD=Task.offset(7);
    private static void check(boolean ok,String message) {
        checks++;if(!ok)throw new AssertionError(message);
    }
    private static void near(double a,double b,double tolerance,String message) {
        check(Double.isFinite(a)&&Double.isFinite(b)&&Math.abs(a-b)<=tolerance,message+": "+a+" != "+b);
    }
    private static void fails(Runnable call,String message) {
        boolean failed=false;try {call.run();}catch(IllegalArgumentException expected){failed=true;}
        check(failed,message);
    }
    private static boolean[] fixture() {
        boolean[] mask=Task.CRAFT_WORKBENCH.mask(46,true);
        Task.only(mask,6,0,1,2,3,5);Task.only(mask,7);
        int[][] slots={{0,7},{7,11,42},{45}};
        for(int op=1;op<=3;op++)for(int slot:slots[op-1])mask[Schema.slotOffset(op)+slot]=true;
        return mask;
    }
    private static float[] logits(long seed) {
        RandomSource rng=new RandomSource(seed);float[] result=new float[Schema.OUTPUTS];
        for(int i=0;i<result.length;i++)result[i]=rng.symmetric(2);return result;
    }
    private static double[] probabilities(float[] logits,boolean[] mask) {
        double[] p=new double[Schema.DISTRIBUTION];Distribution.probabilities(logits,mask,p);return p;
    }
    /** The first six heads in this fixture are deterministic. Flatten the whole remaining joint. */
    private static Map<Integer,Double> joint(float[] logits,boolean[] mask,boolean uniformPrior) {
        Map<Integer,Double> weights=new LinkedHashMap<>();double parentTotal=0;
        for(int op=0;op<6;op++)if(mask[PARENT+op])parentTotal+=uniformPrior?1:Math.exp(logits[PARENT+op]);
        for(int op=0;op<6;op++)if(mask[PARENT+op]) {
            double parent=(uniformPrior?1:Math.exp(logits[PARENT+op]))/parentTotal;
            if(!Schema.slotActive(op)) {weights.put(op*64,parent);continue;}
            int offset=Schema.slotOffset(op);double total=0;
            for(int slot=0;slot<64;slot++)if(mask[offset+slot])total+=uniformPrior?1:Math.exp(logits[CHILD+slot]);
            for(int slot=0;slot<64;slot++)if(mask[offset+slot])
                weights.put(op*64+slot,parent*(uniformPrior?1:Math.exp(logits[CHILD+slot]))/total);
        }
        return weights;
    }
    private static int[] action(int key) {
        int[] a=Schema.IDLE.clone();a[6]=key/64;a[7]=key%64;return a;
    }
    private static double objective(float[] logits,boolean[] mask,int[] action,double advantage,double beta) {
        double[] p=probabilities(logits,mask);
        return -advantage*Distribution.logProbability(p,action)-beta*Distribution.entropy(p);
    }
    private static void enumerate() {
        boolean[] mask=fixture();float[] a=logits(51),b=logits(903);
        double[] before=probabilities(a,mask),after=probabilities(b,mask);
        Map<Integer,Double> expected=joint(a,mask,false),other=joint(b,mask,false),prior=joint(a,mask,true);
        double sum=0,entropy=0,kl=0,ce=0;
        for(var entry:expected.entrySet()) {
            int key=entry.getKey();double p=entry.getValue();
            near(Math.exp(Distribution.logProbability(before,action(key))),p,1e-14,"enumerated joint probability");
            sum+=p;entropy-=p*Math.log(p);kl+=p*Math.log(p/other.get(key));ce-=prior.get(key)*Math.log(p);
        }
        near(sum,1,1e-14,"normalized joint");
        near(Distribution.entropy(before),entropy,1e-13,"joint entropy");
        near(Distribution.divergence(before,after),kl,1e-13,"joint KL weights EACH child by its parent");
        near(Distribution.divergence(before,before),0,1e-13,"identity KL");
        near(Exploration.loss(a,mask,1),ce,1e-13,"fixed joint prior cross entropy");
        // A high-probability slot in another branch cannot become a legal selected pair.
        int[] impossible=Schema.IDLE.clone();impossible[6]=3;impossible[7]=7;
        fails(()->Distribution.logProbability(before,impossible),"cross-branch action rejected");
        int[] counts=new int[6*64];RandomSource rng=new RandomSource(971);
        for(int n=0;n<40000;n++) {
            Distribution.Choice choice=Distribution.choose(before,rng,false);int[] x=choice.actions();int key=x[6]*64+x[7];
            check(expected.containsKey(key),"sample belongs to exact joint support");counts[key]++;
            near(choice.logProbability(),Math.log(expected.get(key)),1e-13,"sample likelihood");
            near(choice.entropy(),entropy,1e-13,"sample reports full joint entropy");
        }
        for(var entry:expected.entrySet()) {
            double p=entry.getValue(),tolerance=6*Math.sqrt(p*(1-p)/40000)+.001;
            near(counts[entry.getKey()]/40000.0,p,tolerance,"sample frequency");
        }
        int[] greedy=Distribution.choose(before,new RandomSource(1),true).actions();
        check(expected.containsKey(greedy[6]*64+greedy[7]),"greedy conditional support");
    }
    private static void gradients() {
        RandomSource rng=new RandomSource(142);
        for(int trial=0;trial<8;trial++) {
            boolean[] mask=fixture();float[] x=logits(300+trial);
            // Also exercise nontrivial gradients through the six non-menu heads.
            if(trial%2==0)for(int head=0;head<6;head++) {
                int off=Task.offset(head);
                for(int j=0;j<Schema.HEADS[head];j++)mask[off+j]=true;
            }
            double[] p=probabilities(x,mask);
            for(int op:new int[]{0,1,2,3,5})for(int term=0;term<3;term++) {
                int[] action=Distribution.choose(p,rng,false).actions();action[6]=op;
                action[7]=Schema.slotActive(op)?(op==1?7:op==2?11:45):0;
                double advantage=term==1?0:.73,beta=term==0?0:.037;
                float[] gradient=new float[Schema.OUTPUTS];
                Distribution.gradient(p,action,advantage,beta,gradient);
                for(int i=0;i<Schema.LOGITS;i++) {
                    float old=x[i],h=.002f;x[i]=old+h;double plus=objective(x,mask,action,advantage,beta);
                    x[i]=old-h;double minus=objective(x,mask,action,advantage,beta);x[i]=old;
                    near(gradient[i],(plus-minus)/(2*h),7e-5,"conditional gradient "+trial+"/"+op+"/"+term+"/"+i);
                }
                near(gradient[Schema.LOGITS],0,0,"policy gradient leaves value output untouched");
            }
            float[] gradient=new float[Schema.OUTPUTS];Exploration.addGradient(p,mask,.017,gradient);
            for(int i=0;i<Schema.LOGITS;i++) {
                float old=x[i],h=.002f;x[i]=old+h;double plus=Exploration.loss(x,mask,.017);
                x[i]=old-h;double minus=Exploration.loss(x,mask,.017);x[i]=old;
                near(gradient[i],(plus-minus)/(2*h),2e-6,"conditional prior gradient");
            }
        }
    }
    private static void boundaries() {
        boolean[] mask=fixture();float[] x=logits(73);
        double[] reuse=probabilities(x,mask);
        Task.only(mask,6,0,5);
        Distribution.probabilities(x,mask,reuse);
        for(int i=CHILD;i<Schema.DISTRIBUTION;i++)near(reuse[i],0,0,"inactive branch clears reused workspace");
        float[] gradient=new float[Schema.OUTPUTS];int[] idle=Schema.IDLE.clone();
        Distribution.gradient(reuse,idle,.8,.02,gradient);Exploration.addGradient(reuse,mask,.03,gradient);
        for(int i=CHILD;i<Schema.LOGITS;i++)near(gradient[i],0,0,"inactive child contributes no gradient");
        double[] modified=reuse.clone();modified[Schema.slotOffset(2)+7]=1;
        near(Distribution.divergence(reuse,modified),0,0,"inactive child KL ignored without zero times infinity");
        boolean[] empty=fixture();Arrays.fill(empty,Schema.slotOffset(2),Schema.slotOffset(2)+64,false);
        fails(()->probabilities(x,empty),"enabled parent with empty child rejected");
        fails(()->Distribution.probabilities(x,new boolean[Schema.LOGITS],reuse),"old transient mask shape rejected");
        fails(()->Distribution.probabilities(x,mask,new double[Schema.LOGITS]),"old probability shape rejected");
        fails(()->Schema.slotOffset(0),"inactive offset rejected");
        x[CHILD+63]=Float.NaN;fails(()->probabilities(x,mask),"even masked nonfinite slot logits rejected");
        x[CHILD+63]=1000;
        check(Double.isFinite(Exploration.loss(x,fixture(),.017)),"stable exploration with extreme logits");
        boolean[] disjoint=fixture();Task.only(disjoint,6,1,2,3);Task.only(disjoint,7);
        for(int op=1;op<=3;op++)disjoint[Schema.slotOffset(op)+op]=true;
        double[] p=probabilities(x,disjoint);
        for(int op=1;op<=3;op++)near(p[Schema.slotOffset(op)+op],1,0,"disjoint deterministic conditional");
        int[] a=Schema.IDLE.clone();a[6]=2;a[7]=2;Distribution.gradient(p,a,.7,.05,gradient);
        Exploration.addGradient(p,disjoint,.017,gradient);
        for(int i=CHILD;i<Schema.LOGITS;i++)near(gradient[i],0,0,"deterministic children have zero logit gradient");
        double[] missing=p.clone();missing[Schema.slotOffset(2)+2]=0;
        check(Double.isInfinite(Distribution.divergence(p,missing)),"loss of reached support has infinite KL");
    }
    private record Outcome(int[] action,double probability,double prior) {}
    /** Enumerate actuator-distinct tuples directly; no production likelihood/entropy helpers. */
    private static Map<String,Outcome> worldJoint(float[] x,boolean[] mask) {
        Map<String,Outcome> result=new LinkedHashMap<>();double total=0;int legal=0;
        for(int op=0;op<6;op++)if(mask[PARENT+op]){total+=Math.exp(x[PARENT+op]);legal++;}
        for(int op=0;op<6;op++)if(mask[PARENT+op]) {
            int[] a=Schema.IDLE.clone();a[6]=op;
            expandWorld(x,mask,a,op==4?6:0,Math.exp(x[PARENT+op])/total,1.0/legal,result);
        }
        return result;
    }
    private static void expandWorld(float[] x,boolean[] mask,int[] a,int head,double p,double prior,Map<String,Outcome> out) {
        if(head==6) {
            if(a[6]>=1&&a[6]<=3) {
                int off=CHILD+(a[6]-1)*64,n=0;double sum=0;
                for(int slot=0;slot<64;slot++)if(mask[off+slot]){sum+=Math.exp(x[CHILD+slot]);n++;}
                for(int slot=0;slot<64;slot++)if(mask[off+slot]) {
                    int[] b=a.clone();b[7]=slot;
                    out.put(Arrays.toString(b),new Outcome(b,p*Math.exp(x[CHILD+slot])/sum,prior/n));
                }
            }else out.put(Arrays.toString(a),new Outcome(a.clone(),p,prior));
            return;
        }
        int off=Task.offset(head),n=0;double sum=0;
        for(int j=0;j<Schema.HEADS[head];j++)if(mask[off+j]){sum+=Math.exp(x[off+j]);n++;}
        for(int j=0;j<Schema.HEADS[head];j++)if(mask[off+j]) {
            a[head]=j;expandWorld(x,mask,a,head+1,p*Math.exp(x[off+j])/sum,prior/n,out);
        }
    }
    private static double enumeratedLoss(float[] x,boolean[] mask,int[] a,double advantage,double beta,double priorWeight) {
        var joint=worldJoint(x,mask);double result=-advantage*Math.log(joint.get(Arrays.toString(a)).probability());
        for(Outcome e:joint.values())result+=beta*e.probability()*Math.log(e.probability())-priorWeight*e.prior()*Math.log(e.probability());
        return result;
    }
    private static void worldFocus() {
        boolean[] mask=fixture();Task.only(mask,6,0,1,2,3,4,5);
        for(int head=0;head<6;head++)Task.only(mask,head,Schema.IDLE[head],(Schema.IDLE[head]+1)%Schema.HEADS[head]);
        float[] x=logits(811),y=logits(992);double[] p=probabilities(x,mask),q=probabilities(y,mask);
        var expected=worldJoint(x,mask);var other=worldJoint(y,mask);
        double mass=0,priorMass=0,h=0,kl=0,ce=0;
        for(var entry:expected.entrySet()) {
            Outcome e=entry.getValue();double weight=e.probability();mass+=weight;priorMass+=e.prior();
            near(Math.exp(Distribution.logProbability(p,e.action())),weight,1e-14,"world-focus exact tuple probability");
            h-=weight*Math.log(weight);kl+=weight*Math.log(weight/other.get(entry.getKey()).probability());ce-=e.prior()*Math.log(weight);
        }
        near(mass,1,1e-13,"world-focus normalized joint");near(priorMass,1,1e-13,"world-focus normalized prior");
        near(Distribution.entropy(p),h,1e-12,"world-focus enumerated entropy");
        near(Distribution.divergence(p,q),kl,1e-12,"world-focus old-parent-weighted KL");
        near(Exploration.loss(x,mask,1),ce,1e-12,"world-focus enumerated prior CE");
        int[] opening=Schema.IDLE.clone();opening[6]=4;
        near(Math.exp(Distribution.logProbability(p,opening)),p[PARENT+4],1e-14,"opening marginalizes every unused world tuple");
        float[] score=new float[Schema.OUTPUTS];Distribution.gradient(p,opening,.7,0,score);
        for(int i=0;i<PARENT;i++)near(score[i],0,0,"opening has no world score gradient");
        for(int op=0;op<6;op++) {
            final int selected=op;Outcome e=expected.values().stream().filter(v->v.action()[6]==selected).findFirst().orElseThrow();
            for(int term=0;term<4;term++) {
                double advantage=term==0||term==3?.73:0,beta=term==1||term==3?.037:0,prior=term>=2?.017:0;
                float[] grad=new float[Schema.OUTPUTS];Distribution.gradient(p,e.action(),advantage,beta,grad);
                Exploration.addGradient(p,mask,prior,grad);
                for(int i=0;i<Schema.LOGITS;i++) {
                    float old=x[i],step=.002f;x[i]=old+step;double plus=enumeratedLoss(x,mask,e.action(),advantage,beta,prior);
                    x[i]=old-step;double minus=enumeratedLoss(x,mask,e.action(),advantage,beta,prior);x[i]=old;
                    near(grad[i],(plus-minus)/(2*step),7e-5,"world-focus enumerated derivative "+op+"/"+term+"/"+i);
                }
            }
        }
        Map<String,Integer> sampled=new HashMap<>();RandomSource rng=new RandomSource(622);
        for(int n=0;n<40000;n++) {
            Distribution.Choice choice=Distribution.choose(p,rng,false);String key=Arrays.toString(choice.actions());
            check(expected.containsKey(key),"sample has no ghost world controls");sampled.merge(key,1,Integer::sum);
            near(choice.logProbability(),Math.log(expected.get(key).probability()),1e-12,"world-focus sampled likelihood");
        }
        for(var entry:expected.entrySet()) {
            double weight=entry.getValue().probability();
            near(sampled.getOrDefault(entry.getKey(),0)/40000.0,weight,6*Math.sqrt(weight*(1-weight)/40000)+.001,"world-focus sample frequency");
        }
        int[] greedy=Distribution.choose(p,new RandomSource(1),true).actions();
        check(expected.containsKey(Arrays.toString(greedy)),"world-focus greedy support");
        for(int head=0;head<6;head++) {
            int[] invalid=opening.clone();invalid[head]=(Schema.IDLE[head]+1)%Schema.HEADS[head];
            fails(()->Distribution.logProbability(p,invalid),"noncanonical opening rejected");
        }
        // Only opening is reachable: empty unused heads must never be normalized or sampled.
        boolean[] onlyOpen=new boolean[Schema.DISTRIBUTION];Task.only(onlyOpen,6,4);
        double[] open=probabilities(x,onlyOpen);float[] gradient=new float[Schema.OUTPUTS];
        for(int i=0;i<PARENT;i++)near(open[i],0,0,"unreachable world probabilities cleared");
        check(Arrays.equals(Distribution.choose(open,new RandomSource(2),false).actions(),opening),"deterministic canonical opening");
        near(Distribution.logProbability(open,opening),0,0,"opening-only likelihood");near(Distribution.entropy(open),0,0,"opening-only entropy");
        Distribution.gradient(open,opening,.8,.03,gradient);Exploration.addGradient(open,onlyOpen,.02,gradient);
        for(float value:gradient)near(value,0,0,"unreachable heads have no score entropy or prior gradient");
        double[] changed=open.clone();changed[0]=1;
        near(Distribution.divergence(open,changed),0,0,"unreachable world KL ignores support changes");
        check(Double.isInfinite(Distribution.divergence(p,open)),"losing reached world branch has infinite KL");
        System.out.println("PASS conditional menu-focus tuples="+expected.size());
    }
    public static void main(String[] args) {
        MenuPairRegression.main(args);enumerate();gradients();boundaries();worldFocus();
        System.out.println("PASS conditional joint probability/gradient/prior/KL checks="+checks);
    }
}
