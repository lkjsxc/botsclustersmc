package org.botsclustersmc.plugin;

import org.botsclustersmc.core.*;
import org.bukkit.*;

/** Deployment JAR contains no learner, reset environments, optimizer or training state reader. */
public final class InferencePlugin extends RuntimePlugin {
    @Override protected Policy initialPolicy()throws Exception{return PolicyFile.read(getDataFolder().toPath().resolve("policy.bcmc"));}
    @Override protected void initialize(){
        int n=bounded("count",0,0,maximum);if(n==0)return;
        Bukkit.getGlobalRegionScheduler().runDelayed(this,t->{
            try{
                World world=Bukkit.getWorld(getConfig().getString("world","world"));if(world==null)throw new IllegalArgumentException("configured world not loaded");
                double x=getConfig().getDouble("origin.x",0.5),y=getConfig().getDouble("origin.y",65),z=getConfig().getDouble("origin.z",0.5);
                Task task=Task.at(getConfig().getInt("task",3));Goal goal=new Goal(task,getConfig().getDouble("goal.x",x),getConfig().getDouble("goal.y",y),getConfig().getDouble("goal.z",z),0,1,task.horizon());
                int width=(int)Math.ceil(Math.sqrt(n));double spacing=getConfig().getDouble("spacing",2);
                if(!Double.isFinite(spacing)||spacing<0||spacing>512)throw new IllegalArgumentException("spacing bounds");
                for(int i=0;i<n;i++)requestSpawn(new Location(world,x+(i%width)*spacing,y,z+(i/width)*spacing),goal);
            }catch(Throwable e){fail(e);}
        },1);
    }
}
