package org.botsclustersmc.training;

import java.util.List;
import org.botsclustersmc.core.Policy;

/** A contiguous fragment from ONE actor and ONE episode, never a reset-spanning trace. */
public record Trajectory(long actor,long episode,long sequence,List<Transition> steps) {
    /** Whole-episode task scope: never stitch a trace across a filtered gap. */
    public boolean learnable(Policy policy) {
        if(policy.learningTask()<0)return true;
        int task=Policy.goal(steps.getFirst().observation());
        for(Transition step:steps)
            if(Policy.goal(step.observation())!=task || Policy.goal(step.nextObservation())!=task)
                throw new IllegalArgumentException("focused trajectory changes task");
        return task==policy.learningTask();
    }
    public Trajectory {
        steps=List.copyOf(steps);
        if(actor<0 || episode<0 || sequence<0 || steps.isEmpty() || steps.size()>128)
            throw new IllegalArgumentException("trajectory identity/length");
        for(int i=0;i+1<steps.size();i++) {
            if(steps.get(i).terminal()) throw new IllegalArgumentException("trace crosses terminal");
            if(!java.util.Arrays.equals(steps.get(i).nextObservation(),steps.get(i+1).observation())
                || !java.util.Arrays.equals(steps.get(i).nextMask(),steps.get(i+1).mask()))
                throw new IllegalArgumentException("noncontiguous trace");
        }
    }
}
