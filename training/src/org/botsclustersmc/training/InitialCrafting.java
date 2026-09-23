package org.botsclustersmc.training;

import org.botsclustersmc.core.*;

/** Reset-only raw-ingredient assistance. Never called by the gameplay actuator. */
public final class InitialCrafting {
    private InitialCrafting(){}
    static int[][] layout(int task){return switch(task){
        case 8->new int[][]{{0,36}};
        case 9->new int[][]{{0,36},{0,38}};
        case 10->new int[][]{{0,36},{0,37},{0,38},{0,39}};
        case 11,13->new int[][]{{0,36},{0,37},{0,38},{1,40},{1,43}};
        default->new int[0][];
    };}
    public static void prepare(Pocket pocket,int task,double difficulty,RandomSource rng){
        if(difficulty>=1)return;
        for(int[] cell:layout(task))if(rng.unit()>difficulty&&!pocket.storage(cell[0]).empty()){
            pocket.click(1,cell[0],Pocket.NONE);pocket.click(2,cell[1],Pocket.NONE);pocket.click(1,cell[0],Pocket.NONE);
        }
        // The cursor is an initial state too; all units still originate in furnished raw stock.
        if(rng.unit()>difficulty)for(int slot=0;slot<2;slot++)if(!pocket.storage(slot).empty()){pocket.click(1,slot,Pocket.NONE);break;}
    }
    public static double progress(Pocket pocket,int task){
        int[][] layout=layout(task);if(layout.length==0)return 0;int matches=0;
        for(int[] cell:layout){String item=pocket.get(cell[1],Pocket.NONE).item();boolean match=cell[0]==1?item.equals("STICK"):task==8?org.botsclustersmc.core.Stack.kind(item)==2:task==13?item.equals("COBBLESTONE"):org.botsclustersmc.core.Stack.kind(item)==3;if(match)matches++;}
        return matches/(double)layout.length;
    }
}
