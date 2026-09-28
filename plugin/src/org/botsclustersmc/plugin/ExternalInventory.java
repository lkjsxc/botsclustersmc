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
    /** A type/count-only pocket must never erase components or rewrite stack limits. */
    public static boolean supported(ItemStack item){
        if(item==null||item.getType().isAir())return true;
        if(!item.getType().isItem()||item.getAmount()<1||item.getAmount()>64)return false;
        Stack stack=new Stack(item.getType().name(),item.getAmount());
        return item.getMaxStackSize()==stack.maximum()&&stack.count()<=stack.maximum()
            &&item.isSimilar(new ItemStack(item.getType(),item.getAmount()));
    }
    public static Stack from(ItemStack item){
        if(!supported(item))throw new IllegalArgumentException("Item cannot be represented without data loss");
        return item==null||item.getType().isAir()?Stack.EMPTY:new Stack(item.getType().name(),item.getAmount());
    }
    public static ItemStack to(Stack stack){
        if(stack.empty())return null;
        Material material=Material.valueOf(stack.item());
        if(!material.isItem())throw new IllegalArgumentException("Not an item material");
        ItemStack item=new ItemStack(material,stack.count());
        if(item.getMaxStackSize()!=stack.maximum()||stack.count()>stack.maximum())
            throw new IllegalArgumentException("Pocket/server stack limits differ");
        return item;
    }
    @Override public int size(){return furnace?3:Math.min(27,inventory.getSize());}
    @Override public boolean accessible(int slot){
        if(slot<0||slot>=size())return false;
        ItemStack item=inventory.getItem(slot);
        return supported(item)&&(item==null||item.getType().isAir()||item.getAmount()<=inventory.getMaxStackSize());
    }
    @Override public Stack get(int slot){return accessible(slot)?from(inventory.getItem(slot)):Stack.EMPTY;}
    @Override public void set(int slot,Stack stack){
        if(!accessible(slot))throw new IllegalArgumentException("Unavailable external slot");
        if(stack.count()>inventory.getMaxStackSize())throw new IllegalArgumentException("External stack limit");
        inventory.setItem(slot,to(stack));
    }
    @Override public boolean accepts(int slot,Stack stack){
        if(!accessible(slot))return false;
        if(!stack.empty()) {
            Material material=Material.getMaterial(stack.item());
            if(material==null||!material.isItem()||material.getMaxStackSize()!=stack.maximum()
                    ||stack.count()>stack.maximum()||stack.maximum()>inventory.getMaxStackSize())return false;
        }
        if(!furnace)return true;if(slot==2)return false;
        if(slot==1)return stack.empty()||Material.valueOf(stack.item()).isFuel();
        return true;
    }
    public static Pocket.External locate(Npc npc){
        Location at=npc.container;
        if(at==null||!WorldActions.owned(at))return Pocket.NONE;
        Location body=npc.entity.getLocation();
        if(body.getWorld()!=at.getWorld()||body.distanceSquared(at)>36||!npc.plugin.canChange(npc,at.getBlock()))return Pocket.NONE;
        BlockState state=at.getBlock().getState();
        if(npc.pocket.menu()==Pocket.Menu.FURNACE&&state instanceof Furnace f)return new ExternalInventory(f.getInventory(),true);
        if(npc.pocket.menu()==Pocket.Menu.CHEST&&state instanceof Chest c)return new ExternalInventory(c.getBlockInventory(),false);
        return Pocket.NONE;
    }
}
