package org.botsclustersmc.tests;

import java.util.Arrays;
import org.botsclustersmc.core.RandomSource;
import org.botsclustersmc.training.Course;
import org.botsclustersmc.training.ReviewEffort;

/** Resume phase, exact effort accounting and preserved earned state, not learned skills. */
public final class ResumeReviewTest {
    private static int checks;
    private static void check(boolean value,String message) {
        checks++;if(!value)throw new AssertionError(message);
    }
    private static void ready(Course c) {
        for(int n=0;n<10000&&!c.needsExam(0);n++) {
            Course.Lesson lesson=c.issue(0);
            c.finish(0,lesson.serial(),true);
        }
        check(c.needsExam(0),"synthetic actor exam ready");
    }
    private static void pass(Course c,int stage) {
        ready(c);c.beginExam(0,100+stage);
        int cases=0;
        while(c.examVersion(0)>=0) {
            Course.Lesson lesson=c.issue(0);
            check(lesson.kind()==Course.Kind.EXAM,"unchanged frozen examination");
            check(lesson.task().ordinal()==(cases<16?stage:(cases-16)/4),"exact exam order");
            c.recordEffort(0,lesson.serial(),20);c.finish(0,lesson.serial(),true);cases++;
        }
        check(cases==16+4*stage,"unchanged denominator");
    }
    private static void arithmetic() {
        RandomSource random=new RandomSource(20261004);
        for(int frontier=1;frontier<18;frontier++) {
            ReviewEffort effort=ReviewEffort.resumeWithReview();long[] ticks=new long[18];
            check(effort.credit()==1,"explicit one-unit offset, not earned progress");
            long frontierTicks=0,reviewTicks=0;
            for(int episode=0;episode<10000;episode++) {
                int task=effort.select(frontier,random);
                if(episode==0)check(task<frontier,"review from first actual episode");
                if(task<frontier)for(int i=0;i<frontier;i++)
                    check(ticks[task]<=ticks[i],"original least-observed-effort selection");
                int duration=task==frontier?3000:2+random.nextInt(2999);
                effort.record(frontier,task,1);effort.record(frontier,task,duration-1);
                ticks[task]+=duration;
                if(task==frontier)frontierTicks+=duration;else reviewTicks+=duration;
                check(effort.credit()==1+frontierTicks-4*reviewTicks,"exact repayable phase offset");
                check(effort.credit()<=3001&&effort.credit()>-12000,"one whole-episode overshoot only");
                if(episode==0)check(effort.select(frontier,random)==frontier,"observed review immediately incurs debt");
            }
            check(Math.abs(reviewTicks/(double)(reviewTicks+frontierTicks)-.2)<.003,"long-run actual-tick share unchanged");
            effort.reset();check(effort.credit()==0&&effort.select(frontier,random)==frontier,"promotion/regression reset unchanged");
        }
        ReviewEffort foundation=ReviewEffort.resumeWithReview();
        foundation.record(0,0,Integer.MAX_VALUE);
        check(foundation.select(0,random)==0,"no fabricated earlier foundation task");
    }
    private static void course()throws Exception {
        Course source=new Course(1,20261004);
        for(int stage=0;stage<18;stage++) {
            byte[] saved=source.encode();Course resumed=Course.decode(saved,1),peer=Course.decode(saved,1);
            check(Arrays.equals(saved,resumed.encode()),"resume credit not serialized as learned state");
            check(resumed.effort().equals(new Course.Effort(0,0,0,0)),"no invented observed ticks");
            Course.Progress before=source.progress(0),after=resumed.progress(0);
            check(after.stage()==before.stage()&&after.practiceEpisodes()==before.practiceEpisodes()
                &&after.probes()==before.probes()&&after.practiceSuccess()==before.practiceSuccess()
                &&after.probeSuccess()==before.probeSuccess()&&after.completed()==before.completed(),
                "persisted practice/probe/earned state retained exactly");
            check(after.examCases()==0&&!after.exam(),"completed in-memory exam cursor is transient, as before");
            for(int task=0;task<18;task++)check(resumed.certifiedVersion(0,task)==source.certifiedVersion(0,task),"no invented or replaced certificates");
            Course.Lesson first=resumed.issue(0);
            check(first.equals(peer.issue(0)),"saved random state remains deterministic");
            check(stage==0?first.task().ordinal()==0:first.task().ordinal()<stage,"restored first task stays within already reached tasks");
            byte[] inFlight=resumed.encode();resumed.recordEffort(0,first.serial(),5);
            check(Arrays.equals(inFlight,resumed.encode()),"observed accounting cannot change model/course serialization");
            resumed.finish(0,first.serial(),true);
            check(resumed.issue(0).task().ordinal()==stage,"review debt repaid through subsequent frontier work");
            if(stage<17) {
                pass(source,stage);
                Course.Lesson promoted=source.issue(0);
                check(promoted.task().ordinal()==stage+1,"newly promoted actor still starts frontier");
                source.abandon(0);
            }
        }
    }
    private static void interruptionsAndExams()throws Exception {
        Course source=new Course(1,999);pass(source,0);
        Course c=Course.decode(source.encode(),1);Course.Lesson first=c.issue(0);c.abandon(0);
        check(c.issue(0).task().ordinal()==0,"issuing without observed work does not spend credit");
        c.abandon(0);first=c.issue(0);c.recordEffort(0,first.serial(),3);c.abandon(0);
        check(c.effort().reviewTicks()==3&&c.issue(0).task().ordinal()==1,"interrupted observed review repays offset");
        ready(source);Course examReady=Course.decode(source.encode(),1);
        check(examReady.needsExam(0)&&examReady.issue(0)==null,"resume cannot displace a mandatory frozen exam");
        examReady.beginExam(0,777);int cases=0;
        while(examReady.examVersion(0)>=0) {
            Course.Lesson lesson=examReady.issue(0);
            check(lesson.task().ordinal()==(cases<16?1:0),"resume does not reorder exam trials");
            examReady.recordEffort(0,lesson.serial(),4);examReady.finish(0,lesson.serial(),cases++>=3);
        }
        check(examReady.stage(0)==1,"failed exam remains failed");
        check(examReady.effort().equals(new Course.Effort(0,0,0,80)),"exam cannot create review exposure");
        check(examReady.issue(0).task().ordinal()==0,"failed exam does not consume resume phase");
    }
    public static void main(String[] args)throws Exception {
        arithmetic();course();interruptionsAndExams();
        System.out.println("PASS resume-review phase checks="+checks);
    }
}
