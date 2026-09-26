package org.botsclustersmc.holdout;

import java.util.Arrays;
import org.botsclustersmc.core.Task;

/** Read-only decision-boundary samples, not a per-tick mining log or action controller. */
public final class HarvestTrace {
    private int observations,focused,dig,heldPick,pickContact,otherContact,maxPick,maxOther;
    private final int[] interactions=new int[4];
    public static boolean applies(int task) {
        return task==Task.BREAK_LOG.ordinal()||task==Task.COLLECT_LOG.ordinal()
            ||task==Task.MINE_COBBLESTONE.ordinal();
    }
    public void observe(boolean menuFocused,int interaction,int heldKind,boolean targetContact,int ticks) {
        if(interaction<0||interaction>=4||heldKind<0||ticks<0||!targetContact&&ticks!=0)
            throw new IllegalArgumentException("Invalid harvest diagnostic sample");
        observations++;interactions[interaction]++;
        if(menuFocused)focused++;else if(interaction==1)dig++;
        boolean pick=heldKind==6||heldKind==7;if(pick)heldPick++;
        if(targetContact&&ticks>0) {
            if(pick){pickContact++;maxPick=Math.max(maxPick,ticks);}
            else{otherContact++;maxOther=Math.max(maxOther,ticks);}
        }
    }
    public String json() {
        if(observations==0)throw new IllegalStateException("No harvest observations");
        return "{\"scope\":\"decision-boundary-not-every-tick\",\"observations\":"+observations
            +",\"menu_focused_selections\":"+focused+",\"world_dig_selections\":"+dig
            +",\"held_pick_observations\":"+heldPick+",\"target_pick_contact_observations\":"+pickContact
            +",\"target_other_contact_observations\":"+otherContact+",\"max_target_pick_ticks\":"+maxPick
            +",\"max_target_other_ticks\":"+maxOther+",\"interaction_selections\":"+Arrays.toString(interactions)+"}";
    }
}
