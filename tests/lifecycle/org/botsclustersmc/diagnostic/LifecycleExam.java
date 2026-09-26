package org.botsclustersmc.diagnostic;

import com.google.gson.Gson;
import org.botsclustersmc.core.*;
import org.botsclustersmc.plugin.*;
import org.botsclustersmc.training.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Fixed-policy lifecycle diagnostic. No optimizer, scripted actions, or certificates. */
public final class LifecycleExam extends RuntimePlugin {
    private static final int[] ORDER={11,12,11};
    private static final long EXAM_SALT=0x677d815fab35L,PROBE_SALT=0x173a6ae017a1L;
    private static final class Trial {
        final TrainingEnvironment.Session session;int phase;float[] initial;boolean[] mask;
        int decisions,minTicks=Integer.MAX_VALUE,maxTicks;long sumTicks;long actionSeed;
        Trial(ArenaLayout arena){session=new TrainingEnvironment.Session(arena);}
    }
    private final Map<Long,Trial> trials=new ConcurrentHashMap<>();
    private final Map<Long,Map<String,Object>> results=new ConcurrentHashMap<>();
    private final AtomicInteger prepared=new AtomicInteger();private final AtomicBoolean written=new AtomicBoolean();
    private int cases,count;private long studySeed,started;
    @Override public boolean training(){return true;}
    @Override protected Policy initialPolicy()throws Exception {
        if(!Boolean.getBoolean("bcmc.lifecycle")||!Files.isRegularFile(Path.of(".botsclustersmc-lifecycle")))
            throw new IllegalStateException("Requires an isolated lifecycle study server");
        return PolicyFile.read(getDataFolder().toPath().resolve("policy.bcmc"));
    }
    @Override protected void initialize(){
        cases=bounded("cases-per-arm",32,1,64);count=cases*2;studySeed=getConfig().getLong("seed",2026092641L);started=System.nanoTime();
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(this,t->{
            for(int i=0;i<4;i++){
                int actor=prepared.getAndIncrement();if(actor>=count)return;
                World world=Objects.requireNonNull(Bukkit.getWorld("world"));ArenaLayout arena=ArenaLayout.forActor(actor,count,64);
                Location at=new Location(world,arena.x()+8.5,65,arena.z()+5.5);
                LoadedChunks.use(this,at,chunk->{
                    chunk.addPluginChunkTicket(this);TrainingEnvironment.build(world,arena);trials.put((long)actor,new Trial(arena));
                    requestSpawn(actor,at,new Goal(Task.CRAFT_WOOD_PICK,at.getX(),65,at.getZ()+3,actor+1,1,Task.CRAFT_WOOD_PICK.horizon()));
                },this::fail);
            }
        },1,1);
        io.scheduleAtFixedRate(()->{
            try{
                if(failed.get()!=null)throw new IllegalStateException("Lifecycle runtime failed",failed.get());
                if(System.nanoTime()-started>TimeUnit.SECONDS.toNanos(600))throw new IllegalStateException("Lifecycle study timeout");
                if(results.size()==count*ORDER.length&&written.compareAndSet(false,true))finish();
            }catch(Exception error){
                fail(error);try{PolicyFile.atomicWrite(getDataFolder().toPath().resolve("lifecycle-failed.txt"),error.toString().getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){}
                Bukkit.getGlobalRegionScheduler().run(this,t->Bukkit.shutdown());
            }
        },1,1,TimeUnit.SECONDS);
    }
    private void resetPhase(Npc npc){
        Trial trial=trials.get(npc.id);int task=ORDER[trial.phase],sample=(int)(npc.id%cases),arm=(int)(npc.id/cases);
        long resetSeed=studySeed+task*1000003L+sample*104729L;
        Course.Kind kind=arm==0?Course.Kind.EXAM:Course.Kind.PROBE;
        // Match the actual reset RNG, not merely the externally reported seed.
        if(kind==Course.Kind.PROBE)resetSeed^=EXAM_SALT^PROBE_SALT;
        trial.session.lesson=new Course.Lesson(1+npc.id+trial.phase*(long)count,Task.at(task),1,resetSeed,kind);
        trial.actionSeed=studySeed^((long)sample*7919)^((long)task*65537);
        npc.rng.restore(trial.actionSeed);trial.initial=null;trial.mask=null;trial.decisions=0;trial.minTicks=Integer.MAX_VALUE;trial.maxTicks=0;trial.sumTicks=0;
        TrainingEnvironment.reset(this,npc,trial.session,trial.session.lesson);
    }
    @Override protected void spawned(Npc npc){npc.context=trials.get(npc.id).session;resetPhase(npc);}
    @Override public boolean ready(Npc npc){resetPhase(npc);return false;}
    @Override public boolean greedy(Npc npc){return false;}
    @Override public boolean pickupEnabled(Npc npc){return true;}
    @Override public boolean canPickup(Npc npc,String token){return npc.token().equals(token);}
    @Override public boolean canChange(Npc npc,Block b){
        Trial trial=trials.get(npc.id);
        return trial!=null&&b.getWorld()==npc.anchor.getWorld()&&b.getY()>=65&&b.getY()<70
            &&trial.session.arena.contains(b.getX()+.5,b.getY(),b.getZ()+.5)&&WorldActions.owned(b.getLocation());
    }
    @Override public boolean observed(Npc npc,Npc.Applied previous,Frame next){
        Trial trial=trials.get(npc.id);int task=ORDER[trial.phase];
        if(npc.goal.task().ordinal()!=task||previous.frame().observation()[16+task]!=1||next.observation()[16+task]!=1)
            throw new IllegalStateException("Lesson/action observation identity mismatch");
        if(previous.result().policyVersion()!=policy.updates())throw new IllegalStateException("Policy changed during study");
        if(trial.initial==null){trial.initial=previous.frame().observation().clone();trial.mask=previous.frame().mask().clone();}
        int ticks=Math.toIntExact(next.tick()-previous.frame().tick());trial.decisions++;trial.sumTicks+=ticks;
        trial.minTicks=Math.min(trial.minTicks,ticks);trial.maxTicks=Math.max(trial.maxTicks,ticks);
        boolean success=TrainingEnvironment.success(npc,trial.session,previous,next);long elapsed=next.tick()-npc.episodeStart;
        if(!success&&elapsed<npc.goal.horizon()&&trial.session.arena.contains(next.x(),next.y(),next.z()))return true;
        Map<String,Object> result=new LinkedHashMap<>();result.put("actor",npc.id);result.put("case",npc.id%cases);result.put("arm",trial.session.lesson.kind().name());
        result.put("phase",trial.phase);result.put("task",task);result.put("reset_seed",trial.session.lesson.seed());result.put("action_seed",trial.actionSeed);
        result.put("success",success);result.put("elapsed_ticks",elapsed);result.put("decisions",trial.decisions);result.put("min_transition_ticks",trial.minTicks);
        result.put("max_transition_ticks",trial.maxTicks);result.put("sum_transition_ticks",trial.sumTicks);result.put("initial",trial.initial);result.put("initial_mask",trial.mask);
        result.put("final_crafted",new TreeMap<>(npc.pocket.crafted));result.put("final_broken",new TreeMap<>(npc.broken));result.put("final_collected",new TreeMap<>(npc.collected));
        result.put("final_x",next.x()-trial.session.arena.x());result.put("final_z",next.z()-trial.session.arena.z());result.put("final_yaw",next.yaw());result.put("final_pitch",next.pitch());
        if(results.putIfAbsent(npc.id+trial.phase*(long)count,result)!=null)throw new IllegalStateException("Duplicate completion");
        npc.discardPending();trial.phase++;if(trial.phase==ORDER.length)npc.paused=true;
        return false;
    }
    @Override protected Map<String,Object> extraStatus(){return Map.of("lifecycle_expected_trials",count*ORDER.length,"lifecycle_completed_trials",results.size());}
    private void finish()throws Exception{
        List<Map<String,Object>> ordered=new ArrayList<>();for(long key=0;key<count*(long)ORDER.length;key++)ordered.add(Objects.requireNonNull(results.get(key)));
        Map<String,Object> report=new LinkedHashMap<>();report.put("complete",true);report.put("scope","paired-reset-lifecycle; not training or certification");
        report.put("policy_updates",policy.updates());report.put("policy_samples",policy.samples());report.put("new_training_samples",0);report.put("seed",studySeed);
        report.put("cases_per_arm",cases);report.put("order",ORDER);report.put("trials",ordered);
        PolicyFile.atomicWrite(getDataFolder().toPath().resolve("lifecycle-result.json"),new Gson().toJson(report).getBytes(StandardCharsets.UTF_8));
        getLogger().info("Lifecycle study completed: "+ordered.size()+" trials, zero training samples");
        Bukkit.getGlobalRegionScheduler().run(this,t->Bukkit.shutdown());
    }
}
