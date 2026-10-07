package org.botsclustersmc.commons;

import java.util.Arrays;

/** Detached room evidence. Defensive copies protect both construction and every array accessor. */
public record CommonsResult(int room,int ticks,String end,CommonsStock carried,CommonsStock bank,CommonsStock dropped,
        long craftedSticks,long craftedPicks,CommonsStock lost,int[] memberCarried,long[] actorTicks,
        long[] decisions,long[] guiSelections,long[] chestObservations) {
    public CommonsResult {
        if(memberCarried.length!=6||actorTicks.length!=2||decisions.length!=2||guiSelections.length!=12||chestObservations.length!=2)
            throw new IllegalArgumentException("Room evidence shape");
        memberCarried=memberCarried.clone();actorTicks=actorTicks.clone();decisions=decisions.clone();
        guiSelections=guiSelections.clone();chestObservations=chestObservations.clone();
    }
    @Override public int[] memberCarried(){return memberCarried.clone();}
    @Override public long[] actorTicks(){return actorTicks.clone();}
    @Override public long[] decisions(){return decisions.clone();}
    @Override public long[] guiSelections(){return guiSelections.clone();}
    @Override public long[] chestObservations(){return chestObservations.clone();}
    public CommonsStock total(){return carried.plus(bank).plus(dropped);}
    public String json() {
        return "{\"room\":"+room+",\"ticks\":"+ticks+",\"end\":\""+end+"\",\"carried\":"+carried.json()
            +",\"bank\":"+bank.json()+",\"dropped\":"+dropped.json()+",\"crafted_sticks\":"+craftedSticks+",\"crafted_picks\":"+craftedPicks
            +",\"lost_stock\":"+lost.json()+",\"member_carried\":"+Arrays.toString(memberCarried)
            +",\"actor_ticks\":"+Arrays.toString(actorTicks)+",\"decisions\":"+Arrays.toString(decisions)
            +",\"gui_selections\":"+Arrays.toString(guiSelections)+",\"chest_observations\":"+Arrays.toString(chestObservations)+"}";
    }
}
