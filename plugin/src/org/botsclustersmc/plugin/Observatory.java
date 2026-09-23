package org.botsclustersmc.plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import net.kyori.adventure.text.Component;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

/** Read-only operator views. Camera callbacks always belong to the player. */
public final class Observatory implements CommandExecutor, TabCompleter, Listener {
    private static final Set<String> READ = Set.of("status","progress","inspect","list","watch","tour","overview","unwatch");
    private final RuntimePlugin plugin;
    private final Map<UUID, Watch> watchers = new ConcurrentHashMap<>();
    private static final class Watch {
        long actor, nextTour; boolean tour, overview; volatile boolean positioned; volatile long generation;
        volatile java.util.concurrent.CompletableFuture<Boolean> flight=java.util.concurrent.CompletableFuture.completedFuture(true);
        final AtomicBoolean moving = new AtomicBoolean();
        final GameMode originalMode; final Location originalLocation;
        ScheduledTask task;
        Watch(Player player, long actor) {
            this.actor=actor; originalMode=player.getGameMode(); originalLocation=player.getLocation().clone();
        }
    }
    public Observatory(RuntimePlugin plugin) { this.plugin=plugin; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String op=args.length==0?"status":args[0].toLowerCase(Locale.ROOT);
        if(!READ.contains(op)) return plugin.onCommand(sender,command,label,args);
        if(!sender.hasPermission("botsclustersmc.observe")) { sender.sendMessage("Observation permission denied."); return true; }
        try {
            switch(op) {
                case "status" -> sender.sendMessage(plugin.observerStatus());
                case "progress" -> sender.sendMessage(plugin.observerProgress());
                case "list" -> { int page=args.length>1?Integer.parseInt(args[1]):0;
                    if(page<0||page>1000) throw new IllegalArgumentException("Page must be 0..1000");
                    plugin.npcs.keySet().stream().sorted(Comparator.<Long>comparingDouble(plugin::observerRank).reversed().thenComparingLong(Long::longValue)).skip(page*10L).limit(10).forEach(id -> sender.sendMessage("#"+id+" "+plugin.observerAgent(id))); }
                case "inspect" -> { if(args.length!=2) throw new IllegalArgumentException("/bots inspect <id>"); inspect(sender,Long.parseLong(args[1])); }
                case "unwatch" -> { Player player=player(sender); player.getScheduler().run(plugin,t->stop(player,true),null); }
                default -> { Player player=player(sender); long id=args.length>1?Long.parseLong(args[1]):best();
                    if(!plugin.npcs.containsKey(id)) throw new IllegalArgumentException("Unknown NPC "+id);
                    player.getScheduler().run(plugin,t->start(player,id,op),null); }
            }
        } catch(Exception failure) { sender.sendMessage("Rejected: "+failure.getMessage()); }
        return true;
    }
    private static Player player(CommandSender sender) {
        if(!(sender instanceof Player player)) throw new IllegalArgumentException("This view requires a connected player.");
        return player;
    }
    private long best() {
        return plugin.npcs.keySet().stream().max(Comparator.<Long>comparingDouble(plugin::observerRank).thenComparing(Comparator.reverseOrder())).orElseThrow(()->new IllegalStateException("No NPCs are ready yet."));
    }
    private void inspect(CommandSender sender,long id) {
        Npc npc=Objects.requireNonNull(plugin.npcs.get(id),"Unknown NPC"); AgentSnapshot s=npc.snapshot;
        sender.sendMessage("#"+id+" "+plugin.observerAgent(id));
        if(s==null) { sender.sendMessage("Waiting for the first observation."); return; }
        sender.sendMessage(String.format(Locale.ROOT,"%s | xyz %.2f %.2f %.2f | goal %.2f %.2f %.2f | distance %.3f | speed %.4f",s.task(),s.x(),s.y(),s.z(),s.goalX(),s.goalY(),s.goalZ(),s.distance(),s.speed()));
        sender.sendMessage("body="+s.body()+" action="+s.action()+" policy="+s.policy()+" decisions="+s.decisions()+" health="+s.health()+" fire_ticks="+s.fireTicks()+" burns_in_sunlight="+s.burnsInSunlight());
        sender.sendMessage(String.format(Locale.ROOT,"snapshot_age=%.2fs; %s",(System.nanoTime()-s.capturedNanos())/1e9,npc.status));
        sender.sendMessage(s.controls());
        sender.sendMessage("held="+s.heldItem()+" x"+s.heldCount()+" | mining_ticks="+s.miningTicks()+" mining_block="+s.miningBlock());
        sender.sendMessage("This episode: blocks_broken="+s.broken()+" items_collected="+s.collected()+" items_crafted="+s.crafted()+". These counts are not skill certificates.");
        Frame observed=npc.observedFrame;if(observed!=null)sender.sendMessage(PolicyDiagnostics.describe(plugin.policy,observed));
    }
    private void start(Player player,long id,String mode) {
        Watch old=watchers.remove(player.getUniqueId());
        if(old!=null&&old.task!=null) old.task.cancel();
        Watch w=old==null?new Watch(player,id):old; w.generation++; w.actor=id; w.tour=mode.equals("tour"); w.overview=mode.equals("overview"); w.positioned=false; w.nextTour=System.nanoTime()+10_000_000_000L;
        watchers.put(player.getUniqueId(),w); player.setGameMode(GameMode.SPECTATOR);
        w.task=player.getScheduler().runAtFixedRate(plugin,t->{
            try { tick(player,w); } catch(Throwable failure) { stop(player,false); player.sendMessage("Observation stopped: "+failure.getMessage()); }
        },()->watchers.remove(player.getUniqueId(),w),1,20);
        player.sendMessage("Observing #"+id+". /bots watch [id] | tour | overview | unwatch | progress | inspect <id>. Particles mark the goal.");
    }
    private void tick(Player player,Watch w) {
        if(w.tour&&System.nanoTime()>=w.nextTour) {
            List<Long> ids=plugin.npcs.keySet().stream().sorted().toList(); if(ids.isEmpty()) return;
            int next=Collections.binarySearch(ids,w.actor); w.actor=ids.get(Math.floorMod(next+1,ids.size())); w.nextTour=System.nanoTime()+10_000_000_000L;
        }
        Npc npc=plugin.npcs.get(w.actor); if(npc==null) { player.sendActionBar(Component.text("NPC unavailable; /bots watch to select another.")); return; }
        AgentSnapshot s=npc.snapshot; if(s==null) return;
        double age=(System.nanoTime()-s.capturedNanos())/1e9;
        player.sendActionBar(Component.text("#"+w.actor+" "+s.task()+" | "+plugin.observerHud(w.actor)+(s.task().equals("break-log")||s.task().equals("collect-log")?" | dig "+s.miningTicks()+"t | broken "+s.broken()+" picked "+s.collected():"")+(age>2?" | STALE":"")));
        World world=Bukkit.getWorld(s.world()); if(world==null) return;
        if((!w.overview||!w.positioned)&&age<2&&w.moving.compareAndSet(false,true)) {
            double yaw=Math.toRadians(s.yaw());
            Location camera=w.overview?new Location(world,s.x()+8,s.y()+18,s.z()-8,45,65):new Location(world,s.x()+Math.sin(yaw)*4,s.y()+5,s.z()-Math.cos(yaw)*4,s.yaw(),51);
            long generation=w.generation;w.flight=player.teleportAsync(camera);
            w.flight.whenComplete((ok,error)->{w.moving.set(false);if(w.generation==generation&&error==null&&Boolean.TRUE.equals(ok))w.positioned=true;});
        }
        if(player.getWorld()==world&&age<2) {
            for(int i=0;i<4;i++) {
                double angle=i*Math.PI/2;
                player.spawnParticle(Particle.END_ROD,s.goalX()+Math.cos(angle)*.55,s.goalY()+.2,s.goalZ()+Math.sin(angle)*.55,1,0,0,0,0);
            }
        }
    }
    private void stop(Player player,boolean restore) {
        Watch w=watchers.remove(player.getUniqueId()); if(w==null) return;
        w.generation++;if(w.task!=null) w.task.cancel(); player.sendActionBar(Component.empty());
        if(restore&&!plugin.training()) w.flight.handle((ok,error)->true).thenRun(()->player.getScheduler().run(plugin,t->{
            if(watchers.containsKey(player.getUniqueId()))return;
            player.teleportAsync(w.originalLocation).thenAccept(ok->{if(ok)player.getScheduler().run(plugin,done->{
                if(!watchers.containsKey(player.getUniqueId()))player.setGameMode(w.originalMode);
            },null);});
        },null));
        player.sendMessage("Tracking stopped. You can fly freely.");
    }
    @EventHandler public void left(PlayerQuitEvent event) {
        Watch w=watchers.remove(event.getPlayer().getUniqueId()); if(w!=null&&w.task!=null)w.task.cancel();
    }
    @EventHandler public void joined(PlayerJoinEvent event) {
        if(!plugin.training()) return;
        Player player=event.getPlayer();
        player.getScheduler().runDelayed(plugin,t->{
            if(!plugin.npcs.isEmpty()&&player.hasPermission("botsclustersmc.observe"))start(player,best(),"overview");
        },null,40);
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args) {
        if(!sender.hasPermission("botsclustersmc.observe")&&!sender.hasPermission("botsclustersmc.admin"))return List.of();
        if(args.length==1) {
            List<String> options=new ArrayList<>(READ);
            if(sender.hasPermission("botsclustersmc.admin"))options.addAll(List.of("pause","resume","spawn","remove","goal"));
            return options.stream().filter(s->s.startsWith(args[0].toLowerCase(Locale.ROOT))).sorted().toList();
        }
        if(args.length==2&&Set.of("watch","inspect","tour","overview").contains(args[0]))
            return plugin.npcs.keySet().stream().sorted().map(Object::toString).filter(s->s.startsWith(args[1])).limit(30).toList();
        return List.of();
    }
    public void close() {
        for(Watch w:watchers.values())if(w.task!=null)w.task.cancel();
        watchers.clear();
    }
}
