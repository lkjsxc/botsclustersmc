package org.botsclustersmc.training;

import java.util.*;
import org.botsclustersmc.core.*;

/** One immutable target policy per learner update; gradients never include an exam. */
public final class Gradient {
    private Gradient(){}
    public record Result(float[] weights,int samples,double valueLoss,double entropy,double importance) {}
    public static Result compute(Policy target,List<Trajectory> trajectories) {
        float[] grad=new float[Policy.PARAMETERS]; Policy.Workspace w=new Policy.Workspace();
        int total=0; double loss=0,entropy=0,importance=0;
        for(Trajectory fragment:trajectories) {
            int n=fragment.steps().size(); double[] reward=new double[n],discount=new double[n],value=new double[n],next=new double[n],ratio=new double[n];
            boolean[] carry=new boolean[n];
            for(int i=0;i<n;i++) {
                Transition s=fragment.steps().get(i);
                target.forward(s.observation(),s.mask(),w); value[i]=w.logits[Schema.LOGITS];
                ratio[i]=Distribution.logProbability(w.probabilities,s.action())-s.behaviorLogProbability();
                target.forward(s.nextObservation(),s.nextMask(),w); next[i]=s.terminal()?0:w.logits[Schema.LOGITS];
                reward[i]=s.reward(); discount[i]=VTrace.discount(s.ticks(),s.terminal()); carry[i]=!s.terminal() && i+1<n;
            }
            VTrace.Returns returns=VTrace.compute(reward,discount,value,next,ratio,carry);
            for(int i=0;i<n;i++) {
                Transition s=fragment.steps().get(i); target.forward(s.observation(),s.mask(),w);
                Distribution.gradient(w.probabilities,s.action(),returns.advantages()[i],0.002,w.dout);
                double error=w.logits[Schema.LOGITS]-returns.values()[i];
                // Huber critic loss limits the effect of an unexpectedly large value target.
                w.dout[Schema.LOGITS]=(float)(.5*Math.max(-1,Math.min(1,error)));
                target.backward(s.observation(),w,w.dout,grad);
                double a=Math.abs(error); loss+=a<=1?.5*a*a:a-.5;
                entropy+=Distribution.entropy(w.probabilities); importance+=Math.exp(Math.min(0,ratio[i])); total++;
            }
        }
        return new Result(grad,total,loss,entropy,importance);
    }
}
