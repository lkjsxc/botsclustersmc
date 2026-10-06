package org.botsclustersmc.tests;

import java.util.*;
import java.util.jar.JarFile;
import org.botsclustersmc.core.*;
import org.botsclustersmc.training.*;

/** Independent local derivative and routing checks; no Minecraft skill claim. */
public final class CriticFeaturesTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static boolean same(float a,float b){return Float.floatToRawIntBits(a)==Float.floatToRawIntBits(b);}
    private static void equal(float[] a,float[] b,String message){for(int i=0;i<a.length;i++)check(same(a[i],b[i]),message+" @"+i);}
    private static float[] observation(int task,Random random){
        float[] x=new float[Schema.INPUTS];
        for(int i=0;i<x.length;i++)x[i]=random.nextFloat()-.5f;
        Arrays.fill(x,16,34,0);if(task>=0)x[16+task]=1;
        return x;
    }
    private static boolean critic(int index){
        return index==Policy.B3+Schema.LOGITS
            || index>=Policy.W3+Schema.LOGITS*Schema.HIDDEN&&index<Policy.W3+(Schema.LOGITS+1)*Schema.HIDDEN;
    }
    private static double localLoss(Policy p,float[] x,float[] output,float[] fixedFeatures,boolean detach){
        Policy.Workspace w=new Policy.Workspace();p.forward(x,Schema.unrestrictedMask(),w);
        double loss=0;for(int i=0;i<Schema.LOGITS;i++)loss+=(double)w.logits[i]*output[i];
        if(!detach)return loss+(double)w.logits[Schema.LOGITS]*output[Schema.LOGITS];
        float[] weights=p.copyWeights();double value=weights[Policy.B3+Schema.LOGITS];
        for(int i=0;i<Schema.HIDDEN;i++)value+=(double)weights[Policy.W3+Schema.LOGITS*Schema.HIDDEN+i]*fixedFeatures[i];
        return loss+value*output[Schema.LOGITS];
    }
    private static void derivatives()throws Exception {
        Random random=new Random(7731);Policy p=Policy.initialize(37);
        byte[] original=PolicyFile.encode(p);int changedTrunk=0;
        for(int task=-1;task<18;task++) {
            float[] x=observation(task,random),output=new float[Schema.OUTPUTS];
            for(int i=0;i<output.length;i++)output[i]=(random.nextFloat()-.5f)*.4f;
            output[Schema.LOGITS]=.5f;
            float[] originalOutput=output.clone(),originalInput=x.clone();
            Policy.Workspace w=new Policy.Workspace();p.forward(x,Schema.unrestrictedMask(),w);
            float[] h1=w.h1.clone(),h2=w.h2.clone(),logits=w.logits.clone();
            float[] actual=new float[Policy.PARAMETERS],full=new float[Policy.PARAMETERS],expected=new float[Policy.PARAMETERS];
            CriticFeatures.backward(p,x,w,output,actual);p.backward(x,w,output,full);
            if(task!=12)equal(actual,full,"every other task keeps the exact original derivative");
            else {
                float[] actor=output.clone();actor[Schema.LOGITS]=0;p.backward(x,w,actor,expected);
                for(int i=0;i<actual.length;i++) {
                    check(same(actual[i],critic(i)?full[i]:expected[i]),"independent tensor support @"+i);
                    if(i<Policy.W3&&!same(actual[i],full[i]))changedTrunk++;
                }
                float[] valueOnly=new float[Schema.OUTPUTS];valueOnly[Schema.LOGITS]=.5f;
                float[] valueGradient=new float[Policy.PARAMETERS];CriticFeatures.backward(p,x,w,valueOnly,valueGradient);
                for(int i=0;i<valueGradient.length;i++)if(!critic(i))check(valueGradient[i]==0,"value cannot update actor features");
                check(valueGradient[Policy.B3+Schema.LOGITS]==.5f,"value readout still learns");
                check(Arrays.stream(toDouble(Arrays.copyOf(actual,Policy.W3))).anyMatch(v->v!=0),"actor features still learn");
                int[] indices={0,16,28,Policy.B1,Policy.B1-1,Policy.W2,Policy.B2,Policy.W3,
                    Policy.W3+Schema.LOGITS*Schema.HIDDEN,Policy.B3,Policy.B3+Schema.LOGITS};
                for(int k=0;k<160;k++) {
                    int index=k<indices.length?indices[k]:random.nextInt(Policy.PARAMETERS);
                    float[] weights=p.copyWeights();float center=weights[index],step=.01f;
                    weights[index]=center+step;double plus=localLoss(new Policy(weights,0,0),x,output,h2,true);
                    weights[index]=center-step;double minus=localLoss(new Policy(weights,0,0),x,output,h2,true);
                    double numerical=(plus-minus)/(2*step);
                    check(Math.abs(numerical-actual[index])<.0007,"finite-difference detached local loss @"+index);
                }
                float[] accumulated=new float[Policy.PARAMETERS];Arrays.fill(accumulated,.25f);
                CriticFeatures.backward(p,x,w,output,accumulated);
                for(int i=0;i<actual.length;i++)check(Math.abs(accumulated[i]-(actual[i]+.25f))<1e-6,"additive gradient accumulation");
            }
            equal(output,originalOutput,"caller derivative unchanged");equal(x,originalInput,"observation unchanged");
            equal(w.h1,h1,"first features unchanged");equal(w.h2,h2,"second features unchanged");equal(w.logits,logits,"forward output unchanged");
        }
        check(changedTrunk>0,"ablation actually removes nonzero shared-feature contribution");
        check(Arrays.equals(original,PolicyFile.encode(p)),"policy unchanged");
    }
    private static double[] toDouble(float[] x){double[] a=new double[x.length];for(int i=0;i<a.length;i++)a[i]=x[i];return a;}
    private static void acceptedUpdate()throws Exception {
        Policy p=Policy.initialize(8);RandomSource rng=new RandomSource(44);Policy.Workspace w=new Policy.Workspace();
        boolean[] mask=Task.MINE_COBBLESTONE.mask(0,false);float[] x=observation(12,new Random(19));
        p.forward(x,mask,w);List<Trajectory> batch=new ArrayList<>();
        for(int i=0;i<32;i++) {
            var action=Distribution.choose(w.probabilities,rng,false);
            var t=new Transition(x,mask,action.actions(),action.logProbability(),0,-.15f,5,x,mask,true);
            batch.add(new Trajectory(i,0,0,List.of(t)));
        }
        var result=Gradient.compute(p,batch);check(result.samples()==32,"ordinary sample denominator");
        check(result.weights()[Policy.B3+Schema.LOGITS]!=0,"critic output gradient present in real learner computation");
        var update=new Adam().update(p,result.weights(),result.samples(),.0003);
        check(update.policy().updates()==1&&update.policy().samples()==32,"still learning, not a freeze");
        float[] next=update.policy().copyWeights(),before=p.copyWeights();int actorChanged=0,valueChanged=0;
        for(int i=0;i<next.length;i++)if(!same(next[i],before[i])){if(critic(i))valueChanged++;else actorChanged++;}
        check(actorChanged>0&&valueChanged>0,"actor and critic readout both updated");
    }
    public static void main(String[] args)throws Exception {
        derivatives();acceptedUpdate();
        try(JarFile jar=new JarFile("dist/botsclustersmc.jar")){
            check(jar.stream().noneMatch(e->e.getName().contains("CriticFeatures")),"no training ablation in inference artifact");
        }
        System.out.println("PASS task-12 critic feature detachment: "+checks+" checks; not learned skill evidence");
    }
}
