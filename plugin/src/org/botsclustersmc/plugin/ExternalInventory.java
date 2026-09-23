package org.botsclustersmc.plugin;

import org.botsclustersmc.core.Pocket;
import org.botsclustersmc.core.Stack;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.inventory.*;

/** Accessed only by the NPC's owning entity scheduler after a region ownership check. */
public final class ExternalInventory implements Pocket.External {
    private final Inventory inventory;private final boolean furnace;
    public ExternalInventory(Inventory inventory,boolean furnace){this.inventory=inventory;this.furnace=furnace;}
    public static Stack from(ItemStack s){return s==null||s.getType().isAir()?Stack.EMPTY:new Stack(s.getType().name(),s.getAmount());}
    public static ItemStack to(Stack s){return s.empty()?null:new ItemStack(Material.valueOf(s.item()),s.count());}
    @Override public int size(){return furnace?3:Math.min(27,inventory.getSize());}
    @Override public Stack get(int slot){return from(inventory.getItem(slot));}
    @Override public void set(int slot,Stack stack){inventory.setItem(slot,to(stack));}
    @Override public boolean accepts(int slot,Stack stack){
        if(!furnace)return true;if(slot==2)return false;
        if(slot==1)return stack.empty()||Material.valueOf(stack.item()).isFuel();
        return true;
    }
    public static Pocket.External locate(Npc npc){
        Location at=npc.container;
        if(at==null||!WorldActions.owned(at)||npc.entity.getLocation().distanceSquared(at)>36)return Pocket.NONE;
        BlockState state=at.getBlock().getState();
        if(npc.pocket.menu()==Pocket.Menu.FURNACE&&state instanceof Furnace f)return new ExternalInventory(f.getInventory(),true);
        if(npc.pocket.menu()==Pocket.Menu.CHEST&&state instanceof Chest c)return new ExternalInventory(c.getBlockInventory(),false);
        return Pocket.NONE;
    }
}
