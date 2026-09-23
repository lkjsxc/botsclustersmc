package org.botsclustersmc.training;

import org.botsclustersmc.core.Policy;

/** Transactional Adam: invalid gradients never mutate the original optimizer/model. */
public final class Adam {
    private final float[] first,second;
    private final long step;
    public Adam(){this(new float[Policy.PARAMETERS],new float[Policy.PARAMETERS],0);}
    public Adam(float[] first,float[] second,long step) {
        if(first.length!=Policy.PARAMETERS || second.length!=Policy.PARAMETERS || step<0) throw new IllegalArgumentException("Adam shape");
        for(int i=0;i<first.length;i++) if(!Float.isFinite(first[i]) || !Float.isFinite(second[i]) || second[i]<0)
            throw new IllegalArgumentException("Adam state");
        this.first=first.clone(); this.second=second.clone(); this.step=step;
    }
    public float[] first(){return first.clone();} public float[] second(){return second.clone();} public long step(){return step;}
    public record Update(Policy policy,Adam optimizer,double gradientNorm) {}
    public Update update(Policy old,float[] gradient,int samples,double learningRate) {
        if(samples<1 || gradient.length!=Policy.PARAMETERS || !Double.isFinite(learningRate) || learningRate<=0 || learningRate>0.1)
            throw new IllegalArgumentException("Adam update arguments");
        double norm=0;
        for(float g:gradient){if(!Float.isFinite(g))throw new IllegalArgumentException("gradient is not finite");double a=g/(double)samples;norm+=a*a;}
        norm=Math.sqrt(norm); if(!Double.isFinite(norm))throw new IllegalArgumentException("gradient overflow");
        double scale=Math.min(1,0.5/Math.max(norm,1e-30))/samples;
        float[] w=old.copyWeights(),m=first.clone(),v=second.clone(); long next=Math.addExact(step,1);
        double b1=1-Math.pow(.9,next),b2=1-Math.pow(.999,next);
        for(int i=0;i<w.length;i++) {
            double g=gradient[i]*scale;
            m[i]=(float)(.9*m[i]+.1*g);v[i]=(float)(.999*v[i]+.001*g*g);
            w[i]-=(float)(learningRate*(m[i]/b1)/(Math.sqrt(v[i]/b2)+1e-8));
        }
        return new Update(new Policy(w,Math.addExact(old.updates(),1),Math.addExact(old.samples(),samples)),new Adam(m,v,next),norm);
    }
}
