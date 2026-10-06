package org.botsclustersmc.tests;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import org.botsclustersmc.core.RandomSource;
import org.botsclustersmc.training.Course;
import org.botsclustersmc.training.ReviewEffort;

/** Admission/lifecycle properties only; synthetic passes are never gameplay evidence. */
public final class ReviewAdmissionTest {
    private static int checks;
    private static void check(boolean yes,String what){checks++;if(!yes)throw new AssertionError(what);}
    private static void rejects(Runnable run){
        boolean failed=false;try{run.run();}catch(IllegalArgumentException|IllegalStateException expected){failed=true;}
        check(failed,"invalid operation rejected");
    }
    private static void ready(Course c,int actor){
        for(int n=0;n<30000&&!c.needsExam(actor);n++){
            Course.Lesson l=c.issue(actor);c.recordEffort(actor,l.serial(),l.task().ordinal()==c.stage(actor)?32:8);c.finish(actor,l.serial(),true);
        }
        check(c.needsExam(actor),"synthetic readiness");
    }
    private static void pass(Course c,int actor){
        ready(c,actor);int stage=c.stage(actor);c.beginExam(actor,101+stage);int cases=0;
        while(c.examVersion(actor)>=0){
            Course.Lesson l=c.issue(actor);
            check(l.kind()==Course.Kind.EXAM&&l.task().ordinal()==(cases<16?stage:(cases-16)/4),"unchanged exam order");
            c.recordEffort(actor,l.serial(),5);c.finish(actor,l.serial(),true);cases++;
        }
        check(cases==16+4*stage,"unchanged full exam denominator");
    }
    public static Course earned(int actors,int stage)throws Exception{
        Course c=new Course(actors,6100621);
        for(int actor=0;actor<actors;actor++)for(int t=0;t<stage;t++)pass(c,actor);
        return Course.decode(c.encode(),actors);
    }
    private static void choiceOnly(){
        for(int stage=1;stage<18;stage++){
            ReviewEffort a=ReviewEffort.resumeWithReview(),b=ReviewEffort.resumeWithReview();
            RandomSource x=new RandomSource(7),y=new RandomSource(7);
            for(int n=0;n<1000;n++){
                check(a.selectReview(stage,x)==b.selectReview(stage,y)&&x.state()==y.state(),"same least-effort selector/RNG");
                a.record(stage,n%stage,5);b.record(stage,n%stage,5);
                check(a.credit()==1-20L*(n+1),"forced review is debt, not invented credit");
            }
            check(a.select(stage,x)==stage,"ordinary negative-credit selection unchanged");
        }
        ReviewEffort a=new ReviewEffort();RandomSource r=new RandomSource(7);
        rejects(()->a.selectReview(0,r));rejects(()->a.selectReview(18,r));rejects(()->a.selectReview(-1,r));
        check(a.credit()==0,"invalid selection leaves credit unchanged");
    }
    private static void bound(Course c,int applicant){
        int members=512,frontier=0,stage=c.stage(applicant);
        for(int actor=0;actor<512;actor++){
            Course.Lesson l=c.currentLesson(actor);
            if(l!=null&&l.kind()!=Course.Kind.EXAM&&c.stage(actor)==stage){if(l.task().ordinal()==stage)frontier++;}
        }
        check(frontier<=Math.max(1,members*4/5),"frontier admission keeps a live review opportunity");
    }
    private static void opportunities()throws Exception{
        Course c=earned(512,12);Course.Lesson[] first=new Course.Lesson[512];
        for(int a=0;a<512;a++){
            first[a]=c.issue(a);check(first[a].task().ordinal()<12,"identical review-first phase");
            c.recordEffort(a,first[a].serial(),5);
        }
        long episodes=c.episodes();
        for(int a=0;a<512;a++){
            c.finish(a,first[a].serial(),true);Course.Lesson l=c.issue(a);bound(c,a);
            check(l.kind()!=Course.Kind.EXAM,"no fabricated exam");
        }
        int review=0;for(int a=0;a<512;a++)if(c.currentLesson(a).task().ordinal()<12)review++;
        check(review==103,"409 frontier / 103 review at 512 actors");
        long initial=c.effort().reviewTicks();
        for(int round=0;round<20;round++)for(int a=0;a<512;a++){
            Course.Lesson l=c.currentLesson(a);
            if(l.task().ordinal()<12){c.recordEffort(a,l.serial(),5);c.finish(a,l.serial(),true);c.issue(a);bound(c,a);}
        }
        check(c.effort().reviewTicks()>initial&&c.stageMetrics()[12].trainingEpisodes()==0,"review can continue with zero frontier completions");
        check(c.episodes()>episodes+512,"real synthetic review completions, not assignments alone");
        byte[] before=c.encode();Course.Effort effort=c.effort();
        rejects(()->c.issue(0));rejects(()->c.recordEffort(0,-1,5));rejects(()->c.finish(0,-1,true));
        check(Arrays.equals(before,c.encode())&&effort.equals(c.effort()),"rejected calls preserve state");
        Course.Lesson survivor=c.currentLesson(0);c.abandon(1);c.abandon(2);
        check(c.currentLesson(0).equals(survivor),"cohort shrink never interrupts another episode");
        Course restored=Course.decode(c.encode(),512);
        check(restored.running()==0&&restored.effort().equals(new Course.Effort(0,0,0,0)),"occupancy is transient, not serialized effort");
        for(int a=0;a<512;a++)for(int t=0;t<18;t++)check(restored.certifiedVersion(a,t)==c.certifiedVersion(a,t),"exact certificates persist");
    }
    private static void isolationAndSingleton()throws Exception{
        Course solo=earned(1,1);Course.Lesson l=solo.issue(0);solo.recordEffort(0,l.serial(),5);solo.finish(0,l.serial(),true);
        check(solo.issue(0).task().ordinal()==1,"singleton cannot be starved by a fractional quota");
        rejects(()->solo.withdrawAdmission(0));solo.abandon(0);solo.withdrawAdmission(0);
        check(solo.running()==0,"withdrawal without a current episode is idempotent");
        Course c=earned(4,1);pass(c,0);pass(c,1); // two independent frontier cohorts
        c=Course.decode(c.encode(),4);
        Course.Lesson[] active=new Course.Lesson[4];
        for(int a=0;a<4;a++){
            active[a]=c.issue(a);c.recordEffort(a,active[a].serial(),5);c.finish(a,active[a].serial(),true);
            active[a]=c.issue(a);
        }
        check(active[0].task().ordinal()==2&&active[1].task().ordinal()<2,"stage2 keeps its own reviewer");
        check(active[2].task().ordinal()==1&&active[3].task().ordinal()==0,"stage1 independent of stage2 occupancy");
        Course.Lesson held=active[0];c.abandon(1);
        check(c.currentLesson(0).equals(held),"peer abandonment does not preempt frontier");
        Course exams=earned(3,1);ready(exams,0);ready(exams,1);
        check(exams.issue(0)==null,"mandatory exam cannot become review");
        exams.beginExam(0,999);exams.beginExam(1,998);Course.Lesson other=exams.issue(1);
        Course.Lesson first=exams.issue(2);exams.recordEffort(2,first.serial(),5);exams.finish(2,first.serial(),true);
        check(exams.issue(2).task().ordinal()==1,"exam peers excluded from training occupancy");
        for(int n=0;exams.examVersion(0)>=0;n++){
            l=exams.issue(0);check(l.kind()==Course.Kind.EXAM,"guard never replaces exam cases");
            exams.recordEffort(0,l.serial(),5);exams.finish(0,l.serial(),false);
        }
        check(exams.currentLesson(1).equals(other)&&exams.stage(0)==1,"failed exam and peer exam unchanged");
    }

    private static void concurrency()throws Exception{
        Course c=earned(64,1);AtomicReference<Throwable> error=new AtomicReference<>();Thread[] threads=new Thread[8];
        for(int w=0;w<8;w++){
            final int from=w*8;threads[w]=new Thread(()->{try{
                for(int n=0;n<100;n++)for(int a=from;a<from+8;a++){
                    Course.Lesson l=c.issue(a);c.recordEffort(a,l.serial(),5);
                    if(n%13==0)c.abandon(a);else c.finish(a,l.serial(),l.task().ordinal()==0);
                }
            }catch(Throwable t){error.compareAndSet(null,t);}});threads[w].start();
        }
        for(Thread thread:threads)thread.join();if(error.get()!=null)throw new AssertionError(error.get());
        check(c.running()==0&&c.effort().reviewTicks()+c.effort().frontierTicks()==64*100*5L,"concurrent exact observed accounting");
        check(c.abandoned()==64*8,"each abandoned episode counted once");
    }
    public static void main(String[] args)throws Exception{
        choiceOnly();opportunities();isolationAndSingleton();concurrency();
        System.out.println("PASS review admission checks="+checks);
    }
}
