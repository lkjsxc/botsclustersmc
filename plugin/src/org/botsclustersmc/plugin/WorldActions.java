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
        if(!Bukkit.isOwnedByCurrentRegion(npc.entity)||!npc.entity.isValid()||npc.dropping)return;
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
            if(!focused){if(action[4]==2)use(npc);else if(action[4]==3)DroppedItems.drop(npc);}
            if(!Bukkit.isOwnedByCurrentRegion(mob)||!mob.isValid())return;
            ItemStack held=ExternalInventory.to(npc.pocket.held());npc.entity.getEquipment().setItemInMainHand(held);
        }
        if(!focused&&action[4]==1)mine(npc);else{npc.mining=null;npc.miningTicks=0;}
        if(npc.tick%4==0)pickup(npc);
    }
    private static boolean mayChange(Npc npc,Block b,Material next){
        if(!owned(b.getLocation())||!npc.plugin.canChange(npc,b))return false;
        var before=b.getBlockData().clone();Location origin=npc.entity.getLocation();
        Stack held=npc.pocket.held();int selected=npc.pocket.selected();Goal goal=npc.goal;
        EntityChangeBlockEvent event=new EntityChangeBlockEvent(npc.entity,b,next.createBlockData());
        Bukkit.getPluginManager().callEvent(event);
        // Listeners can replace blocks, revoke permission or change the actor. A selected
        // action is not a reservation; never spend new inventory on an old world edit.
        return !event.isCancelled()&&Bukkit.isOwnedByCurrentRegion(npc.entity)&&npc.entity.isValid()
            &&!npc.remove.get()&&!npc.resetting&&!npc.paused&&!npc.plugin.paused.get()
            &&npc.goal==goal&&npc.pocket.menu()==Pocket.Menu.CLOSED
            &&npc.pocket.selected()==selected&&npc.pocket.held().equals(held)
            &&origin.equals(npc.entity.getLocation())&&owned(b.getLocation())
            &&before.equals(b.getBlockData())&&npc.plugin.canChange(npc,b);
    }
    /** A building action may not entomb another citizen, player or living mob. */
    private static boolean vacant(Block target){
        Location at=target.getLocation();
        if(!owned(at)||!Bukkit.isOwnedByCurrentRegion(at,1))return false;
        var box=new org.bukkit.util.BoundingBox(target.getX(),target.getY(),target.getZ(),target.getX()+1,target.getY()+1,target.getZ()+1);
        for(Entity entity:target.getWorld().getNearbyEntities(box)){
            if(!(entity instanceof LivingEntity))continue;
            if(!Bukkit.isOwnedByCurrentRegion(entity))return false;
            if(entity instanceof Player player&&player.getGameMode()==GameMode.SPECTATOR)continue;
            if(entity.getBoundingBox().overlaps(box))return false;
        }
        return true;
    }
    /** This simplified getDrops/setType path does not spill chest/furnace stock.
     * Require an empty local container until native block-entity spills are supported. */
    private static boolean emptyContainer(Block block){
        BlockState state=block.getState();
        return !(state instanceof Container container)||container.getSnapshotInventory().isEmpty();
    }
    private static void mine(Npc npc){
        Hit hit=trace(npc);if(hit==null||!npc.plugin.canChange(npc,hit.block())){npc.mining=null;npc.miningTicks=0;return;}Block b=hit.block();Material type=b.getType();
        int kind=Stack.kind(type.name());if(kind!=2&&kind!=3&&kind!=5&&kind!=8&&kind!=9&&kind!=11&&kind!=14&&kind!=15){npc.miningTicks=0;return;}
        if((kind==14||kind==15)&&!emptyContainer(b)){npc.mining=null;npc.miningTicks=0;return;}
        String key=b.getX()+":"+b.getY()+":"+b.getZ()+":"+type.name()+":"+npc.pocket.held().item();
        if(!key.equals(npc.mining)){npc.mining=key;npc.miningTicks=0;}
        int tool=Stack.kind(npc.pocket.held().item());boolean pick=tool==6||tool==7;
        int ticks=(kind==9||kind==8||kind==11)?(pick?40:300):60;
        if(++npc.miningTicks<ticks)return;npc.miningTicks=0;
        if(!mayChange(npc,b,Material.AIR)||((kind==14||kind==15)&&!emptyContainer(b)))return;
        ItemStack held=ExternalInventory.to(npc.pocket.held());if(held==null)held=new ItemStack(Material.AIR);
        String token=npc.token();
        Collection<ItemStack> drops=b.getDrops(held,npc.entity);b.setType(Material.AIR,true);
        if(!b.getType().isAir())return;npc.broken.merge(type.name(),1L,Long::sum);
        for(ItemStack stack:drops)b.getWorld().dropItem(b.getLocation().add(.5,.4,.5),stack,item->{
            // The source block has already paid for this drop. Initialize its original
            // episode before ItemSpawnEvent, then preserve listeners' changes/cancellation.
            // In particular, never relabel an old harvest with a callback's new goal.
            item.setPickupDelay(0);item.getPersistentDataContainer().set(npc.plugin.provenance,PersistentDataType.STRING,token);
        });
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
        Block target=hit.previous();if(!target.getType().isAir()||!vacant(target))return;
        if(!mayChange(npc,target,material)||!vacant(target))return;target.setType(material,true);
        if(target.getType()==material){npc.pocket.consumeHeld(1);npc.placed.merge(material.name(),1L,Long::sum);npc.entity.swingMainHand();}
    }
    private static void pickup(Npc npc){
        if(!npc.plugin.pickupEnabled(npc))return;
        Location at=npc.entity.getLocation();
        if(!Bukkit.isOwnedByCurrentRegion(at,1))return;
        for(Entity e:npc.entity.getNearbyEntities(1.1,1.2,1.1)){
            if(!(e instanceof Item item)||!Bukkit.isOwnedByCurrentRegion(item)||item.isDead()||item.getPickupDelay()>0||!item.canMobPickup())continue;
            UUID owner=item.getOwner();if(owner!=null&&!owner.equals(npc.entity.getUniqueId()))continue;
            String token=item.getPersistentDataContainer().get(npc.plugin.provenance,PersistentDataType.STRING);
            if(!npc.plugin.canPickup(npc,token))continue;
            ItemStack observed=item.getItemStack().clone();
            if(!ExternalInventory.supported(observed))continue;
            Stack s=ExternalInventory.from(observed);int moved=Math.min(npc.pocket.capacity(s),s.count());if(moved<=0)continue;
            EntityPickupItemEvent event=new EntityPickupItemEvent(npc.entity,item,s.count()-moved);Bukkit.getPluginManager().callEvent(event);if(event.isCancelled())continue;
            // Event listeners may remove or replace the item. Never insert the stale snapshot.
            if(!Bukkit.isOwnedByCurrentRegion(npc.entity)||!Bukkit.isOwnedByCurrentRegion(item)
                    ||!npc.entity.isValid()||item.isDead()||!item.isValid()||item.getPickupDelay()>0||!item.canMobPickup()
                    ||!Objects.equals(owner,item.getOwner())||!observed.equals(item.getItemStack())
                    ||!Objects.equals(token,item.getPersistentDataContainer().get(npc.plugin.provenance,PersistentDataType.STRING))
                    ||!npc.plugin.pickupEnabled(npc)||!npc.plugin.canPickup(npc,token))continue;
            if(npc.entity.getWorld()!=item.getWorld()
                    ||!npc.entity.getBoundingBox().expand(1.1,1.2,1.1).overlaps(item.getBoundingBox()))continue;
            Stack remaining=npc.pocket.insert(s);int taken=s.count()-remaining.count();
            if(remaining.empty())item.remove();else item.setItemStack(ExternalInventory.to(remaining));
            npc.collected.merge(s.item(),(long)taken,Long::sum);
        }
    }
}
