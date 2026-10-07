package org.botsclustersmc.commons;

import java.util.*;
import org.botsclustersmc.core.*;
import org.botsclustersmc.plugin.*;
import org.bukkit.*;
import org.bukkit.block.Chest;

/** One room belongs to one Folia region. Only immutable results leave that owner. */
final class CommonsRoom {
    final CommonsExam plugin;final CommonsCase specification;final int id,trial,member;
    final World world;final int x,z;final List<Npc> actors=new ArrayList<>();
    private int ticks;private boolean started,finished;private final CommonsLedger ledger;
    private final long[] decisions=new long[2],gui=new long[12],chestObservations=new long[2];
    CommonsRoom(CommonsExam plugin,CommonsCase specification,int id,int trial,int member,World world) {
        this.plugin=plugin;this.specification=specification;this.id=id;this.trial=trial;this.member=member;this.world=world;
        ledger=new CommonsLedger(specification.rooms()==1?CommonsStock.INITIAL:
            new CommonsStock(specification.planks(member),specification.sticks(member),0));
        // Four-by-four room islands, bounded separately; no global settlement-capacity claim.
        x=((id/16)*64+(id%4))*16;z=((id%16)/4)*16;
    }
    Location origin() { return new Location(world,x+8,65,z+8); }
    Location bench() { return new Location(world,x+8,65,z+(specification.mirror()?5:10)); }
    Location bank() { return new Location(world,x+8,65,z+(specification.mirror()?10:5)); }
    Location spawn(int actorMember) { return new Location(world,x+(actorMember==0?4.5:11.5),65,z+8.5,specification.yaw(actorMember),0); }
    boolean contains(double px,double py,double pz) { return CommonsAccess.contains(x,z,px,py,pz); }
    private void requireOwner() {
        CommonsAccess.requireOwner(origin(),actors.stream().map(actor->actor.entity).toList());
    }
    void build() {
        if(!WorldActions.owned(origin()))throw new IllegalStateException("Room is not owned");
        for(int yy=64;yy<=70;yy++)for(int zz=0;zz<16;zz++)for(int xx=0;xx<16;xx++)
            world.getBlockAt(x+xx,yy,z+zz).setType(yy==64||yy==70||xx==0||xx==15||zz==0||zz==15?Material.BEDROCK:Material.AIR,false);
        bench().getBlock().setType(Material.CRAFTING_TABLE,false);bank().getBlock().setType(Material.CHEST,false);
    }
    void add(Npc npc,int actorMember) {
        if(started||actors.contains(npc))throw new IllegalStateException("Duplicate room admission");
        CommonsAccess.requireOwner(origin(),List.of(npc.entity));requireOwner();
        actors.add(npc);npc.context=this;
        if(specification.planks(actorMember)>0)npc.pocket.setStorage(0,new org.botsclustersmc.core.Stack("OAK_PLANKS",3));
        if(specification.sticks(actorMember)>0)npc.pocket.setStorage(1,new org.botsclustersmc.core.Stack("STICK",2));
        npc.rng.restore(specification.actorSeed(actorMember));npc.begin(npc.goal);
        if(actors.size()==(specification.rooms()==1?2:1)) {
            started=true;actors.sort(Comparator.comparingLong(a->a.id));
            for(Npc actor:actors){if(!Bukkit.isOwnedByCurrentRegion(actor.entity))throw new IllegalStateException("Shared-room admission crossed regions");actor.start();}
            Bukkit.getRegionScheduler().runAtFixedRate(plugin,origin(),task->{
                try { if(tick())task.cancel(); }
                catch(Throwable failure){task.cancel();plugin.fail(failure);}
            },1,1);
        }
    }
    void observe(Npc npc,Npc.Applied previous) {
        if(finished)throw new IllegalStateException("Decision after room completion");
        int m=(int)(npc.id%2);decisions[m]++;int operation=previous.result().actions()[6];gui[m*6+operation]++;
        // This is a source-observation count, not evidence that a transfer occurred.
        if(Math.round(previous.frame().observation()[42]*4)==Pocket.Menu.CHEST.ordinal())chestObservations[m]++;
    }
    private static CommonsStock pocket(Pocket pocket) {
        return CommonsStock.carried(pocket);
    }
    private CommonsStock bankStock() {
        if(!(bank().getBlock().getState() instanceof Chest chest))throw new IllegalStateException("Immutable chest changed");
        CommonsStock result=CommonsStock.EMPTY;
        for(var item:chest.getBlockInventory().getContents())if(item!=null&&!item.getType().isAir()) {
            if(!ExternalInventory.supported(item))throw new IllegalStateException("Unrepresentable commons item");
            result=result.plus(CommonsStock.item(item.getType().name(),item.getAmount()));
        }
        return result;
    }
    private CommonsResult snapshot(String end) {
        requireOwner();CommonsStock carried=CommonsStock.EMPTY;long craftedSticks=0,craftedPicks=0;
        int[] memberCarried=new int[6];long[] actorTicks=new long[2];
        for(Npc actor:actors){
            CommonsStock stock=pocket(actor.pocket);int m=(int)(actor.id%2);
            memberCarried[m*3]=stock.planks();memberCarried[m*3+1]=stock.sticks();memberCarried[m*3+2]=stock.picks();actorTicks[m]=actor.tick;
            carried=carried.plus(stock);
            for(var craft:actor.pocket.crafted.entrySet())if((!craft.getKey().equals("STICK")&&!craft.getKey().equals("WOODEN_PICKAXE"))||craft.getValue()<0)
                throw new IllegalStateException("Unexpected commons crafting history");
            craftedSticks=Math.addExact(craftedSticks,actor.pocket.crafted.getOrDefault("STICK",0L));
            craftedPicks=Math.addExact(craftedPicks,actor.pocket.crafted.getOrDefault("WOODEN_PICKAXE",0L));
        }
        CommonsStock dropped=CommonsAccess.dropped(origin()),bank=bankStock();
        CommonsStock lost=ledger.sample(carried.plus(bank).plus(dropped),craftedSticks,craftedPicks);
        return new CommonsResult(id,ticks,end,carried,bank,dropped,craftedSticks,craftedPicks,lost,memberCarried,actorTicks,decisions,gui,chestObservations);
    }
    private boolean tick() {
        if(finished)return true;
        if(plugin.failed.get()!=null)return true;
        requireOwner();ticks++;
        for(Npc actor:actors) {
            if(!Bukkit.isOwnedByCurrentRegion(actor.entity)||!actor.entity.isValid())throw new IllegalStateException("Room actor ownership/lifecycle lost");
            Location p=actor.entity.getLocation();if(p.getWorld()!=world||!contains(p.getX(),p.getY(),p.getZ()))return finish("escaped");
        }
        if(ticks%4==0||ticks>=plugin.horizon()) {
            CommonsResult s=snapshot("sample");
            if(specification.rooms()==1&&CommonsStock.delivered(s.total(),s.bank(),s.craftedSticks(),s.craftedPicks()))return finish("delivered");
        }
        return ticks>=plugin.horizon()&&finish("horizon");
    }
    private boolean finish(String end) {
        requireOwner();
        // Stop both members on this same region before capturing a coherent final stock.
        for(Npc actor:actors){actor.paused=true;actor.discardPending();actor.entity.setVelocity(new org.bukkit.util.Vector());}
        finished=true;plugin.completed(id,snapshot(end));return true;
    }
}
