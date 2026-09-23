package org.botsclustersmc.plugin;

import org.botsclustersmc.core.*;
import org.botsclustersmc.core.Stack;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

/** Local bounded privileged state, identical in training and deployment. Unknown regions stay unknown. */
public final class Sensors {
    private Sensors(){}
    public static double angle(double degrees){double d=degrees%360;return d>180?d-360:d< -180?d+360:d;}
    public static Frame capture(Npc npc){
        long begin=System.nanoTime();Location p=npc.entity.getLocation();Vector velocity=npc.entity.getVelocity();Goal goal=npc.goal;
        double dx=goal.x()-p.getX(),dy=goal.y()-p.getY(),dz=goal.z()-p.getZ(),distance=Math.sqrt(dx*dx+dy*dy+dz*dz);
        double yawError=angle(Math.toDegrees(Math.atan2(-dx,dz))-p.getYaw());
        double eyeDy=goal.y()+.5-(p.getY()+npc.entity.getEyeHeight());
        double pitchError=angle(-Math.toDegrees(Math.atan2(eyeDy,Math.hypot(dx,dz)))-p.getPitch());
        float[] f=new float[Schema.INPUTS];f[0]=1;f[1]=clip(dx/16);f[2]=clip(dy/8);f[3]=clip(dz/16);
        f[4]=(float)Math.sin(Math.toRadians(p.getYaw()));f[5]=(float)Math.cos(Math.toRadians(p.getYaw()));
        f[6]=(float)Math.sin(Math.toRadians(p.getPitch()));f[7]=(float)Math.cos(Math.toRadians(p.getPitch()));
        f[8]=clip(velocity.getX()/.3);f[9]=clip(velocity.getY()/.5);f[10]=clip(velocity.getZ()/.3);f[11]=npc.entity.isOnGround()?1:0;
        f[12]=Math.max(0,1-(npc.tick-npc.episodeStart)/(float)goal.horizon());f[13]=clip(distance/16);f[14]=(float)(pitchError/180);f[15]=(float)(yawError/180);
        f[16+goal.task().ordinal()]=1;
        for(int i=0;i<8;i++)f[34+i]=npc.action[i]/(float)Math.max(1,Schema.HEADS[i]-1);
        Pocket.External external=ExternalInventory.locate(npc);
        if((npc.pocket.menu()==Pocket.Menu.CHEST||npc.pocket.menu()==Pocket.Menu.FURNACE)&&external==Pocket.NONE){npc.pocket.close();npc.container=null;}
        if(npc.pocket.menu()==Pocket.Menu.WORKBENCH&&(npc.container==null||!WorldActions.owned(npc.container)||npc.container.getBlock().getType()!=Material.CRAFTING_TABLE||p.distanceSquared(npc.container)>36)){npc.pocket.close();npc.container=null;}
        f[42]=npc.pocket.menu().ordinal()/4f;f[43]=npc.pocket.selected()/8f;f[44]=Math.min(1,npc.miningTicks/60f);f[45]=clip(npc.entity.getHealth()/20);
        f[46]=(float)goal.difficulty();f[47]=clip((npc.tick-npc.episodeStart)/3000.0);f[48]=clip(npc.collected.values().stream().mapToLong(Long::longValue).sum()/64.0);f[49]=clip(npc.broken.values().stream().mapToLong(Long::longValue).sum()/16.0);
        int index=50;
        for(int y=-1;y<=1;y++)for(int z=-2;z<=2;z++)for(int x=-2;x<=2;x++){
            Location q=new Location(p.getWorld(),p.getBlockX()+x,p.getBlockY()+y,p.getBlockZ()+z);
            if(WorldActions.owned(q)){Block b=q.getBlock();f[index]=Stack.kind(b.getType().name())/20f;f[index+1]=b.isPassable()?0:1;}else{f[index]=-1;f[index+1]=-1;}index+=2;
        }
        for(int i=0;i<64;i++){Stack s=i<36?npc.pocket.storage(i):npc.pocket.get(i,external);f[200+i*2]=Stack.kind(s.item())/20f;f[201+i*2]=s.count()/64f;}
        f[328]=Stack.kind(npc.pocket.cursor().item())/20f;f[329]=npc.pocket.cursor().count()/64f;
        f[330]=clip(npc.pocket.crafted.values().stream().mapToLong(Long::longValue).sum()/64.0);f[331]=clip(npc.pocket.extracted.values().stream().mapToLong(Long::longValue).sum()/64.0);
        if(npc.pocket.menu()==Pocket.Menu.FURNACE&&npc.container!=null&&WorldActions.owned(npc.container)&&npc.container.getBlock().getState() instanceof org.bukkit.block.Furnace furnace){f[332]=clip(furnace.getCookTime()/(double)Math.max(1,furnace.getCookTimeTotal()));f[333]=clip(furnace.getBurnTime()/1600.0);}
        for(int i=0;i<16;i++)f[346+i]=npc.previousKinematics[i];System.arraycopy(f,0,npc.previousKinematics,0,16);
        boolean[] mask=goal.task().mask(npc.pocket.slots(),npc.pocket.menu()!=Pocket.Menu.CLOSED);
        npc.plugin.sensorNanos.add(System.nanoTime()-begin);
        return new Frame(f,mask,npc.tick,p.getX(),p.getY(),p.getZ(),p.getYaw(),p.getPitch(),velocity.getX(),velocity.getY(),velocity.getZ(),npc.entity.isOnGround(),distance,yawError,pitchError);
    }
    private static float clip(double n){return (float)Math.max(-4,Math.min(4,n));}
}
