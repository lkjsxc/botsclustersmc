package org.botsclustersmc.holdout;

import org.botsclustersmc.core.*;
import org.botsclustersmc.plugin.*;
import org.botsclustersmc.training.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Independent stochastic neural evaluation. No scripted gameplay or optimizer. */
public final class FrozenPolicyExam extends RuntimePlugin {
    private int cases,count;private List<Integer> tasks;private long examSeed;
    private final AtomicInteger prepared=new AtomicInteger();
    private final Map<Long,TrainingEnvironment.Session> sessions=new ConcurrentHashMap<>();
    private final Map<Long,Outcome> outcomes=new ConcurrentHashMap<>();
    private final AtomicBoolean written=new AtomicBoolean();
    private record Outcome(int task,long seed,boolean success,long ticks,double distance) {}
    private long started;
    @Override public boolean training(){return true;}
    @Override public Policy initialPolicy()throws Exception {
        if(!Boolean.getBoolean("bcmc.holdout")||!Files.isRegularFile(Path.of(".botsclustersmc-exam")))
            throw new IllegalStateException("An explicitly isolated exam server is required.");
        return PolicyFile.read(getDataFolder().toPath().resolve("policy.bcmc"));
    }
    @Override protected void initialize() {
        cases=bounded("cases-per-task",64,1,256);tasks=List.copyOf(getConfig().getIntegerList("tasks"));
        if(tasks.isEmpty()||tasks.size()>18||new HashSet<>(tasks).size()!=tasks.size())throw new IllegalArgumentException("Distinct task IDs are required");
        for(int task:tasks)Task.at(task);count=cases*tasks.size();
        if(count>2048)throw new IllegalArgumentException("Holdout is bounded to 2048 trials");
        examSeed=getConfig().getLong("seed",19517);started=System.nanoTime();
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(this,t->{
            for(int i=0;i<4;i++) {
                int actor=prepared.getAndIncrement();if(actor>=count)return;
                World world=Objects.requireNonNull(Bukkit.getWorld("world"));
                ArenaLayout arena=ArenaLayout.forActor(actor,count,64);
                Location spawn=new Location(world,arena.x()+7.5,65,arena.z()+3.5);
                LoadedChunks.use(this,spawn,chunk->{
                    chunk.addPluginChunkTicket(this);TrainingEnvironment.build(world,arena);
                    TrainingEnvironment.Session session=new TrainingEnvironment.Session(arena);
                    int task=tasks.get(actor/cases);long seed=examSeed+task*1000003L+(actor%cases)*104729L;
                    session.lesson=new Course.Lesson(actor+1,Task.at(task),1,seed,Course.Kind.EXAM);
                    sessions.put((long)actor,session);
                    requestSpawn(actor,spawn,new Goal(Task.at(task),spawn.getX(),65,spawn.getZ()+3,actor+1,1,Task.at(task).horizon()));
                },this::fail);
            }
        },1,1);
        io.scheduleAtFixedRate(()->{
            try {
                if(failed.get()!=null)throw new IllegalStateException("Exam runtime failed",failed.get());
                if(System.nanoTime()-started>TimeUnit.SECONDS.toNanos(240+count/4))throw new IllegalStateException("Incomplete exam timed out");
                if(outcomes.size()==count&&written.compareAndSet(false,true))finishReport();
            }catch(Exception failure){
                fail(failure);try{Files.writeString(getDataFolder().toPath().resolve("exam-failed.txt"),failure.toString());}catch(Exception ignored){}
                Bukkit.getGlobalRegionScheduler().run(this,t->Bukkit.shutdown());
            }
        },1,1,TimeUnit.SECONDS);
    }
    @Override protected void spawned(Npc npc) {
        TrainingEnvironment.Session session=sessions.get(npc.id);npc.context=session;
        TrainingEnvironment.reset(this,npc,session,session.lesson);
    }
    @Override public boolean greedy(Npc npc){return false;}
    @Override public boolean canPickup(Npc npc,String token){return npc.token().equals(token);}
    @Override public boolean pickupEnabled(Npc npc){int task=npc.goal.task().ordinal();return task==6||task==12||task==17;}
    @Override public boolean canChange(Npc npc,Block block){
        TrainingEnvironment.Session session=sessions.get(npc.id);
        return session!=null&&block.getWorld()==npc.anchor.getWorld()&&block.getY()>=65&&block.getY()<70
            &&session.arena.contains(block.getX()+.5,block.getY(),block.getZ()+.5)&&WorldActions.owned(block.getLocation());
    }
    @Override public boolean observed(Npc npc,Npc.Applied previous,Frame next) {
        if(previous.result().policyVersion()!=policy.updates())throw new IllegalStateException("Frozen policy identity changed");
        TrainingEnvironment.Session session=sessions.get(npc.id);
        boolean success=TrainingEnvironment.success(npc,session,previous,next);
        long elapsed=next.tick()-npc.episodeStart;
        boolean terminal=success||elapsed>=npc.goal.horizon()||!session.arena.contains(next.x(),next.y(),next.z());
        if(!terminal)return true;
        Outcome result=new Outcome(npc.goal.task().ordinal(),session.lesson.seed(),success,elapsed,next.distance());
        if(outcomes.putIfAbsent(npc.id,result)!=null)throw new IllegalStateException("Duplicate trial completion");
        npc.paused=true;npc.discardPending();return false;
    }
    private void finishReport()throws Exception {
        Map<Integer,Integer> passed=new LinkedHashMap<>();for(int task:tasks)passed.put(task,0);
        StringJoiner trials=new StringJoiner(","),summary=new StringJoiner(",");
        for(long actor=0;actor<count;actor++) {
            Outcome o=Objects.requireNonNull(outcomes.get(actor));if(o.success())passed.merge(o.task(),1,Integer::sum);
            trials.add(String.format(Locale.ROOT,"{\"actor\":%d,\"task\":%d,\"seed\":%d,\"success\":%s,\"elapsed_ticks\":%d,\"distance\":%.6f}",actor,o.task(),o.seed(),o.success(),o.ticks(),o.distance()));
        }
        for(int task:tasks)summary.add(String.format(Locale.ROOT,"{\"task\":%d,\"label\":\"%s\",\"passed\":%d,\"cases\":%d}",task,Task.at(task).label(),passed.get(task),cases));
        String report=String.format(Locale.ROOT,"{\"complete\":true,\"schema\":\"%s\",\"policy_updates\":%d,\"policy_trained_samples\":%d,\"new_training_samples\":0,\"stochastic\":true,\"cases_per_task\":%d,\"seed\":%d,\"epoch_millis\":%d,\"tasks\":[%s],\"trials\":[%s]}\n",Schema.ID,policy.updates(),policy.samples(),cases,examSeed,System.currentTimeMillis(),summary,trials);
        PolicyFile.atomicWrite(getDataFolder().toPath().resolve("exam-result.json"),report.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        getLogger().info("FROZEN POLICY EXAM: "+summary+", new training samples=0, policy="+policy.updates());
        Bukkit.getGlobalRegionScheduler().run(this,t->Bukkit.shutdown());
    }
}
