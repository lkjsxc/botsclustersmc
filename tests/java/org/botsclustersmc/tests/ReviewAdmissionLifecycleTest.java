package org.botsclustersmc.tests;

import java.util.Arrays;
import java.util.Random;
import org.botsclustersmc.training.Course;

/** Independent admission state-machine checks, not gameplay or learned certificates. */
public final class ReviewAdmissionLifecycleTest {
    private static int checks;
    private static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    private static Course.Lesson reviewThenIssue(Course c,int actor){
        Course.Lesson first=c.issue(actor);
        check(first.task().ordinal()<c.stage(actor),"first restored lesson is review");
        c.recordEffort(actor,first.serial(),5);c.finish(actor,first.serial(),true);
        return c.issue(actor);
    }
    private static void ready(Course c,int actor){
        for(int n=0;n<30000&&!c.needsExam(actor);n++){
            Course.Lesson l=c.issue(actor);
            c.recordEffort(actor,l.serial(),l.task().ordinal()==c.stage(actor)?32:8);
            c.finish(actor,l.serial(),true);
        }
        check(c.needsExam(actor),"synthetic mandatory-exam readiness");
    }
    private static void pendingExam(){
        try{
            Course c=ReviewAdmissionTest.earned(3,1);ready(c,2);
            byte[] before=c.encode();
            check(c.issue(2)==null&&Arrays.equals(before,c.encode()),"pending exam cannot issue training or mutate state");
            check(reviewThenIssue(c,0).task().ordinal()==1,"first actual trainee may enter frontier");
            check(reviewThenIssue(c,1).task().ordinal()==0,"pending exam is not available reviewer capacity");
            check(c.needsExam(2)&&c.examVersion(2)==-1,"admission does not start or waive an exam");
            c.beginExam(2,20261007);
            Course.Lesson exam=c.issue(2);
            check(exam.kind()==Course.Kind.EXAM&&exam.task().ordinal()==1,"mandatory frozen exam remains unchanged");
        }catch(Exception error){throw new AssertionError(error);}
    }
    private static void gapsAndWithdrawal()throws Exception{
        Course c=ReviewAdmissionTest.earned(5,1);
        for(int a=0;a<5;a++){
            Course.Lesson l=c.issue(a);c.recordEffort(a,l.serial(),5);c.finish(a,l.serial(),true);
        }
        for(int a=0;a<4;a++)check(c.issue(a).task().ordinal()==1,"natural inter-episode gaps retain participating peers");
        Course.Lesson review=c.issue(4);check(review.task().ordinal()==0,"one real reviewer among five participants");
        Course.Lesson held=c.currentLesson(0);c.abandon(4);
        check(held.equals(c.currentLesson(0)),"withdrawal cannot preempt a peer");
        Course.Lesson ending=c.currentLesson(3);c.recordEffort(3,ending.serial(),1);c.finish(3,ending.serial(),false);
        check(c.issue(3).task().ordinal()==0,"withdrawn peer no longer supplies capacity");
        c.abandon(3);c.withdrawAdmission(3);c.withdrawAdmission(3);
        check(c.running()==3,"repeated gap withdrawal is idempotent");
        Course sparse=ReviewAdmissionTest.earned(64,1);
        check(reviewThenIssue(sparse,0).task().ordinal()==1,"single started actor progresses");
        check(reviewThenIssue(sparse,1).task().ordinal()==0,"62 never-started peers cannot inflate the cohort");
    }
    private static void stateMachine()throws Exception{
        final int actors=64;Course c=ReviewAdmissionTest.earned(actors,1);
        boolean[] member=new boolean[actors];Random rng=new Random(2026100701L);long ticks=0;
        for(int step=0;step<40000;step++){
            if(step==20000){
                long[] certificates=new long[actors];for(int a=0;a<actors;a++)certificates[a]=c.certifiedVersion(a,0);
                c=Course.decode(c.encode(),actors);Arrays.fill(member,false);ticks=0;
                for(int a=0;a<actors;a++)check(c.certifiedVersion(a,0)==certificates[a],"restart preserves earned history");
                check(c.running()==0,"restart clears transient admission and current episodes");
            }
            int actor=rng.nextInt(actors);Course.Lesson active=c.currentLesson(actor);
            if(active!=null){
                int observed=1+rng.nextInt(17);ticks+=observed;c.recordEffort(actor,active.serial(),observed);
                if(rng.nextInt(7)==0){c.abandon(actor);member[actor]=false;}
                else c.finish(actor,active.serial(),active.task().ordinal()<c.stage(actor));
            }else if(rng.nextInt(11)==0){
                c.withdrawAdmission(actor);member[actor]=false;
            }else{
                int population=1,frontier=0;
                for(int peer=0;peer<actors;peer++)if(peer!=actor&&member[peer]&&c.stage(peer)==c.stage(actor)
                        &&c.examVersion(peer)<0&&!c.needsExam(peer)){
                    population++;Course.Lesson l=c.currentLesson(peer);
                    if(l!=null&&l.task().ordinal()==c.stage(actor))frontier++;
                }
                check(!c.needsExam(actor),"failed frontier outcomes cannot invent exam readiness");
                Course.Lesson issued=c.issue(actor);member[actor]=true;
                check(issued.kind()!=Course.Kind.EXAM&&issued.task().ordinal()<=1,"training scope is unchanged");
                if(issued.task().ordinal()==c.stage(actor))
                    check(frontier+1<=Math.max(1,population*4/5),"independent model bounds each new admission");
            }
            Course.Effort effort=c.effort();
            check(effort.reviewTicks()+effort.frontierTicks()==ticks&&effort.examTicks()==0&&effort.foundationTicks()==0,
                "lifecycle changes neither invent nor discard observed effort");
        }
    }
    public static void main(String[] args)throws Exception{
        pendingExam();gapsAndWithdrawal();stateMachine();
        System.out.println("PASS review admission lifecycle checks="+checks);
    }
}
