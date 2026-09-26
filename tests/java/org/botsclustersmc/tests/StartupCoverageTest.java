package org.botsclustersmc.tests;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.botsclustersmc.core.Task;
import org.botsclustersmc.training.*;

/** Counting/noninterference evidence, not a learned Minecraft skill test. */
public final class StartupCoverageTest {
    private static int checks;
    private static void check(boolean value,String message) {
        checks++;if(!value)throw new AssertionError(message);
    }
    private static int number(Map<String,Object> s,String key) {
        return (Integer)s.get("startup_"+key);
    }
    private static int sum(Map<String,Object> s,String key) {
        String text=(String)s.get("startup_"+key);
        return Arrays.stream(text.substring(1,text.length()-1).split(",")).mapToInt(x->Integer.parseInt(x.strip())).sum();
    }
    private static void balanced(Map<String,Object> s) {
        int observed=number(s,"observed_agents");
        check(observed+number(s,"unobserved_agents")==number(s,"expected_agents"),"explicit denominator");
        check(observed==number(s,"foundation_agents")+number(s,"frontier_agents")+number(s,"review_agents")+number(s,"exam_agents"),"disjoint first-lesson categories");
        check(sum(s,"task_population")==observed,"task counts cover all issued first lessons");
        check(sum(s,"training_task_population")==observed-number(s,"exam_agents"),"frozen exams excluded from training coverage");
    }
    private static Course.Lesson lesson(int task,Course.Kind kind) {
        return new Course.Lesson(1,Task.at(task),1,71,kind);
    }
    private static void rejects(Runnable action) {
        try { action.run();throw new AssertionError("invalid coverage accepted"); }
        catch(IllegalArgumentException expected) { checks++; }
    }
    private static void counts() {
        StartupCoverage c=new StartupCoverage(6,true);Map<String,Object> empty=c.status();balanced(empty);
        check(number(empty,"observed_agents")==0,"an unobserved population is not measured failure");
        check(Boolean.TRUE.equals(empty.get("startup_restored_checkpoint")),"origin is explicit");
        check(c.record(0,0,lesson(0,Course.Kind.PRACTICE)),"first foundation");
        check(c.record(1,12,lesson(12,Course.Kind.PROBE)),"frontier probes train");
        check(c.record(2,12,lesson(5,Course.Kind.PROBE)),"review probes train");
        check(c.record(3,12,lesson(12,Course.Kind.EXAM)),"frontier exam");
        check(c.record(4,12,lesson(5,Course.Kind.EXAM)),"retained-skill exam");
        check(c.record(5,12,lesson(11,Course.Kind.PRACTICE)),"review practice");
        Map<String,Object> full=c.status();balanced(full);
        check(number(full,"foundation_agents")==1&&number(full,"frontier_agents")==1
            &&number(full,"review_agents")==2&&number(full,"exam_agents")==2,"category oracle");
        for(int i=0;i<1000;i++)check(!c.record(i%6,17,lesson(17,Course.Kind.PRACTICE)),"later lessons cannot overwrite startup");
        check(c.status().equals(full)&&number(empty,"observed_agents")==0,"old snapshot stays immutable");
        try { full.put("startup_observed_agents",99);throw new AssertionError("mutable snapshot"); }
        catch(UnsupportedOperationException expected) { checks++; }
        rejects(()->new StartupCoverage(0,false));rejects(()->new StartupCoverage(10001,false));
        rejects(()->c.record(-1,0,lesson(0,Course.Kind.PROBE)));rejects(()->c.record(6,0,lesson(0,Course.Kind.PROBE)));
        rejects(()->c.record(0,-1,lesson(0,Course.Kind.PROBE)));rejects(()->c.record(0,18,lesson(0,Course.Kind.PROBE)));
        rejects(()->c.record(0,0,lesson(1,Course.Kind.PROBE)));rejects(()->c.record(0,0,null));
        rejects(()->c.record(0,0,new Course.Lesson(0,Task.FORWARD_STOP,1,1,Course.Kind.PROBE)));
        rejects(()->c.record(0,0,new Course.Lesson(1,null,1,1,Course.Kind.PROBE)));
        rejects(()->c.record(0,0,new Course.Lesson(1,Task.FORWARD_STOP,1,1,null)));
        check(c.status().equals(full),"invalid calls cannot alter counted evidence");
        check(number(new StartupCoverage(10000,false).status(),"unobserved_agents")==10000,"bounded maximum population");
    }
    private static void concurrent()throws Exception {
        StartupCoverage c=new StartupCoverage(1024,false);AtomicInteger recorded=new AtomicInteger();
        try(var pool=Executors.newFixedThreadPool(5)) {
            List<Future<?>> jobs=new ArrayList<>();
            for(int thread=0;thread<4;thread++)jobs.add(pool.submit(()->{
                for(int n=0;n<20;n++)for(int actor=0;actor<1024;actor++)
                    if(c.record(actor,17,lesson(actor%18,Course.Kind.PROBE)))recorded.incrementAndGet();
            }));
            jobs.add(pool.submit(()->{for(int n=0;n<1000;n++)balanced(c.status());}));
            for(Future<?> job:jobs)job.get();
        }
        check(recorded.get()==1024,"concurrent first-write wins exactly once");balanced(c.status());
    }
    private static void noninterference()throws Exception {
        Course original=new Course(4,731);StartupCoverage metrics=new StartupCoverage(4,false);
        for(int step=0;step<800;step++)for(int actor=0;actor<4;actor++) {
            if(original.needsExam(actor))original.beginExam(actor,19);
            Course.Lesson l=original.issue(actor);original.recordEffort(actor,l.serial(),4);original.finish(actor,l.serial(),true);
        }
        byte[] checkpoint=original.encode();Course measured=Course.decode(checkpoint,4),peer=Course.decode(checkpoint,4);
        metrics=new StartupCoverage(4,true);
        for(int step=0;step<100;step++)for(int actor=0;actor<4;actor++) {
            check(measured.needsExam(actor)==peer.needsExam(actor),"same exam eligibility");
            if(measured.needsExam(actor)){measured.beginExam(actor,29);peer.beginExam(actor,29);}
            Course.Lesson first=measured.issue(actor),second=peer.issue(actor);
            check(first.equals(second),"diagnostics cannot change lessons, seeds or serials");
            metrics.record(actor,measured.stage(actor),first);balanced(metrics.status());
            check(Arrays.equals(measured.encode(),peer.encode()),"status leaves exact course/RNG bytes unchanged");
            measured.recordEffort(actor,first.serial(),4);peer.recordEffort(actor,second.serial(),4);
            measured.finish(actor,first.serial(),step%7!=0);peer.finish(actor,second.serial(),step%7!=0);
        }
        check(Arrays.equals(measured.encode(),peer.encode()),"same final training history");
        check(number(metrics.status(),"observed_agents")==4,"subsequent episodes never become first observations");
    }
    public static void main(String[] args)throws Exception {
        counts();concurrent();noninterference();
        System.out.println("PASS startup coverage, concurrency and noninterference checks="+checks);
    }
}
