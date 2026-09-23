package org.botsclustersmc.training;

import java.util.List;

/** A contiguous fragment from ONE actor and ONE episode, never a reset-spanning trace. */
public record Trajectory(long actor,long episode,long sequence,List<Transition> steps) {
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
