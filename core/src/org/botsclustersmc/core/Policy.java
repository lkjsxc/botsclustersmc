package org.botsclustersmc.core;

import java.util.Arrays;

/** Immutable published weights. Workspace arrays belong to one worker only. */
public final class Policy {
    public static final int W1=0, B1=Schema.HIDDEN*Schema.INPUTS, W2=B1+Schema.HIDDEN,
        B2=W2+Schema.HIDDEN*Schema.HIDDEN, W3=B2+Schema.HIDDEN,
        B3=W3+Schema.OUTPUTS*Schema.HIDDEN, NETWORK_PARAMETERS=B3+Schema.OUTPUTS;
    public static final int EXPERTS=18, PARAMETERS=EXPERTS*NETWORK_PARAMETERS;
    /** Runtime goals are one-hot. Argmax also defines routing for synthetic finite tensors. */
    public static int expert(float[] observation) {
        if(observation.length!=Schema.INPUTS)throw new IllegalArgumentException("observation shape");
        int selected=0;
        for(int task=1;task<EXPERTS;task++)if(observation[16+task]>observation[16+selected])selected=task;
        return selected;
    }
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
        float[] w=new float[NETWORK_PARAMETERS]; RandomSource r=new RandomSource(seed);
        for(int i=W1;i<B1;i++) w[i]=r.symmetric(Math.sqrt(6.0/(Schema.INPUTS+Schema.HIDDEN)));
        for(int i=W2;i<B2;i++) w[i]=r.symmetric(Math.sqrt(3.0/Schema.HIDDEN));
        for(int row=0;row<Schema.OUTPUTS;row++) for(int col=0;col<Schema.HIDDEN;col++)
            w[W3+row*Schema.HIDDEN+col]=r.symmetric((row==Schema.LOGITS?1:.01)*Math.sqrt(3.0/Schema.HIDDEN));
        float[] bank=new float[PARAMETERS];
        for(int task=0;task<EXPERTS;task++)System.arraycopy(w,0,bank,task*NETWORK_PARAMETERS,NETWORK_PARAMETERS);
        return new Policy(bank,0,0);
    }
    public static final class Workspace {
        public final float[] h1=new float[Schema.HIDDEN], h2=new float[Schema.HIDDEN], logits=new float[Schema.OUTPUTS];
        public final double[] probabilities=new double[Schema.DISTRIBUTION];
        public final float[] dh1=new float[Schema.HIDDEN],dh2=new float[Schema.HIDDEN],dout=new float[Schema.OUTPUTS];
    }
    public void forward(float[] x,boolean[] mask,Workspace s) {
        Schema.checkObservation(x);
        int base=expert(x)*NETWORK_PARAMETERS;
        dense(x,s.h1,weights,base+W1,base+B1,Schema.INPUTS,true);
        dense(s.h1,s.h2,weights,base+W2,base+B2,Schema.HIDDEN,true);
        dense(s.h2,s.logits,weights,base+W3,base+B3,Schema.HIDDEN,false);
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
        int base=expert(x)*NETWORK_PARAMETERS;
        Arrays.fill(s.dh1,0); Arrays.fill(s.dh2,0);
        for(int row=0;row<Schema.OUTPUTS;row++) {
            float g=outGradient[row]; gradient[base+B3+row]+=g;
            for(int col=0;col<Schema.HIDDEN;col++) {int k=base+W3+row*Schema.HIDDEN+col; gradient[k]+=g*s.h2[col]; s.dh2[col]+=g*weights[k];}
        }
        for(int row=0;row<Schema.HIDDEN;row++) {
            float g=s.dh2[row]*(1-s.h2[row]*s.h2[row]); gradient[base+B2+row]+=g;
            for(int col=0;col<Schema.HIDDEN;col++) {int k=base+W2+row*Schema.HIDDEN+col; gradient[k]+=g*s.h1[col]; s.dh1[col]+=g*weights[k];}
        }
        for(int row=0;row<Schema.HIDDEN;row++) {
            float g=s.dh1[row]*(1-s.h1[row]*s.h1[row]); gradient[base+B1+row]+=g;
            for(int col=0;col<Schema.INPUTS;col++) gradient[base+W1+row*Schema.INPUTS+col]+=g*x[col];
        }
    }
    /** Feature-major batched dense layers reuse each weight across contiguous lanes. */
    public static final class BatchWorkspace {
        private final int capacity;
        private final float[] input,first,second,output;
        private final int[] experts,inverse,starts=new int[EXPERTS+1];
        private int count;
        public BatchWorkspace(int capacity) {
            if(capacity<1 || capacity>256) throw new IllegalArgumentException("batch capacity");
            this.capacity=capacity; input=new float[Schema.INPUTS*capacity]; first=new float[Schema.HIDDEN*capacity];
            second=new float[Schema.HIDDEN*capacity]; output=new float[Schema.OUTPUTS*capacity];
            experts=new int[capacity];inverse=new int[capacity];
        }
        public void lane(int index,float[] logits) {
            if(index<0||index>=count||logits.length!=Schema.OUTPUTS)throw new IllegalArgumentException("batch lane");
            for(int row=0;row<Schema.OUTPUTS;row++) logits[row]=output[row*capacity+inverse[index]];
        }
    }
    public void forwardBatch(float[][] observations,int count,BatchWorkspace s) {
        if(count<1 || count>s.capacity || observations.length<count) throw new IllegalArgumentException("batch count");
        s.count=count;Arrays.fill(s.starts,0);
        for(int b=0;b<count;b++) {
            Schema.checkObservation(observations[b]);s.experts[b]=expert(observations[b]);s.starts[s.experts[b]+1]++;
        }
        for(int task=0;task<EXPERTS;task++)s.starts[task+1]+=s.starts[task];
        int[] next=s.starts.clone();
        for(int b=0;b<count;b++) {
            int lane=next[s.experts[b]]++;s.inverse[b]=lane;
            for(int col=0;col<Schema.INPUTS;col++)s.input[col*s.capacity+lane]=observations[b][col];
        }
        for(int task=0;task<EXPERTS;task++) {
            int begin=s.starts[task],end=s.starts[task+1],base=task*NETWORK_PARAMETERS;
            if(begin==end)continue;
            batchLayer(s.input,s.first,base+W1,base+B1,Schema.INPUTS,Schema.HIDDEN,begin,end,s.capacity,true);
            batchLayer(s.first,s.second,base+W2,base+B2,Schema.HIDDEN,Schema.HIDDEN,begin,end,s.capacity,true);
            batchLayer(s.second,s.output,base+W3,base+B3,Schema.HIDDEN,Schema.OUTPUTS,begin,end,s.capacity,false);
        }
    }
    private void batchLayer(float[] x,float[] out,int wi,int bi,int width,int rows,int begin,int end,int stride,boolean tanh) {
        for(int row=0;row<rows;row++) {
            int o=row*stride; Arrays.fill(out,o+begin,o+end,weights[bi+row]);
            for(int col=0;col<width;col++) {
                float w=weights[wi+row*width+col]; int c=col*stride;
                for(int b=begin;b<end;b++) out[o+b]+=w*x[c+b];
            }
            if(tanh) for(int b=begin;b<end;b++) out[o+b]=(float)Math.tanh(out[o+b]);
        }
    }
}
