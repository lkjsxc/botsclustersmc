package org.botsclustersmc.diagnostic;

import java.util.*;
import org.botsclustersmc.core.*;
import org.botsclustersmc.core.Stack;
import org.botsclustersmc.plugin.*;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.Item;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.*;
import org.bukkit.loot.*;

/** Disposable owner-thread actuator regressions, not neural decisions or survival. */
final class ContainerBoundaryChecks implements Listener {
    private static int checks,lootEvents,changeEvents;
    private static Npc active;
    private static String onChange="";
    private static final long LOOT_SEED=2026100737L;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static void install(RuntimePlugin plugin){Bukkit.getPluginManager().registerEvents(new ContainerBoundaryChecks(),plugin);}
    @EventHandler public void loot(LootGenerateEvent event){if(active!=null)lootEvents++;}
    @EventHandler(ignoreCancelled=true) public void changing(EntityChangeBlockEvent event){
        if(active==null||event.getEntity()!=active.entity||onChange.isEmpty())return;
        changeEvents++;
        Container state=(Container)event.getBlock().getState();
        if(onChange.equals("lock"))state.setLock("fixture-key");
        else ((Lootable)state).setLootTable(LootTables.SPAWN_BONUS_CHEST.getLootTable(),LOOT_SEED);
        check(state.update(true,false),"callback protection update failed");
        onChange="";
    }
    private static void aim(Npc npc,Location at){
        Location eye=npc.entity.getEyeLocation();double dx=at.getX()+.5-eye.getX(),dy=at.getY()+.5-eye.getY(),dz=at.getZ()+.5-eye.getZ();
        npc.entity.setRotation((float)Math.toDegrees(Math.atan2(-dx,dz)),(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz))));
    }
    private static Inventory inventory(Block block){
        return block.getState() instanceof Chest chest?chest.getBlockInventory():((Furnace)block.getState()).getInventory();
    }
    private static void prepare(Npc npc,Block block,Material type){
        onChange="";block.setType(Material.AIR,false);block.setType(type,false);
        npc.pocket.clear();npc.container=null;npc.mining=null;npc.miningTicks=0;
        npc.broken.clear();npc.collected.clear();npc.placed.clear();aim(npc,block.getLocation());
    }
    private static void lock(Block block){Container state=(Container)block.getState();state.setLock("fixture-key");check(state.update(true,false),"lock update failed");}
    private static void act(Npc npc,int input,int click,int slot){int[] action=Schema.IDLE.clone();action[4]=input;action[6]=click;action[7]=slot;WorldActions.tick(npc,action,true);}
    private static void locked(Npc npc,Block block,Material type,Pocket.Menu menu){
        prepare(npc,block,type);inventory(block).setItem(0,new ItemStack(Material.OAK_LOG,8));
        npc.pocket.open(menu);npc.container=block.getLocation();
        check(npc.pocket.wouldChange(3,36,ExternalInventory.locate(npc)),"ordinary storage action missing");
        lock(block);
        check(ExternalInventory.locate(npc)==Pocket.NONE,"locked "+type+" exposed its inventory");
        act(npc,0,3,36);
        check(npc.pocket.count("OAK_LOG")==0&&inventory(block).getItem(0).getAmount()==8,"lock revoked after observation did not prevent withdrawal");
        // A rejected external transfer must also preserve a cursor payload.
        npc.pocket.setStorage(0,new Stack("OAK_PLANKS",4));npc.pocket.click(1,0,Pocket.NONE);
        act(npc,0,1,36);
        check(npc.pocket.cursor().equals(new Stack("OAK_PLANKS",4)),"locked deposit spent cursor");
        Sensors.capture(npc);
        check(npc.pocket.menu()==Pocket.Menu.CLOSED&&npc.container==null&&npc.pocket.count("OAK_PLANKS")==4,"invalid menu closure lost carried stock");
        act(npc,2,0,0);
        check(npc.pocket.menu()==Pocket.Menu.CLOSED,"use opened locked "+type);
        // Clear only this fixture's synthetic stock; keep its native lock.
        inventory(block).clear();npc.pocket.clear();
        for(int i=0;i<65;i++)act(npc,1,0,0);
        check(block.getType()==type&&((Container)block.getState()).isLocked(),"mining destroyed locked "+type);
        check(npc.miningTicks==0&&npc.broken.isEmpty(),"locked storage accumulated mining progress/credit");
        Container state=(Container)block.getState();state.setLock(null);check(state.update(true,false),"unlock fixture setup failed");
        act(npc,2,0,0);check(npc.pocket.menu()==menu,"ordinary unlocked storage no longer opens");
        npc.pocket.close();npc.container=null;
    }
    private static void loot(Npc npc,Block block){
        prepare(npc,block,Material.CHEST);Chest chest=(Chest)block.getState();
        chest.setLootTable(LootTables.SPAWN_BONUS_CHEST.getLootTable(),LOOT_SEED);check(chest.update(true,false),"loot setup failed");
        int events=lootEvents;
        npc.pocket.open(Pocket.Menu.CHEST);npc.container=block.getLocation();
        check(ExternalInventory.locate(npc)==Pocket.NONE,"loot-bearing chest exposed an inventory");
        act(npc,0,3,36);Sensors.capture(npc);act(npc,2,0,0);
        check(npc.pocket.menu()==Pocket.Menu.CLOSED&&npc.pocket.count("OAK_LOG")==0,"loot-bearing chest was opened or looted");
        for(int i=0;i<65;i++)act(npc,1,0,0);
        check(block.getType()==Material.CHEST,"mining destroyed deferred loot");
        Chest after=(Chest)block.getState();
        check(after.getLootTable()!=null&&after.getSeed()==LOOT_SEED,"deferred loot identity was altered");
        check(npc.miningTicks==0&&npc.broken.isEmpty()&&lootEvents==events,"protected read generated loot or mining credit");
    }
    private static void callbacks(Npc npc,Block block){
        for(String protection:List.of("lock","loot")){
            prepare(npc,block,Material.CHEST);onChange=protection;int events=changeEvents;
            for(int i=0;i<65;i++)act(npc,1,0,0);
            check(changeEvents==events+1,"protection callback was not exercised exactly once");
            check(block.getType()==Material.CHEST&&npc.broken.isEmpty(),"callback-added "+protection+" was destroyed");
            Chest state=(Chest)block.getState();
            check(protection.equals("lock")?state.isLocked():state.getLootTable()!=null,"callback protection missing");
            check(npc.miningTicks==0,"callback protection retained mining progress");
        }
        prepare(npc,block,Material.CHEST);
        for(int i=0;i<60;i++)act(npc,1,0,0);
        check(block.getType()==Material.AIR&&npc.broken.getOrDefault("CHEST",0L)==1,"ordinary empty chest no longer mines");
    }
    static void verify(Npc npc){
        if(npc.goal.task()!=Task.at(15))return;
        check(npc.pocket.menu()==Pocket.Menu.CLOSED&&npc.pocket.cursor().empty(),"boundary fixture requires initial closed pocket");
        Location at=new Location(npc.entity.getWorld(),npc.goal.x(),npc.goal.y(),npc.goal.z());
        check(WorldActions.owned(at)&&Bukkit.isOwnedByCurrentRegion(at,1),"boundary fixture region unavailable");
        Block block=at.getBlock();check(block.getState() instanceof Chest,"boundary fixture chest missing");
        var originalData=block.getBlockData().clone();ItemStack[] stock=inventory(block).getContents();
        Stack[] pocket=new Stack[36];for(int i=0;i<36;i++)pocket[i]=npc.pocket.storage(i);
        int selected=npc.pocket.selected();Location original=npc.entity.getLocation();var velocity=npc.entity.getVelocity().clone();
        ItemStack held=npc.entity.getEquipment().getItemInMainHand().clone();float[] kinematics=npc.previousKinematics.clone();
        Set<UUID> before=new HashSet<>();for(var entity:at.getWorld().getNearbyEntities(at,2,2,2))before.add(entity.getUniqueId());
        active=npc;
        try{
            locked(npc,block,Material.CHEST,Pocket.Menu.CHEST);
            locked(npc,block,Material.FURNACE,Pocket.Menu.FURNACE);
            loot(npc,block);callbacks(npc,block);
            System.out.println("CONTAINER BOUNDARY LIVE PASS checks="+checks+" callback_changes="+changeEvents+" unexpected_loot_events="+lootEvents);
        }finally{
            active=null;onChange="";
            block.setType(Material.AIR,false);block.setBlockData(originalData,false);inventory(block).setContents(stock);
            for(var entity:at.getWorld().getNearbyEntities(at,2,2,2))if(entity instanceof Item&&!before.contains(entity.getUniqueId()))entity.remove();
            npc.pocket.clear();for(int i=0;i<36;i++)npc.pocket.setStorage(i,pocket[i]);npc.pocket.select(selected);
            npc.container=null;npc.mining=null;npc.miningTicks=0;npc.broken.clear();npc.collected.clear();npc.placed.clear();
            npc.entity.setRotation(original.getYaw(),original.getPitch());npc.entity.setVelocity(velocity);npc.entity.getEquipment().setItemInMainHand(held);
            System.arraycopy(kinematics,0,npc.previousKinematics,0,kinematics.length);
        }
    }
}
