package org.botsclustersmc.training;

import java.util.*;
import org.botsclustersmc.core.Policy;

/** Read-only accepted-data accounting. One learner writer; no rewards, masks or policy inputs. */
public final class LearningContexts {
    public static final int TASK_BUCKETS=TaskBalance.TASKS+1, MENU_BUCKETS=6, UNKNOWN_MENU=5;
    public static final String MENU_ORDER="closed,inventory,workbench,furnace,chest,unknown";
    private static final int CELLS=TASK_BUCKETS*MENU_BUCKETS;
    private volatile Snapshot current;

    public LearningContexts(Policy initial) {
        current=new Snapshot(initial.samples(),initial.updates(),initial.samples(),new long[CELLS],new long[CELLS]);
    }

    /** Exact Sensors observation[42] encoding; noncanonical values stay explicitly unknown. */
    public static int menu(float value) {
        int ordinal=(int)(value*4);
        return ordinal>=0&&ordinal<UNKNOWN_MENU&&value==ordinal/4f?ordinal:UNKNOWN_MENU;
    }

    public Snapshot snapshot(){return current;}

    /** Call only after UpdateGuard accepts a batch, once per published update.
     * Count pre-action states and their elapsed actor ticks, never bootstrap states or resets.
     */
    public void accepted(List<Trajectory> batch,Policy next) {
        Snapshot previous=current;
        if(next.updates()!=Math.addExact(previous.policyUpdates(),1))
            throw new IllegalArgumentException("Context update identity");
        long[] samples=previous.samples(),ticks=previous.ticks();
        long added=0;
        for(Trajectory fragment:batch)for(Transition step:fragment.steps()) {
            int cell=TaskBalance.task(step.observation())*MENU_BUCKETS+menu(step.observation()[42]);
            samples[cell]=Math.addExact(samples[cell],1);
            ticks[cell]=Math.addExact(ticks[cell],step.ticks());
            added++;
        }
        if(added==0)throw new IllegalArgumentException("Empty accepted context batch");
        // Construct and validate before publication; failures cannot alter the last coherent snapshot.
        current=new Snapshot(previous.baseSamples(),next.updates(),next.samples(),samples,ticks);
    }

    public record Snapshot(long baseSamples,long policyUpdates,long trainedSamples,long[] samples,long[] ticks) {
        public Snapshot {
            if(baseSamples<0||policyUpdates<0||trainedSamples<baseSamples||samples.length!=CELLS||ticks.length!=CELLS)
                throw new IllegalArgumentException("Context snapshot dimensions/counters");
            samples=samples.clone();ticks=ticks.clone();
            long total=0,duration=0;
            for(int i=0;i<CELLS;i++) {
                if(samples[i]<0||ticks[i]<samples[i]||(samples[i]==0&&ticks[i]!=0))
                    throw new IllegalArgumentException("Context sample/tick counts");
                total=Math.addExact(total,samples[i]);duration=Math.addExact(duration,ticks[i]);
            }
            if(total!=trainedSamples-baseSamples)
                throw new IllegalArgumentException("Context accepted-sample identity");
        }
        @Override public long[] samples(){return samples.clone();}
        @Override public long[] ticks(){return ticks.clone();}
        public long totalSamples(){return trainedSamples-baseSamples;}
        public long totalTicks(){long n=0;for(long value:ticks)n+=value;return n;}
        public long[] taskSamples() {
            long[] counts=new long[TASK_BUCKETS];
            for(int i=0;i<CELLS;i++)counts[i/MENU_BUCKETS]+=samples[i];
            return counts;
        }
        /** One snapshot supplies every field, including the pre-existing per-task totals. */
        public Map<String,Object> status() {
            Map<String,Object> result=new LinkedHashMap<>();
            result.put("learned_task_samples_this_process",Arrays.toString(taskSamples()));
            result.put("learner_context_scope","accepted-pre-action-observations-this-process");
            result.put("learner_context_layout","task-major");
            result.put("learner_context_task_buckets",TASK_BUCKETS);
            result.put("learner_context_unlabelled_task",TaskBalance.UNLABELLED);
            result.put("learner_context_menu_buckets",MENU_BUCKETS);
            result.put("learner_context_menu_order",MENU_ORDER);
            result.put("learner_context_base_trained_samples",baseSamples);
            result.put("learner_context_policy_updates",policyUpdates);
            result.put("learner_context_trained_samples",trainedSamples);
            result.put("learner_context_samples",totalSamples());
            result.put("learner_context_ticks",totalTicks());
            result.put("learner_context_samples_by_task_and_menu",Arrays.toString(samples));
            result.put("learner_context_ticks_by_task_and_menu",Arrays.toString(ticks));
            return result;
        }
    }
}
