package org.botsclustersmc.training;

import org.botsclustersmc.core.Policy;
import org.botsclustersmc.core.Schema;
import org.botsclustersmc.core.Task;

/** Research ablation: task-12 value loss trains its linear readout, not shared features.
 * Actor gradients still train all shared features. No forward/routing/moment change.
 */
public final class CriticFeatures {
    private CriticFeatures() {}
    public static void backward(Policy policy,float[] observation,Policy.Workspace workspace,
            float[] outputGradient,float[] gradient) {
        if(TaskBalance.task(observation)!=Task.MINE_COBBLESTONE.ordinal()) {
            policy.backward(observation,workspace,outputGradient,gradient);return;
        }
        if(outputGradient.length!=Schema.OUTPUTS||gradient.length!=Policy.PARAMETERS)
            throw new IllegalArgumentException("gradient size");
        float[] actor=outputGradient.clone();
        float critic=actor[Schema.LOGITS];actor[Schema.LOGITS]=0;
        policy.backward(observation,workspace,actor,gradient);
        gradient[Policy.B3+Schema.LOGITS]+=critic;
        int row=Policy.W3+Schema.LOGITS*Schema.HIDDEN;
        for(int column=0;column<Schema.HIDDEN;column++)gradient[row+column]+=critic*workspace.h2[column];
    }
}
