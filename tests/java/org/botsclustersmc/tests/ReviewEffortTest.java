package org.botsclustersmc.tests;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import org.botsclustersmc.core.RandomSource;
import org.botsclustersmc.training.Course;
import org.botsclustersmc.training.ReviewEffort;

/** Allocation arithmetic and real Course integration, not neural skill evidence. */
public final class ReviewEffortTest {
    private static int checks;
    private static void check(boolean value,String message) {
        checks++;if(!value)throw new AssertionError(message);
    }
    private static void rejects(Runnable action) {
        boolean failed=false;
        try{action.run();}catch(IllegalArgumentException|IllegalStateException expected){failed=true;}
        check(failed,"invalid effort must be rejected");
    }
    private static void ready(Course c,long actor) {
        for(int n=0;n<20000&&!c.needsExam(actor);n++) {
            Course.Lesson l=c.issue(actor);
            c.recordEffort(actor,l.serial(),l.task().ordinal()==c.stage(actor)?16:4);
            c.finish(actor,l.serial(),true);
        }
        check(c.needsExam(actor),"effort allocation cannot prevent exam eligibility");
    }
    private static void exam(Course c,long actor,int misses) {
        c.beginExam(actor,31);int stage=c.stage(actor),cases=0;
        while(c.examVersion(actor)>=0) {
            Course.Lesson l=c.issue(actor);
            check(l.kind()==Course.Kind.EXAM&&l.difficulty()==1,"exam conditions unchanged");
            check(l.task().ordinal()==(cases<16?stage:(cases-16)/4),"full fixed exam ordering");
            c.recordEffort(actor,l.serial(),20);
            c.finish(actor,l.serial(),!(l.task().ordinal()==stage&&misses-->0));cases++;
        }
        check(cases==16+4*stage,"exam denominator unchanged");
    }
    private static Course atOne() {
        Course c=new Course(1,924624);ready(c,0);exam(c,0,0);
        check(c.stage(0)==1,"only a passed frozen exam promotes");return c;
    }
    private static void arithmetic() {
        RandomSource rng=new RandomSource(9);ReviewEffort effort=new ReviewEffort();final ReviewEffort initial=effort;
        rejects(()->initial.select(-1,rng));rejects(()->initial.select(18,rng));
        rejects(()->initial.record(1,2,4));rejects(()->initial.record(1,-1,4));
        rejects(()->initial.record(1,0,0));rejects(()->initial.record(1,0,-1));
        check(effort.credit()==0,"invalid input leaves allocation unchanged");
        effort.record(0,0,Integer.MAX_VALUE);
        check(effort.select(0,rng)==0&&effort.credit()==0,"foundation cannot incur review debt");
        effort.record(1,1,Integer.MAX_VALUE);effort.record(1,0,Integer.MAX_VALUE);
        check(effort.credit()==-3L*Integer.MAX_VALUE,"tick multiplication uses long, not int");
        check(effort.select(1,rng)==1,"review overshoot returns to frontier");
        effort.reset();check(effort.credit()==0&&effort.select(17,rng)==17,"new frontier clears debt");
        for(int frontier:new int[]{1,2,5,10,17})for(int variation=0;variation<3;variation++) {
            effort=new ReviewEffort();long[] ticks=new long[18];long all=0,review=0;int reviewEpisodes=0;
            for(int episode=0;episode<30000;episode++) {
                int selected=effort.select(frontier,rng);
                check(selected>=0&&selected<=frontier,"no unreached task selected");
                if(selected<frontier) {
                    for(int i=0;i<frontier;i++)check(ticks[selected]<=ticks[i],"least-reviewed task selected");
                    reviewEpisodes++;
                }
                int duration=selected==frontier?3000:variation==0?20:variation==1?20+selected*12:4+rng.nextInt(750)*4;
                // Split actual elapsed work into fragments; the partition must not affect allocation.
                int first=duration/2;
                effort.record(frontier,selected,first);effort.record(frontier,selected,duration-first);
                ticks[selected]+=duration;all+=duration;if(selected<frontier)review+=duration;
                check(effort.credit()==ticks[frontier]-4*review,"conservation of actual elapsed effort");
                check(effort.credit()<=3000&&effort.credit()>-12000,"whole-episode overshoot bound");
                long min=Long.MAX_VALUE,max=0;
                for(int i=0;i<frontier;i++){min=Math.min(min,ticks[i]);max=Math.max(max,ticks[i]);}
                check(max-min<=3000,"earlier tasks balanced within one episode");
            }
            check(Math.abs(review/(double)all-.2)<.003,"20% actual ticks despite unequal durations");
            if(variation==0)check(reviewEpisodes>28000,"short reviews require more episodes, not fabricated samples");
            System.out.printf(java.util.Locale.ROOT,"ALLOCATION frontier=%d variation=%d review_ticks=%.6f review_episodes=%d/30000%n",frontier,variation,review/(double)all,reviewEpisodes);
        }
    }
    private static void cadenceAndLifecycle()throws Exception {
        Course c=atOne();Course.Effort before=c.effort();int frontierEpisodes=0,frontierProbes=0;
        for(int n=0;n<1000;n++) {
            Course.Lesson l=c.issue(0);boolean frontier=l.task().ordinal()==1;
            if(frontier) {
                check((l.kind()==Course.Kind.PROBE)==(frontierEpisodes%5==0),"per-task probe cadence resists five-episode aliasing");
                if(l.kind()==Course.Kind.PROBE){frontierProbes++;check(l.difficulty()==1,"full difficulty probe");}
                frontierEpisodes++;
            }
            c.recordEffort(0,l.serial(),frontier?16:1);c.finish(0,l.serial(),!frontier);
        }
        Course.Effort after=c.effort();
        check(frontierEpisodes==200&&frontierProbes==40,"frontier is not locked out of full probes");
        check(after.frontierTicks()-before.frontierTicks()==3200&&after.reviewTicks()-before.reviewTicks()==800,"Course routes actual tick costs");
        check(after.examTicks()==before.examTicks(),"practice never fabricates exam time");
        check(c.stage(0)==1&&!c.needsExam(0),"elapsed effort cannot promote a failing skill");

        c=atOne();Course.Lesson partial=c.issue(0);byte[] snapshot=c.encode();
        c.recordEffort(0,partial.serial(),3000);
        check(Arrays.equals(snapshot,c.encode()),"transient effort does not rewrite learned/certified checkpoint state");
        c.abandon(0);Course.Lesson review=c.issue(0);
        check(review.task().ordinal()==0,"observed interrupted work is not forgotten in this process");
        Course stable=c;Course.Effort unchanged=c.effort();
        rejects(()->stable.recordEffort(0,partial.serial(),4));rejects(()->stable.recordEffort(1,review.serial(),4));
        rejects(()->stable.recordEffort(0,review.serial(),0));
        check(c.effort().equals(unchanged),"invalid identities or ticks cannot change counters");
        c.recordEffort(0,review.serial(),10);c.abandon(0);
        check(c.effort().reviewTicks()==10,"partial review contributes only observed work");
        Course restored=Course.decode(c.encode(),1),peer=Course.decode(c.encode(),1);
        check(restored.effort().equals(new Course.Effort(0,0,0,0)),"restart explicitly starts a fresh effort interval");
        check(restored.stage(0)==1&&restored.certifiedVersion(0,0)==31,"earned learning history survives");
        Course.Lesson next=restored.issue(0);
        check(next.task().ordinal()==1&&next.equals(peer.issue(0)),"clean allocation restart and reproducible persisted RNG");

        c=atOne();ready(c,0);long credit=c.effort().frontierTicks()-4*c.effort().reviewTicks();
        before=c.effort();exam(c,0,3);after=c.effort();
        check(c.stage(0)==1,"13/16 still fails the current-task exam");
        check(after.examTicks()-before.examTicks()==400,"only exact observed exam ticks counted");
        check(after.frontierTicks()==before.frontierTicks()&&after.reviewTicks()==before.reviewTicks(),"exam cannot buy or consume review time");
        next=c.issue(0);check((next.task().ordinal()==0)==(credit>0),"failed exam preserves pre-exam effort balance");
        c.abandon(0);
        // Enough full-condition review failures still demote this actor through the existing rule.
        for(int n=0;n<5000&&c.stage(0)==1;n++) {
            Course.Lesson l=c.issue(0);c.recordEffort(0,l.serial(),l.task().ordinal()==1?3000:4);c.finish(0,l.serial(),false);
        }
        check(c.stage(0)==0&&c.regressions()>0,"real regression is not hidden by scheduling");
        check(c.certifiedVersion(0,0)==31,"a historical certificate is not silently rewritten");
    }
    private static void allStagesAndIsolation() {
        Course c=new Course(1,77);
        for(int stage=0;stage<18;stage++) {
            ready(c,0);exam(c,0,0);
            check(c.stage(0)==Math.min(17,stage+1),"every stage remains reachable with recorded effort");
            if(stage<17) {
                Course.Lesson first=c.issue(0);
                check(first.task().ordinal()==stage+1,"promotion clears the previous allocation interval");
                c.abandon(0);
            }
        }
        check(c.completed()&&c.effort().reviewTicks()>0&&c.effort().examTicks()>0,"completed synthetic course separates review and exams");
        Course pair=new Course(2,88);
        for(int actor=0;actor<2;actor++){ready(pair,actor);exam(pair,actor,0);}
        Course.Lesson first=pair.issue(0);pair.recordEffort(0,first.serial(),3000);pair.finish(0,first.serial(),false);
        check(pair.issue(1).task().ordinal()==1,"one actor cannot incur review debt for another");
        check(pair.issue(0).task().ordinal()==0,"the actor that earned review receives it");
    }
    private static void concurrency()throws Exception {
        Course c=new Course(64,11);AtomicReference<Throwable> failure=new AtomicReference<>();Thread[] workers=new Thread[8];
        for(int worker=0;worker<workers.length;worker++) {
            final int start=worker*8;
            workers[worker]=new Thread(()->{try{
                for(int n=0;n<200;n++)for(int actor=start;actor<start+8;actor++) {
                    Course.Lesson l=c.issue(actor);c.recordEffort(actor,l.serial(),4);c.finish(actor,l.serial(),false);
                }
            }catch(Throwable problem){failure.compareAndSet(null,problem);}});
            workers[worker].start();
        }
        for(Thread worker:workers)worker.join();
        if(failure.get()!=null)throw new AssertionError(failure.get());
        check(c.effort().equals(new Course.Effort(51200,0,0,0)),"concurrent actors count each observed interval once");
        check(c.episodes()==12800&&c.successes()==0,"effort is not an outcome");
    }
    public static void main(String[] args)throws Exception {
        arithmetic();cadenceAndLifecycle();allStagesAndIsolation();concurrency();
        System.out.println("PASS elapsed-tick review checks="+checks);
    }
}
