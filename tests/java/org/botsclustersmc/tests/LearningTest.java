package org.botsclustersmc.tests;
import org.botsclustersmc.core.*;
import org.botsclustersmc.training.*;
import java.util.*;
/** A synthetic conditional bandit, explicitly NOT Minecraft skill evidence. */
public final class LearningTest {
    public static void main(String[] args){
        boolean[] mask=new boolean[Schema.DISTRIBUTION];int offset=0;for(int i=0;i<Schema.HEADS.length;i++){mask[offset+Schema.IDLE[i]]=true;offset+=Schema.HEADS[i];}mask[1]=true;
        for(int seed=1;seed<=5;seed++){
            Policy p=Policy.initialize(seed);Adam adam=new Adam();RandomSource random=new RandomSource(seed+777);Policy.Workspace w=new Policy.Workspace();
            for(int update=0;update<400;update++){
                List<Trajectory> batch=new ArrayList<>();
                for(int i=0;i<64;i++){float[] x=new float[Schema.INPUTS];x[0]=1;x[1]=(i%2==0?1:-1);p.forward(x,mask,w);Distribution.Choice action=Distribution.choose(w.probabilities,random,false);float reward=action.actions()[0]==(x[1]>0?1:0)?1:-1;Transition t=new Transition(x,mask,action.actions(),action.logProbability(),p.updates(),reward,4,x,mask,true);batch.add(new Trajectory(i,update,update,List.of(t)));}
                Gradient.Result gradient=Gradient.compute(p,batch);Adam.Update result=adam.update(p,gradient.weights(),UpdateGuard.expertSamples(batch),.0003);p=result.policy();adam=result.optimizer();
            }
            double probability=0;for(int bit:new int[]{-1,1}){float[] x=new float[Schema.INPUTS];x[0]=1;x[1]=bit;p.forward(x,mask,w);probability+=w.probabilities[bit>0?1:0]/2;}
            if(probability<.95)throw new AssertionError("bandit seed="+seed+" probability="+probability);
            System.out.printf(Locale.ROOT,"PASS synthetic bandit seed=%d correct_probability=%.6f samples=%d updates=%d%n",seed,probability,p.samples(),p.updates());
        }
    }
}
