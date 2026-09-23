package org.botsclustersmc.plugin;

import org.botsclustersmc.core.*;
import org.botsclustersmc.core.Stack;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import java.util.*;

/** Literal, bounded NPC actuators. No navigation, auto-aim, tool selection, or recipe macro. */
public final class WorldActions {
    private WorldActions(){}
    public record Hit(Block block,Block previous){}
    /** Ownership must be established before any world/chunk/block query. */
    public static boolean owned(Location p) {
        World world=p.getWorld();
        if(world==null||!Bukkit.isOwnedByCurrentRegion(p))return false;
        int y=p.getBlockY();
        return y>=world.getMinHeight()&&y<world.getMaxHeight()
            &&world.isChunkLoaded(p.getBlockX()>>4,p.getBlockZ()>>4);
    }
    public static Hit trace(Npc npc){
        Location eye=npc.entity.getEyeLocation();Vector direction=eye.getDirection();Block last=null;
        int bx=Integer.MIN_VALUE,by=0,bz=0;
        for(int i=0;i<=23;i++){
            Location p=eye.clone().add(direction.clone().multiply(i*.2));
            if(p.getBlockX()==bx&&p.getBlockY()==by&&p.getBlockZ()==bz)continue;
            bx=p.getBlockX();by=p.getBlockY();bz=p.getBlockZ();if(!owned(p))return null;
            Block b=p.getBlock();if(!b.isPassable())return new Hit(b,last);last=b;
        }
        return null;
    }
    public static void tick(Npc npc,int[] action,boolean justApplied){
        Mob mob=npc.entity;Location p=mob.getLocation();
        boolean focused=MenuFocus.active(npc.pocket.menu()!=Pocket.Menu.CLOSED,action[6]);
        float yaw=(p.getYaw()+(focused?0:new int[]{-8,-2,0,2,8}[action[1]]))%360;
        float pitch=Math.max(-89,Math.min(89,p.getPitch()+(focused?0:new int[]{-4,-1,0,1,4}[action[2]])));mob.setRotation(yaw,pitch);
        double forward=switch(action[0]){case 1,5,6->1;case 2,7,8->-1;default->0;};
        double side=switch(action[0]){case 3,5,7->-1;case 4,6,8->1;default->0;};
        if(focused){forward=0;side=0;}
        double norm=Math.max(1,Math.hypot(forward,side)),speed=action[3]==2?.07:.18,angle=Math.toRadians(yaw);
        Vector velocity=mob.getVelocity();double vy=velocity.getY();if(!focused&&action[3]==1&&justApplied&&mob.isOnGround())vy=.42;
        mob.setVelocity(new Vector((-Math.sin(angle)*forward+Math.cos(angle)*side)*speed/norm,vy,(Math.cos(angle)*forward+Math.sin(angle)*side)*speed/norm));
        if(justApplied){
            if(!focused)npc.pocket.select(action[5]);
            if(action[6]!=0){npc.pocket.click(action[6],action[7],ExternalInventory.locate(npc));if(npc.pocket.menu()==Pocket.Menu.CLOSED)npc.container=null;}
            if(!focused){if(action[4]==2)use(npc);else if(action[4]==3)drop(npc);}
            ItemStack held=ExternalInventory.to(npc.pocket.held());npc.entity.getEquipment().setItemInMainHand(held);
        }
        if(!focused&&action[4]==1)mine(npc);else{npc.mining=null;npc.miningTicks=0;}
        if(npc.tick%4==0)pickup(npc);
    }
    private static boolean mayChange(Npc npc,Block b,Material next){
        if(!npc.plugin.canChange(npc,b))return false;
        EntityChangeBlockEvent event=new EntityChangeBlockEvent(npc.entity,b,next.createBlockData());
        Bukkit.getPluginManager().callEvent(event);return !event.isCancelled();
    }
    private static void mine(Npc npc){
        Hit hit=trace(npc);if(hit==null||!npc.plugin.canChange(npc,hit.block())){npc.mining=null;npc.miningTicks=0;return;}Block b=hit.block();Material type=b.getType();
        int kind=Stack.kind(type.name());if(kind!=2&&kind!=3&&kind!=5&&kind!=8&&kind!=9&&kind!=11&&kind!=14&&kind!=15){npc.miningTicks=0;return;}
        String key=b.getX()+":"+b.getY()+":"+b.getZ()+":"+type.name()+":"+npc.pocket.held().item();
        if(!key.equals(npc.mining)){npc.mining=key;npc.miningTicks=0;}
        int tool=Stack.kind(npc.pocket.held().item());boolean pick=tool==6||tool==7;
        int ticks=(kind==9||kind==8||kind==11)?(pick?40:300):60;
        if(++npc.miningTicks<ticks)return;npc.miningTicks=0;
        if(!mayChange(npc,b,Material.AIR))return;
        ItemStack held=ExternalInventory.to(npc.pocket.held());if(held==null)held=new ItemStack(Material.AIR);
        Collection<ItemStack> drops=b.getDrops(held,npc.entity);b.setType(Material.AIR,true);
        if(!b.getType().isAir())return;npc.broken.merge(type.name(),1L,Long::sum);
        for(ItemStack stack:drops){Item item=b.getWorld().dropItem(b.getLocation().add(.5,.4,.5),stack);item.setPickupDelay(0);item.getPersistentDataContainer().set(npc.plugin.provenance,PersistentDataType.STRING,npc.token());}
        npc.entity.swingMainHand();
    }
    private static void use(Npc npc){
        Hit hit=trace(npc);if(hit==null)return;Block b=hit.block();
        if(b.getType()==Material.CRAFTING_TABLE){npc.container=b.getLocation();npc.pocket.open(Pocket.Menu.WORKBENCH);return;}
        if(b.getType()==Material.FURNACE||b.getType()==Material.CHEST){
            // Container writes also require the explicit world-edit permission/bounds.
            if(!npc.plugin.canChange(npc,b))return;
            npc.container=b.getLocation();npc.pocket.open(b.getType()==Material.FURNACE?Pocket.Menu.FURNACE:Pocket.Menu.CHEST);return;
        }
        if(hit.previous()==null||npc.pocket.held().empty())return;Material material=Material.valueOf(npc.pocket.held().item());
        if(!material.isBlock()||!Set.of(2,3,5,8,9,14,15).contains(Stack.kind(material.name())))return;
        Block target=hit.previous();if(!target.getType().isAir()||new org.bukkit.util.BoundingBox(target.getX(),target.getY(),target.getZ(),target.getX()+1,target.getY()+1,target.getZ()+1).overlaps(npc.entity.getBoundingBox()))return;
        if(!mayChange(npc,target,material))return;target.setType(material,true);
        if(target.getType()==material){npc.pocket.consumeHeld(1);npc.placed.merge(material.name(),1L,Long::sum);npc.entity.swingMainHand();}
    }
    private static void pickup(Npc npc){
        if(!npc.plugin.pickupEnabled(npc))return;
        Location at=npc.entity.getLocation();
        if(!Bukkit.isOwnedByCurrentRegion(at,1))return;
        for(Entity e:npc.entity.getNearbyEntities(1.1,1.2,1.1)){
            if(!(e instanceof Item item)||!Bukkit.isOwnedByCurrentRegion(item)||item.isDead()||item.getPickupDelay()>0)continue;
            String token=item.getPersistentDataContainer().get(npc.plugin.provenance,PersistentDataType.STRING);
            if(!npc.plugin.canPickup(npc,token))continue;
            Stack s=ExternalInventory.from(item.getItemStack());int moved=Math.min(npc.pocket.capacity(s),s.count());if(moved<=0)continue;
            EntityPickupItemEvent event=new EntityPickupItemEvent(npc.entity,item,s.count()-moved);Bukkit.getPluginManager().callEvent(event);if(event.isCancelled())continue;
            Stack remaining=npc.pocket.insert(s);int taken=s.count()-remaining.count();
            if(remaining.empty())item.remove();else item.setItemStack(ExternalInventory.to(remaining));
            npc.collected.merge(s.item(),(long)taken,Long::sum);
        }
    }
    private static void drop(Npc npc){
        Stack s=npc.pocket.held();if(s.empty())return;
        Item item=npc.entity.getWorld().dropItem(npc.entity.getLocation(),ExternalInventory.to(s.withCount(1)));
        EntityDropItemEvent event=new EntityDropItemEvent(npc.entity,item);Bukkit.getPluginManager().callEvent(event);
        if(event.isCancelled()){item.remove();return;}npc.pocket.consumeHeld(1);item.setPickupDelay(20);
        item.getPersistentDataContainer().set(npc.plugin.provenance,PersistentDataType.STRING,npc.token());
    }
}
