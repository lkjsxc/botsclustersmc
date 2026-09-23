package org.botsclustersmc.training;

import org.botsclustersmc.core.Task;

/** Exact completed-episode counts for this process; separate from readiness priors. */
public final class LessonOutcomes {
    public record Totals(long trainingTrials,long trainingSuccesses,long probeTrials,
            long probeSuccesses,long examTrials,long examSuccesses) {}
    private final long[][] trials=new long[3][Task.values().length];
    private final long[][] successes=new long[3][Task.values().length];
    public synchronized void record(Task task,Course.Kind kind,boolean success) {
        int row=kind.ordinal(),column=task.ordinal();
        trials[row][column]++;if(success)successes[row][column]++;
    }
    public synchronized Totals[] snapshot() {
        Totals[] result=new Totals[Task.values().length];
        int practice=Course.Kind.PRACTICE.ordinal(),probe=Course.Kind.PROBE.ordinal(),exam=Course.Kind.EXAM.ordinal();
        for(int i=0;i<result.length;i++)result[i]=new Totals(trials[practice][i]+trials[probe][i],
            successes[practice][i]+successes[probe][i],trials[probe][i],successes[probe][i],
            trials[exam][i],successes[exam][i]);
        return result;
    }
}
