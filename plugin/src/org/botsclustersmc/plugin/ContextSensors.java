package org.botsclustersmc.plugin;

import org.botsclustersmc.core.GoalInputs;
import org.botsclustersmc.core.Stack;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;

/** Bounded local context. Unloaded or foreign regions are unknown, never requested. */
public final class ContextSensors {
    private ContextSensors() {}
    private static boolean hazard(Material m) {
        return m==Material.LAVA||m==Material.FIRE||m==Material.SOUL_FIRE||m==Material.CACTUS||m==Material.MAGMA_BLOCK||m==Material.CAMPFIRE||m==Material.SOUL_CAMPFIRE;
    }
    public static void capture(Npc npc,Location p,float[] f) {
        if(Bukkit.isOwnedByCurrentRegion(p,1)) {
            f[382]=1;
            List<Entity> nearby=new ArrayList<>();
            for(Entity e:npc.entity.getNearbyEntities(6,3,6)) {
                if(!Bukkit.isOwnedByCurrentRegion(e)||!e.isValid())continue;
                if(e instanceof Player player&&player.getGameMode()==GameMode.SPECTATOR)continue;
                if(e instanceof Item||e instanceof LivingEntity)nearby.add(e);
            }
            nearby.sort(Comparator.comparingDouble(e->e.getLocation().distanceSquared(p)));
            for(int i=0;i<Math.min(8,nearby.size());i++)encodeEntity(npc,p,nearby.get(i),f,384+i*8);
        }
        int known=0;
        for(int ray=0;ray<8;ray++) { int offset=448+ray*8;range(p,p.getYaw()+ray*45,f,offset);if(f[offset]>0)known++; }
        f[383]=known/8f;
    }
    private static void encodeEntity(Npc npc,Location p,Entity e,float[] f,int offset) {
        Location q=e.getLocation();double dx=q.getX()-p.getX(),dz=q.getZ()-p.getZ();
        boolean friendly=e instanceof Mob&&e.getPersistentDataContainer().has(npc.plugin.provenance,PersistentDataType.STRING);
        int kind=e instanceof Item?1:e instanceof Player?2:friendly?3:e instanceof Enemy?4:5;
        f[offset]=1;f[offset+1]=GoalInputs.unit(GoalInputs.forward(dx,dz,p.getYaw())/8);
        f[offset+2]=GoalInputs.unit(GoalInputs.right(dx,dz,p.getYaw())/8);f[offset+3]=GoalInputs.unit((q.getY()-p.getY())/4);
        f[offset+4]=kind/5f;
        f[offset+5]=e instanceof Item item?Stack.kind(item.getItemStack().getType().name())/20f:GoalInputs.unit(((LivingEntity)e).getHealth()/20);
        var velocity=e.getVelocity();
        f[offset+6]=GoalInputs.unit(GoalInputs.forward(velocity.getX(),velocity.getZ(),p.getYaw())/.3);
        f[offset+7]=GoalInputs.unit(GoalInputs.right(velocity.getX(),velocity.getZ(),p.getYaw())/.3);
    }
    private static void range(Location origin,double yaw,float[] f,int offset) {
        double angle=Math.toRadians(yaw);
        for(int distance=1;distance<=12;distance++) {
            Location q=origin.clone().add(-Math.sin(angle)*distance,1,Math.cos(angle)*distance);
            if(q.getBlockY()<q.getWorld().getMinHeight()+2||q.getBlockY()>=q.getWorld().getMaxHeight()||!WorldActions.owned(q)) {f[offset]=0;return;}
            Block block=q.getBlock();f[offset]=1;f[offset+1]=distance/12f;
            f[offset+2]=Stack.kind(block.getType().name())/20f;f[offset+3]=block.isPassable()?0:1;
            f[offset+4]=block.isLiquid()?1:0;f[offset+5]=hazard(block.getType())?1:0;
            if(!block.isPassable()||block.isLiquid()||distance==12) {
                Location feet=q.clone().subtract(0,1,0),floor=q.clone().subtract(0,2,0);
                if(floor.getBlockY()<q.getWorld().getMinHeight()||q.getBlockY()>=q.getWorld().getMaxHeight()){f[offset]=0;return;}
                f[offset+6]=feet.getBlock().isPassable()?0:1;
                f[offset+7]=floor.getBlock().isPassable()?1:0;
                if(hazard(feet.getBlock().getType())||hazard(floor.getBlock().getType()))f[offset+5]=1;
                return;
            }
        }
    }
}
