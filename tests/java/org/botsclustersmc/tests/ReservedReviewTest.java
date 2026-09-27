package org.botsclustersmc.tests;

import java.util.*;
import org.botsclustersmc.core.*;
import org.botsclustersmc.training.*;

/** Scheduling and lifecycle evidence, not evidence of learned Minecraft retention. */
public final class ReservedReviewTest {
    private static long checks;
    private static void check(boolean yes,String message){checks++;if(!yes)throw new AssertionError(message);}
    private static void rejects(Runnable action){
        try{action.run();throw new AssertionError("invalid reservation accepted");}
        catch(IllegalArgumentException|IllegalStateException expected){checks++;}
    }
    private static void ledger(ReviewEffort pool,Collection<ReviewEffort.Reservation> active,int frontier,long[] actual){
        long credit=0;long[] pending=new long[18];int count=0;
        for(var r:active){count++;check(r.remainingTicks()>=0,"nonnegative remaining forecast");
            if(frontier>0){if(r.task()==frontier)credit+=r.remainingTicks();
                else{credit-=4L*r.remainingTicks();pending[r.task()]+=r.remainingTicks();}}}
        check(pool.reservations()==count&&pool.reservedCredit()==credit,"owned reservation conservation");
        long review=0;for(int task=0;task<18;task++){
            check(pool.reservedReviewTicks(task)==pending[task],"per-task forecast conservation");
            check(pool.reviewedTicks(task)==(task<frontier?actual[task]:0),"forecasts are not observed ticks");
            if(task<frontier)review+=actual[task];
        }
        check(pool.credit()==(frontier==0?0:actual[frontier]-4*review),"actual credit conservation");
    }
    private static void arithmetic(){
        ReviewEffort a=new ReviewEffort(),b=new ReviewEffort();RandomSource rng=new RandomSource(71);
        var frontier=a.reserve(12,rng);check(frontier.task()==12,"first reservation is frontier");
        var review=a.reserve(12,rng);check(review.task()<12,"second assignment need not await a whole frontier episode");
        check(a.credit()==0,"reserving work does not fabricate observed time");
        long forecast=a.reservedCredit();int remaining=review.remainingTicks();
        rejects(()->b.record(review,4));rejects(()->b.release(review));rejects(()->a.record(review,0));
        rejects(()->a.record(review,-1));rejects(()->a.release(null));rejects(a::reset);
        rejects(()->a.reserve(11,rng));rejects(()->a.reserve(-1,rng));
        check(a.reservedCredit()==forecast&&review.remainingTicks()==remaining&&a.credit()==0,"invalid calls are nonmutating");
        long[] actual=new long[18];List<ReviewEffort.Reservation> active=new ArrayList<>(List.of(frontier,review));
        a.record(frontier,7);actual[12]+=7;
        a.record(review,3);actual[review.task()]+=3;ledger(a,active,12,actual);
        a.record(frontier,Integer.MAX_VALUE);actual[12]+=Integer.MAX_VALUE;
        check(frontier.remainingTicks()==0,"real time may exceed a bounded forecast without becoming negative");
        ledger(a,active,12,actual);
        a.release(review);active.remove(review);ledger(a,active,12,actual);
        rejects(()->a.record(review,4));rejects(()->a.release(review));
        a.release(frontier);active.clear();ledger(a,active,12,actual);
        a.reset();check(a.credit()==0&&a.reservedCredit()==0,"explicit empty reset");

        ReviewEffort split=new ReviewEffort(),whole=new ReviewEffort();
        var s=split.reserve(17,new RandomSource(1));var w=whole.reserve(17,new RandomSource(1));
        split.record(s,11);split.record(s,29);whole.record(w,40);
        check(split.credit()==whole.credit()&&split.reservedCredit()==whole.reservedCredit()
            &&s.remainingTicks()==w.remainingTicks(),"partitioning observed ticks changes no allocation balance");

        ReviewEffort bounded=new ReviewEffort();List<ReviewEffort.Reservation> all=new ArrayList<>();
        for(int i=0;i<10000;i++)all.add(bounded.reserve(0,rng));
        rejects(()->bounded.reserve(0,rng));check(bounded.credit()==0,"foundation never creates rehearsal debt");
        for(var r:all)bounded.release(r);
        check(bounded.reservations()==0&&bounded.reservedCredit()==0,"bounded maximum cohort drains");
    }
    private record Event(long end,long sequence,ReviewEffort.Reservation reservation,int duration){}
    private static Event event(ReviewEffort pool,int frontier,RandomSource rng,long now,long sequence,int variant){
        var r=pool.reserve(frontier,rng);int horizon=Task.at(r.task()).horizon();
        int duration=r.task()==frontier?horizon:variant==0?20+8*r.task():1+rng.nextInt(horizon);
        return new Event(now+duration,sequence,r,duration);
    }
    private static void concurrentAllocation(){
        for(int frontier:new int[]{1,5,12,17})for(int variant=0;variant<2;variant++){
            ReviewEffort pool=new ReviewEffort();RandomSource rng=new RandomSource(719+frontier);
            PriorityQueue<Event> events=new PriorityQueue<>(Comparator.comparingLong(Event::end).thenComparingLong(Event::sequence));
            long[] actual=new long[18];int[] first=new int[18];long sequence=0,now=0;
            for(int actor=0;actor<512;actor++){var e=event(pool,frontier,rng,0,++sequence,variant);events.add(e);first[e.reservation.task()]++;}
            for(int task=0;task<=frontier;task++)check(first[task]>0,"first cohort covers every reachable task before any completion");
            check(pool.credit()==0,"coverage reservations are not learned examples");
            for(int episode=0;episode<250000;episode++){
                Event e=events.remove();now=e.end();var r=e.reservation();
                pool.record(r,e.duration());actual[r.task()]+=e.duration();pool.release(r);
                events.add(event(pool,frontier,rng,now,++sequence,variant));
                check(pool.reservations()==512,"one pending reservation per asynchronous actor");
                if(episode%1000==0)ledger(pool,events.stream().map(Event::reservation).toList(),frontier,actual);
            }
            long all=Arrays.stream(actual).sum(),review=all-actual[frontier];
            check(Math.abs(review/(double)all-.2)<.012,"long-run actual rehearsal remains near 20% with unequal episodes");
            for(var e:events)pool.release(e.reservation());
            ledger(pool,List.of(),frontier,actual);
            System.out.printf(Locale.ROOT,"RESERVATIONS frontier=%d variant=%d first=%s observed_review=%.6f%n",
                frontier,variant,Arrays.toString(first),review/(double)all);
        }
    }
    private static void promote(Course course,int actor){
        for(int step=0;step<20000&&!course.needsExam(actor);step++){
            var l=course.issue(actor);course.recordEffort(actor,l.serial(),4);course.finish(actor,l.serial(),true);
        }
        check(course.needsExam(actor),"earned prerequisite through ordinary course rules");
        course.beginExam(actor,123);int stage=course.stage(actor),index=0;
        while(course.examVersion(actor)>=0){
            var l=course.issue(actor);
            check(l.kind()==Course.Kind.EXAM&&l.difficulty()==1,"unchanged full-condition exam");
            check(l.task().ordinal()==(index<16?stage:(index-16)/4),"unchanged canonical exam order");
            int before=course.reservedAgents();course.recordEffort(actor,l.serial(),4);course.finish(actor,l.serial(),true);
            check(course.reservedAgents()==before,"exam cannot consume or reserve training effort");index++;
        }
        check(course.stage(actor)==stage+1,"real synthetic exam, not an edited certificate");
    }
    private static void lifecycle()throws Exception{
        Course original=new Course(64,941);
        for(int actor=0;actor<64;actor++){promote(original,actor);promote(original,actor);}
        byte[] state=original.encode();Course a=Course.decode(state,64),b=Course.decode(state,64);
        int[] coverage=new int[18];
        for(int actor=0;actor<64;actor++){
            var x=a.issue(actor);var y=b.issue(actor);
            check(x.equals(y),"same source, issue order and saved RNG are reproducible");
            check(a.certifiedVersion(actor,1)==123,"reservation cannot manufacture a certificate");
            coverage[x.task().ordinal()]++;
        }
        check(coverage[0]>0&&coverage[1]>0&&coverage[2]>0,"Course restore covers old and new work immediately");
        check(a.effort().equals(new Course.Effort(0,0,0,0)),"initial reservation is never observed telemetry");
        check(a.reservedAgents()==64,"all 64 non-exam lessons own a forecast");
        var current=a.currentLesson(0);byte[] before=a.encode();
        a.recordEffort(0,current.serial(),19);
        check(Arrays.equals(before,a.encode()),"forecast reconciliation does not change checkpoint/RNG/model state");
        rejects(()->a.recordEffort(0,current.serial()+999999,4));
        for(int actor=0;actor<64;actor++){a.abandon(actor);b.abandon(actor);}
        check(a.reservedAgents()==0&&b.reservedAgents()==0,"abandon releases only owned pending work");
        Course decoded=Course.decode(a.encode(),64);
        check(decoded.reservedAgents()==0&&decoded.effort().equals(new Course.Effort(0,0,0,0)),"resume does not pretend to restore missing transient forecasts");
    }
    public static void main(String[] args)throws Exception{
        arithmetic();concurrentAllocation();lifecycle();
        System.out.println("PASS owned cohort reservation checks="+checks+"; not a learned retention result");
    }
}
