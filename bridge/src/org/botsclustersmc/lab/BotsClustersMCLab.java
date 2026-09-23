package org.botsclustersmc.lab;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Item;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

/** Real Folia environment and human observer UI. Rust owns every policy action. */
public final class BotsClustersMCLab extends JavaPlugin implements Listener {
    private Path directory;private String run,prefix;private int bots,botView;private World world;private ObserverUI observers;private NamespacedKey itemTag;
    private ScheduledExecutorService io;private volatile boolean closing,campusReady;private final AtomicBoolean failed=new AtomicBoolean();
    private final AtomicInteger nextCell=new AtomicInteger(),builtCells=new AtomicInteger();
    private final Set<InetAddress> localAddresses=new HashSet<>();
    private final ConcurrentMap<String,Player> players=new ConcurrentHashMap<>(),joining=new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer,Session> sessions=new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer,Protocol.Request> overlays=new ConcurrentHashMap<>();
    private final ConcurrentMap<String,String> outgoing=new ConcurrentHashMap<>();
    // Only the single IO thread touches these caches.
    private final Map<Integer,String> accepted=new HashMap<>();private final Map<String,String> written=new HashMap<>();private String previousProgress="";
    private static final class Session {
        final Protocol.Request request;volatile boolean ready;int startedAge,craftBaseline,transferStock;
        final AtomicInteger broken=new AtomicInteger(),pickedUp=new AtomicInteger(),placed=new AtomicInteger(),smelted=new AtomicInteger(),deposited=new AtomicInteger();
        final Set<CampusPlan.Pos> placedPositions=ConcurrentHashMap.newKeySet();final Set<UUID> pickupReceipts=ConcurrentHashMap.newKeySet();
        Session(Protocol.Request request){this.request=request;}
    }
    private static int setting(String name,int value,int min,int max){int n=Integer.parseInt(System.getenv().getOrDefault(name,Integer.toString(value)));if(n<min||n>max)throw new IllegalArgumentException(name+" must be "+min+".."+max);return n;}
    @Override public void onEnable(){
        try{
            run=System.getenv().getOrDefault("BCMC_RUN_ID","unknown");directory=Path.of(Objects.requireNonNull(System.getenv("BCMC_ROOT"))).resolve(".runtime/lab");Files.createDirectories(directory);
            if(!"true".equals(System.getenv("BCMC_CURRICULUM")))throw new IllegalStateException("Academy-only plugin");
            run=Objects.requireNonNull(System.getenv("BCMC_RUN_ID"));prefix=System.getenv().getOrDefault("BOT_PREFIX","bcmc");bots=setting("BOTS",64,1,64);botView=setting("BOT_VIEW_DISTANCE",3,3,6);
            EnvironmentConfig.validate(run,prefix,bots);
            world=Objects.requireNonNull(getServer().getWorld("bcmc_academy_v2"),"dedicated Academy v2 world is missing");
            if(world.getWorldType()!=WorldType.FLAT)throw new IllegalStateException("Academy requires its own FLAT world");
            itemTag=new NamespacedKey(this,"episode");
            for(NetworkInterface n:Collections.list(NetworkInterface.getNetworkInterfaces()))localAddresses.addAll(Collections.list(n.getInetAddresses()));
            observers=new ObserverUI(this,world,bots,setting("SPECTATOR_VIEW_DISTANCE",12,3,16),prefix);
            getServer().getPluginManager().registerEvents(this,this);
            io=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"botsclustersmc-lab-io");t.setDaemon(true);return t;});io.scheduleWithFixedDelay(this::poll,0,50,TimeUnit.MILLISECONDS);
            constructNext();constructNext();
        }catch(Throwable t){fail(-1,t);throw new IllegalStateException("Academy initialization failed",t);}
    }
    @Override public void onDisable(){closing=true;if(io!=null)io.shutdownNow();}
    private void constructNext(){
        if(closing||failed.get())return;int id=nextCell.getAndIncrement();if(id>=bots)return;
        int cx=CampusPlan.ox(id)>>4,cz=CampusPlan.oz(id)>>4;
        world.getChunkAtAsync(cx,cz,true).thenAccept(chunk->{
            List<CampusPlan.Edit> edits=CampusPlan.base(id);int[] cursor={0};
            getServer().getRegionScheduler().runAtFixedRate(this,world,cx,cz,task->{
                try{
                    if(closing||failed.get()){task.cancel();return;}
                    if(cursor[0]==0){chunk.addPluginChunkTicket(this);removeDroppedItems(chunk);}
                    int end=Math.min(cursor[0]+256,edits.size());while(cursor[0]<end)putAndVerify(edits.get(cursor[0]++));
                    if(cursor[0]==edits.size()){
                        task.cancel();
                        if(builtCells.incrementAndGet()==bots){outgoing.put("campus.ready",CampusPlan.manifest(run,bots));outgoing.put("bridge.ready","BCMCLAB3 "+run+" ready\n");campusReady=true;getLogger().info("Verified all "+bots+" enclosed training cells");}
                        constructNext();
                    }
                }catch(Throwable t){task.cancel();fail(id,t);}
            },1,1);
        }).exceptionally(t->{fail(id,t);return null;});
    }
    private void putAndVerify(CampusPlan.Edit edit){var p=edit.pos();Block block=world.getBlockAt(p.x(),p.y(),p.z());Material expected=Material.valueOf(edit.material());if(block.getType()!=expected)block.setType(expected,false);if(block.getType()!=expected)throw new IllegalStateException("block readback mismatch at "+p);}
    private void removeDroppedItems(Chunk chunk){for(var e:chunk.getEntities())if(e instanceof Item item)item.getScheduler().run(this,t->item.remove(),null);}
    private String name(int id){return prefix+String.format(Locale.ROOT,"%02d",id);}
    private int id(String name){for(int i=0;i<bots;i++)if(name(i).equalsIgnoreCase(name))return i;return -1;}
    private int id(Player p){return id(p.getName());}
    @EventHandler public void login(AsyncPlayerPreLoginEvent e){
        if(!campusReady||failed.get()){e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,"Training campus is not ready. Inspect the server logs.");return;}
        int id=id(e.getName());if(id>=0&&(!name(id).equals(e.getName())||(!e.getAddress().isLoopbackAddress()&&!localAddresses.contains(e.getAddress()))))e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,"This identity is reserved for the local learner.");
    }
    @EventHandler public void join(PlayerJoinEvent e){
        Player p=e.getPlayer();int id=id(p);p.setPlayerTime(6000,false);p.setPlayerWeather(WeatherType.CLEAR);
        if(id<0){observers.join(p);return;}
        p.setViewDistance(botView);p.setSendViewDistance(botView);p.setSimulationDistance(3);p.setInvulnerable(true);p.setGameMode(GameMode.ADVENTURE);
        joining.put(name(id),p);
        p.teleportAsync(new Location(world,CampusPlan.ox(id)+8,97,CampusPlan.oz(id)+5)).thenAccept(ok->{if(joining.remove(name(id),p)){if(ok)players.put(name(id),p);else if(!stopping())fail(id,new IllegalStateException("holding teleport refused"));}}).exceptionally(t->{if(joining.remove(name(id),p)&&!stopping())fail(id,t);return null;});
        p.getScheduler().runAtFixedRate(this,task->{
            try{
                Session s=sessions.get(id);if(s==null||!s.ready||players.get(p.getName())!=p||failed.get())return;
                requireCell(p,s);Location l=p.getLocation();long tick=Integer.toUnsignedLong(p.getTicksLived()-s.startedAge);
                int[] stock=stock(p);int crafted=craftCount(p,s);int targetStock=containerStock(s);int occupied=occupied(s);
                StringBuilder frame=new StringBuilder(String.format(Locale.ROOT,"BCMCLAB3 %s %s %d %d %.8f %.8f %.8f %.5f %.5f %s",run,s.request.token(),id,tick,l.getX(),l.getY(),l.getZ(),l.getYaw(),l.getPitch(),p.isOnGround()));
                for(int n:stock)frame.append(' ').append(n);
                frame.append(' ').append(s.broken.get()).append(' ').append(s.pickedUp.get()).append(' ').append(crafted).append(' ').append(s.placed.get()).append(' ').append(s.smelted.get()).append(' ').append(s.deposited.get()).append(' ').append(targetStock).append(' ').append(occupied).append('\n');
                outgoing.put("frame-"+id+".txt",frame.toString());
            }catch(Throwable t){fail(id,t);}
        },null,4,4);
    }
    /** Same-chunk ownership is checked before player callbacks access any block. */
    private void requireCell(Player p,Session s){Location l=p.getLocation();if(l.getWorld()!=world||(l.getBlockX()>>4)!=(CampusPlan.ox(s.request.id())>>4)||(l.getBlockZ()>>4)!=(CampusPlan.oz(s.request.id())>>4))throw new IllegalStateException("actor left its owned chunk");}
    private int craftCount(Player p,Session s){String output=TaskFixtures.craftOutput(s.request.stage());if(output==null)return 0;int n=p.getStatistic(Statistic.CRAFT_ITEM,Material.valueOf(output))-s.craftBaseline;if(n<0)throw new IllegalStateException("craft statistic reversed");return n;}
    private int[] stock(Player p){int[] counts=new int[13];for(ItemStack item:p.getInventory().getContents())add(counts,item);add(counts,p.getItemOnCursor());return counts;}
    private static void add(int[] counts,ItemStack item){if(item==null||item.getType().isAir())return;int i=TaskFixtures.itemIndex(item.getType().name());if(i>=0)counts[i]=Math.addExact(counts[i],item.getAmount());}
    private Block goal(Session s){return world.getBlockAt((int)Math.floor(s.request.gx()),(int)Math.floor(s.request.gy()),(int)Math.floor(s.request.gz()));}
    private int containerStock(Session s){if(s.request.stage()!=15)return 0;if(!(goal(s).getState() instanceof Chest chest))throw new IllegalStateException("designated chest removed");int n=0;for(ItemStack i:chest.getBlockInventory().getContents())if(i!=null&&i.getType()==Material.OAK_LOG)n+=i.getAmount();return n;}
    private int occupied(Session s){if(s.request.stage()!=7&&s.request.stage()!=16)return 0;Block b=goal(s);int bits=0,n=s.request.stage()==16?3:1;for(int i=0;i<n;i++)if(world.getBlockAt(b.getX()+i,b.getY(),b.getZ()).getType()==Material.OAK_PLANKS)bits|=1<<i;return bits;}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){if(!(sender instanceof Player p)||id(p)>=0){sender.sendMessage("Use this command as a human observer.");return true;}return observers.command(p,args);}
    @EventHandler public void quit(PlayerQuitEvent e){Player p=e.getPlayer();int id=id(p);if(id<0)return;joining.remove(name(id),p);if(players.remove(name(id),p)){Session s=sessions.remove(id);if(s!=null)s.ready=false;}}
    private void poll(){
        if(closing)return;
        try{
            if(campusReady&&!failed.get())for(int i=0;i<bots;i++){
                Path path=directory.resolve("request-"+i+".txt");if(!Files.isRegularFile(path)||!players.containsKey(name(i)))continue;
                if(Files.size(path)>1024)throw new IllegalArgumentException("oversized request");String text=Files.readString(path,StandardCharsets.UTF_8);if(text.equals(accepted.get(i)))continue;
                Protocol.Request r=Protocol.parse(text,run,bots);if(r.id()!=i)throw new IllegalArgumentException("actor/file mismatch");accepted.put(i,text);reset(r);
            }
            for(var e:outgoing.entrySet())if(!e.getKey().equals("fatal.txt")&&!e.getValue().equals(written.get(e.getKey()))){atomic(directory.resolve(e.getKey()),e.getValue());written.put(e.getKey(),e.getValue());}
            Path progress=directory.resolve("progress.txt");if(Files.isRegularFile(progress)&&Files.size(progress)<=16384){String text=Files.readString(progress);if(!text.equals(previousProgress)){previousProgress=text;try{observers.publish(ObserverState.parse(text,run,bots));}catch(IllegalArgumentException e){getLogger().warning("Rejected observer status: "+e.getMessage());}}}
        }catch(Throwable t){if(!closing)fail(-1,t);}
    }
    private void reset(Protocol.Request r){
        Player p=players.get(name(r.id()));if(p==null)return;Session s=new Session(r);sessions.put(r.id(),s);
        // Retire the old episode before touching its geometry; a reset cannot
        // produce drops, crafted output, fake success or a transition reward.
        p.getScheduler().run(this,task->{
            try{
                if(sessions.get(r.id())!=s)return;p.setGameMode(GameMode.ADVENTURE);p.setItemOnCursor(null);p.closeInventory();p.getInventory().clear();
                int cx=CampusPlan.ox(r.id())>>4,cz=CampusPlan.oz(r.id())>>4;List<CampusPlan.Edit> edits=CampusPlan.reset(overlays.get(r.id()),r);int[] cursor={0};
                getServer().getRegionScheduler().runAtFixedRate(this,world,cx,cz,t->{
                    try{
                        if(sessions.get(r.id())!=s||failed.get()||closing){t.cancel();return;}
                        if(cursor[0]==0){removeDroppedItems(world.getChunkAt(cx,cz));Protocol.Request old=overlays.get(r.id());if(old!=null&&(old.stage()==14||old.stage()==15))world.getBlockAt((int)old.gx(),(int)old.gy(),(int)old.gz()).setType(Material.AIR,false);}
                        int end=Math.min(cursor[0]+256,edits.size());while(cursor[0]<end)putAndVerify(edits.get(cursor[0]++));
                        if(cursor[0]==edits.size()){t.cancel();overlays.put(r.id(),r);placePlayer(p,s);}
                    }catch(Throwable error){t.cancel();fail(r.id(),error);}
                },1,1);
            }catch(Throwable error){fail(r.id(),error);}
        },()->retired(p,s));
    }
    private void placePlayer(Player p,Session s){Protocol.Request r=s.request;
        p.getScheduler().run(this,task->{
            try{
                if(sessions.get(r.id())!=s)return;
                p.setInvulnerable(true);p.setAllowFlight(false);p.setFoodLevel(20);p.setSaturation(20);p.setHealth(20);p.setFireTicks(0);
                p.teleportAsync(new Location(world,r.x(),r.y(),r.z(),r.yaw(),r.pitch())).thenAccept(ok->{
                    if(!ok){if(!stopping()&&sessions.get(r.id())==s)fail(r.id(),new IllegalStateException("reset teleport refused"));return;}
                    p.getScheduler().runDelayed(this,ignored->{
                        try{
                            if(sessions.get(r.id())!=s||players.get(name(r.id()))!=p)return;requireCell(p,s);
                            p.setVelocity(new Vector(0,0,0));p.setItemOnCursor(null);p.closeInventory();p.getInventory().clear();p.getInventory().setHeldItemSlot(0);
                            p.setGameMode(r.stage()>=5?GameMode.SURVIVAL:GameMode.ADVENTURE);
                            furnish(p,s);String output=TaskFixtures.craftOutput(r.stage());s.craftBaseline=output==null?0:p.getStatistic(Statistic.CRAFT_ITEM,Material.valueOf(output));
                            s.transferStock=containerStock(s);s.startedAge=p.getTicksLived();s.ready=true;
                        }catch(Throwable error){fail(r.id(),error);}
                    },()->retired(p,s),8);
                }).exceptionally(error->{if(!stopping())fail(r.id(),error);return null;});
            }catch(Throwable error){fail(r.id(),error);}
        },()->retired(p,s));
    }
    private void furnish(Player p,Session s){
        Protocol.Request r=s.request;SplittableRandom random=new SplittableRandom(r.seed());List<Integer> slots=new ArrayList<>();for(int i=0;i<36;i++)slots.add(i);
        for(TaskFixtures.Supply supply:TaskFixtures.supplies(r.stage())){int index=random.nextInt(slots.size());p.getInventory().setItem(slots.remove(index),new ItemStack(Material.valueOf(supply.material()),supply.amount()));}
        // Assistance changes reset states only. Full probes/exams NEVER open a
        // menu or prefill a grid. The actor must still acquire the real output.
        int assistance=TaskFixtures.assistance(r);
        if(assistance>0){
            if(r.stage()==11||r.stage()==13)p.openWorkbench(goal(s).getLocation(),false);
            if(!(p.getOpenInventory().getTopInventory() instanceof CraftingInventory grid))throw new IllegalStateException("crafting reset grid unavailable");
            ItemStack[] matrix=grid.getMatrix();Arrays.fill(matrix,null);List<TaskFixtures.Ingredient> recipe=TaskFixtures.recipe(r.stage());
            for(int i=0;i<assistance;i++){var ingredient=recipe.get(i);matrix[ingredient.slot()]=takeOne(p,Material.valueOf(ingredient.material()));}
            grid.setMatrix(matrix);p.updateInventory();
        }
        if(r.stage()==14&&TaskFixtures.mayOpenAtReset(r)){
            if(!(goal(s).getState() instanceof Furnace furnace))throw new IllegalStateException("furnace reset missing");
            if(r.difficulty()<0.55)furnace.getInventory().setFuel(takeOne(p,Material.COAL));
            if(r.difficulty()<0.35)furnace.getInventory().setSmelting(takeOne(p,Material.RAW_IRON));
            p.openInventory(furnace.getInventory());
        }
        if(r.stage()==15&&TaskFixtures.mayOpenAtReset(r)){if(!(goal(s).getState() instanceof Chest chest))throw new IllegalStateException("chest reset missing");p.openInventory(chest.getBlockInventory());}
    }
    private ItemStack takeOne(Player p,Material material){
        for(int i=0;i<36;i++){ItemStack item=p.getInventory().getItem(i);if(item!=null&&item.getType()==material&&item.getAmount()>0){ItemStack result=new ItemStack(material,1);if(item.getAmount()==1)p.getInventory().setItem(i,null);else{item.setAmount(item.getAmount()-1);p.getInventory().setItem(i,item);}return result;}}
        throw new IllegalStateException("reset attempted to invent an ingredient");
    }
    private boolean stopping(){return closing||(directory!=null&&Files.exists(directory.getParent().resolve("stop")));}
    private void retired(Player p,Session s){s.ready=false;if(sessions.remove(s.request.id(),s)){players.remove(name(s.request.id()),p);outgoing.remove("frame-"+s.request.id()+".txt");}}
    private boolean target(Session s,Block b){return b.getWorld()==world&&b.getX()==(int)Math.floor(s.request.gx())&&b.getY()==(int)Math.floor(s.request.gy())&&b.getZ()==(int)Math.floor(s.request.gz());}
    private boolean resource(Session s,Block b){return target(s,b)&&((Set.of(5,6,17).contains(s.request.stage())&&b.getType()==Material.OAK_LOG)||(s.request.stage()==12&&b.getType()==Material.STONE));}
    private String tag(Session s,String kind){return run+"/"+s.request.token()+"/"+kind;}
    private void tag(Item item,Session s,String kind){item.getPersistentDataContainer().set(itemTag,PersistentDataType.STRING,tag(s,kind));}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void breaking(BlockBreakEvent e){
        Block b=e.getBlock();if(b.getWorld()!=world)return;Session s=sessions.get(id(e.getPlayer()));
        if(s==null||!s.ready||!CampusPlan.inside(s.request.id(),b.getX(),b.getY(),b.getZ())){e.setCancelled(true);return;}
        boolean own=s.placedPositions.contains(new CampusPlan.Pos(b.getX(),b.getY(),b.getZ()))&&b.getType()==Material.OAK_PLANKS;
        if(!resource(s,b)&&!own)e.setCancelled(true);else if(s.request.stage()==5)e.setDropItems(false);
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void broken(BlockBreakEvent e){
        Session s=sessions.get(id(e.getPlayer()));if(s==null||!s.ready||e.getBlock().getWorld()!=world)return;Block b=e.getBlock();boolean resource=resource(s,b);int x=b.getX(),y=b.getY(),z=b.getZ();
        getServer().getRegionScheduler().run(this,world,x>>4,z>>4,t->{if(sessions.get(s.request.id())==s&&s.ready&&world.getBlockAt(x,y,z).getType()==Material.AIR){if(resource)s.broken.incrementAndGet();s.placedPositions.remove(new CampusPlan.Pos(x,y,z));}});
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void drops(BlockDropItemEvent e){
        if(e.getBlock().getWorld()!=world)return;Session s=sessions.get(id(e.getPlayer()));if(s==null||!s.ready){e.setCancelled(true);return;}
        String kind=target(s,e.getBlock())&&(e.getBlockState().getType()==Material.OAK_LOG||e.getBlockState().getType()==Material.STONE)?"target":"placed";
        for(Item item:e.getItems())tag(item,s,kind);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void dropped(PlayerDropItemEvent e){
        if(e.getPlayer().getWorld()!=world)return;Session s=sessions.get(id(e.getPlayer()));if(s==null||!s.ready)e.setCancelled(true);else tag(e.getItemDrop(),s,"inventory");
    }
    // Each target pickup retains its individual episode provenance until removal.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void merging(ItemMergeEvent e){if(e.getEntity().getWorld()==world)e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void pickup(EntityPickupItemEvent e){
        if(e.getEntity().getWorld()!=world)return;if(!(e.getEntity() instanceof Player p)){e.setCancelled(true);return;}
        Session s=sessions.get(id(p));String actual=e.getItem().getPersistentDataContainer().get(itemTag,PersistentDataType.STRING);
        if(s==null||!s.ready||actual==null||!(actual.equals(tag(s,"target"))||actual.equals(tag(s,"placed"))||actual.equals(tag(s,"inventory"))))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void acquired(EntityPickupItemEvent e){
        if(!(e.getEntity() instanceof Player p)||p.getWorld()!=world)return;Session s=sessions.get(id(p));if(s==null||!s.ready)return;
        Item item=e.getItem();String actual=item.getPersistentDataContainer().get(itemTag,PersistentDataType.STRING);if(!tag(s,"target").equals(actual))return;
        UUID receipt=item.getUniqueId();Material material=item.getItemStack().getType();int before=p.getStatistic(Statistic.PICKUP,material);
        p.getScheduler().run(this,t->{if(sessions.get(s.request.id())==s&&s.ready&&p.getStatistic(Statistic.PICKUP,material)>before&&s.pickupReceipts.add(receipt))s.pickedUp.incrementAndGet();},null);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void placing(BlockPlaceEvent e){
        if(e.getBlock().getWorld()!=world)return;Session s=sessions.get(id(e.getPlayer()));Block b=e.getBlock();
        if(s==null||!s.ready||!(s.request.stage()==7||s.request.stage()==16)||b.getType()!=Material.OAK_PLANKS||!CampusPlan.placeable(s.request.id(),b.getX(),b.getY(),b.getZ()))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void placed(BlockPlaceEvent e){
        if(e.getBlock().getWorld()!=world)return;Session s=sessions.get(id(e.getPlayer()));if(s==null||!s.ready)return;Block b=e.getBlock();int x=b.getX(),y=b.getY(),z=b.getZ();
        getServer().getRegionScheduler().run(this,world,x>>4,z>>4,t->{if(sessions.get(s.request.id())==s&&s.ready&&world.getBlockAt(x,y,z).getType()==Material.OAK_PLANKS){s.placed.incrementAndGet();s.placedPositions.add(new CampusPlan.Pos(x,y,z));}});
    }
    @EventHandler(priority=EventPriority.MONITOR) public void extracted(FurnaceExtractEvent e){
        Session s=sessions.get(id(e.getPlayer()));if(s!=null&&s.ready&&s.request.stage()==14&&target(s,e.getBlock())&&e.getItemType()==Material.IRON_INGOT&&e.getItemAmount()>0)s.smelted.addAndGet(e.getItemAmount());
    }
    private void transfer(Player p,Inventory top){
        Session s=sessions.get(id(p));if(s==null||!s.ready||s.request.stage()!=15)return;Location l=top.getLocation();if(l==null||l.getWorld()!=world||!target(s,l.getBlock()))return;
        p.getScheduler().run(this,t->{try{if(sessions.get(s.request.id())==s&&s.ready){requireCell(p,s);int after=containerStock(s);if(after>s.transferStock)s.deposited.addAndGet(after-s.transferStock);s.transferStock=after;}}catch(Throwable error){fail(s.request.id(),error);}},null);
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void clicked(InventoryClickEvent e){if(e.getWhoClicked() instanceof Player p)transfer(p,e.getView().getTopInventory());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void dragged(InventoryDragEvent e){if(e.getWhoClicked() instanceof Player p)transfer(p,e.getView().getTopInventory());}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void mobs(CreatureSpawnEvent e){if(e.getLocation().getWorld()==world)e.setCancelled(true);}
    private void fail(int id,Throwable t){
        failed.set(true);Session s=sessions.get(id);if(s!=null)s.ready=false;String error=run+" agent="+id+" "+t.toString().replace('\n',' ')+"\n";outgoing.put("fatal.txt",error);
        if(directory!=null)synchronized(this){try{atomic(directory.resolve("fatal.txt"),error);}catch(Exception writeError){getLogger().severe("Cannot publish fatal receipt: "+writeError);}}
        getLogger().severe("Academy environment failed, actor "+id+": "+t);
    }
    private static void atomic(Path path,String text)throws Exception{Path tmp=path.resolveSibling(path.getFileName()+".tmp");Files.writeString(tmp,text,StandardCharsets.UTF_8);Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
}
