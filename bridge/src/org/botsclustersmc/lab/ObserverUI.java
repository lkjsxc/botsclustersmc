package org.botsclustersmc.lab;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/** Human-only UI. No observer operation changes an actor, reward or course. */
public final class ObserverUI implements Listener {
    private final JavaPlugin plugin;private final World world;private final int bots,defaultView;private final String prefix;
    private final Map<UUID,View> viewers=new ConcurrentHashMap<>();private volatile ObserverState.Snapshot snapshot;
    private static final class View{int selected,page,view;boolean touring;int tourTicks;volatile boolean moving;View(int distance){view=distance;}}
    private static final class Menu implements InventoryHolder{final int page;Inventory inventory;Menu(int page){this.page=page;}@Override public Inventory getInventory(){return inventory;}}
    public ObserverUI(JavaPlugin plugin,World world,int bots,int distance,String prefix){this.plugin=plugin;this.world=world;this.bots=bots;this.defaultView=distance;this.prefix=prefix;plugin.getServer().getPluginManager().registerEvents(this,plugin);}
    public void publish(ObserverState.Snapshot snapshot){this.snapshot=snapshot;}
    public void join(Player p){
        View v=new View(defaultView);viewers.put(p.getUniqueId(),v);p.setGameMode(GameMode.SPECTATOR);distance(p,v.view);watch(p,v,0);
        p.sendMessage("Academy observer: /academy opens all "+bots+" bots. /academy overview, next, prev, tour, view 12.");
        p.getScheduler().runAtFixedRate(plugin,task->{
            if(!p.isOnline()||viewers.get(p.getUniqueId())!=v){task.cancel();return;}
            if(v.touring&&++v.tourTicks>=12){v.tourTicks=0;watch(p,v,(v.selected+1)%bots);}
            ObserverState.Snapshot s=snapshot;
            if(s==null||!s.fresh(System.currentTimeMillis()/1000)){p.sendActionBar(Component.text("Waiting for a fresh learner status",NamedTextColor.GRAY));return;}
            ObserverState.Row r=s.rows().get(v.selected);
            String text=String.format(Locale.ROOT,"%s%02d | %s | %s | success %.0f%% | difficulty %.2f | PPO %d",prefix,r.id(),TaskFixtures.NAMES[r.task()],r.state(),r.success()*100,r.difficulty(),s.policy());
            p.sendActionBar(Component.text(text,NamedTextColor.AQUA));
            p.setPlayerListHeaderFooter("botsclustersmc | "+bots+" RL actors",String.format(Locale.ROOT,"Stage %d/18 | %s | %,d trained samples | %d/%d at update barrier",s.frontier()+1,s.phase(),s.samples(),s.sealed(),bots));
        },null,20,20);
    }
    private void distance(Player p,int n){p.setViewDistance(n);p.setSendViewDistance(n);p.setSimulationDistance(3);}
    private void move(Player p,View v,Location location){
        if(v.moving)return;v.moving=true;
        p.teleportAsync(location).whenComplete((ok,error)->{v.moving=false;if(error!=null)plugin.getLogger().warning("Observer teleport failed: "+error);});
    }
    private void watch(Player p,View v,int id){if(id<0||id>=bots)throw new IllegalArgumentException("bot range");if(v.moving)return;v.selected=id;move(p,v,new Location(world,CampusPlan.ox(id)+8,109,CampusPlan.oz(id)+8,0,80));}
    private void overview(Player p,View v){v.touring=false;move(p,v,new Location(world,64,185,((bots+7)/8)*8,0,90));}
    public boolean command(Player p,String[] args){
        View v=viewers.get(p.getUniqueId());if(v==null)return false;
        try{
            if(args.length==0){menu(p,v,v.page);return true;}
            switch(args[0]){
                case "watch"->{if(args.length!=2)throw new IllegalArgumentException();v.touring=false;watch(p,v,Integer.parseInt(args[1]));}
                case "next"->{v.touring=false;watch(p,v,(v.selected+1)%bots);}
                case "prev"->{v.touring=false;watch(p,v,(v.selected+bots-1)%bots);}
                case "overview"->overview(p,v);
                case "tour"->{v.touring=!v.touring;v.tourTicks=0;p.sendMessage("Automatic 12-second tour: "+(v.touring?"on":"off"));}
                case "view"->{if(args.length!=2)throw new IllegalArgumentException();int n=Integer.parseInt(args[1]);if(n<3||n>16)throw new IllegalArgumentException();v.view=n;distance(p,n);p.sendMessage("Observer view distance: "+n+" chunks. Set your client render distance at least this high.");}
                default->throw new IllegalArgumentException();
            }
        }catch(IllegalArgumentException e){p.sendMessage("/academy [watch 0.."+(bots-1)+" | next | prev | overview | tour | view 3..16]");}
        return true;
    }
    private static ItemStack item(Material material,String name,String...lore){ItemStack i=new ItemStack(material);var m=i.getItemMeta();m.displayName(Component.text(name,NamedTextColor.AQUA));m.lore(Arrays.stream(lore).map(s->Component.text(s,NamedTextColor.GRAY)).toList());i.setItemMeta(m);return i;}
    private void menu(Player p,View v,int page){
        v.page=Math.floorMod(page,ObserverState.pages(bots));Menu holder=new Menu(v.page);
        holder.inventory=Bukkit.createInventory(holder,54,Component.text("RL Academy | "+(v.page+1)+"/"+ObserverState.pages(bots)));
        var inv=holder.inventory;var s=snapshot;boolean fresh=s!=null&&s.fresh(System.currentTimeMillis()/1000);
        for(int slot=0;slot<45;slot++){
            int id=v.page*45+slot;if(id>=bots)break;
            if(!fresh){inv.setItem(slot,item(Material.GRAY_CONCRETE,prefix+String.format(Locale.ROOT,"%02d",id),"Waiting for current learner status","Click to watch this cell"));continue;}
            var r=s.rows().get(id);Material color=r.state().equals("waiting")?Material.GRAY_CONCRETE:r.state().equals("exam")?Material.YELLOW_CONCRETE:Material.LIME_CONCRETE;
            inv.setItem(slot,item(color,prefix+String.format(Locale.ROOT,"%02d",id),TaskFixtures.NAMES[r.task()],"State: "+r.state(),String.format(Locale.ROOT,"Success EMA %.0f%% | difficulty %.2f",r.success()*100,r.difficulty()),"Attempts: "+r.attempts()+" | exam "+r.successes()+"/"+r.trials(),"Click to watch (read-only)"));
        }
        inv.setItem(45,item(Material.ARROW,"Previous page"));inv.setItem(48,item(Material.SPYGLASS,"View distance: "+v.view,"Click to cycle 8 / 12 / 16 chunks","Only your observer view changes"));
        inv.setItem(49,item(Material.COMPASS,"Campus overview"));inv.setItem(50,item(Material.CLOCK,"Tour: "+(v.touring?"on":"off"),"Automatically visit each cell"));inv.setItem(53,item(Material.ARROW,"Next page"));p.openInventory(inv);
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void click(InventoryClickEvent e){
        if(!(e.getView().getTopInventory().getHolder() instanceof Menu m)||!(e.getWhoClicked() instanceof Player p))return;
        e.setCancelled(true);int slot=e.getRawSlot();View v=viewers.get(p.getUniqueId());if(v==null||slot<0||slot>=54)return;
        p.getScheduler().run(plugin,t->{
            if(viewers.get(p.getUniqueId())!=v)return;
            if(slot<45){int id=m.page*45+slot;if(id<bots){p.closeInventory();v.touring=false;watch(p,v,id);}}
            else switch(slot){case 45->menu(p,v,m.page-1);case 53->menu(p,v,m.page+1);case 49->{p.closeInventory();overview(p,v);}case 50->{v.touring=!v.touring;v.tourTicks=0;menu(p,v,m.page);}case 48->{v.view=v.view<8?8:v.view<12?12:v.view<16?16:8;distance(p,v.view);menu(p,v,m.page);}default->{}}
        },null);
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void drag(InventoryDragEvent e){if(e.getView().getTopInventory().getHolder() instanceof Menu)e.setCancelled(true);}
    @EventHandler public void quit(PlayerQuitEvent e){viewers.remove(e.getPlayer().getUniqueId());}
}
