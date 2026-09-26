package org.botsclustersmc.training;

import org.botsclustersmc.core.*;
import java.util.*;

/** Measured policy-change guard. Rejected candidates never publish weights or Adam state. */
public final class UpdateGuard {
    private UpdateGuard() {}
    public record Change(double mean,double maximum) {}
    public record Result(Adam.Update update,double learningRate,Change change,int backtracks) {}
    public static List<Transition> observations(List<Trajectory> batch) {
        List<Transition> all=new ArrayList<>();for(Trajectory trajectory:batch)all.addAll(trajectory.steps());
        int n=Math.min(128,all.size());if(all.size()<=n)return all;
        BitSet selected=new BitSet(all.size());boolean[] seen=new boolean[TaskBalance.TASKS+1];
        for(int i=0;i<all.size();i++) {
            int task=TaskBalance.task(all.get(i).observation());
            if(!seen[task]){selected.set(i);seen[task]=true;}
        }
        // Rare tasks must not disappear between evenly spaced guard samples.
        for(int i=0;i<n&&selected.cardinality()<n;i++)selected.set(i*all.size()/n);
        for(int i=0;selected.cardinality()<n;i++)selected.set(i);
        List<Transition> result=new ArrayList<>(n);
        for(int i=selected.nextSetBit(0);i>=0;i=selected.nextSetBit(i+1))result.add(all.get(i));
        return result;
    }
    private static List<double[]> distributions(Policy policy,List<Transition> samples) {
        Policy.Workspace w=new Policy.Workspace();List<double[]> result=new ArrayList<>(samples.size());
        for(Transition s:samples){policy.forward(s.observation(),s.mask(),w);result.add(w.probabilities.clone());}return result;
    }
    private static Change measure(Policy candidate,List<Transition> samples,List<double[]> before) {
        Policy.Workspace w=new Policy.Workspace();double sum=0,maximum=0;
        for(int i=0;i<samples.size();i++) {
            Transition s=samples.get(i);candidate.forward(s.observation(),s.mask(),w);
            double kl=Distribution.divergence(before.get(i),w.probabilities);sum+=kl;maximum=Math.max(maximum,kl);
        }
        return new Change(sum/samples.size(),maximum);
    }
    public static int[] expertSamples(List<Trajectory> batch) {
        int[] counts=new int[Policy.EXPERTS];
        for(Trajectory trajectory:batch)for(Transition transition:trajectory.steps()) {
            int task=Policy.expert(transition.observation());counts[task]=Math.addExact(counts[task],1);
        }
        return counts;
    }
    public static Result update(Policy policy,Adam optimizer,float[] gradient,int count,List<Trajectory> batch) {
        List<Transition> samples=observations(batch);if(samples.isEmpty())throw new IllegalArgumentException("Empty update");
        List<double[]> before=distributions(policy,samples);
        int[] counts=expertSamples(batch);
        if(Arrays.stream(counts).sum()!=count)throw new IllegalArgumentException("expert sample accounting");
        double[] rates=new double[Policy.EXPERTS];double rate=0;
        for(int task=0;task<Policy.EXPERTS;task++) {
            rates[task]=.00015*Math.sqrt(Math.min(1,counts[task]/512.0));rate=Math.max(rate,rates[task]);
        }
        for(int attempt=0;attempt<12;attempt++,rate*=.5) {
            Adam.Update candidate=optimizer.update(policy,gradient,counts,rates);
            Change change=measure(candidate.policy(),samples,before);
            if(Double.isFinite(change.mean())&&change.mean()<=.005&&change.maximum()<=.05)
                return new Result(candidate,rate,change,attempt);
            for(int task=0;task<rates.length;task++)rates[task]*=.5;
        }
        // Explicitly accounted rejection; caller retains the original policy/optimizer.
        return new Result(null,0,new Change(0,0),12);
    }
}
