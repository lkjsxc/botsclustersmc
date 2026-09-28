package org.botsclustersmc.diagnostic;

import java.util.*;
import org.botsclustersmc.core.*;
import org.botsclustersmc.core.Stack;
import org.botsclustersmc.plugin.*;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.Item;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;

/** Native harvest-spawn boundaries in disposable rooms; not a learned-policy test. */
final class HarvestDropChecks implements Listener {
    private static final ThreadLocal<Trial> current=new ThreadLocal<>();
    private static int checks,cases;
    private static final List<String> failures=new ArrayList<>();
    private static void check(boolean value,String message){checks++;if(!value)failures.add(message);}
    static void install(RuntimePlugin plugin){Bukkit.getPluginManager().registerEvents(new HarvestDropChecks(),plugin);}
    private static final class Trial {
        final Npc npc;final String mode,token;final Material material;
        Item item;String observedToken;int observedDelay,spawnEvents,dropEvents;Throwable callbackFailure;
        Trial(Npc npc,Material material,String mode){this.npc=npc;this.material=material;this.mode=mode;token=npc.token();}
        String label(){return material+"/"+mode;}
    }
    @EventHandler public void spawn(ItemSpawnEvent event){
        Trial t=current.get();if(t==null)return;
        t.spawnEvents++;t.item=event.getEntity();
        try{
            t.observedToken=t.item.getPersistentDataContainer().get(t.npc.plugin.provenance,PersistentDataType.STRING);
            t.observedDelay=t.item.getPickupDelay();
            switch(t.mode){
                case "cancel"->event.setCancelled(true);
                case "delay"->t.item.setPickupDelay(73);
                case "provenance"->t.item.getPersistentDataContainer().set(t.npc.plugin.provenance,PersistentDataType.STRING,"listener-owned");
                case "remove-provenance"->t.item.getPersistentDataContainer().remove(t.npc.plugin.provenance);
                case "goal"->{Goal g=t.npc.goal;t.npc.goal=new Goal(g.task(),g.x(),g.y(),g.z(),g.episode()+1,1,3000);}
                case "stack"->{ItemStack s=new ItemStack(Material.DIAMOND,3);s.editMeta(m->m.displayName(Component.text("Listener replacement")));t.item.setItemStack(s);}
                case "mob-pickup"->t.item.setCanMobPickup(false);
                case "owner"->t.item.setOwner(t.npc.entity.getUniqueId());
                case "thrower"->t.item.setThrower(t.npc.entity.getUniqueId());
                default->{}
            }
        }catch(Throwable failure){t.callbackFailure=failure;}
    }
    @EventHandler public void drop(EntityDropItemEvent event){
        Trial t=current.get();if(t!=null&&event.getEntity()==t.npc.entity)t.dropEvents++;
    }
    private static void aim(Npc n,Location target){
        Location eye=n.entity.getEyeLocation();double dx=target.getX()-eye.getX(),dy=target.getY()-eye.getY(),dz=target.getZ()-eye.getZ();
        n.entity.setRotation((float)Math.toDegrees(Math.atan2(-dx,dz)),(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz))));
    }
    private static void trial(Npc n,Block target,Material material,String mode){
        cases++;Goal original=n.goal;
        n.pocket.clear();n.pocket.setStorage(0,new Stack("WOODEN_PICKAXE",1));
        n.mining=null;n.miningTicks=0;n.broken.clear();n.collected.clear();n.tick=1;
        target.setType(material,false);aim(n,target.getLocation().add(.5,.5,.5));
        Trial t=new Trial(n,material,mode);current.set(t);
        try{
            int[] action=Schema.IDLE.clone();action[4]=1;
            for(int i=0;i<(material==Material.STONE?40:60);i++)WorldActions.tick(n,action,i==0);
            check(t.callbackFailure==null,"callback failed: "+t.label()+" "+t.callbackFailure);
            check(t.spawnEvents==1,"spawn coverage: "+t.label()+" "+t.spawnEvents);
            check(t.dropEvents==0,"harvesting invented an inventory-drop event: "+t.label());
            check(target.getType().isAir(),"block not harvested: "+t.label());
            check(n.broken.getOrDefault(material.name(),0L)==1,"missing single break credit: "+t.label());
            check(n.pocket.storage(0).equals(new Stack("WOODEN_PICKAXE",1)),"harvest consumed held stock: "+t.label());
            check(n.collected.isEmpty(),"spawn alone earned pickup credit: "+t.label());
            if(t.item==null)return;
            check(t.token.equals(t.observedToken),"provenance unavailable inside spawn event: "+t.label());
            check(t.observedDelay==0,"initial delay unavailable inside spawn event: "+t.label()+" "+t.observedDelay);
            check(t.item.isValid()!=mode.equals("cancel"),"spawn cancellation not respected: "+t.label());
            String expected=mode.equals("provenance")?"listener-owned":mode.equals("remove-provenance")?null:t.token;
            String actual=t.item.getPersistentDataContainer().get(n.plugin.provenance,PersistentDataType.STRING);
            check(Objects.equals(actual,expected),"provenance changed after listener: "+t.label()+" expected="+expected+" actual="+actual);
            check(t.item.getPickupDelay()==(mode.equals("delay")?73:0),"listener delay overwritten: "+t.label());
            if(mode.equals("goal"))check(!n.plugin.canPickup(n,actual),"old harvest eligible for new episode: "+t.label());
            if(mode.equals("stack"))check(t.item.getItemStack().getType()==Material.DIAMOND&&t.item.getItemStack().getAmount()==3&&t.item.getItemStack().hasItemMeta(),"listener item overwritten: "+t.label());
            else check(t.item.getItemStack().getType()==(material==Material.STONE?Material.COBBLESTONE:Material.OAK_LOG)&&t.item.getItemStack().getAmount()==1,"unexpected harvest yield: "+t.label());
            if(mode.equals("mob-pickup"))check(!t.item.canMobPickup(),"listener mob-pickup denial overwritten: "+t.label());
            if(mode.equals("owner"))check(n.entity.getUniqueId().equals(t.item.getOwner()),"listener owner overwritten: "+t.label());
            if(mode.equals("thrower"))check(n.entity.getUniqueId().equals(t.item.getThrower()),"listener thrower overwritten: "+t.label());
        }finally{
            current.remove();n.goal=original;if(t.item!=null)t.item.remove();target.setType(Material.AIR,false);
        }
    }
    static void verify(Npc n){
        if(n.goal.task()!=Task.SUPPLY_CHEST)return;
        Block target=new Location(n.entity.getWorld(),n.goal.x(),n.goal.y(),n.goal.z()).getBlock();
        var data=target.getBlockData().clone();
        Inventory chest=((Chest)target.getState()).getBlockInventory();
        ItemStack[] contents=Arrays.stream(chest.getContents()).map(s->s==null?null:s.clone()).toArray(ItemStack[]::new);
        Stack[] storage=new Stack[36];for(int i=0;i<36;i++)storage[i]=n.pocket.storage(i);
        int selected=n.pocket.selected();long tick=n.tick;Location at=n.entity.getLocation();
        var velocity=n.entity.getVelocity().clone();var equipment=n.entity.getEquipment().getItemInMainHand().clone();
        var broken=new HashMap<>(n.broken);var collected=new HashMap<>(n.collected);
        try{
            for(Material material:List.of(Material.OAK_LOG,Material.STONE))
                for(String mode:List.of("normal","cancel","delay","provenance","remove-provenance","goal","stack","mob-pickup","owner","thrower"))trial(n,target,material,mode);
        }finally{
            target.setBlockData(data,false);((Chest)target.getState()).getBlockInventory().setContents(contents);
            n.pocket.clear();for(int i=0;i<36;i++)n.pocket.setStorage(i,storage[i]);n.pocket.select(selected);
            n.tick=tick;n.mining=null;n.miningTicks=0;n.entity.setRotation(at.getYaw(),at.getPitch());
            n.entity.setVelocity(velocity);n.entity.getEquipment().setItemInMainHand(equipment);
            n.broken.clear();n.broken.putAll(broken);n.collected.clear();n.collected.putAll(collected);
        }
        if(!failures.isEmpty()){
            for(String failure:failures)System.out.println("HARVEST DROP FAILURE "+failure);
            throw new AssertionError("harvest spawn boundary: "+failures.size()+" failures / "+checks+" checks / "+cases+" cases");
        }
        System.out.println("HARVEST DROP LIVE PASS checks="+checks+" cases="+cases);
    }
}
