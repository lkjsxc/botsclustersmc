package org.botsclustersmc.diagnostic;

import org.botsclustersmc.core.*;
import org.botsclustersmc.plugin.*;
import org.botsclustersmc.training.*;
import org.bukkit.*;
import org.bukkit.event.entity.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Explicitly scripted reachability diagnostics. Never included in either public JAR. */
public final class Fixtures extends RuntimePlugin {
    private final Map<Long,TrainingEnvironment.Session> sessions=new ConcurrentHashMap<>();
    private final Set<Long> passed=ConcurrentHashMap.newKeySet();
    private final AtomicInteger prepared=new AtomicInteger();
    private static final class Control {Frame before;int[] action=Schema.IDLE.clone();int step,placed,harvestPeak;boolean broken,harvestContactChecked,harvestBreakChecked;final ArrayDeque<int[]> clicks=new ArrayDeque<>();}
    @Override public boolean training(){return true;}
    @Override protected Policy initialPolicy(){return new Policy(new float[Policy.PARAMETERS],0,0);}
    @Override public boolean pickupEnabled(Npc n){return true;}
    @Override public boolean canPickup(Npc n,String token){return n.token().equals(token);}
    @Override public boolean canChange(Npc n,org.bukkit.block.Block b){return WorldActions.owned(b.getLocation())&&b.getY()>=65&&b.getY()<70;}
    @Override protected void initialize(){
        if(!Boolean.getBoolean("bcmc.fixtures"))throw new IllegalStateException("diagnostics require explicit isolated-test flag");
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(this,t->{
            int actor=prepared.getAndIncrement();if(actor>=18)return;World w=Bukkit.getWorld("world");ArenaLayout a=ArenaLayout.forActor(actor,18,64);
            Location at=new Location(w,a.x()+8.5,65,a.z()+5.5);
            LoadedChunks.use(this,at,chunk->{chunk.addPluginChunkTicket(this);TrainingEnvironment.build(w,a);sessions.put((long)actor,new TrainingEnvironment.Session(a));requestSpawn(actor,at,new Goal(Task.at(actor),at.getX(),65,at.getZ()+3,1,1,3000));},this::fail);
        },1,1);
        io.scheduleAtFixedRate(()->{try{if(failed.get()!=null)Files.writeString(getDataFolder().toPath().resolve("fixtures-failed.txt"),failed.get().toString());else if(passed.size()==18){Files.writeString(getDataFolder().toPath().resolve("fixtures-passed.txt"),"PASS all 18 full-difficulty real-server scripted fixtures. No learning performed.\n");Bukkit.getGlobalRegionScheduler().run(this,t->Bukkit.shutdown());}}catch(Exception e){fail(e);}},1,1,TimeUnit.SECONDS);
    }
    @Override protected void spawned(Npc n){
        EntityCombustEvent ambient=new EntityCombustEvent(n.entity,8.0f);ambient.callEvent();
        EntityCombustEvent block=new EntityCombustByBlockEvent(null,n.entity,8.0f);block.callEvent();
        EntityCombustEvent attacker=new EntityCombustByEntityEvent(n.entity,n.entity,8.0f);attacker.callEvent();
        if(ambient.isCancelled()||block.isCancelled()||attacker.isCancelled()||!(n.entity instanceof org.bukkit.entity.Villager))throw new IllegalStateException("Citizen bodies must not gain blanket combustion immunity");
        TrainingEnvironment.Session s=sessions.get(n.id);s.lesson=new Course.Lesson(1,Task.at((int)n.id),1,700+n.id,Course.Kind.PROBE);n.context=new Control();TrainingEnvironment.reset(this,n,s,s.lesson);
    }
    @Override protected void startNpc(Npc n){n.entity.getScheduler().runAtFixedRate(this,t->{try{tick(n);}catch(Throwable e){fail(e);}},()->{},1,1);}
    private void tick(Npc n){
        n.tick++;n.lastStepNanos=System.nanoTime();if(n.resetting||failed.get()!=null||passed.contains(n.id))return;
        Control c=(Control)n.context;TrainingEnvironment.Session session=sessions.get(n.id);
        if(c.before==null&&c.step==0) {
            InputChecks.verify(n);StationChecks.verify(n,session);
            Location original=n.entity.getLocation();n.entity.setRotation(0,89);
            int[] rejected=Schema.IDLE.clone();rejected[4]=1;WorldActions.tick(n,rejected,true);
            if(n.miningTicks!=0||n.mining!=null||!n.broken.isEmpty())
                throw new IllegalStateException("A protected arena floor must not report mining progress");
            n.entity.setRotation(original.getYaw(),original.getPitch());
        }

        if(n.tick-n.episodeStart>2900)throw new IllegalStateException("fixture timeout task="+n.id+" menu="+n.pocket.menu()+" pos="+n.entity.getLocation()+" broken="+n.broken+" collected="+n.collected+" placed="+n.placed+" crafted="+n.pocket.crafted+" extracted="+n.pocket.extracted);
        boolean fresh=n.tick%4==0;
        if(fresh){Frame current=Sensors.capture(n);
            if(c.before!=null){Npc.Applied previous=new Npc.Applied(c.before,new InferencePool.Result(0,c.action,0,0,0,0,0));if(TrainingEnvironment.success(n,session,previous,current)){
                if(HarvestPractice.applies(n.goal.task())&&(!c.harvestContactChecked||!c.harvestBreakChecked))
                    throw new IllegalStateException("Harvest fixture missed contact/collection reward checks");
                passed.add(n.id);getLogger().info("FIXTURE PASS "+n.id+" "+n.goal.task()+" ticks="+(n.tick-n.episodeStart));c.action=Schema.IDLE.clone();WorldActions.tick(n,c.action,true);return;}}
            c.before=current;c.action=choose(n,c,session);n.action=c.action;
        }
        WorldActions.tick(n,c.action,fresh);verifyHarvest(n,session,c);
        var at=n.entity.getLocation();n.x=at.getX();n.y=at.getY();n.z=at.getZ();
    }
    private static void verifyHarvest(Npc n,TrainingEnvironment.Session session,Control c) {
        Task task=n.goal.task();if(!HarvestPractice.applies(task))return;
        boolean stone=task==Task.MINE_COBBLESTONE;
        boolean broken=n.broken.getOrDefault(stone?"STONE":"OAK_LOG",0L)>0;
        String target=(session.arena.x()+8)+":"+(stone?65:66)+":"+(session.arena.z()+8)+":";
        boolean contact=n.mining!=null&&n.mining.startsWith(target)&&n.miningTicks>0;
        if(!broken&&contact) {
            c.harvestPeak=Math.max(c.harvestPeak,n.miningTicks);
            double progress=n.miningTicks/(stone?40.0:60.0),weight=task==Task.BREAK_LOG?.2:.15;
            if(Math.abs(TrainingEnvironment.potential(n,session)-progress*weight)>1e-9)
                throw new IllegalStateException("Real target contact missing or mis-scaled in potential");
            Frame frame=Sensors.capture(n);
            double expected=HarvestPractice.controlReward(task,false,frame.yawError(),frame.pitchError(),
                Math.hypot(n.goal.x()-frame.x(),n.goal.z()-frame.z()),Math.hypot(frame.vx(),frame.vz()),progress,1);
            if(Math.abs(TrainingEnvironment.harvestReward(n,session,frame,1)-expected)>1e-9)
                throw new IllegalStateException("Real target contact missing in sustained harvesting cost");
            c.harvestContactChecked=true;
        }
        if(broken&&!c.harvestBreakChecked) {
            if(c.harvestPeak!=(stone?39:59))
                throw new IllegalStateException("Harvest shaping duration differs from actual block breaking");
            Frame frame=Sensors.capture(n);double distance=Math.hypot(n.goal.x()-frame.x(),n.goal.z()-frame.z());
            double expected=task==Task.BREAK_LOG?0:-.025*Math.min(1,Math.max(0,distance-.6)/6)/4;
            if(Math.abs(TrainingEnvironment.harvestReward(n,session,frame,1)-expected)>1e-9)
                throw new IllegalStateException("Broken resource did not switch to actual collection cost");
            c.harvestBreakChecked=true;
            n.plugin.getLogger().info("HARVEST MECHANICS PASS "+task+" peak="+c.harvestPeak);
        }
    }
    private static boolean aim(Npc n,int[] a,double x,double y,double z){
        Location p=n.entity.getEyeLocation();double dx=x-p.getX(),dy=y-p.getY(),dz=z-p.getZ();
        double yaw=Sensors.angle(Math.toDegrees(Math.atan2(-dx,dz))-p.getYaw()),pitch=Sensors.angle(-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)))-p.getPitch());
        a[1]=Math.abs(yaw)<4?2:yaw>0?(yaw>24?4:3):(yaw< -24?0:1);
        a[2]=Math.abs(pitch)<2?2:pitch>0?(pitch>10?4:3):(pitch< -10?0:1);
        return Math.abs(yaw)<5&&Math.abs(pitch)<3;
    }
    private static int[] click(int op,int slot){return new int[]{op,slot};}
    private static void recipe(Control c,int task){
        switch(task){
            case 8->{c.clicks.add(click(1,0));c.clicks.add(click(1,36));c.clicks.add(click(3,40));}
            case 9->{c.clicks.add(click(1,0));c.clicks.add(click(2,36));c.clicks.add(click(2,38));c.clicks.add(click(3,40));}
            case 10,17->{c.clicks.add(click(1,0));for(int i=36;i<40;i++)c.clicks.add(click(2,i));c.clicks.add(click(3,40));}
            case 11,13->{c.clicks.add(click(1,0));for(int i=36;i<39;i++)c.clicks.add(click(2,i));c.clicks.add(click(1,1));c.clicks.add(click(2,40));c.clicks.add(click(2,43));c.clicks.add(click(3,45));}
            case 14->{c.clicks.add(click(1,0));c.clicks.add(click(1,36));c.clicks.add(click(1,1));c.clicks.add(click(1,37));}
            case 15->{c.clicks.add(click(1,0));for(int i=0;i<4;i++)c.clicks.add(click(2,36));c.clicks.add(click(1,0));}
            default->{}
        }
    }
    private static int[] choose(Npc n,Control c,TrainingEnvironment.Session s){
        int[] a=Schema.IDLE.clone();int task=(int)n.id;var p=n.entity.getLocation();double dx=n.goal.x()-p.getX(),dz=n.goal.z()-p.getZ(),distance=Math.hypot(dx,dz);
        if(task<5){
            if(task==2){aim(n,a,n.goal.x(),n.goal.y()+.5,n.goal.z());return a;}
            double yaw=Sensors.angle(Math.toDegrees(Math.atan2(-dx,dz))-p.getYaw());
            if(task!=0&&task!=4)a[1]=Math.abs(yaw)<4?2:yaw>0?(yaw>24?4:3):(yaw< -24?0:1);
            if(distance>.4&&(task==0||task==4||Math.abs(yaw)<12))a[0]=1;
            if(task==4&&p.getZ()>s.arena.z()+5.7&&p.getZ()<s.arena.z()+8.3)a[3]=1;
            return a;
        }
        if(task==5||task==6||task==12||task==17){
            boolean broken=!n.broken.isEmpty();if(!broken){if(aim(n,a,n.goal.x(),n.goal.y()+.5,n.goal.z()))a[4]=1;return a;}
            if(task==5)return a;
            boolean acquired=task==12?n.pocket.count("COBBLESTONE")>0:n.pocket.count("OAK_LOG")>0;
            if(!acquired&&!(task==17&&c.step>0)){aim(n,a,n.goal.x(),p.getY()+n.entity.getEyeHeight(),n.goal.z());if(distance>.25&&a[1]==2)a[0]=1;return a;}
            if(task!=17)return a;
        }
        if(task==7||task==16){
            if(task==16){while(c.placed<3&&n.anchor.getWorld().getBlockAt(s.arena.x()+7+c.placed,65,s.arena.z()+8).getType()==Material.OAK_PLANKS)c.placed++;if(c.placed>=3)return a;}
            double x=s.arena.x()+(task==7?8:7+c.placed)+.5,z=s.arena.z()+8.5;
            if(aim(n,a,x,65,z))a[4]=2;return a;
        }
        if(n.pocket.menu()==Pocket.Menu.CLOSED){
            if(task==8||task==9||task==10||task==17)a[6]=4;
            else if(aim(n,a,n.goal.x(),65.5,n.goal.z()))a[4]=2;
            return a;
        }
        if(c.step==0){recipe(c,task==17?8:task);c.step=1;}
        if(!c.clicks.isEmpty()){int[] next=c.clicks.remove();a[6]=next[0];a[7]=next[1];return a;}
        if(task==14){a[6]=3;a[7]=38;}
        if(task==17&&c.step==1){recipe(c,17);c.step=2;}
        return a;
    }
}
