package org.botsclustersmc.plugin;

import java.util.Objects;
import org.botsclustersmc.core.Pocket;
import org.botsclustersmc.core.Stack;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** One owner-thread drop, committed only after both synchronous event boundaries. */
final class DroppedItems {
    private DroppedItems(){}
    private static final int PENDING_DELAY=32767;
    private static boolean ready(Npc n){
        return Bukkit.isOwnedByCurrentRegion(n.entity)&&n.entity.isValid()&&!n.entity.isDead()
            &&!n.remove.get()&&!n.resetting&&!n.paused&&!n.plugin.paused.get()
            &&n.plugin.failed.get()==null&&n.pocket.menu()==Pocket.Menu.CLOSED;
    }
    private record Source(Stack held,int selected,Goal goal,String token,Location at){
        boolean unchanged(Npc n){
            return ready(n)&&n.goal==goal&&n.pocket.selected()==selected&&n.pocket.held().equals(held)
                &&token.equals(n.token())&&at.equals(n.entity.getLocation());
        }
    }
    private static boolean unchanged(Npc n,Item item,Source source,ItemStack expected){
        return Bukkit.isOwnedByCurrentRegion(item)&&item.isValid()&&!item.isDead()
            &&item.getWorld()==source.at().getWorld()&&item.getLocation().distanceSquared(source.at())<1e-12
            &&item.getItemStack().equals(expected)&&item.getPickupDelay()==PENDING_DELAY&&!item.canMobPickup()
            &&item.getOwner()==null&&item.getThrower()==null
            &&Objects.equals(source.token(),item.getPersistentDataContainer().get(n.plugin.provenance,PersistentDataType.STRING));
    }
    static void drop(Npc n){
        if(n.dropping||!ready(n)||n.pocket.held().empty())return;
        Source source=new Source(n.pocket.held(),n.pocket.selected(),n.goal,n.token(),n.entity.getLocation());
        ItemStack expected=ExternalInventory.to(source.held().withCount(1));
        Item[] created={null};boolean committed=false;n.dropping=true;
        try{
            Item item=source.at().getWorld().dropItem(source.at(),expected,candidate->{
                created[0]=candidate;
                // Initialize before ItemSpawnEvent. No native or NPC pickup before debit.
                candidate.setPickupDelay(PENDING_DELAY);candidate.setCanMobPickup(false);
                candidate.getPersistentDataContainer().set(n.plugin.provenance,PersistentDataType.STRING,source.token());
            });
            if(!source.unchanged(n)||!unchanged(n,item,source,expected))return;
            EntityDropItemEvent event=new EntityDropItemEvent(n.entity,item);
            Bukkit.getPluginManager().callEvent(event);
            if(event.isCancelled()||!source.unchanged(n)||!unchanged(n,item,source,expected))return;
            n.pocket.consumeHeld(1);committed=true;
            item.setCanMobPickup(true);item.setPickupDelay(20);
        }finally{
            try{if(!committed&&created[0]!=null)discard(n,created[0]);}
            finally{n.dropping=false;}
        }
    }
    private static void discard(Npc n,Item item){
        // A listener may have moved the provisional entity to another Folia region.
        // Never access that region's mutable entity state from the caller's thread.
        if(Bukkit.isOwnedByCurrentRegion(item))item.remove();
        else item.getScheduler().run(n.plugin,task->item.remove(),()->{});
    }
}
