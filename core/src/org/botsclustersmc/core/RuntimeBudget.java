package org.botsclustersmc.core;

/** Available processors honors the JVM's effective container allocation. */
public record RuntimeBudget(int processors,int regionThreads,int inferenceThreads,int learnerThreads) {
    public static RuntimeBudget automatic(int processors) {
        if(processors<1) throw new IllegalArgumentException("processor budget");
        int usable=Math.max(1,processors-1);
        int inference=Math.max(1,usable/8);
        int learner=Math.max(1,usable/3);
        int regions=Math.max(1,usable-inference-learner);
        return new RuntimeBudget(processors,regions,inference,learner);
    }
    public static RuntimeBudget current(){return automatic(Runtime.getRuntime().availableProcessors());}
}
