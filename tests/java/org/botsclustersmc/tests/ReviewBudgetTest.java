package org.botsclustersmc.tests;

import org.botsclustersmc.core.*;
import org.botsclustersmc.training.*;

public final class ReviewBudgetTest {
    private static int checks;
    private static void check(boolean ok,String message) {
        checks++;if(!ok)throw new AssertionError(message);
    }
    public static void main(String[] args)throws Exception {
        ReviewBudget budget=new ReviewBudget();
        budget.record(true,3000);check(budget.shouldReview(),"long current episodes earn review work");
        for(int i=0;i<24;i++){budget.record(false,30);check(budget.shouldReview(),"short reviews cannot repay a long episode in one turn");}
        budget.record(false,30);check(!budget.shouldReview()&&budget.debt()==0,"25 thirty-tick reviews repay 3000 current ticks");
        check(budget.olderTicks()/(double)(budget.currentTicks()+budget.olderTicks())==.2,"20 percent of actual work, not episodes");
        for(int current:new int[]{4,40,600,3000})for(int older:new int[]{4,35,600,3000}) {
            ReviewBudget b=new ReviewBudget();
            for(int n=0;n<10000;n++) {
                if(b.shouldReview())b.record(false,older);else b.record(true,current);
                check(b.debt()<=current*.25&&b.debt()>-older,"debt stays within one actual episode");
                check(Math.abs(b.currentTicks()*.25-b.olderTicks()-b.debt())<1e-8,"tick accounting identity");
            }
            double total=b.currentTicks()+b.olderTicks();
            check(Math.abs(b.olderTicks()/total-.2)<=Math.max(current,older)/total,"bounded rounding from whole episodes");
        }
        try{budget.record(true,0);throw new AssertionError("zero work accepted");}catch(IllegalArgumentException expected){checks++;}
        Course course=new Course(1,924603);
        for(int n=0;n<2000&&course.stage(0)==0;n++) {
            if(course.needsExam(0))course.beginExam(0,7);
            Course.Lesson lesson=course.issue(0);
            if(lesson.kind()!=Course.Kind.EXAM)course.recordWork(0,lesson.serial(),40);
            else {
                Course.ReviewMetrics before=course.reviewMetrics();
                try{course.recordWork(0,lesson.serial(),40);throw new AssertionError("exam work admitted");}
                catch(IllegalStateException expected){checks++;}
                check(before.equals(course.reviewMetrics()),"exam rejection cannot alter work counters");
            }
            course.finish(0,lesson.serial(),true);
        }
        check(course.stage(0)==1,"a real frozen exam, not workload debt, promotes the actor");
        long episodes=course.episodes();
        Course.Lesson first=course.issue(0);
        try{course.recordWork(0,first.serial(),0);throw new AssertionError("zero episode duration accepted");}catch(IllegalArgumentException expected){checks++;}
        check(course.episodes()==episodes,"invalid work must not finish a lesson");
        course.recordWork(0,first.serial(),1500);
        check(course.reviewMetrics().currentTicks()==1500,"unfinished episodes already count their observed intervals");
        course.recordWork(0,first.serial(),1500);course.finish(0,first.serial(),false);
        int[] issued=new int[18],probes=new int[18];
        for(int n=0;n<1500;n++) {
            Course.Lesson lesson=course.issue(0);int task=lesson.task().ordinal();issued[task]++;
            if(lesson.kind()==Course.Kind.PROBE)probes[task]++;
            course.recordWork(0,lesson.serial(),task==0?30:3000);course.finish(0,lesson.serial(),task==0);
        }
        check(issued[0]>issued[1]*20,"many short reviews balance a long failed current lesson");
        check(probes[1]>0&&probes[0]>0,"review cycles cannot starve current or prior-task probes");
        check(Math.abs(probes[1]-issued[1]/5.0)<=1,"probe cadence is per task");
        Course.ReviewMetrics work=course.reviewMetrics();
        check(Math.abs(work.fraction()-.2)<.01,"measured integrated course work allocation");
        Course.Lesson active=course.issue(0);Course.ReviewMetrics before=course.reviewMetrics();
        try{course.recordWork(0,active.serial()+1,20);throw new AssertionError("foreign lesson work admitted");}
        catch(IllegalStateException expected){checks++;}
        check(before.equals(course.reviewMetrics()),"wrong-lesson rejection cannot alter work counters");
        course.recordWork(0,active.serial(),20);before=course.reviewMetrics();course.abandon(0);
        check(before.equals(course.reviewMetrics()),"abandoning a partial episode preserves work already observed");
        Course restored=Course.decode(course.encode(),1);
        check(restored.stage(0)==course.stage(0)&&restored.certifiedVersion(0,0)==7,"historical certificates and stage persist");
        check(restored.reviewMetrics().currentTicks()==0&&restored.reviewMetrics().olderTicks()==0,"runtime scheduling debt explicitly resets on process restart");
        System.out.println("PASS elapsed-tick review budget checks="+checks);
    }
}
