package org.botsclustersmc.training;

import org.botsclustersmc.core.Task;

/** Read-only accounting of the immutable behavior policies actually applied in each probe. */
public final class ProbePolicies {
    public record Usage(long decisions,long minimum,long maximum,long changes) {
        public Usage {
            if(decisions<0||changes<0||changes>=Math.max(1,decisions)
                    ||(decisions==0?(minimum!=-1||maximum!=-1||changes!=0):(minimum<0||maximum<minimum))
                    ||(decisions>0&&((minimum==maximum)!=(changes==0))))
                throw new IllegalArgumentException("Behavior-policy usage");
        }
        public boolean mixed(){return decisions>0&&minimum!=maximum;}
    }
    /** One entity-owner thread. Does not sample RNG, inspect weights, or choose actions. */
    public static final class Trace {
        private long decisions,minimum=-1,maximum=-1,previous=-1,changes;
        public void observe(long version){
            if(version<0)throw new IllegalArgumentException("Negative behavior policy");
            long next=Math.addExact(decisions,1),different=changes;
            if(decisions>0&&version!=previous)different=Math.addExact(changes,1);
            minimum=decisions==0?version:Math.min(minimum,version);maximum=Math.max(maximum,version);
            previous=version;changes=different;decisions=next;
        }
        public Usage snapshot(){return new Usage(decisions,minimum,maximum,changes);}
        public void clear(){decisions=0;minimum=maximum=previous=-1;changes=0;}
    }
    public record Totals(long singleTrials,long singleSuccesses,long mixedTrials,long mixedSuccesses,
                         long decisions,long policyChanges,long maximumVersionSpan,long lastMinimum,long lastMaximum) {}
    private static final class Counts {
        long singleTrials,singleSuccesses,mixedTrials,mixedSuccesses,decisions,changes,maximumSpan,lastMinimum=-1,lastMaximum=-1;
        Totals snapshot(){return new Totals(singleTrials,singleSuccesses,mixedTrials,mixedSuccesses,decisions,changes,maximumSpan,lastMinimum,lastMaximum);}
    }
    private final Counts[] tasks=new Counts[Task.values().length];
    public ProbePolicies(){for(int i=0;i<tasks.length;i++)tasks[i]=new Counts();}
    /** Only completed PROBE episodes count. Practice, exams and abandoned episodes are not probes. */
    public synchronized void record(Task task,Course.Kind kind,Usage usage,boolean success){
        if(task==null||kind==null||usage==null)throw new IllegalArgumentException("Missing probe identity");
        if(usage.decisions()==0)throw new IllegalArgumentException("An empty episode is not evidence");
        if(kind!=Course.Kind.PROBE)return;
        Counts c=tasks[task.ordinal()];
        long singleTrials=c.singleTrials,singleSuccesses=c.singleSuccesses,mixedTrials=c.mixedTrials,mixedSuccesses=c.mixedSuccesses;
        if(usage.mixed()){mixedTrials=Math.addExact(mixedTrials,1);if(success)mixedSuccesses=Math.addExact(mixedSuccesses,1);}
        else{singleTrials=Math.addExact(singleTrials,1);if(success)singleSuccesses=Math.addExact(singleSuccesses,1);}
        long decisions=Math.addExact(c.decisions,usage.decisions()),changes=Math.addExact(c.changes,usage.changes());
        c.singleTrials=singleTrials;c.singleSuccesses=singleSuccesses;c.mixedTrials=mixedTrials;c.mixedSuccesses=mixedSuccesses;
        c.decisions=decisions;c.changes=changes;c.maximumSpan=Math.max(c.maximumSpan,usage.maximum()-usage.minimum());
        c.lastMinimum=usage.minimum();c.lastMaximum=usage.maximum();
    }
    public synchronized Totals[] snapshot(){
        Totals[] result=new Totals[tasks.length];for(int i=0;i<tasks.length;i++)result[i]=tasks[i].snapshot();return result;
    }
}
