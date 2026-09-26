package org.botsclustersmc.training;

import org.botsclustersmc.core.Policy;
import java.util.Arrays;

/** Transactional Adam: invalid gradients never mutate the original optimizer/model. */
public final class Adam {
    private final float[] first,second;
    private final long step;
    private final long[] expertSteps;
    public Adam(){this(new float[Policy.PARAMETERS],new float[Policy.PARAMETERS],0);}
    public Adam(float[] first,float[] second,long step) {this(first,second,step,commonSteps(step));}
    private static long[] commonSteps(long step) {
        long[] result=new long[Policy.EXPERTS];Arrays.fill(result,step);return result;
    }
    public Adam(float[] first,float[] second,long step,long[] expertSteps) {
        if(expertSteps.length!=Policy.EXPERTS)throw new IllegalArgumentException("Adam expert clocks");
        for(long clock:expertSteps)if(clock<0||clock>step)throw new IllegalArgumentException("Adam expert clock range");
        if(first.length!=Policy.PARAMETERS || second.length!=Policy.PARAMETERS || step<0) throw new IllegalArgumentException("Adam shape");
        for(int i=0;i<first.length;i++) if(!Float.isFinite(first[i]) || !Float.isFinite(second[i]) || second[i]<0)
            throw new IllegalArgumentException("Adam state");
        this.first=first.clone(); this.second=second.clone(); this.step=step;this.expertSteps=expertSteps.clone();
    }
    public float[] first(){return first.clone();} public float[] second(){return second.clone();} public long step(){return step;}
    public long[] expertSteps(){return expertSteps.clone();}
    /** gradientNorm is the maximum pre-clipping expert mean-gradient norm. */
    public record Update(Policy policy,Adam optimizer,double gradientNorm) {}
    public Update update(Policy old,float[] gradient,int[] counts,double learningRate) {
        double[] rates=new double[Policy.EXPERTS];Arrays.fill(rates,learningRate);
        return update(old,gradient,counts,rates);
    }
    /** Only explicitly sampled experts may change, including momentum and bias correction. */
    public Update update(Policy old,float[] gradient,int[] counts,double[] rates) {
        if(old.updates()!=step||gradient.length!=Policy.PARAMETERS||counts.length!=Policy.EXPERTS||rates.length!=Policy.EXPERTS)
            throw new IllegalArgumentException("Adam update dimensions/identity");
        int samples=0;double maximumNorm=0;double[] scales=new double[Policy.EXPERTS];
        for(int task=0;task<Policy.EXPERTS;task++) {
            int count=counts[task];double rate=rates[task];
            if(count<0||!Double.isFinite(rate)||rate<0||rate>0.1||count>0&&rate==0)
                throw new IllegalArgumentException("Adam expert update arguments");
            samples=Math.addExact(samples,count);
            double norm=0;int start=task*Policy.NETWORK_PARAMETERS,end=start+Policy.NETWORK_PARAMETERS;
            for(int i=start;i<end;i++) {
                float g=gradient[i];if(!Float.isFinite(g))throw new IllegalArgumentException("gradient is not finite");
                if(count==0){if(g!=0)throw new IllegalArgumentException("gradient for absent expert");}
                else{double a=g/(double)count;norm+=a*a;}
            }
            if(count==0)continue;
            norm=Math.sqrt(norm);if(!Double.isFinite(norm))throw new IllegalArgumentException("gradient overflow");
            maximumNorm=Math.max(maximumNorm,norm);
            scales[task]=Math.min(1,0.5/Math.max(norm,1e-30))/count;
        }
        if(samples==0)throw new IllegalArgumentException("empty Adam update");
        float[] w=old.copyWeights(),m=first.clone(),v=second.clone();
        long next=Math.addExact(step,1);long[] clocks=expertSteps.clone();
        for(int task=0;task<Policy.EXPERTS;task++) {
            if(counts[task]==0)continue;
            clocks[task]=Math.addExact(clocks[task],1);
            double b1=1-Math.pow(.9,clocks[task]),b2=1-Math.pow(.999,clocks[task]);
            int start=task*Policy.NETWORK_PARAMETERS,end=start+Policy.NETWORK_PARAMETERS;
            for(int i=start;i<end;i++) {
                double g=gradient[i]*scales[task];
                m[i]=(float)(.9*m[i]+.1*g);v[i]=(float)(.999*v[i]+.001*g*g);
                w[i]-=(float)(rates[task]*(m[i]/b1)/(Math.sqrt(v[i]/b2)+1e-8));
            }
        }
        return new Update(new Policy(w,next,Math.addExact(old.samples(),samples)),new Adam(m,v,next,clocks),maximumNorm);
    }
}
