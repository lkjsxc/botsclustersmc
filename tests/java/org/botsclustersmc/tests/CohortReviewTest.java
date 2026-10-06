package org.botsclustersmc.tests;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import org.botsclustersmc.core.RandomSource;
import org.botsclustersmc.training.Course;
import org.botsclustersmc.training.ReviewEffort;

/** Synthetic allocation/lifecycle evidence only. No teacher or Minecraft gameplay. */
public final class CohortReviewTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void rejects(Runnable fn){
        boolean failed=false;
        try{fn.run();}catch(IllegalArgumentException|IllegalStateException|ArithmeticException expected){failed=true;}
        check(failed,"invalid operation must fail");
    }
    private static void ready(Course c,int actor,boolean observe){
        for(int i=0;i<30000&&!c.needsExam(actor);i++){
            Course.Lesson l=c.issue(actor);
            if(observe)c.recordEffort(actor,l.serial(),l.task().ordinal()==c.stage(actor)?32:8);
            c.finish(actor,l.serial(),true);
        }
        check(c.needsExam(actor),"synthetic success must reach an exam");
    }
    private static void pass(Course c,int actor){
        int stage=c.stage(actor);c.beginExam(actor,100+stage);int n=0;
        while(c.examVersion(actor)>=0){
            Course.Lesson l=c.issue(actor);
            check(l.kind()==Course.Kind.EXAM&&l.task().ordinal()==(n<16?stage:(n-16)/4),"unchanged exam order");
            c.recordEffort(actor,l.serial(),4);c.finish(actor,l.serial(),true);n++;
        }
        check(n==16+4*stage,"unchanged exam denominator");
    }
    /** These earned synthetic courses are used only in unit tests, never a learning Academy. */
    private static Course earned(int actors,int frontier)throws Exception{
        Course c=new Course(actors,61006);
        for(int actor=0;actor<actors;actor++)for(int stage=0;stage<frontier;stage++){
            ready(c,actor,false);pass(c,actor);
        }
        return Course.decode(c.encode(),actors);
    }
    private static void eligibilityOnly(){
        RandomSource a=new RandomSource(123),b=new RandomSource(123);
        for(int frontier=0;frontier<18;frontier++){
            ReviewEffort own=ReviewEffort.resumeWithReview();
            for(int i=0;i<1000;i++){
                long credit=own.credit();int task=own.select(frontier,a);
                check(task==own.select(frontier,b,credit)&&a.state()==b.state(),"same sign preserves exact choice and RNG");
                check(own.credit()==credit,"selection does not invent actual effort");
                if(frontier>0){
                    check(own.select(frontier,new RandomSource(i),0)==frontier,"nonpositive shared credit selects frontier");
                    check(own.select(frontier,new RandomSource(i),Long.MAX_VALUE)<frontier,"positive shared credit permits only earlier tasks");
                }
                own.record(frontier,task,1+i%127);
            }
        }
    }
    private static void peerCreditAndFirstLesson()throws Exception{
        Course c=earned(3,1);long certificates=c.certifiedVersion(0,0);
        check(c.reviewCohortCredit()[1]==3,"one explicit offset per restored member, not actual ticks");
        for(int actor=0;actor<2;actor++){
            Course.Lesson l=c.issue(actor);check(l.task().ordinal()==0,"same first review");
            c.recordEffort(actor,l.serial(),5);c.finish(actor,l.serial(),true);
        }
        check(c.reviewCohortCredit()[1]==-37,"negative pool consists of real reviewed work plus offsets");
        Course.Lesson delayed=c.issue(2);
        check(delayed.task().ordinal()==0,"peer timing cannot change a delayed actor's first assignment");
        c.recordEffort(2,delayed.serial(),5);c.finish(2,delayed.serial(),true);
        Course.Lesson donor=c.issue(0);check(donor.task().ordinal()==1,"frontier must actually run");
        c.recordEffort(0,donor.serial(),100);
        check(c.currentLesson(0).equals(donor),"donor episode still unfinished");
        Course.Lesson review=c.issue(1);
        check(review.task().ordinal()==0,"peer observed frontier work sustains review before donor finishes");
        check(c.stage(0)==1&&c.stage(1)==1&&c.certifiedVersion(0,0)==certificates,"sharing cannot manufacture earned progress");
        byte[] saved=c.encode();Course.Effort observed=c.effort();long[] pool=c.reviewCohortCredit();
        rejects(()->c.recordEffort(1,donor.serial(),5));rejects(()->c.recordEffort(3,review.serial(),5));
        rejects(()->c.recordEffort(1,review.serial(),0));
        check(Arrays.equals(saved,c.encode())&&observed.equals(c.effort())&&Arrays.equals(pool,c.reviewCohortCredit()),"invalid operations leave all state unchanged");
        c.recordEffort(1,review.serial(),7);c.abandon(1);
        check(c.reviewCohortCredit()[1]==15,"abandonment retains only actual work");
        long[] detached=c.reviewCohortCredit();detached[1]=999;
        check(c.reviewCohortCredit()[1]==15,"read-only independent snapshot");
    }
    private static void membershipAndExams()throws Exception{
        Course c=earned(2,1);
        for(int actor=0;actor<2;actor++){
            Course.Lesson l=c.issue(actor);c.recordEffort(actor,l.serial(),5);c.finish(actor,l.serial(),true);
        }
        ready(c,0,true);
        long before=c.reviewCohortCredit()[1];Course.Effort observed=c.effort();
        c.beginExam(0,701);
        for(int n=0;c.examVersion(0)>=0;n++){
            Course.Lesson l=c.issue(0);c.recordEffort(0,l.serial(),4);
            check(c.reviewCohortCredit()[1]==before,"frozen exam neither buys nor spends shared credit");
            c.finish(0,l.serial(),n>=3); // 13/16 current-task success fails.
        }
        check(c.stage(0)==1&&c.reviewCohortCredit()[1]==before,"failed exam retains member contribution");
        check(c.effort().frontierTicks()==observed.frontierTicks()&&c.effort().reviewTicks()==observed.reviewTicks(),"exam observations separate from training");
        ready(c,0,true);pass(c,0);
        check(c.stage(0)==2&&c.reviewCohortCredit()[1]==-19&&c.reviewCohortCredit()[2]==0,"promotion withdraws only this member's old credit");
        Course.Lesson first=c.issue(0);check(first.task().ordinal()==2,"new stage starts with ordinary frontier lesson");
        c.recordEffort(0,first.serial(),3000);c.finish(0,first.serial(),false);
        Course.Lesson peer=c.issue(1);
        check(peer.task().ordinal()==1,"a different frontier cannot buy peer rehearsal");c.abandon(1);
        check(c.reviewCohortCredit()[1]==-19,"other cohort debt unchanged");
        for(int n=0;n<10000&&c.stage(0)==2;n++){
            Course.Lesson l=c.issue(0);c.recordEffort(0,l.serial(),l.task().ordinal()==2?3000:8);c.finish(0,l.serial(),false);
        }
        check(c.stage(0)<2&&c.regressions()>0,"ordinary failed review probes still cause individual regression");
        check(c.reviewCohortCredit()[2]==0,"departed member cannot leave stranded credit");
        check(c.reviewCohortCredit()[1]==-19,"regression starts with zero contribution in the new cohort");
        check(c.certifiedVersion(0,0)>=0,"historical certificate preserved, not relabelled current mastery");
    }
    private static void serializationAndAbandonment()throws Exception{
        Course c=earned(4,5);byte[] before=c.encode();Course copy=Course.decode(before,4);
        check(Arrays.equals(before,copy.encode()),"cohort sums are not checkpoint state");
        check(copy.effort().equals(new Course.Effort(0,0,0,0)),"resume creates no actual ticks");
        for(int actor=0;actor<4;actor++){
            Course.Lesson a=c.issue(actor),b=copy.issue(actor);
            check(a.equals(b),"same saved random state and first lesson per actor");
            byte[] running=c.encode();c.recordEffort(actor,a.serial(),9);
            check(Arrays.equals(running,c.encode()),"recording is serialization neutral");
            c.abandon(actor);copy.abandon(actor);
        }
        check(c.reviewCohortCredit()[5]==4-4*36,"actual abandoned intervals counted once");
        Course restored=Course.decode(c.encode(),4);
        check(restored.reviewCohortCredit()[5]==4,"one explicit fresh process interval, not a continued debt ledger");
        check(Arrays.equals(restored.encode(),copy.encode()),"physical accounting alone never edits persisted progress");
        Course base=Course.decode(new Course(4,17).encode(),4);
        for(int actor=0;actor<4;actor++){
            Course.Lesson l=base.issue(actor);base.recordEffort(actor,l.serial(),Integer.MAX_VALUE);base.finish(actor,l.serial(),false);
        }
        check(base.reviewCohortCredit()[0]==0&&base.effort().foundationTicks()==4L*Integer.MAX_VALUE,"foundation is never shared rehearsal credit");
    }
    private static void overflowAtomicity()throws Exception{
        Course c=earned(1,1);Course.Lesson l=c.issue(0);c.recordEffort(0,l.serial(),5);c.finish(0,l.serial(),true);
        Course.Lesson frontier=c.issue(0);Course.Effort before=c.effort();byte[] bytes=c.encode();
        Field field=Course.class.getDeclaredField("cohortCredit");field.setAccessible(true);
        long[] credit=(long[])field.get(c);credit[1]=Long.MAX_VALUE;
        rejects(()->c.recordEffort(0,frontier.serial(),1));
        check(credit[1]==Long.MAX_VALUE&&before.equals(c.effort())&&Arrays.equals(bytes,c.encode()),"overflow is rejected before counters or local ledger change");
        credit[1]=-19;c.recordEffort(0,frontier.serial(),1);
        check(c.reviewCohortCredit()[1]==-18,"failed pooled operation left local effort intact");
    }
    private static void asynchronousSimulation()throws Exception{
        int actors=32,frontier=12,interval=10;Course c=earned(actors,frontier);
        Course.Lesson[] active=new Course.Lesson[actors];int[] remaining=new int[actors];long[][] actual=new long[actors][18];
        long frontierTicks=0,reviewTicks=0,firstReview=0,firstTotal=0;
        for(int clock=0;clock<60000;clock+=interval){
            for(int actor=0;actor<actors;actor++){
                if(active[actor]==null){
                    active[actor]=c.issue(actor);int task=active[actor].task().ordinal();
                    if(task<frontier)for(int earlier=0;earlier<frontier;earlier++)check(actual[actor][task]<=actual[actor][earlier],"individual least-observed-task balancing preserved");
                    remaining[actor]=task==frontier?3000:20+10*((task*13+actor*7)%100);
                }
                int task=active[actor].task().ordinal();c.recordEffort(actor,active[actor].serial(),interval);
                actual[actor][task]+=interval;remaining[actor]-=interval;
                if(task==frontier)frontierTicks+=interval;else reviewTicks+=interval;
                if(remaining[actor]==0){c.finish(actor,active[actor].serial(),task<frontier);active[actor]=null;}
            }
            check(c.reviewCohortCredit()[frontier]==actors+frontierTicks-4*reviewTicks,"exact fixed-membership conservation with concurrent unfinished episodes");
            if(clock+interval==5000){firstReview=reviewTicks;firstTotal=reviewTicks+frontierTicks;}
        }
        double share=reviewTicks/(double)(reviewTicks+frontierTicks);
        check(Math.abs(share-.2)<.04,"actual-only sharing remains near the declared long-run rate, not an exact finite quota");
        check(c.stage(0)==frontier,"effort is not a certificate");
        System.out.printf(java.util.Locale.ROOT,"COHORT synthetic early_share=%.6f long_share=%.6f later_share=%.6f; not Minecraft evidence%n",firstReview/(double)firstTotal,share,(reviewTicks-firstReview)/(double)(reviewTicks+frontierTicks-firstTotal));
    }
    private static void concurrency()throws Exception{
        Course c=earned(64,1);long startEpisodes=c.episodes();AtomicReference<Throwable> error=new AtomicReference<>();
        Thread[] threads=new Thread[8];
        for(int worker=0;worker<threads.length;worker++){
            final int from=worker*8;
            threads[worker]=new Thread(()->{try{
                for(int i=0;i<200;i++)for(int actor=from;actor<from+8;actor++){
                    Course.Lesson l=c.issue(actor);boolean frontier=l.task().ordinal()==1;
                    c.recordEffort(actor,l.serial(),frontier?31:7);
                    if(i%19==0)c.abandon(actor);else c.finish(actor,l.serial(),!frontier);
                }
            }catch(Throwable failure){error.compareAndSet(null,failure);}});threads[worker].start();
        }
        for(Thread thread:threads)thread.join();if(error.get()!=null)throw new AssertionError(error.get());
        Course.Effort sum=c.effort();
        check(c.reviewCohortCredit()[1]==64+sum.frontierTicks()-4*sum.reviewTicks(),"concurrent exact actual credit");
        check(c.running()==0&&c.episodes()-startEpisodes==64*(200-11),"completion and cancellation denominators separate");
        check(c.abandoned()==64*11,"every cancellation counted once");
        for(int actor=0;actor<64;actor++)check(c.stage(actor)==1,"no cross-actor promotion");
    }
    public static void main(String[] args)throws Exception{
        eligibilityOnly();peerCreditAndFirstLesson();membershipAndExams();serializationAndAbandonment();overflowAtomicity();asynchronousSimulation();concurrency();
        System.out.println("PASS observed-only cohort review checks="+checks);
    }
}
