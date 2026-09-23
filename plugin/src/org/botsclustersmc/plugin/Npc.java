package org.botsclustersmc.plugin;

import org.botsclustersmc.core.*;
import org.bukkit.*;
import org.bukkit.entity.Zombie;
import org.bukkit.util.Vector;
import java.util.*;
import java.util.concurrent.atomic.*;

/** Mutable body state is exclusively owned by this entity's region thread. */
public final class Npc {
    public final RuntimePlugin plugin;public final long id;public final Zombie entity;public final Pocket pocket=new Pocket();
    public final Map<String,Long> broken=new HashMap<>(),collected=new HashMap<>(),placed=new HashMap<>();
    public final float[] previousKinematics=new float[16];public final RandomSource rng;
    public final AtomicReference<Goal> requestedGoal=new AtomicReference<>();public final AtomicBoolean remove=new AtomicBoolean();
    private final AtomicReference<InferencePool.Result> mailbox=new AtomicReference<>();private final AtomicReference<Throwable> error=new AtomicReference<>();
    public Goal goal;public Location container;public String mining;public int miningTicks;public volatile long tick;public long episodeStart;
    public int[] action=Schema.IDLE.clone();public Object context;
    private boolean wasPaused;private int leasedX=Integer.MIN_VALUE,leasedZ=Integer.MIN_VALUE;private java.util.UUID leasedWorld;
    private long requestId,requestedTick,nextDecision;private boolean waiting;private Frame requestFrame;private Applied applied;
    public volatile boolean resetting=true,ready,paused;public volatile String status="initializing";public volatile double x,y,z;
    public volatile long decisions,lastStepNanos;public volatile double horizontalTravel;public volatile World currentWorld;public final Location anchor;
    public record Applied(Frame frame,InferencePool.Result result){}
    public Npc(RuntimePlugin plugin,long id,Zombie entity,Goal goal){this.plugin=plugin;this.id=id;this.entity=entity;this.goal=goal;rng=new RandomSource(id*7919+plugin.seed);anchor=entity.getLocation().clone();currentWorld=anchor.getWorld();x=anchor.getX();y=anchor.getY();z=anchor.getZ();}
    public String token(){return plugin.run+":"+id+":"+goal.episode();}
    public void start(){entity.getScheduler().runAtFixedRate(plugin,t->{try{step();}catch(Throwable failure){status="failed";paused=true;plugin.fail(failure);}},()->{status="retired";plugin.retired.increment();plugin.npcs.remove(id);plugin.released(id);if(plugin.training()&&!remove.get()&&plugin.isEnabled())plugin.fail(new IllegalStateException("Training NPC retired unexpectedly: "+id));},1,1);}
    private void step(){
        tick++;lastStepNanos=System.nanoTime();
        if(remove.get()){plugin.npcs.remove(id);plugin.released(id);entity.remove();status="removed";return;}
        if(resetting){status="resetting";return;}
        if(plugin.failed.get()!=null||plugin.paused.get()||paused){
            if(!wasPaused){plugin.interrupted(this);discardPending();plugin.abandoned.increment();wasPaused=true;}
            entity.setVelocity(new Vector(0,entity.getVelocity().getY(),0));status="paused";return;
        }
        wasPaused=false;
        if(error.get()!=null)throw new IllegalStateException("inference worker failed",error.get());
        Goal requested=requestedGoal.getAndSet(null);
        if(requested!=null){plugin.abandoned.increment();discardPending();goal=requested;episodeStart=tick;Arrays.fill(previousKinematics,0);}
        InferencePool.Result result=mailbox.getAndSet(null);boolean justApplied=false;
        if(result!=null&&result.request()==requestId){waiting=false;applied=new Applied(requestFrame,result);action=result.actions();nextDecision=tick+Schema.DECISION_TICKS;justApplied=true;}
        if(applied!=null&&tick>=nextDecision){
            Frame next=Sensors.capture(this);Applied completed=applied;applied=null;decisions++;plugin.transitions.increment();
            if(!plugin.observed(this,completed,next)){status="episode-boundary";return;}
            requestFrame=next;
        }
        if(applied==null&&!waiting){
            if(!ready){if(!plugin.ready(this)){status="waiting-for-lesson";return;}ready=true;}
            if(requestFrame==null)requestFrame=Sensors.capture(this);
            Policy policy=plugin.policyFor(this);long id=++requestId;requestedTick=requestFrame.tick();long seed=rng.nextLong();
            waiting=plugin.inference.offer(new InferencePool.Request(id,policy,requestFrame.observation(),requestFrame.mask(),seed,plugin.greedy(this),mailbox::set,error::set,System.nanoTime()));
            if(!waiting)plugin.inferenceRejected.increment();
        }
        WorldActions.tick(this,action,justApplied);
        Location location=entity.getLocation();int cx=location.getBlockX()>>4,cz=location.getBlockZ()>>4;if(cx!=leasedX||cz!=leasedZ||!location.getWorld().getUID().equals(leasedWorld)){plugin.leases.follow(id,location);leasedX=cx;leasedZ=cz;leasedWorld=location.getWorld().getUID();}if(currentWorld==location.getWorld()){double distance=Math.hypot(location.getX()-x,location.getZ()-z);if(distance<4)horizontalTravel+=distance;}currentWorld=location.getWorld();x=location.getX();y=location.getY();z=location.getZ();status=waiting?"inference-wait":"acting";
    }
    public void discardPending(){requestId++;waiting=false;mailbox.set(null);error.set(null);applied=null;requestFrame=null;action=Schema.IDLE.clone();ready=false;}
    /** Callback and supplies run on the owning entity scheduler after teleport completion. */
    public void reset(Goal newGoal,Location target,Runnable supplies){
        resetting=true;discardPending();entity.setVelocity(new Vector());
        entity.teleportAsync(target).whenComplete((ok,failure)->{
            if(failure!=null||!Boolean.TRUE.equals(ok)){plugin.fail(new IllegalStateException("NPC reset teleport failed: id="+id+", valid="+entity.isValid()+", dead="+entity.isDead()+", passenger="+entity.isInsideVehicle(),failure));return;}
            entity.getScheduler().run(plugin,t->{goal=newGoal;episodeStart=tick;pocket.clear();broken.clear();collected.clear();placed.clear();mining=null;miningTicks=0;container=null;Arrays.fill(previousKinematics,0);supplies.run();Location at=entity.getLocation();x=at.getX();y=at.getY();z=at.getZ();currentWorld=at.getWorld();resetting=false;ready=true;status="acting";},()->plugin.fail(new IllegalStateException("NPC retired during reset")));
        });
    }
    public void begin(Goal goal){this.goal=goal;episodeStart=tick;resetting=false;ready=true;}
}
