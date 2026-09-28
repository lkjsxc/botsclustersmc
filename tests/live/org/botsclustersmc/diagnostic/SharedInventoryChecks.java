package org.botsclustersmc.diagnostic;

import java.util.*;
import org.botsclustersmc.core.*;
import org.botsclustersmc.core.Stack;
import org.botsclustersmc.plugin.*;
import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;

/** Scripted resource-boundary tests in a disposable room; no learned teamwork claim. */
final class SharedInventoryChecks {
    private static int checks;
    private static final Set<Long> denied=java.util.concurrent.ConcurrentHashMap.newKeySet();
    static boolean denied(Npc npc){return denied.contains(npc.id);}
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static Pocket pocket(){Pocket p=new Pocket();p.open(Pocket.Menu.CHEST);return p;}
    private static ItemStack named(){
        ItemStack item=new ItemStack(Material.OAK_LOG,8);
        item.editMeta(meta->meta.displayName(Component.text("Preserve this name")));
        return item;
    }
    private static List<ItemStack> unsupported(Npc npc){
        ItemStack damaged=new ItemStack(Material.WOODEN_PICKAXE);
        damaged.editMeta(meta->((Damageable)meta).setDamage(7));
        ItemStack enchanted=new ItemStack(Material.WOODEN_PICKAXE);
        enchanted.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.UNBREAKING,1);
        ItemStack tagged=new ItemStack(Material.OAK_LOG,8);
        tagged.editMeta(meta->meta.getPersistentDataContainer().set(
            new NamespacedKey(npc.plugin,"fixture-data"),PersistentDataType.STRING,"preserve"));
        ItemStack limited=new ItemStack(Material.OAK_LOG,8);
        limited.editMeta(meta->meta.setMaxStackSize(16));
        return List.of(named(),damaged,enchanted,tagged,limited,new ItemStack(Material.ENDER_PEARL,16),
            new ItemStack(Material.SHEARS));
    }
    private static void defaults(){
        int materials=0;
        for(Material material:Material.values()){
            if(material.isLegacy()||!material.isItem()||material.isAir())continue;
            ItemStack original=new ItemStack(material);
            boolean expected=original.getMaxStackSize()==new Stack(material.name(),1).maximum();
            check(ExternalInventory.supported(original)==expected,"default material support: "+material);
            if(expected)check(original.equals(ExternalInventory.to(ExternalInventory.from(original))),"lossy default round trip: "+material);
            materials++;
        }
        System.out.println("SHARED ITEM DEFAULTS PASS materials="+materials);
    }
    private static void blocked(Npc npc,Inventory inventory){
        ExternalInventory external=new ExternalInventory(inventory,false);
        for(ItemStack invalid:List.of(new ItemStack(Material.WOODEN_PICKAXE,2),new ItemStack(Material.OAK_LOG,65))){
            check(!ExternalInventory.supported(invalid),"oversized input accepted");
            try{ExternalInventory.from(invalid);throw new AssertionError("oversized conversion accepted");}
            catch(IllegalArgumentException expected){checks++;}
        }
        try{ExternalInventory.to(new Stack("WOODEN_PICKAXE",2));throw new AssertionError("oversized output accepted");}
        catch(IllegalArgumentException expected){checks++;}
        for(ItemStack original:unsupported(npc)){
            check(!ExternalInventory.supported(original),"unsupported item accepted: "+original);
            try{ExternalInventory.from(original);throw new AssertionError("lossy conversion accepted");}
            catch(IllegalArgumentException expected){checks++;}
            for(int operation=1;operation<=3;operation++){
                inventory.clear();inventory.setItem(0,original.clone());Pocket p=pocket();
                check(inventory.getItem(0).equals(original),"server normalized the test input before the actuator ran");
                check(!external.accessible(0),"unsupported source remained accessible: requested="+original+" stored="+inventory.getItem(0));
                check(!p.wouldChange(operation,36,external),"unsupported withdrawal offered");
                p.click(operation,36,external);
                check(p.cursor().empty()&&p.count(original.getType().name())==0,"unsupported withdrawal changed pocket");
                check(inventory.getItem(0).equals(original),"unsupported withdrawal changed world item");
                PocketView view=PocketView.capture(p,external);
                check((view.unavailableSlots()&1)==1&&view.contents().contains("36=unavailable"),"blocked slot displayed as empty");
                p.setStorage(0,new Stack("OAK_PLANKS",4));p.click(1,0,external);
                p.click(operation,36,external);
                check(p.cursor().equals(new Stack("OAK_PLANKS",4))&&inventory.getItem(0).equals(original),"blocked swap/deposit changed items");
            }
            inventory.clear();inventory.setItem(0,original.clone());Pocket p=pocket();
            p.setStorage(0,new Stack("OAK_LOG",8));p.click(3,0,external);
            check(p.count("OAK_LOG")==0&&inventory.getItem(0).equals(original)
                &&inventory.getItem(1).equals(new ItemStack(Material.OAK_LOG,8)),"shift deposit overwrote blocked destination");
        }
        // Lower the limit AFTER insertion; setItem itself may normalize oversized inputs.
        inventory.clear();inventory.setItem(0,new ItemStack(Material.OAK_LOG,64));inventory.setMaxStackSize(16);
        Pocket p=pocket();check(!external.accessible(0),"oversized external stack accessible");
        for(int operation=1;operation<=3;operation++)p.click(operation,36,external);
        check(p.count("OAK_LOG")==0&&inventory.getItem(0).getAmount()==64,"oversized source partially mutated");
        p.setStorage(0,new Stack("OAK_LOG",8));p.click(3,0,external);
        check(p.count("OAK_LOG")==8&&inventory.getItem(1)==null,"unsupported container limit ignored");
        inventory.clear();inventory.setMaxStackSize(64);
    }
    private static void sharing(Inventory inventory){
        inventory.clear();Pocket a=pocket(),b=pocket();
        ExternalInventory first=new ExternalInventory(inventory,false),second=new ExternalInventory(inventory,false);
        a.setStorage(0,new Stack("OAK_LOG",8));a.click(3,0,first);
        check(a.count("OAK_LOG")==0&&inventory.getItem(0).getAmount()==8,"shared deposit failed");
        check(a.wouldChange(3,36,first)&&b.wouldChange(3,36,second),"both actors should observe withdrawal");
        b.click(3,36,second);a.click(3,36,first);
        check(a.count("OAK_LOG")==0&&b.count("OAK_LOG")==8&&inventory.getItem(0)==null,"stale second withdrawal duplicated shared stock");
        for(int i=0;i<100;i++){
            b.click(3,0,second);a.click(3,36,first);a.click(3,0,first);b.click(3,36,second);
            check(a.count("OAK_LOG")==0&&b.count("OAK_LOG")==8&&inventory.getItem(0)==null,"transfer cycle changed total resources");
            check(a.crafted.isEmpty()&&b.crafted.isEmpty()&&a.extracted.isEmpty()&&b.extracted.isEmpty(),"transfer fabricated crafting credit");
        }
    }
    static void verify(Npc npc){
        if(npc.goal.task().ordinal()!=15)return;
        check(npc.pocket.menu()==Pocket.Menu.CLOSED&&npc.pocket.cursor().empty(),"expected fresh fixture pocket");
        check(npc.pocket.crafted.isEmpty()&&npc.pocket.extracted.isEmpty(),"fixture already crafted");
        Location location=new Location(npc.anchor.getWorld(),npc.goal.x(),npc.goal.y(),npc.goal.z());
        var block=location.getBlock();var originalBlock=block.getBlockData().clone();
        check(block.getState() instanceof Chest,"fixture chest missing");
        Inventory inventory=((Chest)block.getState()).getBlockInventory();
        ItemStack[] contents=Arrays.stream(inventory.getContents()).map(s->s==null?null:s.clone()).toArray(ItemStack[]::new);
        int maximum=inventory.getMaxStackSize(),selected=npc.pocket.selected();
        Stack[] original=new Stack[36];for(int i=0;i<36;i++)original[i]=npc.pocket.storage(i);
        Location container=npc.container;var velocity=npc.entity.getVelocity().clone();
        ItemStack held=npc.entity.getEquipment().getItemInMainHand().clone();
        try{
            defaults();blocked(npc,inventory);sharing(inventory);
            inventory.clear();inventory.setItem(0,new ItemStack(Material.OAK_LOG,8));
            npc.pocket.clear();npc.pocket.open(Pocket.Menu.CHEST);npc.container=location;
            check(npc.pocket.wouldChange(3,36,ExternalInventory.locate(npc)),"pre-removal withdrawal not offered");
            int[] action=Schema.IDLE.clone();action[6]=3;action[7]=36;
            denied.add(npc.id);WorldActions.tick(npc,action,true);
            check(npc.pocket.count("OAK_LOG")==0&&inventory.getItem(0).getAmount()==8,"revoked container permission was ignored");
            check(ExternalInventory.locate(npc)==Pocket.NONE,"revoked container remained available");
            denied.remove(npc.id);
            block.setType(Material.AIR,false);
            WorldActions.tick(npc,action,true);
            check(npc.pocket.count("OAK_LOG")==0,"removed chest fabricated a withdrawal");
            check(ExternalInventory.locate(npc)==Pocket.NONE,"removed chest still accessible");
            System.out.println("SHARED INVENTORY LIVE PASS checks="+checks+" serialized_transfer_cycles=100");
        }finally{
            denied.remove(npc.id);
            block.setBlockData(originalBlock,false);
            Inventory restored=((Chest)block.getState()).getBlockInventory();
            restored.setMaxStackSize(maximum);restored.setContents(contents);
            npc.pocket.clear();for(int i=0;i<36;i++)npc.pocket.setStorage(i,original[i]);
            npc.pocket.select(selected);npc.container=container;
            npc.entity.setVelocity(velocity);npc.entity.getEquipment().setItemInMainHand(held);
        }
    }
}
