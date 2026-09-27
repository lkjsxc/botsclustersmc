package org.botsclustersmc.training;

import java.util.Arrays;
import java.util.Objects;
import org.botsclustersmc.core.RandomSource;
import org.botsclustersmc.core.Task;

/** Observed effort plus separately tracked episode reservations; never gameplay actions. */
public final class ReviewEffort {
    public static final int FRONTIER_PER_REVIEW = 4;
    private static final int TASKS = Task.values().length;
    private final long[] reviewedTicks = new long[TASKS];
    private long credit;
    private final long[] reservedReviewTicks = new long[TASKS];
    private long reservedCredit;
    private int reservations,reservationFrontier=-1;

    /** An owned, process-local forecast. It is never an observed tick or training sample. */
    public static final class Reservation {
        private final ReviewEffort owner;
        private final int frontier,task;
        private int remaining;
        private boolean active=true;
        private Reservation(ReviewEffort owner,int frontier,int task,int remaining) {
            this.owner=owner;this.frontier=frontier;this.task=task;this.remaining=remaining;
        }
        public int task(){return task;}
        public int remainingTicks(){return remaining;}
    }

    /** Called under the Course lock, shared only by actors with the same frontier. */
    public Reservation reserve(int frontier,RandomSource random) {
        if(reservationFrontier>=0&&reservationFrontier!=frontier)
            throw new IllegalArgumentException("Review pool frontier differs");
        if(reservations>=10000)throw new IllegalStateException("Too many review reservations");
        int task=select(frontier,random,true),duration=Task.at(task).horizon();
        long delta=frontier==0?0:task==frontier?duration:-FRONTIER_PER_REVIEW*(long)duration;
        long nextCredit=Math.addExact(reservedCredit,delta);
        long nextReview=task<frontier?Math.addExact(reservedReviewTicks[task],duration):0;
        Reservation result=new Reservation(this,frontier,task,duration);
        reservedCredit=nextCredit;if(task<frontier)reservedReviewTicks[task]=nextReview;
        reservationFrontier=frontier;reservations++;return result;
    }
    private void owned(Reservation reservation) {
        if(reservation==null||reservation.owner!=this||!reservation.active)
            throw new IllegalStateException("Inactive or foreign review reservation");
    }
    public void record(Reservation reservation,int ticks) {
        owned(reservation);
        // Record real time first. Invalid input cannot release a forecast or mutate its owner.
        record(reservation.frontier,reservation.task,ticks);
        releaseTicks(reservation,Math.min(ticks,reservation.remaining));
    }
    public void release(Reservation reservation) {
        owned(reservation);releaseTicks(reservation,reservation.remaining);
        reservation.active=false;reservations--;
    }
    private void releaseTicks(Reservation reservation,int ticks) {
        int frontier=reservation.frontier,task=reservation.task;
        if(frontier>0) {
            reservedCredit-=task==frontier?ticks:-FRONTIER_PER_REVIEW*(long)ticks;
            if(task<frontier)reservedReviewTicks[task]-=ticks;
        }
        reservation.remaining-=ticks;
    }
    public long reservedCredit(){return reservedCredit;}
    public long reservedReviewTicks(int task){checkFrontier(task);return reservedReviewTicks[task];}
    public long reviewedTicks(int task){checkFrontier(task);return reviewedTicks[task];}
    public int reservations(){return reservations;}

    /** Prefer the frontier until it earns review time; break equal-effort ties randomly. */
    public int select(int frontier, RandomSource random) {
        return select(frontier,random,false);
    }
    private int select(int frontier,RandomSource random,boolean includeReservations) {
        checkFrontier(frontier);
        Objects.requireNonNull(random, "random");
        long balance=includeReservations?Math.addExact(credit,reservedCredit):credit;
        if (frontier == 0 || balance <= 0) return frontier;
        long least = Long.MAX_VALUE;
        int selected = -1, ties = 0;
        for (int task = 0; task < frontier; task++) {
            long spent = includeReservations?Math.addExact(reviewedTicks[task],reservedReviewTicks[task]):reviewedTicks[task];
            if (spent < least) { least = spent; selected = task; ties = 1; }
            else if (spent == least && random.nextInt(++ties) == 0) selected = task;
        }
        return selected;
    }

    /** The caller excludes exams. Successful, failed and interrupted work cost equally. */
    public void record(int frontier, int task, int ticks) {
        checkFrontier(frontier);
        if (task < 0 || task > frontier || ticks <= 0)
            throw new IllegalArgumentException("training effort");
        if (frontier == 0) return; // There is no earlier skill to revisit yet.
        if (task == frontier) credit = Math.addExact(credit, ticks);
        else {
            long nextCredit = Math.subtractExact(credit, FRONTIER_PER_REVIEW * (long) ticks);
            long nextTicks = Math.addExact(reviewedTicks[task], ticks);
            credit = nextCredit;
            reviewedTicks[task] = nextTicks;
        }
    }

    /** A changed frontier starts a new allocation interval, not a new learned model. */
    public void reset() {
        if(reservations!=0)throw new IllegalStateException("Cannot reset owned reservations");
        credit=0;reservedCredit=0;reservationFrontier=-1;
        Arrays.fill(reviewedTicks,0);Arrays.fill(reservedReviewTicks,0);
    }
    public long credit() { return credit; }
    private static void checkFrontier(int frontier) {
        if (frontier < 0 || frontier >= TASKS) throw new IllegalArgumentException("frontier");
    }
}
