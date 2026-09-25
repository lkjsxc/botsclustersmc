package org.botsclustersmc.core;

/** Remove only effect-free click/slot pairs; never compare an item with a task goal. */
public final class MenuInputs {
    private MenuInputs() {}

    public static void restrict(Pocket pocket,Pocket.External external,boolean[] mask) {
        if(mask.length!=Schema.DISTRIBUTION)throw new IllegalArgumentException("action mask dimension");
        if(pocket.menu()==Pocket.Menu.CLOSED)return;
        int operation=Task.offset(6);
        for(int op=1;op<=3;op++) {
            int slot=Schema.slotOffset(op);
            boolean permitted=mask[operation+op],possible=false;
            for(int i=0;i<Schema.HEADS[7];i++) {
                boolean useful=permitted&&mask[slot+i]&&i<pocket.slots()&&pocket.wouldChange(op,i,external);
                mask[slot+i]=useful;possible|=useful;
            }
            mask[operation+op]=possible;
        }
        mask[operation+4]=false; // Opening an already-open inventory has no effect.
        // Empty branches stay empty. Wait/close have no slot distribution.
    }
}
