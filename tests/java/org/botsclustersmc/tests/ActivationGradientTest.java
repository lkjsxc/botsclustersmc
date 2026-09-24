package org.botsclustersmc.tests;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.botsclustersmc.core.*;
import org.botsclustersmc.training.*;

/** Metamorphic checks: diagnostics must not change the actual gradient or optimizer transaction. */
public final class ActivationGradientTest {
    private static int checks;
    private static void check(boolean ok){checks++;if(!ok)throw new AssertionError("activation integration check "+checks);}
    private static void same(double a,double b){check(Double.doubleToLongBits(a)==Double.doubleToLongBits(b));}
    private static void same(Gradient.Result a,Gradient.Result b){
        check(Arrays.equals(a.weights(),b.weights()));check(a.samples()==b.samples());
        same(a.valueLoss(),b.valueLoss());same(a.entropy(),b.entropy());same(a.importance(),b.importance());
    }
    private static List<Trajectory> batch(Policy policy,boolean varied) {
        List<Trajectory> result=new ArrayList<>();RandomSource rng=new RandomSource(802);
        Policy.Workspace workspace=new Policy.Workspace();
        for(int i=0;i<64;i++) {
            int task=varied?(i<19?i:10):0;
            float[] x=new float[Schema.INPUTS];x[0]=1;x[1]=(i%7-3)/3f;
            if(task<TaskBalance.TASKS)x[16+task]=1;
            boolean[] mask=varied?Schema.unrestrictedMask():Task.FORWARD_STOP.mask(0,false);
            List<Transition> steps=new ArrayList<>();
            // Identical next observation permits a two-step trace without inventing resets.
            for(int j=0;j<2;j++) {
                policy.forward(x,mask,workspace);var choice=Distribution.choose(workspace.probabilities,rng,false);
                steps.add(new Transition(x,mask,choice.actions(),choice.logProbability(),policy.updates(),j==0?.2f:1,4+j*4,x,mask,j==1));
            }
            result.add(new Trajectory(i,0,0,steps));
        }
        return result;
    }
    private static void gradients(Policy policy,boolean varied) {
        List<Trajectory> batch=batch(policy,varied);TaskBalance balance=TaskBalance.forBatch(batch);
        float[] weights=policy.copyWeights();List<float[]> observations=new ArrayList<>();
        for(var t:batch)for(var s:t.steps())observations.add(s.observation().clone());
        for(TaskBalance allocation:new TaskBalance[]{null,balance}) {
            var accumulator=new ActivationHealth.Accumulator(policy.updates(),Schema.HIDDEN,TaskBalance.TASKS+1);
            Gradient.Result baseline=Gradient.compute(policy,batch,allocation);
            Gradient.Result observed=Gradient.compute(policy,batch,allocation,accumulator);same(baseline,observed);
            var snapshot=accumulator.snapshot();check(snapshot.totalSamples()==128);
            int[] taskCounts=balance.counts();long[] measured=snapshot.samples();
            for(int i=0;i<taskCounts.length;i++)check(measured[i]==taskCounts[i]); // Raw, not loss-weighted counts.
            long[][] counts=new long[2][TaskBalance.TASKS+1];double[][] slopes=new double[2][TaskBalance.TASKS+1];
            Policy.Workspace workspace=new Policy.Workspace();
            for(var t:batch)for(var s:t.steps()) {
                policy.forward(s.observation(),s.mask(),workspace);int task=TaskBalance.task(s.observation());
                for(int layer=0;layer<2;layer++)for(float h:layer==0?workspace.h1:workspace.h2){
                    if(Math.abs(h)>ActivationHealth.SATURATION_THRESHOLD)counts[layer][task]++;
                    slopes[layer][task]+=1-(double)h*h;
                }
            }
            for(int layer=0;layer<2;layer++)for(int task=0;task<measured.length;task++) {
                double denominator=(double)measured[task]*Schema.HIDDEN;
                same(snapshot.saturation(layer)[task],denominator==0?-1:counts[layer][task]/denominator);
                double expected=denominator==0?-1:slopes[layer][task]/denominator;
                check(Math.abs(snapshot.meanSlope(layer)[task]-expected)<1e-12);
            }
            Adam optimizer=new Adam();
            var before=UpdateGuard.update(policy,optimizer,baseline.weights(),baseline.samples(),batch);
            var after=UpdateGuard.update(policy,optimizer,observed.weights(),observed.samples(),batch);
            check(before.update()!=null&&after.update()!=null);
            check(Arrays.equals(before.update().policy().copyWeights(),after.update().policy().copyWeights()));
            check(Arrays.equals(before.update().optimizer().first(),after.update().optimizer().first()));
            check(Arrays.equals(before.update().optimizer().second(),after.update().optimizer().second()));
            same(before.learningRate(),after.learningRate());check(before.backtracks()==after.backtracks());
            check(Arrays.equals(weights,policy.copyWeights()));
        }
        int index=0;for(var t:batch)for(var s:t.steps())check(Arrays.equals(observations.get(index++),s.observation()));
    }
    private static void learner(boolean reject,boolean future)throws Exception {
        Policy policy=Policy.initialize(8);List<Trajectory> batch=batch(future?new Policy(policy.copyWeights(),1,0):policy,false);
        float[] first=new float[Policy.PARAMETERS],second=new float[Policy.PARAMETERS];
        if(reject){first[Policy.B3]=1e10f;second[Policy.B3]=1e-20f;}
        AtomicReference<Throwable> failed=new AtomicReference<>();
        Learner learner=new Learner(policy,new Adam(first,second,0),3,128,65536,64,p->{},failed::set);
        try {
            check(learner.activationHealth()==null);
            for(var trajectory:batch)check(learner.offer(trajectory));
        }finally{learner.close();check(learner.awaitTermination(30000));}
        check(failed.get()==null);
        var measurement=learner.activationHealth();
        if(future){check(measurement==null);check(learner.stale.sum()==128);check(learner.policy().samples()==0);return;}
        check(measurement!=null&&measurement.epochMillis()>0);
        check(measurement.snapshot().totalSamples()==learner.updateSamples);
        check(measurement.snapshot().policyUpdates()==learner.policy().updates()-(reject?0:1));
        check(measurement.updateAccepted()!=reject);
        check(learner.policy().samples()==(reject?0:128));
        check(learner.guardRejectedSamples.sum()==(reject?128:0));
    }
    public static void main(String[] args)throws Exception {
        gradients(Policy.initialize(17),true);
        float[] saturated=Policy.initialize(4).copyWeights();
        for(int i=0;i<Schema.HIDDEN;i++) {
            saturated[Policy.B1+i]=i%3==0?20:i%3==1?-20:0;
            saturated[Policy.B2+i]=i%3==0?-20:i%3==1?20:0;
        }
        gradients(new Policy(saturated,0,0),true);
        learner(false,false);learner(true,false);learner(false,true);
        System.out.println("PASS activation integration: bit-identical gradients/Adam, raw samples, accepted/rejected/stale checks="+checks);
    }
}
