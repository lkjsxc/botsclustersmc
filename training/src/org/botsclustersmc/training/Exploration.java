package org.botsclustersmc.training;

import org.botsclustersmc.core.Schema;

/** A weak, task-wide uniform prior over legal controls; never a preferred gameplay action. */
public final class Exploration {
    private Exploration() {}
    public static final double COEFFICIENT=.005;
    public static void addGradient(double[] probabilities,boolean[] mask,double coefficient,float[] gradient) {
        int offset=0;
        for(int size:Schema.HEADS) {
            int legal=0;for(int j=0;j<size;j++)if(mask[offset+j])legal++;
            if(legal==0)throw new IllegalArgumentException("Empty legal control set");
            if(legal>1)for(int j=0;j<size;j++)if(mask[offset+j])
                gradient[offset+j]+=(float)(coefficient*(probabilities[offset+j]-1.0/legal));
            offset+=size;
        }
    }
    /** Stable cross entropy H(uniform legal prior, policy), differentiated head by head. */
    public static double loss(float[] logits,boolean[] mask,double coefficient) {
        double loss=0;int offset=0;
        for(int size:Schema.HEADS) {
            int n=0;double max=Double.NEGATIVE_INFINITY,sumLogits=0;
            for(int j=0;j<size;j++)if(mask[offset+j]){n++;max=Math.max(max,logits[offset+j]);sumLogits+=logits[offset+j];}
            if(n==0)throw new IllegalArgumentException("Empty legal control set");double sum=0;
            for(int j=0;j<size;j++)if(mask[offset+j])sum+=Math.exp(logits[offset+j]-max);
            if(n>1)loss+=coefficient*(max+Math.log(sum)-sumLogits/n);offset+=size;
        }
        return loss;
    }
}
