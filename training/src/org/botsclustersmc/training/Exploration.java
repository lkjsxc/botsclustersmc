package org.botsclustersmc.training;

import org.botsclustersmc.core.*;

/** Uniform legal parent controls, then uniform legal slots conditional on each click type. */
public final class Exploration {
    private Exploration() {}
    public static final double COEFFICIENT=.005;
    private static int legal(boolean[] mask,int off,int size) {
        int n=0;for(int j=0;j<size;j++)if(mask[off+j])n++;
        if(n==0)throw new IllegalArgumentException("Empty legal control set");return n;
    }
    public static void addGradient(double[] p,boolean[] mask,double coefficient,float[] gradient) {
        if(p.length!=Schema.DISTRIBUTION||mask.length!=Schema.DISTRIBUTION||gradient.length!=Schema.OUTPUTS)
            throw new IllegalArgumentException("exploration dimensions");
        int off=0;
        for(int head=0;head<7;head++) {
            add(p,mask,off,off,Schema.HEADS[head],coefficient,gradient);off+=Schema.HEADS[head];
        }
        int parent=Task.offset(6),parents=legal(mask,parent,Schema.HEADS[6]);
        for(int op=1;op<=3;op++)if(mask[parent+op])
            add(p,mask,Schema.slotOffset(op),Task.offset(7),Schema.HEADS[7],coefficient/parents,gradient);
    }
    private static void add(double[] p,boolean[] mask,int off,int logitOffset,int size,double weight,float[] gradient) {
        int n=legal(mask,off,size);
        if(n>1)for(int j=0;j<size;j++)if(mask[off+j])
            gradient[logitOffset+j]+=(float)(weight*(p[off+j]-1.0/n));
    }
    /** Stable cross entropy of that fixed joint prior, not a policy-weighted surrogate. */
    public static double loss(float[] logits,boolean[] mask,double coefficient) {
        if(logits.length<Schema.LOGITS||mask.length!=Schema.DISTRIBUTION)
            throw new IllegalArgumentException("exploration dimensions");
        double loss=0;int off=0;
        for(int head=0;head<7;head++) {
            loss+=crossEntropy(logits,mask,off,off,Schema.HEADS[head]);off+=Schema.HEADS[head];
        }
        int parent=Task.offset(6),parents=legal(mask,parent,Schema.HEADS[6]);
        for(int op=1;op<=3;op++)if(mask[parent+op])
            loss+=crossEntropy(logits,mask,Schema.slotOffset(op),Task.offset(7),Schema.HEADS[7])/parents;
        return coefficient*loss;
    }
    private static double crossEntropy(float[] logits,boolean[] mask,int off,int logitOffset,int size) {
        int n=legal(mask,off,size);if(n==1)return 0;
        double max=Double.NEGATIVE_INFINITY,sumLogits=0,sum=0;
        for(int j=0;j<size;j++)if(mask[off+j]) {
            max=Math.max(max,logits[logitOffset+j]);sumLogits+=logits[logitOffset+j];
        }
        for(int j=0;j<size;j++)if(mask[off+j])sum+=Math.exp(logits[logitOffset+j]-max);
        return max+Math.log(sum)-sumLogits/n;
    }
}
