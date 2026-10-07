package org.botsclustersmc.commons;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.botsclustersmc.core.*;
import org.botsclustersmc.plugin.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityChangeBlockEvent;

/** Independent two-citizen neural commons exam; no teacher, role router, curriculum or learner. */
public final class CommonsExam extends RuntimePlugin {
    private int cases,horizon,expectedRooms;private long started,bankSeed;
    private final Map<Integer,CommonsRoom> rooms=new ConcurrentHashMap<>();
    private final Map<Long,CommonsRoom> actorRooms=new ConcurrentHashMap<>();
    private final Map<Integer,CommonsResult> results=new ConcurrentHashMap<>();
    private final List<CommonsCase> bank=new ArrayList<>();
    private final AtomicInteger prepared=new AtomicInteger();private final AtomicBoolean written=new AtomicBoolean();
    @Override public boolean training(){return true;} // Explicitly invulnerable test bodies, NOT a learner.
    @Override protected Policy initialPolicy()throws Exception {
        if(!Boolean.getBoolean("bcmc.commons")||!Files.isRegularFile(Path.of(".botsclustersmc-commons")))
            throw new IllegalStateException("An explicitly owned disposable commons exam is required");
        return PolicyFile.read(getDataFolder().toPath().resolve("policy.bcmc"));
    }
    @Override protected void initialize() {
        cases=bounded("cases-per-condition",4,4,32);if(cases%4!=0)throw new IllegalArgumentException("Cases must balance supply swap and station mirror in groups of four");
        horizon=bounded("horizon-ticks",3000,40,3000);bankSeed=getConfig().getLong("seed",2026100711L);started=System.nanoTime();
        for(var condition:CommonsCase.Condition.values())for(int i=0;i<cases;i++)bank.add(CommonsCase.of(i,condition,bankSeed));
        expectedRooms=cases*4;
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(this,task->{
            try {
                if(failed.get()!=null){task.cancel();return;}
                for(int i=0;i<2;i++) {
                    int trial=prepared.getAndIncrement();if(trial>=bank.size()){task.cancel();return;}
                    CommonsCase specification=bank.get(trial);World world=Objects.requireNonNull(Bukkit.getWorld("world"));
                    for(int member=0;member<specification.rooms();member++) {
                        int slot=member;CommonsRoom room=new CommonsRoom(this,specification,trial*2+slot,trial,slot,world);
                        if(rooms.putIfAbsent(room.id,room)!=null)throw new IllegalStateException("Duplicate room");
                        LoadedChunks.use(this,room.origin(),chunk->{
                            try {
                                chunk.addPluginChunkTicket(this);room.build();
                                for(int m=0;m<2;m++)if(specification.rooms()==1||m==slot) {
                                    long actor=trial*2L+m;actorRooms.put(actor,room);Location at=room.bench();
                                    // Both actors retain this SAME task and workbench target for the whole trial.
                                    requestSpawn(actor,room.spawn(m),new Goal(Task.CRAFT_WOOD_PICK,at.getX()+.5,65,at.getZ()+.5,trial+1,1,horizon));
                                }
                            }catch(Throwable failure){fail(failure);}
                        },this::fail);
                    }
                }
            }catch(Throwable failure){fail(failure);}
        },1,1);
        io.scheduleAtFixedRate(()->{
            try {
                if(failed.get()!=null)throw new IllegalStateException("Commons runtime failed",failed.get());
                if(System.nanoTime()-started>TimeUnit.SECONDS.toNanos(240+horizon/10))throw new IllegalStateException("Incomplete commons exam timed out");
                if(results.size()==expectedRooms&&written.compareAndSet(false,true))finishReport();
            }catch(Throwable failure){
                fail(failure);try{PolicyFile.atomicWrite(getDataFolder().toPath().resolve("commons-failed.txt"),failure.toString().getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){}
                Bukkit.getGlobalRegionScheduler().run(this,t->Bukkit.shutdown());
            }
        },1,1,TimeUnit.SECONDS);
    }
    int horizon(){return horizon;}
    @Override protected void spawned(Npc npc){Objects.requireNonNull(actorRooms.get(npc.id)).add(npc,(int)(npc.id%2));}
    @Override protected void startNpc(Npc npc){} // The room's admission barrier starts all members together.
    @Override public boolean greedy(Npc npc){return false;}
    @Override public boolean pickupEnabled(Npc npc){return true;}
    @Override public boolean canPickup(Npc npc,String token) {
        CommonsRoom room=actorRooms.get(npc.id);if(room==null||token==null)return false;
        for(int m=0;m<2;m++)if(room.specification.rooms()==1||m==room.member)
            if(token.equals(run+":"+(room.trial*2L+m)+":"+(room.trial+1)))return true;
        return false;
    }
    @Override public boolean canChange(Npc npc,Block block) {
        CommonsRoom room=actorRooms.get(npc.id);
        return room!=null&&block.getWorld()==room.world&&WorldActions.owned(block.getLocation())
            &&block.getX()==room.bank().getBlockX()&&block.getY()==65&&block.getZ()==room.bank().getBlockZ();
    }
    @EventHandler public void fixedInfrastructure(EntityChangeBlockEvent event) {
        // This is a disclosed fixed-station resource-combination task, not survival.
        if(event.getEntity().getPersistentDataContainer().has(provenance))event.setCancelled(true);
    }
    @Override public boolean observed(Npc npc,Npc.Applied previous,Frame next) {
        if(previous.result().policyVersion()!=policy.updates())throw new IllegalStateException("Frozen policy changed");
        ((CommonsRoom)npc.context).observe(npc,previous);return true;
    }
    void completed(int id,CommonsResult result) {
        if(!rooms.containsKey(id)||results.putIfAbsent(id,result)!=null)throw new IllegalStateException("Duplicate/unknown room completion");
    }
    @Override protected Map<String,Object> extraStatus() {
        return Map.of("mode","commons-exam","commons_rooms_total",expectedRooms,"commons_rooms_completed",results.size(),"new_training_samples",0);
    }
    private void finishReport()throws Exception {
        StringJoiner trials=new StringJoiner(","),summaries=new StringJoiner(",");int[] passed=new int[3];
        for(int trial=0;trial<bank.size();trial++) {
            CommonsCase c=bank.get(trial);CommonsStock total=CommonsStock.EMPTY,stock=CommonsStock.EMPTY,lost=CommonsStock.EMPTY;long sticks=0,crafted=0;int elapsed=0;boolean delivered=true;StringJoiner cells=new StringJoiner(",");
            for(int m=0;m<c.rooms();m++) {
                var r=Objects.requireNonNull(results.get(trial*2+m));total=total.plus(r.total());stock=stock.plus(r.bank());lost=lost.plus(r.lost());sticks+=r.craftedSticks();crafted+=r.craftedPicks();delivered&=r.end().equals("delivered");elapsed=Math.max(elapsed,r.ticks());cells.add(r.json());
            }
            boolean success=CommonsStock.delivered(total,stock,sticks,crafted)&&delivered&&c.rooms()==1;if(success)passed[c.condition().ordinal()]++;
            trials.add("{\"trial\":"+trial+",\"case\":"+c.index()+",\"condition\":\""+c.condition().label()+"\",\"seed\":"+c.seed()
                +",\"supplies_owner\":"+c.owner()+",\"mirror\":"+c.mirror()+",\"success\":"+success+",\"elapsed_ticks\":"+elapsed
                +",\"total\":"+total.json()+",\"bank\":"+stock.json()+",\"lost_stock\":"+lost.json()+",\"lost_wood_units\":"+lost.woodUnits()+",\"rooms\":["+cells+"]}");
        }
        for(var c:CommonsCase.Condition.values())summaries.add("{\"condition\":\""+c.label()+"\",\"cases\":"+cases+",\"passed\":"+passed[c.ordinal()]+"}");
        String report="{\"complete\":true,\"protocol\":\"commons-fixed-stations-v2\",\"schema\":\""+Schema.ID+"\",\"seed\":"+bankSeed
            +",\"policy_updates\":"+policy.updates()+",\"policy_trained_samples\":"+policy.samples()+",\"new_training_samples\":0,\"stochastic\":true"
            +",\"cases_per_condition\":"+cases+",\"horizon_ticks\":"+horizon+",\"conditions\":["+summaries+"],\"trials\":["+trials+"]}\n";
        PolicyFile.atomicWrite(getDataFolder().toPath().resolve("commons-result.json"),report.getBytes(StandardCharsets.UTF_8));
        getLogger().info("COMMONS EXAM COMPLETE "+summaries+"; all actions neural, new training samples=0");
        Bukkit.getGlobalRegionScheduler().run(this,t->Bukkit.shutdown());
    }
}
