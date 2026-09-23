package org.botsclustersmc.lab;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.*;
import java.util.function.Function;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Resets and server-grounded evidence only. No bot control, pathfinding or recipe execution. */
public final class TaskEnvironment implements Listener {
    public static final Material[] ITEMS={Material.OAK_LOG,Material.OAK_PLANKS,Material.STICK,Material.CRAFTING_TABLE,Material.WOODEN_PICKAXE,
        Material.COBBLESTONE,Material.STONE_PICKAXE,Material.COAL,Material.FURNACE,Material.RAW_IRON,Material.IRON_INGOT,Material.DIRT,Material.CHEST};
    public static final class Session {
        final Protocol.Request request;
        volatile boolean ready;
        final AtomicLong tick=new AtomicLong(),transfers=new AtomicLong();
        final AtomicInteger broken=new AtomicInteger(),picked=new AtomicInteger(),placed=new AtomicInteger(),smelted=new AtomicInteger(),deposited=new AtomicInteger();
        final Set<CampusPlan.Pos> placedBlocks=ConcurrentHashMap.newKeySet();
        int craftBaseline,lastTarget;long lastTransfer;
        Session(Protocol.Request request){this.request=request;}
    }
    private final JavaPlugin plugin;private final World world;private final Function<Player,Session> lookup;
    private final NamespacedKey ownerKey,targetKey;
    public TaskEnvironment(JavaPlugin plugin,World world,Function<Player,Session> lookup){
        this.plugin=plugin;this.world=world;this.lookup=lookup;ownerKey=new NamespacedKey(plugin,"lesson-owner");targetKey=new NamespacedKey(plugin,"target-drop");
    }
    private Session active(Player p){Session s=lookup.apply(p);return s!=null&&s.ready&&p.getWorld()==world?s:null;}
    private boolean ownsRegion(Session s){return Bukkit.isOwnedByCurrentRegion(world,CampusPlan.ox(s.request.id())>>4,CampusPlan.oz(s.request.id())>>4);}
    private String owner(Session s){return s.request.run()+":"+s.request.id()+":"+s.request.token();}
    private CampusPlan.Pos pos(Block b){return new CampusPlan.Pos(b.getX(),b.getY(),b.getZ());}
    private boolean target(Session s,Block b){return b.getWorld()==world&&CampusPlan.goal(s.request).equals(pos(b));}
    private boolean resource(Session s,Block b){
        int stage=s.request.stage();return target(s,b)&&((stage==5||stage==6||stage==17)&&b.getType()==Material.OAK_LOG||stage==12&&b.getType()==Material.STONE);
    }
    private static Material output(int stage){return switch(stage){case 8->Material.OAK_PLANKS;case 9->Material.STICK;case 10,17->Material.CRAFTING_TABLE;case 11->Material.WOODEN_PICKAXE;case 13->Material.STONE_PICKAXE;default->null;};}
    private static int amount(ItemStack[] items,Material material){int count=0;for(ItemStack item:items)if(item!=null&&item.getType()==material)count+=item.getAmount();return count;}
    private static int stock(Player p,Material material){ItemStack cursor=p.getItemOnCursor();return amount(p.getInventory().getContents(),material)+(cursor.getType()==material?cursor.getAmount():0);}
    /** Region scheduler only; every mutable fixture is inside one owned chunk. */
    public void clearRegion(Session s){
        if(!ownsRegion(s))throw new IllegalStateException("reset outside owned region");
        for(Entity entity:world.getChunkAt(CampusPlan.ox(s.request.id())>>4,CampusPlan.oz(s.request.id())>>4).getEntities()){
            if(entity instanceof Item item&&item.getPersistentDataContainer().has(ownerKey,PersistentDataType.STRING))item.remove();
        }
        // Clear existing containers even when the same block type will remain.
        for(int x=2;x<=13;x++)for(int z=2;z<=13;z++){
            BlockState state=world.getBlockAt(CampusPlan.ox(s.request.id())+x,97,CampusPlan.oz(s.request.id())+z).getState();
            if(state instanceof Container c)c.getInventory().clear();
        }
    }
    private void give(Player p,Random random,Material material,int count,boolean hotbar){
        for(int attempt=0;attempt<80;attempt++){int slot=hotbar?random.nextInt(9):random.nextInt(36);
            if(p.getInventory().getItem(slot)==null){p.getInventory().setItem(slot,new ItemStack(material,count));return;}}
        throw new IllegalStateException("fixture inventory unexpectedly full");
    }
    /** Raw-material initial states only. Output items are never furnished. */
    public void furnish(Player p,Session s){
        Random random=new Random(s.request.seed());int stage=s.request.stage();
        switch(stage){
            case 7->give(p,random,Material.OAK_PLANKS,4,true);
            case 8->give(p,random,Material.OAK_LOG,1,false);
            case 9->give(p,random,Material.OAK_PLANKS,2,false);
            case 10->give(p,random,Material.OAK_PLANKS,4,false);
            case 11->{give(p,random,Material.OAK_PLANKS,3,false);give(p,random,Material.STICK,2,false);}
            case 12->give(p,random,Material.WOODEN_PICKAXE,1,true);
            case 13->{give(p,random,Material.COBBLESTONE,3,false);give(p,random,Material.STICK,2,false);}
            case 14->{give(p,random,Material.RAW_IRON,1,false);give(p,random,Material.COAL,1,false);}
            case 15->give(p,random,Material.OAK_LOG,4,false);
            case 16->give(p,random,Material.OAK_PLANKS,12,true);
            default->{}
        }
        if(s.request.difficulty()<0.5&&(stage==7||stage==12||stage==16)){
            for(int i=0;i<9;i++)if(p.getInventory().getItem(i)!=null){p.getInventory().setHeldItemSlot(i);break;}
        }
    }
    private void take(Player p,Material material){if(!p.getInventory().removeItem(new ItemStack(material,1)).isEmpty())throw new IllegalStateException("reset scaffold exceeds furnished materials");}
    /** Optional reverse-curriculum assistance, executed ONCE before ready=true.
     * Full probes and exams use difficulty=1: no open menus or prepared grids.
     * Even a prepared recipe must be acquired through a policy-selected click.
     */
    public void start(Player p,Session s){
        if(!ownsRegion(s))throw new IllegalStateException("fixture start outside owned region");
        int stage=s.request.stage();double d=s.request.difficulty();CampusPlan.Pos g=CampusPlan.goal(s.request);
        if(d<0.85&&(stage==11||stage==13))p.openWorkbench(new Location(world,g.x(),g.y(),g.z()),false);
        if(d<0.85&&(stage==14||stage==15)){
            BlockState state=world.getBlockAt(g.x(),g.y(),g.z()).getState();if(!(state instanceof Container c))throw new IllegalStateException("missing task container");
            if(stage==14&&d<0.65&&state instanceof Furnace f){
                take(p,Material.COAL);f.getInventory().setFuel(new ItemStack(Material.COAL));
                if(d<0.4){take(p,Material.RAW_IRON);f.getInventory().setSmelting(new ItemStack(Material.RAW_IRON));}
            }
            p.openInventory(c.getInventory());
        }
        if(d<0.85&&(stage==8||stage==9||stage==10||stage==11||stage==13)){
            Inventory inventory=p.getOpenInventory().getTopInventory();
            if(!(inventory instanceof CraftingInventory c))throw new IllegalStateException("expected vanilla crafting grid");
            int[] cells=switch(stage){case 8->new int[]{0};case 9->new int[]{0,2};case 10->new int[]{0,1,2,3};default->new int[]{0,1,2,4,7};};
            int count=d<0.4?cells.length:d<0.65?Math.max(0,cells.length-1):cells.length/2;
            ItemStack[] matrix=c.getMatrix();
            for(int i=0;i<count;i++){
                Material material=stage==8?Material.OAK_LOG:stage==13&&i<3?Material.COBBLESTONE:(stage==11||stage==13)&&i>=3?Material.STICK:Material.OAK_PLANKS;
                take(p,material);matrix[cells[i]]=new ItemStack(material,1);
            }
            c.setMatrix(matrix);p.updateInventory();
        }
        Material material=output(stage);s.craftBaseline=material==null?0:p.getStatistic(Statistic.CRAFT_ITEM,material);
    }
    /** Player entity scheduler only. Excludes all crafting result previews. */
    public String frame(Player p,Session s,String run){
        Location l=p.getLocation();long tick=s.tick.addAndGet(4);int targetStock=0,occupied=0;
        if(ownsRegion(s)){
            if(s.request.stage()==15){CampusPlan.Pos g=CampusPlan.goal(s.request);BlockState state=world.getBlockAt(g.x(),g.y(),g.z()).getState();
                if(state instanceof Chest c)targetStock=amount(c.getBlockInventory().getContents(),Material.OAK_LOG);
                long transfer=s.transfers.get();if(transfer!=s.lastTransfer&&targetStock>s.lastTarget)s.deposited.addAndGet(targetStock-s.lastTarget);
                s.lastTarget=targetStock;s.lastTransfer=transfer;
            }
            if(s.request.stage()==7||s.request.stage()==16){int bit=0;for(CampusPlan.Pos g:CampusPlan.targets(s.request)){
                if(world.getBlockAt(g.x(),g.y(),g.z()).getType()==Material.OAK_PLANKS)occupied|=1<<bit;bit++;}}
        }
        Material crafted=output(s.request.stage());int craftCount=crafted==null?0:Math.max(0,p.getStatistic(Statistic.CRAFT_ITEM,crafted)-s.craftBaseline);
        StringBuilder out=new StringBuilder(String.format(Locale.ROOT,"BCMCLAB3 %s %s %d %d %.8f %.8f %.8f %.5f %.5f %s",run,s.request.token(),s.request.id(),tick,l.getX(),l.getY(),l.getZ(),l.getYaw(),l.getPitch(),p.isOnGround()));
        for(Material material:ITEMS)out.append(' ').append(stock(p,material));
        return out.append(' ').append(s.broken.get()).append(' ').append(s.picked.get()).append(' ').append(craftCount).append(' ').append(s.placed.get()).append(' ').append(s.smelted.get()).append(' ').append(s.deposited.get()).append(' ').append(targetStock).append(' ').append(occupied).append('\n').toString();
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void breaking(BlockBreakEvent e){
        if(e.getBlock().getWorld()!=world)return;Session s=active(e.getPlayer());
        if(s==null||!(resource(s,e.getBlock())||s.placedBlocks.contains(pos(e.getBlock())))){e.setCancelled(true);return;}
        if(s.request.stage()==5)e.setDropItems(false);
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)public void broken(BlockBreakEvent e){
        Session s=active(e.getPlayer());if(s==null||e.getBlock().getWorld()!=world)return;boolean resource=resource(s,e.getBlock());CampusPlan.Pos b=pos(e.getBlock());
        plugin.getServer().getRegionScheduler().run(plugin,world,b.x()>>4,b.z()>>4,task->{
            if(lookup.apply(e.getPlayer())==s&&s.ready&&world.getBlockAt(b.x(),b.y(),b.z()).getType()==Material.AIR){if(resource)s.broken.incrementAndGet();s.placedBlocks.remove(b);}
        });
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)public void drops(BlockDropItemEvent e){
        Session s=active(e.getPlayer());if(s==null||!target(s,e.getBlock())||!(s.request.stage()==6||s.request.stage()==12||s.request.stage()==17))return;
        for(Item item:e.getItems()){item.getPersistentDataContainer().set(ownerKey,PersistentDataType.STRING,owner(s));item.getPersistentDataContainer().set(targetKey,PersistentDataType.INTEGER,1);}
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void playerDrop(PlayerDropItemEvent e){
        if(e.getPlayer().getWorld()!=world)return;Session s=active(e.getPlayer());if(s==null){e.setCancelled(true);return;}
        e.getItemDrop().getPersistentDataContainer().set(ownerKey,PersistentDataType.STRING,owner(s));e.getItemDrop().getPersistentDataContainer().set(targetKey,PersistentDataType.INTEGER,0);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void pickupGuard(EntityPickupItemEvent e){
        if(e.getEntity().getWorld()!=world)return;
        if(!(e.getEntity() instanceof Player p)){e.setCancelled(true);return;}Session s=active(p);
        if(s==null||!owner(s).equals(e.getItem().getPersistentDataContainer().get(ownerKey,PersistentDataType.STRING)))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)public void picked(EntityPickupItemEvent e){
        if(!(e.getEntity() instanceof Player p))return;Session s=active(p);if(s==null||!Integer.valueOf(1).equals(e.getItem().getPersistentDataContainer().get(targetKey,PersistentDataType.INTEGER)))return;
        Material material=e.getItem().getItemStack().getType();int before=p.getStatistic(Statistic.PICKUP,material);int accepted=e.getItem().getItemStack().getAmount()-e.getRemaining();
        p.getScheduler().run(plugin,task->{if(active(p)==s){int delta=p.getStatistic(Statistic.PICKUP,material)-before;if(delta>0)s.picked.addAndGet(Math.min(delta,accepted));}},null);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void placing(BlockPlaceEvent e){
        if(e.getBlock().getWorld()!=world)return;Session s=active(e.getPlayer());
        if(s==null||!(s.request.stage()==7||s.request.stage()==16)||e.getBlock().getType()!=Material.OAK_PLANKS||!CampusPlan.interior(s.request.id(),pos(e.getBlock())))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)public void placed(BlockPlaceEvent e){
        Session s=active(e.getPlayer());if(s==null||e.getBlock().getWorld()!=world)return;CampusPlan.Pos b=pos(e.getBlock());
        plugin.getServer().getRegionScheduler().run(plugin,world,b.x()>>4,b.z()>>4,task->{if(lookup.apply(e.getPlayer())==s&&s.ready&&world.getBlockAt(b.x(),b.y(),b.z()).getType()==Material.OAK_PLANKS){s.placed.incrementAndGet();s.placedBlocks.add(b);}});
    }
    @EventHandler(priority=EventPriority.MONITOR)public void extracted(FurnaceExtractEvent e){
        Player p=e.getPlayer();Session s=active(p);if(s==null||s.request.stage()!=14||e.getItemType()!=Material.IRON_INGOT||!target(s,e.getBlock()))return;
        // The vanilla event is emitted on an actual extraction, not recipe preview.
        s.smelted.addAndGet(e.getItemAmount());
    }
    private boolean ownContainer(Session s,Inventory inventory){Location l=inventory.getLocation();return l!=null&&l.getWorld()==world&&CampusPlan.goal(s.request).equals(new CampusPlan.Pos(l.getBlockX(),l.getBlockY(),l.getBlockZ()));}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void opening(InventoryOpenEvent e){
        if(!(e.getPlayer() instanceof Player p)||p.getWorld()!=world)return;Session s=lookup.apply(p);
        if(s==null)return;int stage=s.request.stage();
        if(!(stage==11||stage==13||stage==14||stage==15)||!ownContainer(s,e.getInventory()))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void clickGuard(InventoryClickEvent e){
        if(e.getWhoClicked().getWorld()==world&&(!(e.getWhoClicked() instanceof Player p)||active(p)==null))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)public void clicked(InventoryClickEvent e){
        if(e.getWhoClicked() instanceof Player p){Session s=active(p);if(s!=null&&s.request.stage()==15&&ownContainer(s,e.getView().getTopInventory()))s.transfers.incrementAndGet();}
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void dragGuard(InventoryDragEvent e){
        if(e.getWhoClicked().getWorld()==world&&(!(e.getWhoClicked() instanceof Player p)||active(p)==null))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)public void dragged(InventoryDragEvent e){
        if(e.getWhoClicked() instanceof Player p){Session s=active(p);if(s!=null&&s.request.stage()==15&&ownContainer(s,e.getView().getTopInventory()))s.transfers.incrementAndGet();}
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void interact(PlayerInteractEvent e){
        if(e.getPlayer().getWorld()!=world||e.getClickedBlock()==null)return;Session s=active(e.getPlayer());if(s==null){e.setCancelled(true);return;}
        Block b=e.getClickedBlock();int id=s.request.id();if(b.getWorld()!=world||b.getX()<CampusPlan.ox(id)||b.getX()>=CampusPlan.ox(id)+16||b.getZ()<CampusPlan.oz(id)||b.getZ()>=CampusPlan.oz(id)+16)e.setCancelled(true);
    }
}
