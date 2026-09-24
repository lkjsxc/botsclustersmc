package org.botsclustersmc.training;

import org.botsclustersmc.core.Task;

/** Exact assisted-reset outcomes by task and starting missing-cell count, this process only. */
public final class CraftingOutcomes {
    public static final int BUCKETS=6;
    public record Totals(long[] trials,long[] successes) {}
    private final long[] trials=new long[Task.values().length*BUCKETS],successes=new long[trials.length];
    public synchronized void record(Task task,int missingCells,boolean success) {
        int cells=InitialCrafting.ingredientCells(task.ordinal());
        if(cells==0||missingCells<0||missingCells>cells)throw new IllegalArgumentException("crafting reset bucket");
        int index=task.ordinal()*BUCKETS+missingCells;
        trials[index]++;if(success)successes[index]++;
    }
    public synchronized Totals snapshot() {return new Totals(trials.clone(),successes.clone());}
}
