package org.botsclustersmc.commonslearning;

import org.botsclustersmc.core.RandomSource;

/** Independent keyed randomness: behavior choices cannot change ownership or application order. */
public final class SupplyScenario {
    private SupplyScenario(){}
    private static final long MIX=0x9e3779b97f4a7c15L;
    public static long key(long seed,long episode,int step,int member){
        return seed^episode*MIX^(long)step*0xbf58476d1ce4e5b9L^(long)member*0x94d049bb133111ebL;
    }
    public static SupplyRoom room(long seed,int episode,int members){
        RandomSource r=new RandomSource(key(seed,episode,-1,0));int[] codes=new int[members];
        for(int i=0;i<members;i++)codes[i]=(i<members/2?0:2)+r.nextInt(2);
        for(int i=members-1;i>0;i--){int j=r.nextInt(i+1),v=codes[i];codes[i]=codes[j];codes[j]=v;}
        return new SupplyRoom(episode%2,codes);
    }
    public static int[] order(long seed,int episode,int step,int members){
        RandomSource r=new RandomSource(key(seed,episode,step,-1));int[] result=new int[members];
        for(int i=0;i<members;i++)result[i]=i;
        for(int i=members-1;i>0;i--){int j=r.nextInt(i+1),v=result[i];result[i]=result[j];result[j]=v;}
        return result;
    }
}
