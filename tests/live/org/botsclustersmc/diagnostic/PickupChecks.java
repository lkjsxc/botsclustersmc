package org.botsclustersmc.diagnostic;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.botsclustersmc.core.*;
import org.botsclustersmc.core.Stack;
import org.botsclustersmc.plugin.*;
import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;

/** Test-only event mutations. Never shipped, used as teacher actions or used for learning. */
final class PickupChecks implements Listener {
    private static final Map<UUID,Consumer<EntityPickupItemEvent>> callbacks=new ConcurrentHashMap<>();
    private static int checks;
    private static final Set<Long> denied=ConcurrentHashMap.newKeySet();
    static boolean denied(Npc npc){return denied.contains(npc.id);}
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static void install(RuntimePlugin plugin){Bukkit.getPluginManager().registerEvents(new PickupChecks(),plugin);}
    @EventHandler public void pickup(EntityPickupItemEvent event){
        Consumer<EntityPickupItemEvent> callback=callbacks.get(event.getItem().getUniqueId());
        if(callback!=null)callback.accept(event);
    }
    private static void trial(Npc npc,String mode){
        npc.pocket.clear();npc.collected.clear();npc.tick=4;
        ItemStack source=new ItemStack(Material.OAK_LOG,8);
        if(mode.equals("unsupported"))source.editMeta(meta->meta.displayName(Component.text("Leave me here")));
        Item item=npc.entity.getWorld().dropItem(npc.entity.getLocation().add(0,.2,0),source);
        item.setVelocity(new org.bukkit.util.Vector());item.setGravity(false);item.setPickupDelay(0);
        item.getPersistentDataContainer().set(npc.plugin.provenance,PersistentDataType.STRING,npc.token());
        int[] events={0};ItemStack[] after={source.clone()};
        callbacks.put(item.getUniqueId(),event->{
            events[0]++;
            switch(mode){
                case "cancel"->event.setCancelled(true);
                case "remove"->item.remove();
                case "replace"->item.setItemStack(new ItemStack(Material.DIAMOND));
                case "count"->item.setItemStack(new ItemStack(Material.OAK_LOG,2));
                case "metadata"->{ItemStack named=item.getItemStack().clone();named.editMeta(meta->meta.displayName(Component.text("Changed in event")));item.setItemStack(named);}
                case "provenance"->item.getPersistentDataContainer().set(npc.plugin.provenance,PersistentDataType.STRING,"different-owner");
                case "delay"->item.setPickupDelay(20);
                case "permission"->denied.add(npc.id);
                default->{}
            }
            if(!item.isDead())after[0]=item.getItemStack().clone();
        });
        try{
            WorldActions.tick(npc,Schema.IDLE.clone(),true);
            check(events[0]==(mode.equals("unsupported")?0:1),"unexpected pickup event count: "+mode);
            if(mode.equals("normal")){
                check(npc.pocket.count("OAK_LOG")==8&&npc.collected.getOrDefault("OAK_LOG",0L)==8,"plain pickup failed");
                check(item.isDead(),"picked-up item remained in world");
            }else{
                check(npc.pocket.count("OAK_LOG")==0&&npc.pocket.count("DIAMOND")==0&&npc.collected.isEmpty(),"stale event snapshot inserted: "+mode);
                if(!mode.equals("remove"))check(!item.isDead()&&item.getItemStack().equals(after[0]),"callback result overwritten: "+mode);
            }
        }finally{denied.remove(npc.id);callbacks.remove(item.getUniqueId());if(!item.isDead())item.remove();}
    }
    static void verify(Npc npc){
        if(npc.goal.task().ordinal()!=15)return;
        check(npc.pocket.menu()==Pocket.Menu.CLOSED&&npc.pocket.cursor().empty(),"expected fresh pocket");
        check(npc.pocket.crafted.isEmpty()&&npc.pocket.extracted.isEmpty(),"expected no crafting counters");
        Stack[] storage=new Stack[36];for(int i=0;i<36;i++)storage[i]=npc.pocket.storage(i);
        int selected=npc.pocket.selected();long tick=npc.tick;
        var collected=new HashMap<>(npc.collected);var velocity=npc.entity.getVelocity().clone();
        ItemStack held=npc.entity.getEquipment().getItemInMainHand().clone();
        try{
            for(String mode:List.of("normal","cancel","remove","replace","count","metadata","provenance","delay","permission","unsupported"))trial(npc,mode);
            System.out.println("PICKUP EVENT LIVE PASS checks="+checks+" event_modes=10");
        }finally{
            npc.pocket.clear();for(int i=0;i<36;i++)npc.pocket.setStorage(i,storage[i]);npc.pocket.select(selected);
            npc.collected.clear();npc.collected.putAll(collected);npc.tick=tick;
            npc.entity.setVelocity(velocity);npc.entity.getEquipment().setItemInMainHand(held);
        }
    }
}
