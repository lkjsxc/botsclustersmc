package org.botsclustersmc.tests;

import org.botsclustersmc.core.*;
import org.botsclustersmc.training.*;
import java.util.*;

/** Stability checks use synthetic distributions; they do not certify Minecraft skills. */
public final class UpdateTest {
    private static int checks;
    private static void check(boolean ok){checks++;if(!ok)throw new AssertionError("check "+checks);}
    private static void near(double a,double b,double tolerance){check(Math.abs(a-b)<=tolerance);}
    public static void main(String[] args)throws Exception {
        ActivationHealthTest.main(args);ActivationGradientTest.main(args);
        RandomSource random=new RandomSource(63);float[] logits=new float[Schema.OUTPUTS];
        for(int i=0;i<logits.length;i++)logits[i]=random.symmetric(3);
        for(boolean[] mask:new boolean[][]{Schema.unrestrictedMask(),Task.FORWARD_STOP.mask(0,false)}) {
            double[] p=new double[Schema.DISTRIBUTION];Distribution.probabilities(logits,mask,p);
            float[] g=new float[Schema.OUTPUTS];Exploration.addGradient(p,mask,.017,g);
            for(int i=0;i<Schema.LOGITS;i++) {
                float old=logits[i],h=.001f;logits[i]=old+h;double plus=Exploration.loss(logits,mask,.017);
                logits[i]=old-h;double minus=Exploration.loss(logits,mask,.017);logits[i]=old;
                near(g[i],(plus-minus)/(2*h),1e-5);
            }
            near(Distribution.divergence(p,p),0,1e-12);
        }
        boolean[] mask=Task.FORWARD_STOP.mask(0,false);Arrays.fill(logits,0);logits[0]=1000;
        double[] p=new double[Schema.DISTRIBUTION];Distribution.probabilities(logits,mask,p);
        float[] prior=new float[Schema.OUTPUTS];Exploration.addGradient(p,mask,.005,prior);
        check(prior[0]>0&&prior[1]<0&&prior[2]<0);check(Double.isFinite(Exploration.loss(logits,mask,.005)));
        double[] before=new double[Schema.DISTRIBUTION],after=new double[Schema.DISTRIBUTION];int offset=0;
        for(int size:Schema.HEADS){before[offset]=after[offset]=1;offset+=size;}
        int child=Task.offset(7);before[child]=.5;before[child+1]=.5;
        after[child]=.9;after[child+1]=.1;
        near(Distribution.divergence(before,after),0,1e-12); // GUI slot is inactive.
        after[child]=1;after[child+1]=0;near(Distribution.divergence(before,after),0,1e-12);
        after[child]=.9;after[child+1]=.1;
        int parent=Task.offset(6);before[parent]=after[parent]=0;before[parent+1]=after[parent+1]=1;
        near(Distribution.divergence(before,after),.5*Math.log(.5/.9)+.5*Math.log(.5/.1),1e-12);
        Policy policy=Policy.initialize(8);Adam adam=new Adam();Policy.Workspace workspace=new Policy.Workspace();
        List<Trajectory> batch=new ArrayList<>();
        for(int i=0;i<64;i++) {
            float[] x=new float[Schema.INPUTS];x[0]=1;x[1]=i%2==0?1:-1;
            policy.forward(x,mask,workspace);Distribution.Choice action=Distribution.choose(workspace.probabilities,random,false);
            Transition step=new Transition(x,mask,action.actions(),action.logProbability(),policy.updates(),1,4,x,mask,true);
            batch.add(new Trajectory(i,0,0,List.of(step)));
        }
        Gradient.Result gradient=Gradient.compute(policy,batch);
        UpdateGuard.Result result=UpdateGuard.update(policy,adam,gradient.weights(),gradient.samples(),batch);
        check(result.update()!=null);check(result.learningRate()>0);check(result.change().mean()<=.005);check(result.change().maximum()<=.05);
        check(policy.updates()==0&&adam.step()==0);check(result.update().policy().samples()==64&&result.update().optimizer().step()==1);
        check(UpdateGuard.observations(batch).size()==64);
        float[] first=new float[Policy.PARAMETERS],second=new float[Policy.PARAMETERS],zero=new float[Policy.PARAMETERS];
        first[Policy.B3]=1000;second[Policy.B3]=.0001f;
        Adam strongMomentum=new Adam(first,second,0);
        UpdateGuard.Result reduced=UpdateGuard.update(policy,strongMomentum,zero,64,batch);
        check(reduced.update()!=null&&reduced.backtracks()>0);check(reduced.change().mean()<=.005);
        first[Policy.B3]=1e10f;second[Policy.B3]=1e-20f;
        Adam pathological=new Adam(first,second,0);
        UpdateGuard.Result rejected=UpdateGuard.update(policy,pathological,zero,64,batch);
        check(rejected.update()==null&&rejected.backtracks()==12);check(pathological.step()==0&&policy.updates()==0);
        System.out.println("PASS guarded update and exploration checks="+checks);
    }
}
