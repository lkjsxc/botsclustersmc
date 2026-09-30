package org.botsclustersmc.tests;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.jar.JarFile;
import org.botsclustersmc.core.*;
import org.botsclustersmc.training.*;

/** Synthetic observation/accounting fixtures, not Minecraft acquisition evidence. */
public final class LearningContextsTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void reject(Runnable operation) {
        try{operation.run();}catch(IllegalArgumentException|ArithmeticException expected){checks++;return;}
        throw new AssertionError("Expected context rejection");
    }
    private static Policy counters(Policy source,long updates,long samples){return new Policy(source.copyWeights(),updates,samples);}
    private static float[] observation(int task,float menu) {
        float[] x=new float[Schema.INPUTS];x[0]=1;x[42]=menu;
        if(task>=0&&task<18)x[16+task]=1;
        return x;
    }
    private static Trajectory single(long actor,float[] before,float[] after,int ticks,boolean terminal) {
        boolean[] mask=Schema.unrestrictedMask();
        return new Trajectory(actor,0,0,List.of(new Transition(before,mask,Schema.IDLE.clone(),0,0,0,ticks,after,mask,terminal)));
    }
    private static void classification() {
        check(Arrays.equals(Arrays.stream(Pocket.Menu.values()).map(Enum::name).toArray(String[]::new),
            new String[]{"CLOSED","INVENTORY","WORKBENCH","FURNACE","CHEST"}),"sensor menu order");
        for(int i=0;i<5;i++)check(LearningContexts.menu(i/4f)==i,"exact canonical menu");
        check(LearningContexts.menu(-0f)==0,"signed zero is closed");
        for(float value:new float[]{-.25f,1.25f,.1f,.49999f,Math.nextDown(.5f),Math.nextUp(.5f),
                Float.MIN_VALUE,Float.MAX_VALUE,Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY})
            check(LearningContexts.menu(value)==5,"no rounding noncanonical menu");
    }
    private static void counts()throws Exception {
        Policy initial=counters(Policy.initialize(12),17,9000);
        byte[] unchanged=PolicyFile.encode(initial);
        LearningContexts coverage=new LearningContexts(initial);
        var zero=coverage.snapshot();
        check(zero.totalSamples()==0&&zero.totalTicks()==0&&zero.baseSamples()==9000&&zero.policyUpdates()==17,"warm zero origin");
        List<Trajectory> batch=new ArrayList<>();
        long[] expectedSamples=new long[114],expectedTicks=new long[114];
        for(int task=0;task<19;task++)for(int menu=0;menu<6;menu++) {
            int cell=task*6+menu;
            float[] before=observation(task,menu==5?.123f:menu/4f);
            float[] after=observation((task+1)%18,(menu+1)%5/4f);
            batch.add(single(cell,before,after,cell%2==0?1:12000,cell%3==0));
            expectedSamples[cell]=1;expectedTicks[cell]=cell%2==0?1:12000;
        }
        coverage.accepted(batch,counters(initial,18,9114));
        var first=coverage.snapshot();
        check(Arrays.equals(first.samples(),expectedSamples)&&Arrays.equals(first.ticks(),expectedTicks),"independent task-major oracle");
        check(first.totalSamples()==114&&first.totalTicks()==57L*12001,"elapsed actor ticks");
        check(Arrays.equals(first.taskSamples(),Arrays.stream(TaskBalance.forBatch(batch).counts()).asLongStream().toArray()),"existing task totals unchanged");
        check(zero.totalSamples()==0&&Arrays.stream(zero.samples()).sum()==0,"old snapshot immutable");
        long[] samples=first.samples(),ticks=first.ticks();samples[0]=999;ticks[0]=999;
        check(first.samples()[0]==1&&first.ticks()[0]==1,"defensive accessors");
        var copy=new LearningContexts.Snapshot(9000,18,9114,first.samples(),first.ticks());
        var status=copy.status();
        check(status.get("learner_context_menu_order").equals("closed,inventory,workbench,furnace,chest,unknown"),"explicit menu labels");
        check(status.get("learner_context_layout").equals("task-major")&&status.get("learner_context_samples").equals(114L),"status scope/denominator");
        check(status.get("learned_task_samples_this_process").equals(Arrays.toString(copy.taskSamples())),"coherent task/status marginal");
        coverage.accepted(batch,counters(initial,19,9228));
        for(int i=0;i<114;i++) {
            check(coverage.snapshot().samples()[i]==2,"cumulative samples");
            check(coverage.snapshot().ticks()[i]==2*expectedTicks[i],"cumulative ticks");
            check(batch.get(i).steps().getFirst().observation()[42]==(i%6==5?.123f:(i%6)/4f),"original observations untouched");
        }
        check(Arrays.equals(unchanged,PolicyFile.encode(initial)),"source policy bytes unchanged");
        var retained=coverage.snapshot();
        reject(()->coverage.accepted(batch,counters(initial,19,9342)));
        reject(()->coverage.accepted(batch,counters(initial,21,9342)));
        reject(()->coverage.accepted(batch,counters(initial,20,9341)));
        reject(()->coverage.accepted(List.of(),counters(initial,20,9228)));
        check(coverage.snapshot()==retained,"rejected accounting cannot partially publish");
        reject(()->new LearningContexts.Snapshot(1,0,0,new long[114],new long[114]));
        reject(()->new LearningContexts.Snapshot(0,0,0,new long[113],new long[114]));
        reject(()->new LearningContexts.Snapshot(0,0,1,new long[114],new long[114]));
        long[] bad=new long[114];bad[0]=1;
        reject(()->new LearningContexts.Snapshot(0,0,0,new long[114],bad));
        reject(()->new LearningContexts.Snapshot(0,0,1,bad,new long[114]));
        long[] overflow=new long[114];overflow[0]=Long.MAX_VALUE;overflow[1]=1;
        reject(()->new LearningContexts.Snapshot(0,0,Long.MAX_VALUE,overflow,overflow));
        float[] ambiguous=observation(0,.5f);ambiguous[17]=1;
        LearningContexts unknown=new LearningContexts(Policy.initialize(3));
        unknown.accepted(List.of(single(0,ambiguous,observation(1,0),5,true)),counters(initial,1,1));
        check(unknown.snapshot().samples()[18*6+2]==1,"ambiguous task stays unlabelled");
    }
    private static Trajectory fragment(Policy policy,long actor,long version) {
        RandomSource rng=new RandomSource(444);
        boolean[] mask=Task.FORWARD_STOP.mask(0,false);
        float[] x=observation(0,0);x[1]=1;
        Policy.Workspace workspace=new Policy.Workspace();policy.forward(x,mask,workspace);
        List<Transition> steps=new ArrayList<>();
        for(int i=0;i<32;i++) {
            var action=Distribution.choose(workspace.probabilities,rng,false);
            steps.add(new Transition(x,mask,action.actions(),action.logProbability(),version,1,4,x,mask,i==31));
        }
        return new Trajectory(actor,0,0,steps);
    }
    private static void numerical()throws Exception {
        Policy initial=Policy.initialize(8);Adam optimizer=new Adam();
        List<Trajectory> batch=List.of(fragment(initial,0,0));
        TaskBalance balance=TaskBalance.forBatch(batch);
        var before=Gradient.compute(initial,batch,balance);
        var result=UpdateGuard.update(initial,optimizer,before.weights(),before.samples(),batch);
        check(result.update()!=null,"valid synthetic accepted update");
        Policy next=result.update().policy();byte[] frozen=PolicyFile.encode(next);
        LearningContexts contexts=new LearningContexts(initial);contexts.accepted(batch,next);
        var after=Gradient.compute(initial,batch,balance);
        check(Arrays.equals(before.weights(),after.weights())&&before.samples()==after.samples(),"gradient bitwise unchanged");
        check(Double.doubleToRawLongBits(before.valueLoss())==Double.doubleToRawLongBits(after.valueLoss())
            &&Double.doubleToRawLongBits(before.entropy())==Double.doubleToRawLongBits(after.entropy())
            &&Double.doubleToRawLongBits(before.importance())==Double.doubleToRawLongBits(after.importance()),"all gradient statistics unchanged");
        var repeated=UpdateGuard.update(initial,optimizer,after.weights(),after.samples(),batch);
        check(Arrays.equals(frozen,PolicyFile.encode(repeated.update().policy())),"identical guarded policy update");
        check(Arrays.equals(frozen,PolicyFile.encode(next))&&optimizer.step()==0,"observer changes no policy/optimizer");
        check(contexts.snapshot().samples()[0]==32&&contexts.snapshot().ticks()[0]==128,"fragment contributes each start once");
    }
    private static void learner(boolean pathological)throws Exception {
        Policy initial=Policy.initialize(8);
        float[] first=new float[Policy.PARAMETERS],second=new float[Policy.PARAMETERS];
        if(pathological){first[Policy.B3]=1e10f;second[Policy.B3]=1e-20f;}
        Adam optimizer=new Adam(first,second,0);
        AtomicReference<Throwable> failed=new AtomicReference<>();AtomicInteger published=new AtomicInteger();
        Learner learner=new Learner(initial,optimizer,1,8,32,1,p->published.incrementAndGet(),failed::set);
        try {
            check(learner.contexts().totalSamples()==0,"new learner no inherited contexts");
            learner.pause(true);
            check(!learner.offer(fragment(initial,2,0)),"paused offer rejected");
            learner.pause(false);
            check(learner.offer(fragment(initial,0,100)),"future-version fixture offered");
            check(learner.offer(fragment(initial,1,0)),"valid fixture offered");
            learner.close();
            check(learner.awaitTermination(15000),"learner drains and stops");
            check(failed.get()==null,"no learner failure: "+failed.get());
            check(learner.stale.sum()==32&&learner.rejected.sum()==32,"stale and rejected samples excluded");
            var coverage=learner.contexts();
            if(pathological) {
                check(learner.guardRejectedSamples.sum()==32&&published.get()==0,"guard really rejected fixture");
                check(coverage.totalSamples()==0&&coverage.totalTicks()==0&&learner.policy().samples()==0,"guard-rejected batch not counted");
            } else {
                check(published.get()==1&&learner.policy().samples()==32,"one accepted batch");
                check(coverage.totalSamples()==32&&coverage.totalTicks()==128&&coverage.policyUpdates()==learner.policy().updates(),"accepted-only coherent counters");
                check(Arrays.equals(coverage.taskSamples(),learner.taskSamples()),"public task marginal agrees");
            }
        } finally {learner.close();check(learner.awaitTermination(15000),"no leaked learner");}
    }
    private static void concurrentSnapshots()throws Exception {
        Policy initial=Policy.initialize(18);LearningContexts coverage=new LearningContexts(initial);
        List<Trajectory> batch=List.of(single(0,observation(11,.5f),observation(11,0),7,true));
        AtomicReference<Throwable> failed=new AtomicReference<>();
        java.util.concurrent.CountDownLatch ready=new java.util.concurrent.CountDownLatch(1);
        Thread writer=new Thread(()->{
            try {
                ready.await();
                for(int i=1;i<=128;i++)coverage.accepted(batch,counters(initial,i,i));
            } catch(Throwable error){failed.set(error);}
        },"context-fixture-writer");
        var original=coverage.snapshot();writer.start();ready.countDown();
        do {
            var snapshot=coverage.snapshot();long count=snapshot.totalSamples();
            check(snapshot.samples()[68]==count&&snapshot.ticks()[68]==count*7,"concurrent arrays stay paired");
            check(snapshot.policyUpdates()==count&&snapshot.trainedSamples()==count,"concurrent snapshot identity");
            check(snapshot.taskSamples()[11]==count&&snapshot.totalTicks()==count*7,"concurrent marginals");
            Thread.yield();
        } while(writer.isAlive());
        writer.join();
        check(failed.get()==null&&coverage.snapshot().totalSamples()==128,"bounded writer completes without failure");
        check(original.totalSamples()==0&&original.ticks()[68]==0,"retained snapshot never mutates");
    }
    public static void main(String[] args)throws Exception {
        classification();counts();numerical();learner(false);learner(true);concurrentSnapshots();
        try(JarFile jar=new JarFile("dist/botsclustersmc.jar")) {
            check(jar.stream().noneMatch(e->e.getName().contains("LearningContexts")),"observer absent from inference artifact");
        }
        System.out.println("PASS accepted learning contexts: "+checks+" checks");
    }
}
