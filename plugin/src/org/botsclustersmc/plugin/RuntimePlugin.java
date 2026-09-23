package org.botsclustersmc.plugin;

import org.botsclustersmc.core.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Portable public-API host shared by inference and training; no NMS or network clients. */
public abstract class RuntimePlugin extends JavaPlugin implements Listener,CommandExecutor {
    public final ConcurrentHashMap<Long,Npc> npcs=new ConcurrentHashMap<>();
    public final AtomicReference<Throwable> failed=new AtomicReference<>();public final AtomicBoolean paused=new AtomicBoolean();
    public final LongAdder transitions=new LongAdder(),abandoned=new LongAdder(),retired=new LongAdder(),sensorNanos=new LongAdder(),inferenceRejected=new LongAdder(),ambientCombustions=new LongAdder();
    public final String run=UUID.randomUUID().toString();public long seed;public NamespacedKey provenance;public InferencePool inference;
    private final Map<Long,Long> lastDecisions=new HashMap<>();private long lastStatusNanos,lastSamples,lastTransitions,lastCpu;
    protected volatile Policy policy;protected ScheduledExecutorService io;protected int maximum;protected final AtomicLong nextId=new AtomicLong();
    protected final AtomicInteger pendingSpawns=new AtomicInteger();protected final ConcurrentLinkedQueue<Spawn> spawnQueue=new ConcurrentLinkedQueue<>();
    public ChunkLeases leases;protected int maxChunks;
    private final AtomicInteger admitted=new AtomicInteger();private final Set<Long> issued=ConcurrentHashMap.newKeySet();
    protected record Spawn(long id,Location at,Goal goal){}
    protected abstract Policy initialPolicy()throws Exception;
    protected abstract void initialize()throws Exception;
    protected void closing()throws Exception{}
    public boolean training(){return false;}
    @Override public final void onEnable(){
        try {
            PolicyFile.managedPath(getDataFolder().toPath().resolve("config.yml"));
            saveDefaultConfig();seed=getConfig().getLong("seed",7);provenance=new NamespacedKey(this,"provenance");
            maximum=bounded("max-agents",4096,1,10000);maxChunks=bounded("max-loaded-chunks",2048,1,10000);leases=new ChunkLeases(this,maxChunks);
            RuntimeBudget budget=RuntimeBudget.automatic(Runtime.getRuntime().availableProcessors());
            int threads=bounded("inference-threads",budget.inferenceThreads(),1,128);
            policy=initialPolicy();inference=new InferencePool(threads,maximum);
            io=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"bcmc-io");t.setDaemon(true);return t;});
            getServer().getPluginManager().registerEvents(this,this);Objects.requireNonNull(getCommand("bots")).setExecutor(this);
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(this,t->admit(),1,1);
            io.scheduleAtFixedRate(()->{try{writeStatus();}catch(Throwable e){fail(e);}},1,5,TimeUnit.SECONDS);
            initialize();getLogger().info("Ready: in-JVM NPC inference, capacity="+maximum+", inferenceThreads="+threads+", schema="+Schema.ID);
        }catch(Throwable e){fail(e);getServer().getPluginManager().disablePlugin(this);}
    }
    public int bounded(String key,int fallback,int min,int max){int n=getConfig().getInt(key,fallback);if(n<min||n>max)throw new IllegalArgumentException(key+" must be "+min+".."+max);return n;}
    public Policy policyFor(Npc npc){return policy;}
    public boolean greedy(Npc npc){return getConfig().getBoolean("greedy",false);}
    public boolean ready(Npc npc){return true;}
    public boolean observed(Npc npc,Npc.Applied previous,Frame next){return true;}
    public void interrupted(Npc npc){}
    public boolean pickupEnabled(Npc npc){return getConfig().getBoolean("world-edits",false);}
    public boolean canPickup(Npc npc,String token){return getConfig().getBoolean("world-edits",false);}
    public boolean canChange(Npc npc,Block block){
        if(!getConfig().getBoolean("world-edits",false)||!WorldActions.owned(block.getLocation()))return false;
        int radius=getConfig().getInt("edit-radius",32);return block.getWorld()==npc.anchor.getWorld()&&block.getLocation().distanceSquared(npc.anchor)<=radius*(double)radius;
    }
    protected void spawned(Npc npc){npc.begin(npc.goal);}
    protected void startNpc(Npc npc){npc.start();}
    public long requestSpawn(Location at,Goal goal){
        if(failed.get()!=null)throw new IllegalStateException("runtime failed");
        long id=nextId.getAndIncrement();requestSpawn(id,at,goal);return id;
    }
    protected void requestSpawn(long id,Location at,Goal goal){
        synchronized(spawnQueue){if(admitted.get()>=maximum||!issued.add(id))throw new IllegalStateException("agent capacity/identity exceeded");admitted.incrementAndGet();nextId.accumulateAndGet(id+1,Math::max);spawnQueue.add(new Spawn(id,at.clone(),goal));}
    }
    public void released(long id){if(issued.remove(id))admitted.decrementAndGet();if(leases!=null)leases.release(id);}
    private void admit(){
        if(failed.get()!=null)return;
        for(int i=0;i<8&&pendingSpawns.get()<16;i++){
            Spawn spawn=spawnQueue.poll();if(spawn==null)return;pendingSpawns.incrementAndGet();Location at=spawn.at();
            LoadedChunks.use(this,at,chunk->{try{
                    if(at.getWorld().getDifficulty()==Difficulty.PEACEFUL)throw new IllegalStateException("NPC bodies require a non-peaceful world; the plugin never changes your world difficulty.");
                    if(!issued.contains(spawn.id()))return;
                    leases.follow(spawn.id(),at);
                    Zombie zombie=at.getWorld().spawn(at,Zombie.class,CreatureSpawnEvent.SpawnReason.CUSTOM,false,z->{
                        z.setAdult();z.setPersistent(false);z.setRemoveWhenFarAway(false);z.setSilent(true);z.setInvulnerable(training());
                        z.setShouldBurnInDay(false);z.setCanBreakDoors(false);z.setCanPickupItems(false);z.setCollidable(false);z.setAI(true);z.setAware(false);
                        Bukkit.getMobGoals().removeAllGoals(z);z.setTarget(null);z.customName(Component.text("bcmc"+spawn.id()));z.setCustomNameVisible(false);
                        z.getPersistentDataContainer().set(provenance,PersistentDataType.STRING,run);
                    });
                    Npc npc=new Npc(this,spawn.id(),zombie,spawn.goal());npcs.put(spawn.id(),npc);
                    // Spawn registration finishes before the first owning-entity callback.
                    zombie.getScheduler().run(this,first->{try{if(!issued.contains(npc.id)){npcs.remove(npc.id);zombie.remove();released(npc.id);return;}if(!zombie.isValid())throw new IllegalStateException("NPC spawn was cancelled: "+npc.id);spawned(npc);startNpc(npc);}catch(Throwable e){fail(e);}},()->fail(new IllegalStateException("NPC retired before initialization: "+npc.id)));
                }catch(Throwable e){released(spawn.id());fail(e);}finally{pendingSpawns.decrementAndGet();}
            },failure->{pendingSpawns.decrementAndGet();released(spawn.id());fail(failure);});
        }
    }
    /** Some server builds ignore Zombie.shouldBurnInDay in their daylight tag path.
     * Cancel only unattributed combustion for our bodies; block/entity fire remains real. */
    @EventHandler(ignoreCancelled=true) public void ambientCombustion(EntityCombustEvent e){
        if(e.getClass()==EntityCombustEvent.class&&e.getEntity() instanceof Zombie
                &&run.equals(e.getEntity().getPersistentDataContainer().get(provenance,PersistentDataType.STRING))){
            e.setCancelled(true);ambientCombustions.increment();
        }
    }
    @EventHandler(ignoreCancelled=true) public void merging(ItemMergeEvent e){
        if(e.getEntity().getPersistentDataContainer().has(provenance,PersistentDataType.STRING)||e.getTarget().getPersistentDataContainer().has(provenance,PersistentDataType.STRING))e.setCancelled(true);
    }
    @EventHandler(ignoreCancelled=true) public void transforming(EntityTransformEvent e){if(e.getEntity().getPersistentDataContainer().has(provenance,PersistentDataType.STRING))e.setCancelled(true);}
    public void fail(Throwable e){if(failed.compareAndSet(null,e)){getLogger().log(java.util.logging.Level.SEVERE,"Runtime stopped accepting work; no fallback policy",e);paused.set(true);}}
    protected Map<String,Object> extraStatus(){return Map.of();}
    protected synchronized void writeStatus()throws Exception{
        Map<String,Object> status=new LinkedHashMap<>();Policy p=policy;
        status.put("mode",training()?"training":"inference");status.put("state",failed.get()!=null?"failed":paused.get()?"paused":"running");status.put("schema",Schema.ID);
        status.put("policy_updates",p.updates());status.put("trained_samples",p.samples());status.put("active_agents",npcs.size());status.put("pending_agents",pendingSpawns.get()+spawnQueue.size());
        long now=System.nanoTime(),min=Long.MAX_VALUE,max=0,minDelta=Long.MAX_VALUE;int live=0,waiting=0,resetting=0,progressed=0,moved=0;double oldest=0,travel=0;
        for(Npc n:npcs.values()){if(n.horizontalTravel>.5)moved++;travel+=n.horizontalTravel;long delta=n.decisions-lastDecisions.getOrDefault(n.id,n.decisions);lastDecisions.put(n.id,n.decisions);minDelta=Math.min(minDelta,delta);if(delta>0)progressed++;if(n.lastStepNanos>0){double age=(now-n.lastStepNanos)/1e9;oldest=Math.max(oldest,age);if(age<2)live++;}if(n.status.equals("inference-wait"))waiting++;if(n.resetting)resetting++;min=Math.min(min,n.decisions);max=Math.max(max,n.decisions);}
        lastDecisions.keySet().retainAll(npcs.keySet());status.put("progressed_agents_since_status",progressed);status.put("min_decisions_since_status",npcs.isEmpty()?0:minDelta);double interval=lastStatusNanos==0?0:(now-lastStatusNanos)/1e9;status.put("status_interval_seconds",interval);status.put("learner_samples_per_second",interval==0?0:(p.samples()-lastSamples)/interval);status.put("decisions_per_second",interval==0?0:(transitions.sum()-lastTransitions)/interval);lastStatusNanos=now;lastSamples=p.samples();lastTransitions=transitions.sum();
        status.put("moved_agents",moved);status.put("horizontal_blocks_total",travel);
        status.put("ticking_agents",live);status.put("inference_wait_agents",waiting);status.put("resetting_agents",resetting);status.put("oldest_tick_age_seconds",oldest);status.put("min_agent_decisions",npcs.isEmpty()?0:min);status.put("max_agent_decisions",max);
        status.put("retired_agents",retired.sum());status.put("decision_transitions",transitions.sum());status.put("inference_completed",inference.completed.sum());status.put("inference_queue",inference.queued());
        status.put("inference_rejected",inferenceRejected.sum());status.put("inference_failed",inference.failed.sum());status.put("inference_compute_ns",inference.computeNanos.sum());status.put("inference_queue_ns",inference.queueNanos.sum());
        status.put("suppressed_ambient_combustions",ambientCombustions.sum());status.put("sensor_ns",sensorNanos.sum());status.put("abandoned_actions",abandoned.sum());status.put("leased_chunks",leases.size());
        var os=java.lang.management.ManagementFactory.getOperatingSystemMXBean();status.put("available_processors",Runtime.getRuntime().availableProcessors());
        if(os instanceof com.sun.management.OperatingSystemMXBean o){long cpu=o.getProcessCpuTime();status.put("process_cpu_ns",cpu);double cores=interval==0?0:Math.max(0,(cpu-lastCpu)/interval/1e9);status.put("process_cpu_cores",cores);lastCpu=cpu;status.put("process_cpu_fraction",cores/Runtime.getRuntime().availableProcessors());}
        Runtime runtime=Runtime.getRuntime();status.put("heap_used_mib",(runtime.totalMemory()-runtime.freeMemory())/(1024L*1024));status.put("java_threads",java.lang.management.ManagementFactory.getThreadMXBean().getThreadCount());status.put("epoch_millis",System.currentTimeMillis());status.putAll(extraStatus());
        PolicyFile.atomicWrite(getDataFolder().toPath().resolve("status.json"),json(status).getBytes(StandardCharsets.UTF_8));
    }
    private static String json(Map<String,Object> map){StringJoiner s=new StringJoiner(",\n","{\n","\n}\n");map.forEach((k,v)->{String value=v instanceof Number||v instanceof Boolean?v.toString():"\""+v.toString().replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n")+"\"";s.add("  \""+k+"\": "+value);});return s.toString();}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(!sender.hasPermission("botsclustersmc.admin")){sender.sendMessage("Permission denied.");return true;}
        try {
            String op=args.length==0?"status":args[0];
            switch(op){
                case "status"->sender.sendMessage("BotsClustersMC "+(training()?"training":"inference")+" agents="+npcs.size()+" pending="+(pendingSpawns.get()+spawnQueue.size())+" updates="+policy.updates()+" trained="+policy.samples()+" decisions="+transitions.sum()+" state="+(failed.get()==null?"running":"failed"));
                case "watch"->{if(args.length!=2||!(sender instanceof Player player))throw new IllegalArgumentException("Player: /bots watch <id>");Npc npc=Objects.requireNonNull(npcs.get(Long.parseLong(args[1])),"unknown NPC");player.teleportAsync(new Location(npc.currentWorld,npc.x,npc.y+6,npc.z,0,60));}
                case "pause"->{paused.set(true);sender.sendMessage("Paused. In-flight actions are not treated as completed episodes.");}
                case "resume"->{if(failed.get()!=null)throw new IllegalStateException("failed runtime requires restart");paused.set(false);sender.sendMessage("Resumed.");}
                case "remove"->{if(training())throw new IllegalArgumentException("Training actors are fixed for this run.");if(args.length!=2)throw new IllegalArgumentException("/bots remove <id|all>");synchronized(spawnQueue){for(long id:List.copyOf(issued))if(args[1].equals("all")||id==Long.parseLong(args[1])){Npc npc=npcs.get(id);if(npc==null)released(id);else npc.remove.set(true);}spawnQueue.removeIf(spawn->!issued.contains(spawn.id()));}sender.sendMessage("Removal requested, including pending spawns.");}
                case "spawn"->{if(training())throw new IllegalArgumentException("Set BOTS before starting a new training run.");if(args.length!=2&&args.length!=6)throw new IllegalArgumentException("/bots spawn <count> [world x y z]");int n=Integer.parseInt(args[1]);if(n<1||n>maximum)throw new IllegalArgumentException("count bounds");Location at;
                    if(args.length==6){World world=Objects.requireNonNull(Bukkit.getWorld(args[2]),"unknown world");at=new Location(world,Double.parseDouble(args[3]),Double.parseDouble(args[4]),Double.parseDouble(args[5]));at.checkFinite();}
                    else if(sender instanceof Player player)at=player.getLocation();else throw new IllegalArgumentException("Console requires world x y z");
                    synchronized(spawnQueue){if(n>maximum-admitted.get())throw new IllegalArgumentException("not enough free agent capacity");}
                    for(int i=0;i<n;i++)requestSpawn(at,new Goal(Task.NAVIGATE_STOP,at.getX(),at.getY(),at.getZ(),0,1,3000));sender.sendMessage("Spawn requested: "+n);}
                case "goal"->{if(training())throw new IllegalArgumentException("Training goals belong to the curriculum.");if(args.length!=6)throw new IllegalArgumentException("/bots goal <id|all> <task 0..17> <x> <y> <z>");Goal goal=new Goal(Task.at(Integer.parseInt(args[2])),Double.parseDouble(args[3]),Double.parseDouble(args[4]),Double.parseDouble(args[5]),System.currentTimeMillis(),1,3000);for(Npc npc:npcs.values())if(args[1].equals("all")||npc.id==Long.parseLong(args[1]))npc.requestedGoal.set(goal);sender.sendMessage("Goal requested.");}
                default->sender.sendMessage("/bots status | pause | resume | spawn <count> | remove <id|all> | goal <id|all> <task> <x> <y> <z>");
            }
        }catch(Exception e){sender.sendMessage("Rejected: "+e.getMessage());}return true;
    }
    @Override public final void onDisable(){
        paused.set(true);if(inference!=null)inference.close();
        try{closing();}catch(Throwable e){getLogger().log(java.util.logging.Level.SEVERE,"Final save failed; do not overwrite the checkpoint",e);}
        if(io!=null)io.shutdownNow();getLogger().info("Stopped. NPC bodies are ephemeral; training state is separate from Minecraft world persistence.");
    }
}
