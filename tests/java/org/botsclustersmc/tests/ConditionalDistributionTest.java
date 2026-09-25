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
    public static void main(String[] args) {
        MenuPairRegression.main(args);enumerate();gradients();boundaries();
        System.out.println("PASS conditional joint probability/gradient/prior/KL checks="+checks);
    }
}
