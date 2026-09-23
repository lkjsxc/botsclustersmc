package org.botsclustersmc.core;

import java.util.Arrays;

/** Remove only effect-free menu controls; never compare an item with a task goal. */
public final class MenuInputs {
    private MenuInputs() {}

    public static void restrict(Pocket pocket,Pocket.External external,boolean[] mask) {
        if(mask.length!=Schema.LOGITS)throw new IllegalArgumentException("action mask dimension");
        if(pocket.menu()==Pocket.Menu.CLOSED)return;
        int operation=Task.offset(6),slot=Task.offset(7);
        boolean[] useful=new boolean[Schema.HEADS[7]];
        boolean any=false;
        for(int op=1;op<=3;op++) {
            boolean possible=false;
            if(mask[operation+op])for(int i=0;i<pocket.slots();i++) {
                if(mask[slot+i]&&pocket.wouldChange(op,i,external)) {
                    useful[i]=true;possible=true;any=true;
                }
            }
            mask[operation+op]=possible;
        }
        mask[operation+4]=false; // Opening an already-open inventory has no effect.
        // The slot head is shared across click operations. Its mask is their UNION,
        // not a recipe answer or an operation-conditioned action distribution.
        Arrays.fill(mask,slot,slot+Schema.HEADS[7],false);
        if(any)System.arraycopy(useful,0,mask,slot,useful.length);
        else mask[slot]=true; // Canonical dummy slot for the inactive conditional head.
    }
}
