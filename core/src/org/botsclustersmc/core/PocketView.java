package org.botsclustersmc.core;

import java.util.*;

/** Immutable, bounded operator view; its contents are never selected gameplay inputs. */
public record PocketView(Pocket.Menu menu,Stack cursor,Stack output,List<Stack> cells,long unavailableSlots) {
    public PocketView {
        Objects.requireNonNull(menu);Objects.requireNonNull(cursor);Objects.requireNonNull(output);
        cells=List.copyOf(cells);
        int expected=switch(menu){case CLOSED->0;case INVENTORY->4;case WORKBENCH->9;case FURNACE->3;case CHEST->27;};
        if(cells.size()!=expected||unavailableSlots<0||(unavailableSlots>>>expected)!=0)
            throw new IllegalArgumentException("menu view dimensions/availability");
    }
    public static PocketView capture(Pocket pocket,Pocket.External external) {
        int outputSlot=switch(pocket.menu()){case INVENTORY->40;case WORKBENCH->45;default->-1;};
        int end=outputSlot>=0?outputSlot:pocket.slots();List<Stack> cells=new ArrayList<>();
        long unavailable=0;
        for(int slot=36;slot<end;slot++) {
            if((pocket.menu()==Pocket.Menu.CHEST||pocket.menu()==Pocket.Menu.FURNACE)&&!external.accessible(slot-36))
                unavailable|=1L<<(slot-36);
            cells.add(pocket.get(slot,external));
        }
        return new PocketView(pocket.menu(),pocket.cursor(),outputSlot<0?Stack.EMPTY:pocket.get(outputSlot,external),cells,unavailable);
    }
    public String contents() {
        StringJoiner text=new StringJoiner(", ","[","]");
        for(int i=0;i<cells.size();i++) {Stack item=cells.get(i);text.add((36+i)+"="+((unavailableSlots&(1L<<i))!=0?"unavailable":item.empty()?"empty":item.item()+" x"+item.count()));}
        return text.toString();
    }
    public String summary() {return "menu="+menu+" cursor="+cursor.item()+" x"+cursor.count()+" output="+output.item()+" x"+output.count();}
}
