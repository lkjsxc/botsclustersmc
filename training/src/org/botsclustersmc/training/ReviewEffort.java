package org.botsclustersmc.training;

import java.util.Arrays;
import java.util.Objects;
import org.botsclustersmc.core.RandomSource;
import org.botsclustersmc.core.Task;

/** Episode-boundary allocation of observed training ticks, never gameplay actions. */
public final class ReviewEffort {
    public static final int FRONTIER_PER_REVIEW = 4;
    private static final int TASKS = Task.values().length;
    private final long[] reviewedTicks = new long[TASKS];
    private long credit;

    /** Prefer the frontier until it earns review time; break equal-effort ties randomly. */
    public int select(int frontier, RandomSource random) {
        checkFrontier(frontier);
        Objects.requireNonNull(random, "random");
        if (frontier == 0 || credit <= 0) return frontier;
        long least = Long.MAX_VALUE;
        int selected = -1, ties = 0;
        for (int task = 0; task < frontier; task++) {
            long spent = reviewedTicks[task];
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
    public void reset() { credit = 0; Arrays.fill(reviewedTicks, 0); }
    public long credit() { return credit; }
    private static void checkFrontier(int frontier) {
        if (frontier < 0 || frontier >= TASKS) throw new IllegalArgumentException("frontier");
    }
}
