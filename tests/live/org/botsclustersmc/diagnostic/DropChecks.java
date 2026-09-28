package org.botsclustersmc.diagnostic;

import java.util.*;
import org.botsclustersmc.core.*;
import org.botsclustersmc.core.Stack;
import org.botsclustersmc.plugin.*;
import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;

/** Scripted adapter regressions only; never included in either runtime JAR. */
final class DropChecks implements Listener {
    private static final ThreadLocal<Trial> current=new ThreadLocal<>();
    private static int checks,cases;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static void install(RuntimePlugin plugin){Bukkit.getPluginManager().registerEvents(new DropChecks(),plugin);}
    private static int[] action(){int[] a=Schema.IDLE.clone();a[4]=3;return a;}
    private static Stack[] storage(Npc n){Stack[] s=new Stack[36];for(int i=0;i<s.length;i++)s[i]=n.pocket.storage(i);return s;}
    private static final class Trial {
        final Npc npc;final String phase,mode;final List<Item> items=new ArrayList<>();
        int spawnEvents,dropEvents;Stack[] expected;Throwable callbackFailure;
        Trial(Npc npc,String phase,String mode){this.npc=npc;this.phase=phase;this.mode=mode;expected=storage(npc);}
        void mutate(Item item,Cancellable event){
            switch(mode){
                case "cancel"->event.setCancelled(true);
                case "held-kind"->npc.pocket.setStorage(0,new Stack("OAK_PLANKS",4));
                case "held-count"->npc.pocket.setStorage(0,new Stack("OAK_LOG",2));
                case "selected"->npc.pocket.select(1);
                case "menu"->npc.pocket.open(Pocket.Menu.INVENTORY);
                case "goal"->npc.goal=new Goal(Task.at(6),0,65,0,999,1,100);
                case "paused"->npc.paused=true;
                case "global-pause"->npc.plugin.paused.set(true);
                case "resetting"->npc.resetting=true;
                case "remove-actor"->npc.remove.set(true);
                case "remove-item"->item.remove();
                case "item-kind"->item.setItemStack(new ItemStack(Material.DIAMOND));
                case "item-count"->item.setItemStack(new ItemStack(Material.OAK_LOG,32));
                case "metadata"->{ItemStack changed=item.getItemStack().clone();changed.editMeta(meta->meta.displayName(Component.text("Callback replacement")));item.setItemStack(changed);}
                case "provenance"->item.getPersistentDataContainer().set(npc.plugin.provenance,PersistentDataType.STRING,"different-owner");
                case "delay"->item.setPickupDelay(0);
                case "mob-pickup"->item.setCanMobPickup(true);
                case "owner"->item.setOwner(npc.entity.getUniqueId());
                case "thrower"->item.setThrower(npc.entity.getUniqueId());
                case "unrelated"->npc.pocket.setStorage(35,new Stack("DIAMOND",2));
                case "reentrant"->WorldActions.tick(npc,action(),true);
                case "initialized"->{
                    check(item.getPickupDelay()==32767&&!item.canMobPickup(),"drop must block pickup before "+phase+" callback");
                    check(npc.token().equals(item.getPersistentDataContainer().get(npc.plugin.provenance,PersistentDataType.STRING)),"drop must have provenance before "+phase+" callback");
                }
                default->{}
            }
            expected=storage(npc);
        }
    }
    @EventHandler public void spawn(ItemSpawnEvent event){
        Trial t=current.get();if(t==null)return;
        t.items.add(event.getEntity());t.spawnEvents++;
        if(t.phase.equals("spawn")&&t.spawnEvents==1)try{t.mutate(event.getEntity(),event);}catch(Throwable failure){t.callbackFailure=failure;}
    }
    @EventHandler public void drop(EntityDropItemEvent event){
        Trial t=current.get();if(t==null||event.getEntity()!=t.npc.entity)return;
        t.dropEvents++;if(t.phase.equals("drop")&&t.dropEvents==1)try{t.mutate(event.getItemDrop(),event);}catch(Throwable failure){t.callbackFailure=failure;}
    }
    private static void trial(Npc npc,String phase,String mode){
        cases++;npc.pocket.clear();npc.pocket.setStorage(0,new Stack("OAK_LOG",8));npc.pocket.setStorage(1,new Stack("STICK",3));npc.tick=1;
        Goal originalGoal=npc.goal;Trial t=new Trial(npc,phase,mode);current.set(t);
        boolean accepted=Set.of("normal","initialized","unrelated","reentrant").contains(mode);
        try{
            WorldActions.tick(npc,action(),true);
            if(t.callbackFailure!=null)throw new AssertionError("drop callback check failed: "+phase+" "+mode,t.callbackFailure);
            check(t.spawnEvents==1,"unexpected spawn count: "+phase+" "+mode+" "+t.spawnEvents);
            check(t.dropEvents==(phase.equals("spawn")&&!accepted?0:1),"stale spawn must not emit drop event: "+phase+" "+mode);
            if(accepted)t.expected[0]=t.expected[0].withCount(t.expected[0].count()-1);
            for(int i=0;i<t.expected.length;i++)check(npc.pocket.storage(i).equals(t.expected[i]),"drop consumed stale inventory: "+phase+" "+mode+" slot="+i);
            check(npc.pocket.crafted.isEmpty()&&npc.pocket.extracted.isEmpty()&&npc.collected.isEmpty(),"drop must not earn resource credit");
            Item item=t.items.getFirst();
            if(accepted){
                check(item.isValid()&&!item.isDead(),"normal drop missing: "+phase+" "+mode);
                check(item.getItemStack().equals(new ItemStack(Material.OAK_LOG)),"drop changed contents: "+phase+" "+mode);
                check(item.getPickupDelay()==20&&item.canMobPickup(),"normal drop lost pickup settings");
                check(npc.token().equals(item.getPersistentDataContainer().get(npc.plugin.provenance,PersistentDataType.STRING)),"normal drop lost provenance");
            }else check(!item.isValid()||item.isDead(),"rejected drop remains available: "+phase+" "+mode);
        }finally{
            current.remove();npc.goal=originalGoal;npc.paused=false;npc.resetting=false;npc.remove.set(false);npc.plugin.paused.set(false);
            for(Item item:t.items)if(Bukkit.isOwnedByCurrentRegion(item)&&!item.isDead())item.remove();
        }
    }
    static void verify(Npc npc){
        if(npc.goal.task().ordinal()!=15)return;
        check(npc.pocket.menu()==Pocket.Menu.CLOSED&&npc.pocket.cursor().empty(),"expected fresh pocket for drop checks");
        Stack[] saved=storage(npc);int selected=npc.pocket.selected();long tick=npc.tick;
        var collected=new HashMap<>(npc.collected);var velocity=npc.entity.getVelocity().clone();
        ItemStack held=npc.entity.getEquipment().getItemInMainHand().clone();String mining=npc.mining;int miningTicks=npc.miningTicks;
        try{
            npc.collected.clear();
            // First failure against the original implementation proves the stale debit.
            trial(npc,"drop","held-kind");
            List<String> modes=List.of("normal","cancel","held-count","selected","menu","goal","paused","global-pause","resetting","remove-actor","remove-item","item-kind","item-count","metadata","provenance","delay","mob-pickup","owner","thrower","unrelated","reentrant","initialized");
            for(String phase:List.of("drop","spawn")){
                if(phase.equals("spawn"))trial(npc,phase,"held-kind");
                for(String mode:modes)trial(npc,phase,mode);
            }
            npc.pocket.clear();Trial empty=new Trial(npc,"drop","normal");current.set(empty);
            try{WorldActions.tick(npc,action(),true);check(empty.spawnEvents==0&&empty.dropEvents==0,"empty hand created drop events");}finally{current.remove();}
            System.out.println("DROP EVENT LIVE PASS checks="+checks+" cases="+(cases+1));
        }finally{
            npc.pocket.clear();for(int i=0;i<saved.length;i++)npc.pocket.setStorage(i,saved[i]);npc.pocket.select(selected);
            npc.collected.clear();npc.collected.putAll(collected);npc.tick=tick;npc.mining=mining;npc.miningTicks=miningTicks;
            npc.entity.setVelocity(velocity);npc.entity.getEquipment().setItemInMainHand(held);
        }
    }
}
