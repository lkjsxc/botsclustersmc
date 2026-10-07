package org.botsclustersmc.commonslearning;

import java.util.*;
import org.botsclustersmc.core.*;
import org.botsclustersmc.training.*;

/** Fixed-policy joint rollouts; synchronous sampling is deliberately not an asynchronous MARL claim. */
final class SupplyEpisode {
    record Result(List<Trajectory> trajectories,SupplyRoom.Outcome outcome,int[] actions,int[] orders) {}
    static Result play(Policy policy,boolean visible,long seed,int episode,int population,boolean retain){
        SupplyRoom room=SupplyScenario.room(seed,episode,population);Policy.Workspace workspace=new Policy.Workspace();
        List<List<Transition>> paths=new ArrayList<>();for(int i=0;i<population;i++)paths.add(new ArrayList<>());
        int[] actions=new int[population*4],orders=new int[actions.length];
        for(int step=0;step<4;step++){
            float[][] before=new float[population][];boolean[][] masks=new boolean[population][];
            int[][] selected=new int[population][];double[] logs=new double[population];
            for(int i=0;i<population;i++){
                SupplyRoom.View view=room.view(i,visible);before[i]=view.observation();masks[i]=view.mask();
                policy.forward(before[i],masks[i],workspace);
                var choice=Distribution.choose(workspace.probabilities,new RandomSource(SupplyScenario.key(seed^0x519e735L,episode,step,i)),false);
                selected[i]=choice.actions();logs[i]=choice.logProbability();actions[step*population+i]=SupplyRoom.code(selected[i]);
            }
            int[] order=SupplyScenario.order(seed,episode,step,population);System.arraycopy(order,0,orders,step*population,population);
            float reward=room.advance(selected,order);
            if(retain)for(int i=0;i<population;i++){
                var next=room.view(i,visible);
                paths.get(i).add(new Transition(before[i],masks[i],selected[i],logs[i],policy.updates(),reward,4,next.observation(),next.mask(),step==3));
            }
        }
        List<Trajectory> result=new ArrayList<>();if(retain)for(int i=0;i<population;i++)result.add(new Trajectory(i,episode,episode,paths.get(i)));
        return new Result(List.copyOf(result),room.snapshot(),actions,orders);
    }
}
