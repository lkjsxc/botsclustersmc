package org.botsclustersmc.diagnostic;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.botsclustersmc.core.*;
import org.botsclustersmc.core.Stack;
import org.botsclustersmc.plugin.*;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Orientable;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.*;

/** Adversarial callbacks and peer occupancy in disposable rooms, not learned behavior. */
final class WorldMutationChecks implements Listener {
    private static final Map<UUID,Consumer<EntityChangeBlockEvent>> callbacks=new ConcurrentHashMap<>();
    private static final Set<Long> denied=ConcurrentHashMap.newKeySet();
    private static int checks;
    static boolean denied(Npc n){return denied.contains(n.id);}
    static void install(RuntimePlugin p){Bukkit.getPluginManager().registerEvents(new WorldMutationChecks(),p);}
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    @EventHandler public void change(EntityChangeBlockEvent event){
        var callback=callbacks.get(event.getEntity().getUniqueId());if(callback!=null)callback.accept(event);
    }
    private static void aim(Npc n,Location target){
        Location eye=n.entity.getEyeLocation();double dx=target.getX()-eye.getX(),dy=target.getY()-eye.getY(),dz=target.getZ()-eye.getZ();
        n.entity.setRotation((float)Math.toDegrees(Math.atan2(-dx,dz)),(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz))));
    }
    private static Set<UUID> items(Npc n){
        Set<UUID> ids=new HashSet<>();for(Entity e:n.entity.getNearbyEntities(5,5,5))if(e instanceof Item)ids.add(e.getUniqueId());return ids;
    }
    private static void removeNewItems(Npc n,Set<UUID> before){
        for(Entity e:n.entity.getNearbyEntities(5,5,5))if(e instanceof Item&&!before.contains(e.getUniqueId()))e.remove();
    }
    private static void trial(Npc n,Block support,boolean placing,String mode){
        n.pocket.clear();n.broken.clear();n.placed.clear();n.collected.clear();n.mining=null;n.miningTicks=0;n.tick=1;
        n.pocket.setStorage(0,new Stack(placing?"OAK_PLANKS":"WOODEN_PICKAXE",placing?3:1));
        n.pocket.setStorage(1,n.pocket.storage(0));
        support.setType(Material.OAK_LOG,false);aim(n,support.getLocation().add(.5,.5,.5));
        WorldActions.Hit hit=WorldActions.trace(n);
        check(hit!=null&&hit.block().equals(support),"test must aim at its support");
        Block target=placing?hit.previous():support;check(target!=null,"missing placement target");
        var original=target.getBlockData().clone();
        if(placing)check(target.getType().isAir(),"placement target must start empty");
        Goal goal=n.goal;int[] events={0};String[] expectedBlock={original.getAsString()};
        List<LivingEntity> peers=new ArrayList<>();Set<UUID> originalItems=items(n);
        Runnable peer=()->peers.add(NpcBody.spawn(n.plugin,target.getLocation().add(.5,0,.5),100_000+n.id));
        if(mode.equals("peer-before"))peer.run();
        callbacks.put(n.entity.getUniqueId(),event->{
            if(!event.getBlock().equals(target))throw new AssertionError("event targeted another block");
            events[0]++;
            switch(mode){
                case "cancel"->event.setCancelled(true);
                case "replace"->target.setType(Material.DIAMOND_BLOCK,false);
                case "data"->{Orientable data=(Orientable)target.getBlockData();data.setAxis(Axis.X);target.setBlockData(data,false);}
                case "permission"->denied.add(n.id);
                case "held"->n.pocket.setStorage(0,new Stack("COBBLESTONE",3));
                case "selected"->n.pocket.select(1);
                case "goal"->n.goal=new Goal(goal.task(),goal.x(),goal.y(),goal.z(),goal.episode()+1,1,3000);
                case "look"->n.entity.setRotation(n.entity.getLocation().getYaw()+90,0);
                case "menu"->n.pocket.open(Pocket.Menu.INVENTORY);
                case "remove"->n.remove.set(true);
                case "peer-event"->peer.run();
                default->{}
            }
            expectedBlock[0]=target.getBlockData().getAsString();
        });
        try{
            int[] action=Schema.IDLE.clone();action[4]=placing?2:1;
            for(int i=0;i<(placing?1:60);i++)WorldActions.tick(n,action,i==0);
            check(events[0]==(mode.equals("peer-before")?0:1),"event coverage: "+placing+" "+mode+" count="+events[0]);
            if(mode.equals("normal")){
                check(target.getType()==(placing?Material.OAK_PLANKS:Material.AIR),"ordinary action failed: "+placing);
                check((placing?n.placed:n.broken).getOrDefault(placing?"OAK_PLANKS":"OAK_LOG",0L)==1,"ordinary action missing credit");
                if(placing)check(n.pocket.storage(0).count()==2,"ordinary placement did not consume one");
            }else{
                check(target.getBlockData().getAsString().equals(expectedBlock[0]),"stale world overwrite: "+placing+" "+mode);
                check(n.broken.isEmpty()&&n.placed.isEmpty(),"stale operation earned credit: "+placing+" "+mode);
                check(items(n).equals(originalItems),"rejected edit spawned drops: "+placing+" "+mode);
                if(placing)check(n.pocket.storage(0).count()==3&&n.pocket.storage(1).count()==3,"rejected placement consumed stock: "+mode);
            }
        }finally{
            callbacks.remove(n.entity.getUniqueId());denied.remove(n.id);n.goal=goal;n.remove.set(false);
            for(LivingEntity p:peers)p.remove();removeNewItems(n,originalItems);target.setBlockData(original,false);
        }
    }
    private static void containerTrial(Npc n,Block target,Material material,String mode){
        n.pocket.clear();n.pocket.setStorage(0,new Stack("WOODEN_PICKAXE",1));
        n.mining=null;n.miningTicks=0;n.broken.clear();n.placed.clear();n.tick=1;
        target.setType(material,false);aim(n,target.getLocation().add(.5,.5,.5));
        ItemStack reserve=new ItemStack(Material.DIAMOND,7);
        reserve.editMeta(meta->meta.displayName(net.kyori.adventure.text.Component.text("Shared reserve")));
        if(mode.equals("full-before"))((Container)target.getState()).getInventory().setItem(0,reserve.clone());
        int[] events={0};Set<UUID> before=items(n);
        callbacks.put(n.entity.getUniqueId(),event->{
            events[0]++;
            if(mode.equals("full-event"))((Container)target.getState()).getInventory().setItem(0,reserve.clone());
        });
        try{
            int[] action=Schema.IDLE.clone();action[4]=1;
            for(int i=0;i<60;i++)WorldActions.tick(n,action,i==0);
            if(mode.equals("empty")){
                check(target.getType().isAir()&&n.broken.getOrDefault(material.name(),0L)==1,"empty container cannot be mined: "+material);
                check(events[0]==1,"empty container event coverage");
            }else{
                if(target.getType()!=material)System.out.println("CONTAINER REMOVAL DIAGNOSTIC material="+material+" mode="+mode+" drops="+n.entity.getNearbyEntities(5,5,5).stream().filter(e->e instanceof Item&&!before.contains(e.getUniqueId())).map(e->((Item)e).getItemStack().toString()).toList());
                check(target.getType()==material,"shared stock container destroyed: "+material+" "+mode);
                check(reserve.equals(((Container)target.getState()).getSnapshotInventory().getItem(0)),"shared reserve was changed");
                check(n.broken.isEmpty()&&items(n).equals(before),"blocked container mining produced drops or credit");
                check(events[0]==(mode.equals("full-before")?0:1),"filled container event coverage");
            }
        }finally{callbacks.remove(n.entity.getUniqueId());removeNewItems(n,before);target.setType(Material.AIR,false);}
    }
    static void verify(Npc n){
        if(n.goal.task()!=Task.SUPPLY_CHEST)return;
        Block support=new Location(n.entity.getWorld(),n.goal.x(),n.goal.y(),n.goal.z()).getBlock();
        var blockData=support.getBlockData().clone();
        Inventory chest=((Chest)support.getState()).getBlockInventory();
        ItemStack[] contents=Arrays.stream(chest.getContents()).map(s->s==null?null:s.clone()).toArray(ItemStack[]::new);
        Stack[] storage=new Stack[36];for(int i=0;i<36;i++)storage[i]=n.pocket.storage(i);
        int selected=n.pocket.selected();long tick=n.tick;Location at=n.entity.getLocation();
        var velocity=n.entity.getVelocity().clone();var equipment=n.entity.getEquipment().getItemInMainHand().clone();
        var broken=new HashMap<>(n.broken);var placed=new HashMap<>(n.placed);var collected=new HashMap<>(n.collected);
        try{
            for(boolean placing:new boolean[]{false,true}){
                for(String mode:List.of("normal","cancel","replace","permission","held","selected","goal","look","menu","remove"))trial(n,support,placing,mode);
                if(placing){trial(n,support,true,"peer-before");trial(n,support,true,"peer-event");}
                else trial(n,support,false,"data");
            }
            for(Material material:List.of(Material.CHEST,Material.FURNACE))
                for(String mode:List.of("empty","full-before","full-event"))containerTrial(n,support,material,mode);
            System.out.println("WORLD MUTATION LIVE PASS checks="+checks+" cases=29");
        }finally{
            support.setBlockData(blockData,false);((Chest)support.getState()).getBlockInventory().setContents(contents);
            n.pocket.clear();for(int i=0;i<36;i++)n.pocket.setStorage(i,storage[i]);n.pocket.select(selected);
            n.tick=tick;n.mining=null;n.miningTicks=0;n.entity.setRotation(at.getYaw(),at.getPitch());
            n.entity.setVelocity(velocity);n.entity.getEquipment().setItemInMainHand(equipment);
            n.broken.clear();n.broken.putAll(broken);n.placed.clear();n.placed.putAll(placed);n.collected.clear();n.collected.putAll(collected);
        }
    }
}
