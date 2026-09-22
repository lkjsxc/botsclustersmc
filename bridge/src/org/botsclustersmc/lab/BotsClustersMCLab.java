package org.botsclustersmc.lab;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

/** Environment only. Rust chooses every policy action and every promotion. */
public final class BotsClustersMCLab extends JavaPlugin implements Listener {
    private Path directory;
    private String run,prefix;
    private int bots;
    private World world;
    private ScheduledExecutorService io;
    private volatile boolean closing,campusReady;
    private final AtomicBoolean failed=new AtomicBoolean();
    private final AtomicInteger nextCell=new AtomicInteger(),builtCells=new AtomicInteger();
    private final Set<InetAddress> localAddresses=new HashSet<>();
    private final ConcurrentMap<String,Player> players=new ConcurrentHashMap<>();
    private final ConcurrentMap<String,Player> joining=new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer,Session> sessions=new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer,Protocol.Request> overlays=new ConcurrentHashMap<>();
    private final ConcurrentMap<String,String> outgoing=new ConcurrentHashMap<>();
    // Only the single file-IO thread touches these maps.
    private final Map<Integer,String> accepted=new HashMap<>();
    private final Map<String,String> written=new HashMap<>();
    private static final class Session {
        final Protocol.Request request;
        volatile boolean ready,broken;
        final AtomicLong tick=new AtomicLong();
        Session(Protocol.Request r) { request=r; }
    }
    @Override public void onEnable() {
        try {
            // Initialize the failure channel BEFORE validating any setting.
            run=System.getenv().getOrDefault("BCMC_RUN_ID","unknown");
            directory=Path.of(Objects.requireNonNull(System.getenv("BCMC_ROOT"))).resolve(".runtime/lab");
            Files.createDirectories(directory);
            if(!"true".equals(System.getenv("BCMC_CURRICULUM"))) throw new IllegalStateException("academy-only plugin");
            run=Objects.requireNonNull(System.getenv("BCMC_RUN_ID"));
            prefix=System.getenv().getOrDefault("BOT_PREFIX","bcmc");
            bots=Integer.parseInt(System.getenv().getOrDefault("BOTS","32"));
            EnvironmentConfig.validate(run,prefix,bots);
            directory=Path.of(Objects.requireNonNull(System.getenv("BCMC_ROOT"))).resolve(".runtime/lab");
            Files.createDirectories(directory);
            world=Objects.requireNonNull(getServer().getWorld("bcmc_academy_v1"),"dedicated Academy world is missing");
            if(world.getWorldType()!=WorldType.FLAT) throw new IllegalStateException("Academy requires a newly generated FLAT world");
            for(NetworkInterface n:Collections.list(NetworkInterface.getNetworkInterfaces()))
                localAddresses.addAll(Collections.list(n.getInetAddresses()));
            getServer().getPluginManager().registerEvents(this,this);
            io=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"botsclustersmc-lab-io");t.setDaemon(true);return t;});
            io.scheduleWithFixedDelay(this::poll,0,50,TimeUnit.MILLISECONDS);
            // At most two chunk builds, each bounded to 256 edits/readbacks per tick.
            constructNext(); constructNext();
        } catch(Throwable t) { fail(-1,t); throw new IllegalStateException("Academy initialization failed",t); }
    }
    @Override public void onDisable() { closing=true; if(io!=null) io.shutdownNow(); }
    private void constructNext() {
        if(closing||failed.get()) return;
        int id=nextCell.getAndIncrement(); if(id>=bots) return;
        int cx=CampusPlan.ox(id)>>4,cz=CampusPlan.oz(id)>>4;
        world.getChunkAtAsync(cx,cz,true).thenAccept(chunk->{
            List<CampusPlan.Edit> edits=CampusPlan.base(id); int[] cursor={0};
            getServer().getRegionScheduler().runAtFixedRate(this,world,cx,cz,task->{
                try {
                    if(closing||failed.get()) {task.cancel();return;}
                    if(cursor[0]==0) chunk.addPluginChunkTicket(this);
                    int end=Math.min(cursor[0]+256,edits.size());
                    while(cursor[0]<end) putAndVerify(edits.get(cursor[0]++));
                    if(cursor[0]==edits.size()) {
                        task.cancel();
                        if(builtCells.incrementAndGet()==bots) {
                            outgoing.put("campus.ready",CampusPlan.manifest(run,bots));
                            outgoing.put("bridge.ready","BCMCLAB2 "+run+" ready\n");
                            campusReady=true;
                            getLogger().info("Verified all "+bots+" enclosed training cells; no wilderness fallback");
                        }
                        constructNext();
                    }
                } catch(Throwable t) {task.cancel();fail(id,t);}
            },1,1);
        }).exceptionally(t->{fail(id,t);return null;});
    }
    private void putAndVerify(CampusPlan.Edit edit) {
        var p=edit.pos(); var block=world.getBlockAt(p.x(),p.y(),p.z());
        Material expected=Material.valueOf(edit.material());
        if(block.getType()!=expected) block.setType(expected,false);
        if(block.getType()!=expected) throw new IllegalStateException("block readback mismatch at "+p);
    }
    private String name(int id) { return prefix+String.format(Locale.ROOT,"%02d",id); }
    private int id(String name) {for(int i=0;i<bots;i++) if(name(i).equalsIgnoreCase(name)) return i;return -1;}
    private int id(Player p) {return id(p.getName());}
    @EventHandler public void login(AsyncPlayerPreLoginEvent e) {
        if(!campusReady||failed.get()) {e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,"Training campus is not ready. Inspect the server logs.");return;}
        int id=id(e.getName());
        if(id>=0&&(!name(id).equals(e.getName())||(!e.getAddress().isLoopbackAddress()&&!localAddresses.contains(e.getAddress()))))
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,"This training identity is reserved for the local learner.");
    }
    @EventHandler public void join(PlayerJoinEvent e) {
        Player p=e.getPlayer(); int id=id(p);
        p.setPlayerTime(6000,false);p.setPlayerWeather(WeatherType.CLEAR);
        if(id<0) {
            p.setGameMode(GameMode.SPECTATOR);watch(p,0);
            p.sendMessage("botsclustersmc Academy: /academy watch 0.."+(bots-1)+" to observe a room.");return;
        }
        p.setInvulnerable(true);p.setGameMode(GameMode.ADVENTURE);
        // This is environment placement, not an RL movement action. Wait for it
        // before accepting a lesson reset, avoiding competing teleport futures.
        joining.put(name(id),p);
        p.teleportAsync(new Location(world,CampusPlan.ox(id)+8,97,CampusPlan.oz(id)+5)).thenAccept(ok->{
            if(joining.remove(name(id),p)) {
                if(ok) players.put(name(id),p);
                else if(!stopping()) fail(id,new IllegalStateException("holding teleport refused"));
            }
        }).exceptionally(t->{if(joining.remove(name(id),p)&&!stopping()) fail(id,t);return null;});
        p.getScheduler().runAtFixedRate(this,task->{
            try {
                Session s=sessions.get(id);
                if(s==null||!s.ready||players.get(p.getName())!=p||failed.get()) return;
                if(p.getWorld()!=world) {fail(id,new IllegalStateException("bot changed world"));return;}
                Location l=p.getLocation();long tick=s.tick.addAndGet(4);
                outgoing.put("frame-"+id+".txt",String.format(Locale.ROOT,"BCMCLAB2 %s %s %d %d %.8f %.8f %.8f %.5f %.5f %s %s%n",
                    run,s.request.token(),id,tick,l.getX(),l.getY(),l.getZ(),l.getYaw(),l.getPitch(),p.isOnGround(),s.broken));
            } catch(Throwable t) {fail(id,t);}
        },null,4,4);
    }
    private void watch(Player p,int id) {
        p.teleportAsync(new Location(world,CampusPlan.ox(id)+8,110,CampusPlan.oz(id)+8,0,75));
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(!(sender instanceof Player p)||id(p)>=0) {sender.sendMessage("Use this command as an observer.");return true;}
        try {
            if(args.length!=2||!args[0].equals("watch")) throw new IllegalArgumentException();
            int target=Integer.parseInt(args[1]); if(target<0||target>=bots) throw new IllegalArgumentException();
            watch(p,target);
        } catch(IllegalArgumentException e) {sender.sendMessage("Usage: /academy watch 0.."+(bots-1));}
        return true;
    }
    @EventHandler public void quit(PlayerQuitEvent e) {
        Player p=e.getPlayer();int id=id(p);if(id<0) return;
        joining.remove(name(id),p);
        if(players.remove(name(id),p)) {Session s=sessions.remove(id);if(s!=null) s.ready=false;}
    }
    private void poll() {
        if(closing) return;
        try {
            if(campusReady&&!failed.get()) for(int i=0;i<bots;i++) {
                Path path=directory.resolve("request-"+i+".txt");
                if(!Files.isRegularFile(path)||!players.containsKey(name(i))) continue;
                if(Files.size(path)>1024) throw new IllegalArgumentException("oversized request");
                String text=Files.readString(path,StandardCharsets.UTF_8);
                if(text.equals(accepted.get(i))) continue;
                Protocol.Request r=Protocol.parse(text,run,bots);
                if(r.id()!=i) throw new IllegalArgumentException("agent/file mismatch");
                accepted.put(i,text);reset(r);
            }
            for(var e:outgoing.entrySet()) if(!e.getKey().equals("fatal.txt")&&!e.getValue().equals(written.get(e.getKey()))) {
                atomic(directory.resolve(e.getKey()),e.getValue());written.put(e.getKey(),e.getValue());
            }
        } catch(Throwable t) {
            if(closing) return;
            fail(-1,t);

        }
    }
    private void reset(Protocol.Request r) {
        Player p=players.get(name(r.id()));if(p==null) return;
        Session s=new Session(r);sessions.put(r.id(),s);
        int cx=CampusPlan.ox(r.id())>>4,cz=CampusPlan.oz(r.id())>>4;
        getServer().getRegionScheduler().execute(this,world,cx,cz,()->{
            try {
                if(sessions.get(r.id())!=s||failed.get()) return;
                for(var edit:CampusPlan.reset(overlays.get(r.id()),r)) putAndVerify(edit);
                overlays.put(r.id(),r);
                p.getScheduler().run(this,task->{
                    try {
                        if(sessions.get(r.id())!=s) return;
                        p.closeInventory();p.getInventory().clear();p.getInventory().setHeldItemSlot(0);
                        p.setGameMode(r.stage()==5?GameMode.SURVIVAL:GameMode.ADVENTURE);
                        p.setInvulnerable(true);p.setAllowFlight(false);p.setFoodLevel(20);p.setSaturation(20);p.setHealth(20);p.setFireTicks(0);
                        p.teleportAsync(new Location(world,r.x(),r.y(),r.z(),r.yaw(),r.pitch())).thenAccept(ok->{
                            if(!ok) {
                                if(!stopping()&&sessions.get(r.id())==s&&players.get(name(r.id()))==p)
                                    fail(r.id(),new IllegalStateException("reset teleport refused"));
                                return;
                            }
                            p.getScheduler().runDelayed(this,ignored->{if(sessions.get(r.id())==s) {p.setVelocity(new Vector(0,0,0));s.ready=true;}},
                                ()->retired(p,s),8);
                        }).exceptionally(t->{fail(r.id(),t);return null;});
                    } catch(Throwable t) {fail(r.id(),t);}
                },()->retired(p,s));
            } catch(Throwable t) {fail(r.id(),t);}
        });
    }
    private boolean stopping() {
        return closing || (directory!=null && Files.exists(directory.getParent().resolve("stop")));
    }
    // Folia retires entity tasks during ordinary disconnection and shutdown.
    // Discard that session; never fabricate an observation, reward or success.
    private void retired(Player p,Session s) {
        s.ready=false;
        if(sessions.remove(s.request.id(),s)) {
            players.remove(name(s.request.id()),p);
            outgoing.remove("frame-"+s.request.id()+".txt");
        }
    }
    private boolean target(Session s,int x,int y,int z) {
        return x==(int)Math.floor(s.request.gx())&&y==(int)Math.floor(s.request.gy())&&z==(int)Math.floor(s.request.gz());
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void breaking(BlockBreakEvent e) {
        if(e.getBlock().getWorld()!=world) return;
        Session s=sessions.get(id(e.getPlayer()));
        if(s==null||!s.ready||s.request.stage()!=5||!target(s,e.getBlock().getX(),e.getBlock().getY(),e.getBlock().getZ())||e.getBlock().getType()!=Material.OAK_LOG) e.setCancelled(true);
        else e.setDropItems(false);
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void broken(BlockBreakEvent e) {
        int id=id(e.getPlayer());Session s=sessions.get(id);
        if(s==null||!s.ready||s.request.stage()!=5||e.getBlock().getWorld()!=world||!target(s,e.getBlock().getX(),e.getBlock().getY(),e.getBlock().getZ())) return;
        int x=e.getBlock().getX(),y=e.getBlock().getY(),z=e.getBlock().getZ();
        getServer().getRegionScheduler().run(this,world,x>>4,z>>4,ignored->{
            if(sessions.get(id)==s&&world.getBlockAt(x,y,z).getType()==Material.AIR) s.broken=true;
        });
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void placing(BlockPlaceEvent e) {if(e.getBlock().getWorld()==world)e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void mobs(CreatureSpawnEvent e) {if(e.getLocation().getWorld()==world)e.setCancelled(true);}
    private void fail(int id,Throwable t) {
        failed.set(true);Session s=sessions.get(id);if(s!=null)s.ready=false;
        String error=run+" agent="+id+" "+t.toString().replace('\n',' ')+"\n";
        outgoing.put("fatal.txt",error);
        // onEnable may fail before the IO executor exists, or be disabled by Folia.
        // Never leave the supervisor waiting for a receipt that cannot arrive.
        if(directory!=null) synchronized(this) {
            try { atomic(directory.resolve("fatal.txt"),error); }
            catch(Exception writeError) { getLogger().severe("Cannot publish fatal receipt: "+writeError); }
        }
        getLogger().severe("Academy environment failed, agent "+id+": "+t);
    }
    private static void atomic(Path path,String text) throws Exception {
        Path tmp=path.resolveSibling(path.getFileName()+".tmp");
        Files.writeString(tmp,text,StandardCharsets.UTF_8);
        Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
    }
}
