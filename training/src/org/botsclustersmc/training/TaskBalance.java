package org.botsclustersmc.training;

import java.util.List;
import org.botsclustersmc.core.Task;

/** Bounded per-task loss allocation within one real batch, not replay or new experience. */
public final class TaskBalance {
    public static final int TASKS=Task.values().length, UNLABELLED=TASKS;
    public static final double MAX_WEIGHT=8;
    private final int[] counts;private final double[] weights;
    private TaskBalance(int[] counts,double[] weights){this.counts=counts;this.weights=weights;}
    public int[] counts(){return counts.clone();}
    public double[] weights(){return weights.clone();}
    public double weight(float[] observation){return weights[task(observation)];}
    public static int task(float[] observation) {
        if(observation.length<16+TASKS)throw new IllegalArgumentException("Missing task observation");
        int selected=UNLABELLED;
        for(int i=0;i<TASKS;i++) {
            float value=observation[16+i];
            if(value==1&&selected==UNLABELLED)selected=i;
            else if(value!=0)return UNLABELLED;
        }
        return selected;
    }
    public static TaskBalance forBatch(List<Trajectory> batch) {
        int[] counts=new int[TASKS+1];
        for(Trajectory fragment:batch)for(Transition step:fragment.steps())counts[task(step.observation())]++;
        return fromCounts(counts);
    }
    public static TaskBalance fromCounts(int[] input) {
        if(input.length!=TASKS+1)throw new IllegalArgumentException("Task count dimension");
        int[] counts=input.clone();int total=0,groups=0;
        for(int count:counts) {
            if(count<0||count>65567)throw new IllegalArgumentException("Task count bounds");
            total=Math.addExact(total,count);if(count>0)groups++;
        }
        if(total<1||total>65567)throw new IllegalArgumentException("Batch sample bounds");
        double[] weights=new double[counts.length];boolean[] capped=new boolean[counts.length];
        double mass=total;int remaining=groups;
        for(int pass=0;pass<groups;pass++) {
            boolean changed=false;
            for(int i=0;i<counts.length;i++)if(counts[i]>0&&!capped[i]
                    &&mass/remaining>counts[i]*MAX_WEIGHT) {
                weights[i]=MAX_WEIGHT;capped[i]=true;mass-=counts[i]*MAX_WEIGHT;remaining--;changed=true;
            }
            if(!changed)break;
        }
        if(remaining<1)throw new IllegalStateException("Loss allocation exhausted");
        for(int i=0;i<counts.length;i++)if(counts[i]>0&&!capped[i])weights[i]=mass/(remaining*counts[i]);
        return new TaskBalance(counts,weights);
    }
}
