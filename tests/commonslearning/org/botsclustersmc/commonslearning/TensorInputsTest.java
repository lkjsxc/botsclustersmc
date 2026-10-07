package org.botsclustersmc.commonslearning;

import java.util.*;
import org.botsclustersmc.core.*;

public final class TensorInputsTest {
    private static int checks;
    private static void check(boolean test,String why){checks++;if(!test)throw new AssertionError(why);}
    private static void rejected(Runnable test){try{test.run();throw new AssertionError("invalid tensor input accepted");}catch(IllegalArgumentException expected){checks++;}}
    public static void main(String[] args){
        for(int seed=0;seed<64;seed++){
            float[] x=new float[Schema.INPUTS];RandomSource r=new RandomSource(seed);
            for(int i=0;i<x.length;i++)if(i<220||i>=436)x[i]=r.symmetric(2);
            float[] saved=x.clone(),y=TensorInputs.bind(x);
            check(Arrays.equals(x,saved),"binding mutated source");
            for(int i=0;i<512;i++)if(i<220||i>=436)check(y[i]==x[i],"unrelated observation changed");
            for(int i=0;i<108;i++)for(int j=0;j<2;j++)check(y[220+i*2+j]==x[50+i]*x[206+j],"missing or selected Cartesian pair");
            y[0]=99;check(x[0]!=99,"output alias");
        }
        for(int seed=0;seed<64;seed++){
            SupplyRoom room=SupplyScenario.room(seed,seed%8,8);
            for(int i=0;i<8;i++){
                var known=room.view(i,true);var hidden=room.view(i,false);float[] h=hidden.observation();
                check(h[205]==0&&h[206]==0&&h[207]==0,"hidden demand leaked");
                check(known.observation()[205]==1&&known.observation()[206+room.demand()]==1,"visible demand missing");
                float[] bound=TensorInputs.bind(h);for(int j=220;j<436;j++)check(bound[j]==0,"unknown demand became a product");
                boolean[] mask=known.mask();TensorInputs.bind(known.observation());
                check(Arrays.equals(mask,room.view(i,true).mask()),"binding changed mechanical action support");
            }
        }
        float[] x=new float[512];x[50]=1;x[207]=1;
        check(TensorInputs.bind(x)[221]==1,"unequal material pair was removed");
        x[220]=1;rejected(()->TensorInputs.bind(x));x[220]=0;x[50]=Float.MAX_VALUE;x[207]=2;rejected(()->TensorInputs.bind(x));
        rejected(()->TensorInputs.bind(new float[511]));x[50]=Float.NaN;rejected(()->TensorInputs.bind(x));
        // Plain rollout remains byte-for-byte the original path; treatment changes no stock/mask code.
        var p=Policy.initialize(57);var old=SupplyEpisode.play(p,true,12,0,2,true);
        var plain=SupplyEpisode.play(p,true,12,0,2,true,false);
        check(Arrays.equals(old.actions(),plain.actions())&&Arrays.equals(old.orders(),plain.orders()),"plain rollout changed");
        for(int i=0;i<2;i++)for(int step=0;step<4;step++){
            var a=old.trajectories().get(i).steps().get(step);var b=plain.trajectories().get(i).steps().get(step);
            check(Arrays.equals(a.observation(),b.observation())&&a.reward()==b.reward()&&a.behaviorLogProbability()==b.behaviorLogProbability(),"plain transition changed");
        }
        System.out.println("PASS full tensor binding checks="+checks+"; no labels, mask changes, Minecraft or policy export");
    }
}
