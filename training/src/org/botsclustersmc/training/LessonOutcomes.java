package org.botsclustersmc.training;

import org.botsclustersmc.core.Task;

/** Exact completed-episode counts for this process; separate from readiness priors. */
public final class LessonOutcomes {
    public record Totals(long trainingTrials,long trainingSuccesses,long probeTrials,
            long probeSuccesses,long examTrials,long examSuccesses,ProbePolicies.Totals policyUse) {}
    private final ProbePolicies policies=new ProbePolicies();
    private final long[][] trials=new long[3][Task.values().length];
    private final long[][] successes=new long[3][Task.values().length];
    public synchronized void record(Task task,Course.Kind kind,boolean success,ProbePolicies.Usage usage) {
        if(task==null||kind==null)throw new IllegalArgumentException("Missing lesson identity");
        int row=kind.ordinal(),column=task.ordinal();
        long nextTrials=Math.addExact(trials[row][column],1);
        long nextSuccesses=Math.addExact(successes[row][column],success?1:0);
        // The total and policy-use partition are published under the same monitor.
        policies.record(task,kind,usage,success);
        trials[row][column]=nextTrials;successes[row][column]=nextSuccesses;
    }
    public synchronized Totals[] snapshot() {
        Totals[] result=new Totals[Task.values().length];ProbePolicies.Totals[] usage=policies.snapshot();
        int practice=Course.Kind.PRACTICE.ordinal(),probe=Course.Kind.PROBE.ordinal(),exam=Course.Kind.EXAM.ordinal();
        for(int i=0;i<result.length;i++)result[i]=new Totals(trials[practice][i]+trials[probe][i],
            successes[practice][i]+successes[probe][i],trials[probe][i],successes[probe][i],
            trials[exam][i],successes[exam][i],usage[i]);
        return result;
    }
}
