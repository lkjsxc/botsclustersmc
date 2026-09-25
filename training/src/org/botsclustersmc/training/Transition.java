package org.botsclustersmc.training;

import org.botsclustersmc.core.*;

/** Arrays are immutable after ownership transfer to a trajectory. */
public record Transition(float[] observation, boolean[] mask, int[] action,
        double behaviorLogProbability, long behaviorVersion, float reward, int ticks,
        float[] nextObservation, boolean[] nextMask, boolean terminal) {
    public Transition {
        Schema.checkObservation(observation); Schema.checkObservation(nextObservation); Schema.checkAction(action);
        if(mask.length!=Schema.DISTRIBUTION || nextMask.length!=Schema.DISTRIBUTION || behaviorVersion<0
            || !Double.isFinite(behaviorLogProbability) || behaviorLogProbability>1e-6
            || !Float.isFinite(reward) || ticks<1 || ticks>12000) throw new IllegalArgumentException("invalid transition");
    }
}
