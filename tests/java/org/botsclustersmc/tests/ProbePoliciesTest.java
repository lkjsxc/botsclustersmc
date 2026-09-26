package org.botsclustersmc.tests;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.botsclustersmc.core.*;
import org.botsclustersmc.training.*;

/** Behavior-version accounting is not a frozen-policy certificate or a learning signal. */
public final class ProbePoliciesTest {
    private static int checks;
    private interface Attempt {void run()throws Exception;}
    private static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    private static void rejects(Attempt attempt,String why)throws Exception{
        try{attempt.run();}catch(IllegalArgumentException|ArithmeticException expected){checks++;return;}
        throw new AssertionError("Accepted "+why);
    }
    public static void main(String[] args)throws Exception{
        traces();totals();concurrent();coupled();noninterference();
        System.out.println("PASS probe behavior-policy accounting checks="+checks);
    }
    private static void traces()throws Exception{
        var trace=new ProbePolicies.Trace();var empty=trace.snapshot();
        check(empty.equals(new ProbePolicies.Usage(0,-1,-1,0))&&!empty.mixed(),"absent is not single-policy evidence");
        trace.observe(5);trace.observe(5);var first=trace.snapshot();
        check(first.equals(new ProbePolicies.Usage(2,5,5,0))&&!first.mixed(),"same version repeated");
        trace.observe(100);trace.observe(7);trace.observe(5);
        check(trace.snapshot().equals(new ProbePolicies.Usage(5,5,100,3))&&trace.snapshot().mixed(),"range and transitions are distinct");
        check(first.equals(new ProbePolicies.Usage(2,5,5,0)),"immutable earlier observation");
        var before=trace.snapshot();rejects(()->trace.observe(-1),"negative version");check(before.equals(trace.snapshot()),"invalid observation is nonmutating");
        trace.clear();check(trace.snapshot().equals(empty),"interrupted/new episode clears all usage");
        trace.observe(Long.MAX_VALUE);trace.observe(0);
        check(trace.snapshot().maximum()-trace.snapshot().minimum()==Long.MAX_VALUE,"full nonnegative range does not overflow");
        for(long[] v:new long[][]{{-1,-1,-1,0},{0,0,0,0},{1,0,1,0},{1,0,0,1},{2,0,0,1},{2,0,1,2},{2,1,0,1}})
            rejects(()->new ProbePolicies.Usage(v[0],v[1],v[2],v[3]),"inconsistent usage");
    }
    private static void totals()throws Exception{
        var stats=new ProbePolicies();var single=new ProbePolicies.Usage(4,5,5,0);var mixed=new ProbePolicies.Usage(9,5,17,3);
        var original=stats.snapshot();
        for(var row:original)check(row.singleTrials()==0&&row.mixedTrials()==0&&row.lastMinimum()==-1,"absent task sentinel");
        for(Course.Kind kind:List.of(Course.Kind.PRACTICE,Course.Kind.EXAM))stats.record(Task.CRAFT_WOOD_PICK,kind,mixed,true);
        check(Arrays.equals(original,stats.snapshot()),"practice and exams never enter probe totals");
        stats.record(Task.CRAFT_WOOD_PICK,Course.Kind.PROBE,single,true);
        stats.record(Task.CRAFT_WOOD_PICK,Course.Kind.PROBE,single,false);
        stats.record(Task.CRAFT_WOOD_PICK,Course.Kind.PROBE,mixed,true);
        stats.record(Task.CRAFT_WOOD_PICK,Course.Kind.PROBE,mixed,false);
        var row=stats.snapshot()[11];
        check(row.equals(new ProbePolicies.Totals(2,1,2,1,26,6,12,5,17)),"all failed/successful probes counted exactly");
        for(int i=0;i<18;i++)if(i!=11)check(original[i].equals(stats.snapshot()[i]),"other tasks unchanged");
        var copy=stats.snapshot();copy[11]=null;check(stats.snapshot()[11].equals(row),"snapshot array does not alias");
        rejects(()->stats.record(Task.CRAFT_WOOD_PICK,Course.Kind.PROBE,new ProbePolicies.Usage(0,-1,-1,0),true),"empty trial");
        check(stats.snapshot()[11].equals(row),"invalid trial not counted");
        var overflow=new ProbePolicies();var large=new ProbePolicies.Usage(Long.MAX_VALUE,0,0,0);
        overflow.record(Task.FORWARD_STOP,Course.Kind.PROBE,large,true);var saved=overflow.snapshot()[0];
        rejects(()->overflow.record(Task.FORWARD_STOP,Course.Kind.PROBE,single,false),"decision count overflow");
        check(saved.equals(overflow.snapshot()[0]),"overflow does not partially publish counts");
    }
    private static void concurrent()throws Exception{
        var stats=new ProbePolicies();var failed=new AtomicReference<Throwable>();var start=new CountDownLatch(1);List<Thread> threads=new ArrayList<>();
        for(int actor=0;actor<8;actor++){
            final int id=actor;
            Thread thread=new Thread(()->{try{start.await();for(int n=0;n<2000;n++){
                var trace=new ProbePolicies.Trace();trace.observe(id);trace.observe(id+1);
                stats.record(Task.CRAFT_WOOD_PICK,Course.Kind.PROBE,trace.snapshot(),n%2==0);
                var row=stats.snapshot()[11];
                if(row.decisions()!=2*row.mixedTrials()||row.policyChanges()!=row.mixedTrials()||row.mixedSuccesses()>row.mixedTrials())
                    throw new AssertionError("Torn concurrent report");
            }}catch(Throwable error){failed.compareAndSet(null,error);}});
            threads.add(thread);thread.start();
        }
        start.countDown();for(Thread thread:threads){thread.join(20000);check(!thread.isAlive(),"owner finished");}
        check(failed.get()==null,"concurrency healthy: "+failed.get());var row=stats.snapshot()[11];
        check(row.singleTrials()==0&&row.mixedTrials()==16000&&row.mixedSuccesses()==8000&&row.decisions()==32000,"concurrent totals");
    }
    private static void coupled()throws Exception{
        var outcomes=new LessonOutcomes();var failed=new AtomicReference<Throwable>();
        var single=new ProbePolicies.Usage(1,3,3,0);var mixed=new ProbePolicies.Usage(2,3,7,1);
        List<Thread> owners=new ArrayList<>();
        for(int i=0;i<4;i++){
            Thread owner=new Thread(()->{try{for(int n=0;n<2000;n++){
                outcomes.record(Task.CRAFT_WOOD_PICK,Course.Kind.PROBE,n%3==0,n%2==0?single:mixed);
                LessonOutcomes.Totals row=outcomes.snapshot()[11];var use=row.policyUse();
                if(row.probeTrials()!=use.singleTrials()+use.mixedTrials()
                        ||row.probeSuccesses()!=use.singleSuccesses()+use.mixedSuccesses())
                    throw new AssertionError("Total and policy partition observed at different times");
            }}catch(Throwable error){failed.compareAndSet(null,error);}});
            owners.add(owner);owner.start();
        }
        for(Thread owner:owners){owner.join(20000);check(!owner.isAlive(),"coherent report owner finished");}
        check(failed.get()==null,"coherent partition: "+failed.get());var row=outcomes.snapshot()[11];
        check(row.probeTrials()==8000&&row.policyUse().singleTrials()==4000&&row.policyUse().mixedTrials()==4000,"complete partition");
        rejects(()->outcomes.record(Task.CRAFT_WOOD_PICK,Course.Kind.PROBE,true,new ProbePolicies.Usage(0,-1,-1,0)),"empty coherent record");
        check(row.equals(outcomes.snapshot()[11]),"rejected record preserves both total and partition");
        Course without=new Course(2,91),with=new Course(2,91);
        for(int i=0;i<64;i++){
            int actor=i%2;var a=without.issue(actor);var b=with.issue(actor);
            check(a.equals(b),"measurement does not alter lesson selection");
            boolean success=i%3==0;
            without.finish(actor,a.serial(),success);
            var trace=new ProbePolicies.Trace();trace.observe(17);trace.observe(18);
            outcomes.record(b.task(),b.kind(),success,trace.snapshot());with.finish(actor,b.serial(),success);
            check(Arrays.equals(without.encode(),with.encode()),"course RNG/statistics/certificates unchanged by measurement");
        }
    }
    private static void noninterference()throws Exception{
        Policy policy=Policy.initialize(723);RandomSource a=new RandomSource(115),b=new RandomSource(115);
        var before=new TrainingState(policy,new Adam(),new Course(2,11).encode()).encode();
        var workspace=new Policy.Workspace();var trace=new ProbePolicies.Trace();var stats=new ProbePolicies();
        float[] observation=new float[Schema.INPUTS];observation[0]=1;observation[27]=1;
        boolean[] mask=Schema.unrestrictedMask();policy.forward(observation,mask,workspace);
        double[] probabilities=workspace.probabilities.clone();float[] weights=policy.copyWeights();
        for(int n=0;n<128;n++){
            var without=Distribution.choose(probabilities,a,false);
            trace.observe(policy.updates());var with=Distribution.choose(probabilities,b,false);
            check(Arrays.equals(without.actions(),with.actions())&&without.logProbability()==with.logProbability(),"diagnostics never consume policy RNG");
        }
        stats.record(Task.CRAFT_WOOD_PICK,Course.Kind.PROBE,trace.snapshot(),false);
        check(a.state()==b.state(),"RNG state preserved");check(Arrays.equals(weights,policy.copyWeights()),"weights preserved");
        check(Arrays.equals(before,new TrainingState(policy,new Adam(),new Course(2,11).encode()).encode()),"diagnostics do not enter canonical model/optimizer/course bytes");
        check(Arrays.equals(probabilities,workspace.probabilities),"distribution preserved");
    }
}
