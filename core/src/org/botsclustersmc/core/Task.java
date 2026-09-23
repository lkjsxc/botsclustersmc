package org.botsclustersmc.core;

import java.util.Arrays;

/** Task identity is observable. Masks restrict lesson-wide controls, never the answer. */
public enum Task {
    FORWARD_STOP, TURN_STOP, AIM_HOLD, NAVIGATE_STOP, STEP_OVER,
    BREAK_LOG, COLLECT_LOG, PLACE_BLOCK, CRAFT_PLANKS, CRAFT_STICKS,
    CRAFT_WORKBENCH, CRAFT_WOOD_PICK, MINE_COBBLESTONE, CRAFT_STONE_PICK,
    SMELT_IRON, SUPPLY_CHEST, BUILD_PLATFORM, LOG_TO_WORKBENCH;
    public static Task at(int index){if(index<0||index>=values().length)throw new IllegalArgumentException("task must be 0..17");return values()[index];}
    public int horizon(){return ordinal()<5?600:ordinal()<8?1200:3000;}
    public String label(){return name().toLowerCase(java.util.Locale.ROOT).replace('_','-');}
    public boolean[] mask(int availableSlots,boolean menuOpen) {
        boolean[] mask=Schema.unrestrictedMask();int id=ordinal();
        if(id==0){only(mask,0,0,1,2);only(mask,1,2);only(mask,2,2);}
        if(id==1){only(mask,0,0,1);only(mask,2,2);}
        if(id==2)only(mask,0,0);
        if(id==3)only(mask,2,2);
        if(id<4)only(mask,3,0);
        if(id<5){only(mask,4,0);only(mask,5,0);only(mask,6,0);}
        else if(id<8){only(mask,6,0);only(mask,4,0,id==7?2:1);}
        if(!menuOpen){only(mask,6,id<8?new int[]{0}:new int[]{0,4});only(mask,7,0);}
        else{int off=offset(7);for(int i=Math.max(1,availableSlots);i<Schema.HEADS[7];i++)mask[off+i]=false;}
        return mask;
    }
    public static int offset(int head){int n=0;for(int i=0;i<head;i++)n+=Schema.HEADS[i];return n;}
    public static void only(boolean[] mask,int head,int... choices){int off=offset(head);Arrays.fill(mask,off,off+Schema.HEADS[head],false);for(int choice:choices)mask[off+choice]=true;}
}
