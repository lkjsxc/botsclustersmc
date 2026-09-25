package org.botsclustersmc.core;

import java.util.Arrays;

/** Immutable published weights. Workspace arrays belong to one worker only. */
public final class Policy {
    public static final int W1=0, B1=Schema.HIDDEN*Schema.INPUTS, W2=B1+Schema.HIDDEN,
        B2=W2+Schema.HIDDEN*Schema.HIDDEN, W3=B2+Schema.HIDDEN,
        B3=W3+Schema.OUTPUTS*Schema.HIDDEN, PARAMETERS=B3+Schema.OUTPUTS;
    private final float[] weights;
    private final long updates, samples;
    public Policy(float[] weights,long updates,long samples) {
        if(weights.length!=PARAMETERS || updates<0 || samples<0) throw new IllegalArgumentException("policy dimensions/counters");
        for(float w:weights) if(!Float.isFinite(w)) throw new IllegalArgumentException("non-finite weights");
        this.weights=weights.clone(); this.updates=updates; this.samples=samples;
    }
    public long updates() { return updates; }
    public long samples() { return samples; }
    public float[] copyWeights() { return weights.clone(); }
    public static Policy initialize(long seed) {
        float[] w=new float[PARAMETERS]; RandomSource r=new RandomSource(seed);
        for(int i=W1;i<B1;i++) w[i]=r.symmetric(Math.sqrt(6.0/(Schema.INPUTS+Schema.HIDDEN)));
        for(int i=W2;i<B2;i++) w[i]=r.symmetric(Math.sqrt(3.0/Schema.HIDDEN));
        for(int row=0;row<Schema.OUTPUTS;row++) for(int col=0;col<Schema.HIDDEN;col++)
            w[W3+row*Schema.HIDDEN+col]=r.symmetric((row==Schema.LOGITS?1:.01)*Math.sqrt(3.0/Schema.HIDDEN));
        return new Policy(w,0,0);
    }
    public static final class Workspace {
        public final float[] h1=new float[Schema.HIDDEN], h2=new float[Schema.HIDDEN], logits=new float[Schema.OUTPUTS];
        public final double[] probabilities=new double[Schema.DISTRIBUTION];
        public final float[] dh1=new float[Schema.HIDDEN],dh2=new float[Schema.HIDDEN],dout=new float[Schema.OUTPUTS];
    }
    public void forward(float[] x,boolean[] mask,Workspace s) {
        Schema.checkObservation(x);
        dense(x,s.h1,weights,W1,B1,Schema.INPUTS,true);
        dense(s.h1,s.h2,weights,W2,B2,Schema.HIDDEN,true);
        dense(s.h2,s.logits,weights,W3,B3,Schema.HIDDEN,false);
        Distribution.probabilities(s.logits,mask,s.probabilities);
    }
    private static void dense(float[] x,float[] out,float[] w,int wi,int bi,int width,boolean tanh) {
        for(int row=0;row<out.length;row++) {
            float sum=w[bi+row]; int start=wi+row*width;
            for(int col=0;col<width;col++) sum+=w[start+col]*x[col];
            out[row]=tanh?(float)Math.tanh(sum):sum;
        }
    }
    public void backward(float[] x,Workspace s,float[] outGradient,float[] gradient) {
        if(gradient.length!=PARAMETERS || outGradient.length!=Schema.OUTPUTS) throw new IllegalArgumentException("gradient size");
        Arrays.fill(s.dh1,0); Arrays.fill(s.dh2,0);
        for(int row=0;row<Schema.OUTPUTS;row++) {
            float g=outGradient[row]; gradient[B3+row]+=g;
            for(int col=0;col<Schema.HIDDEN;col++) {int k=W3+row*Schema.HIDDEN+col; gradient[k]+=g*s.h2[col]; s.dh2[col]+=g*weights[k];}
        }
        for(int row=0;row<Schema.HIDDEN;row++) {
            float g=s.dh2[row]*(1-s.h2[row]*s.h2[row]); gradient[B2+row]+=g;
            for(int col=0;col<Schema.HIDDEN;col++) {int k=W2+row*Schema.HIDDEN+col; gradient[k]+=g*s.h1[col]; s.dh1[col]+=g*weights[k];}
        }
        for(int row=0;row<Schema.HIDDEN;row++) {
            float g=s.dh1[row]*(1-s.h1[row]*s.h1[row]); gradient[B1+row]+=g;
            for(int col=0;col<Schema.INPUTS;col++) gradient[W1+row*Schema.INPUTS+col]+=g*x[col];
        }
    }
    /** Feature-major batched dense layers reuse each weight across contiguous lanes. */
    public static final class BatchWorkspace {
        private final int capacity;
        private final float[] input,first,second,output;
        public BatchWorkspace(int capacity) {
            if(capacity<1 || capacity>256) throw new IllegalArgumentException("batch capacity");
            this.capacity=capacity; input=new float[Schema.INPUTS*capacity]; first=new float[Schema.HIDDEN*capacity];
            second=new float[Schema.HIDDEN*capacity]; output=new float[Schema.OUTPUTS*capacity];
        }
        public void lane(int index,float[] logits) {
            for(int row=0;row<Schema.OUTPUTS;row++) logits[row]=output[row*capacity+index];
        }
    }
    public void forwardBatch(float[][] observations,int count,BatchWorkspace s) {
        if(count<1 || count>s.capacity || observations.length<count) throw new IllegalArgumentException("batch count");
        for(int b=0;b<count;b++) {Schema.checkObservation(observations[b]); for(int col=0;col<Schema.INPUTS;col++) s.input[col*s.capacity+b]=observations[b][col];}
        batchLayer(s.input,s.first,W1,B1,Schema.INPUTS,Schema.HIDDEN,count,s.capacity,true);
        batchLayer(s.first,s.second,W2,B2,Schema.HIDDEN,Schema.HIDDEN,count,s.capacity,true);
        batchLayer(s.second,s.output,W3,B3,Schema.HIDDEN,Schema.OUTPUTS,count,s.capacity,false);
    }
    private void batchLayer(float[] x,float[] out,int wi,int bi,int width,int rows,int n,int stride,boolean tanh) {
        for(int row=0;row<rows;row++) {
            int o=row*stride; Arrays.fill(out,o,o+n,weights[bi+row]);
            for(int col=0;col<width;col++) {
                float w=weights[wi+row*width+col]; int c=col*stride;
                for(int b=0;b<n;b++) out[o+b]+=w*x[c+b];
            }
            if(tanh) for(int b=0;b<n;b++) out[o+b]=(float)Math.tanh(out[o+b]);
        }
    }
}
