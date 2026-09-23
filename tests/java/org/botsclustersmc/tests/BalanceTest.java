package org.botsclustersmc.tests;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.botsclustersmc.core.*;
import org.botsclustersmc.training.*;

/** Bounded weighting and parallel-gradient parity, not learned-skill evidence. */
public final class BalanceTest {
    private static int checks;
    private static void check(boolean yes){checks++;if(!yes)throw new AssertionError("check "+checks);}
    private static void near(double a,double b,double tolerance){check(Math.abs(a-b)<=tolerance);}
    private static float[] observation(int task) {
        float[] x=new float[Schema.INPUTS];x[0]=1;if(task>=0)x[16+task]=1;return x;
    }
    public static void main(String[] args)throws Exception {
        int[] counts=new int[19];counts[0]=500;counts[5]=12;
        TaskBalance b=TaskBalance.fromCounts(counts);double[] weights=b.weights();
        near(weights[0],416.0/500,1e-12);near(weights[5],8,1e-12);
        counts[0]=0;check(b.counts()[0]==500);weights[0]=99;check(b.weights()[0]<1);
        for(int task=0;task<18;task++)check(TaskBalance.task(observation(task))==task);
        check(TaskBalance.task(observation(-1))==TaskBalance.UNLABELLED);
        float[] ambiguous=observation(0);ambiguous[17]=1;check(TaskBalance.task(ambiguous)==TaskBalance.UNLABELLED);
        RandomSource random=new RandomSource(8329);
        for(int trial=0;trial<1000;trial++) {
            counts=new int[19];counts[random.nextInt(19)]=1;
            for(int i=0;i<19;i++)if(random.unit()<.6)counts[i]+=random.nextInt(1000);
            b=TaskBalance.fromCounts(counts);weights=b.weights();double mass=0;int total=0;
            for(int i=0;i<19;i++){mass+=weights[i]*counts[i];total+=counts[i];check(weights[i]>=0&&weights[i]<=8);check((counts[i]>0)==(weights[i]>0));}
            near(mass,total,1e-8);
        }
        Policy policy=Policy.initialize(64);Policy.Workspace work=new Policy.Workspace();
        List<Trajectory> batch=new ArrayList<>();
        for(int i=0;i<32;i++) {
            int task=i<30?5:2;float[] x=observation(task);x[1]=(i%3-1)*.1f;
            boolean[] mask=Task.at(task).mask(0,false);policy.forward(x,mask,work);
            Distribution.Choice choice=Distribution.choose(work.probabilities,random,false);
            Transition step=new Transition(x,mask,choice.actions(),choice.logProbability(),0,i%2==0?1:-1,4,x,mask,true);
            batch.add(new Trajectory(i,0,0,List.of(step)));
        }
        b=TaskBalance.forBatch(batch);check(b.counts()[5]==30&&b.counts()[2]==2);
        Gradient.Result serial=Gradient.compute(policy,batch,b);
        Gradient.Result left=Gradient.compute(policy,batch.subList(0,17),b),right=Gradient.compute(policy,batch.subList(17,32),b);
        for(int i=0;i<Policy.PARAMETERS;i++)near(serial.weights()[i],left.weights()[i]+right.weights()[i],1e-5);
        near(serial.valueLoss(),left.valueLoss()+right.valueLoss(),1e-9);
        near(serial.entropy(),left.entropy()+right.entropy(),1e-9);
        check(serial.samples()==32&&left.samples()+right.samples()==32);
        List<Trajectory> guardBatch=new ArrayList<>();Transition rare=batch.get(31).steps().get(0),common=batch.get(0).steps().get(0);
        for(int i=0;i<160;i++)guardBatch.add(new Trajectory(i,0,0,List.of(i==4?rare:common)));
        List<Transition> selected=UpdateGuard.observations(guardBatch);
        check(selected.size()==128&&selected.contains(rare));
        check(selected.stream().filter(s->s==rare).count()==1); // Index four was missed by the old even stride.
        AtomicReference<Throwable> error=new AtomicReference<>();
        Learner learner=new Learner(policy,new Adam(),2,64,512,100,p->{},error::set);
        for(Trajectory trajectory:batch)check(learner.offer(trajectory));
        learner.close();check(learner.awaitTermination(10000));check(error.get()==null);
        long[] accepted=learner.taskSamples();check(accepted[5]==30&&accepted[2]==2);
        check(Arrays.stream(accepted).sum()==learner.policy().samples());
        accepted[5]=999;check(learner.taskSamples()[5]==30);
        System.out.println("PASS bounded task balance checks="+checks);
    }
}
