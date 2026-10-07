package org.botsclustersmc.commons;

import java.util.*;
import org.botsclustersmc.core.*;
import org.botsclustersmc.plugin.*;
import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.entity.Item;

/** One room belongs to one Folia region. Only immutable results leave that owner. */
final class CommonsRoom {
    record Result(int room,int ticks,String end,CommonsStock carried,CommonsStock bank,CommonsStock dropped,
                  long crafted,int[] memberCarried,long[] actorTicks,long[] decisions,long[] guiSelections,long[] chestObservations) {
        Result { memberCarried=memberCarried.clone();actorTicks=actorTicks.clone();decisions=decisions.clone();guiSelections=guiSelections.clone();chestObservations=chestObservations.clone(); }
        CommonsStock total() { return carried.plus(bank).plus(dropped); }
        String json() {
            return "{\"room\":"+room+",\"ticks\":"+ticks+",\"end\":\""+end+"\",\"carried\":"+carried.json()
                +",\"bank\":"+bank.json()+",\"dropped\":"+dropped.json()+",\"crafted_picks\":"+crafted
                +",\"member_carried\":"+Arrays.toString(memberCarried)+",\"actor_ticks\":"+Arrays.toString(actorTicks)+",\"decisions\":"+Arrays.toString(decisions)+",\"gui_selections\":"+Arrays.toString(guiSelections)
                +",\"chest_observations\":"+Arrays.toString(chestObservations)+"}";
        }
    }
    final CommonsExam plugin;final CommonsCase specification;final int id,trial,member;
    final World world;final int x,z;final List<Npc> actors=new ArrayList<>();
    private int ticks;private boolean started,finished;
    private final long[] decisions=new long[2],gui=new long[12],chestObservations=new long[2];
    CommonsRoom(CommonsExam plugin,CommonsCase specification,int id,int trial,int member,World world) {
        this.plugin=plugin;this.specification=specification;this.id=id;this.trial=trial;this.member=member;this.world=world;
        // Four-by-four room islands, bounded separately; no global settlement-capacity claim.
        x=((id/16)*64+(id%4))*16;z=((id%16)/4)*16;
    }
    Location origin() { return new Location(world,x+8,65,z+8); }
    Location bench() { return new Location(world,x+8,65,z+(specification.mirror()?5:10)); }
    Location bank() { return new Location(world,x+8,65,z+(specification.mirror()?10:5)); }
    Location spawn(int actorMember) { return new Location(world,x+(actorMember==0?4.5:11.5),65,z+8.5,specification.yaw(actorMember),0); }
    boolean contains(double px,double py,double pz) { return px>=x+1&&px<x+15&&pz>=z+1&&pz<z+15&&py>=64&&py<70; }
    void build() {
        if(!WorldActions.owned(origin()))throw new IllegalStateException("Room is not owned");
        for(int yy=64;yy<=70;yy++)for(int zz=0;zz<16;zz++)for(int xx=0;xx<16;xx++)
            world.getBlockAt(x+xx,yy,z+zz).setType(yy==64||yy==70||xx==0||xx==15||zz==0||zz==15?Material.BEDROCK:Material.AIR,false);
        bench().getBlock().setType(Material.CRAFTING_TABLE,false);bank().getBlock().setType(Material.CHEST,false);
    }
    void add(Npc npc,int actorMember) {
        if(started||actors.contains(npc))throw new IllegalStateException("Duplicate room admission");
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
    private Result snapshot(String end) {
        CommonsStock carried=CommonsStock.EMPTY,dropped=CommonsStock.EMPTY;long crafted=0;int[] memberCarried=new int[6];long[] actorTicks=new long[2];
        for(Npc actor:actors){
            CommonsStock stock=pocket(actor.pocket);int m=(int)(actor.id%2);
            memberCarried[m*3]=stock.planks();memberCarried[m*3+1]=stock.sticks();memberCarried[m*3+2]=stock.picks();actorTicks[m]=actor.tick;
            carried=carried.plus(stock);crafted+=actor.pocket.crafted.getOrDefault("WOODEN_PICKAXE",0L);
        }
        for(var entity:world.getChunkAt(x>>4,z>>4).getEntities())if(entity instanceof Item item) {
            Location at=item.getLocation();if(!contains(at.getX(),at.getY(),at.getZ()))continue;
            if(!Bukkit.isOwnedByCurrentRegion(item))throw new IllegalStateException("Foreign item in room");
            var stack=item.getItemStack();if(!ExternalInventory.supported(stack))throw new IllegalStateException("Unrepresentable dropped item");
            dropped=dropped.plus(CommonsStock.item(stack.getType().name(),stack.getAmount()));
        }
        return new Result(id,ticks,end,carried,bankStock(),dropped,crafted,memberCarried,actorTicks,decisions,gui,chestObservations);
    }
    private boolean tick() {
        if(finished)return true;
        if(plugin.failed.get()!=null)return true;
        ticks++;
        for(Npc actor:actors) {
            if(!Bukkit.isOwnedByCurrentRegion(actor.entity)||!actor.entity.isValid())throw new IllegalStateException("Room actor ownership/lifecycle lost");
            Location p=actor.entity.getLocation();if(p.getWorld()!=world||!contains(p.getX(),p.getY(),p.getZ()))return finish("escaped");
        }
        if(ticks%4==0||ticks>=plugin.horizon()) {
            Result s=snapshot("sample");int supply=specification.rooms()==1?8:specification.planks(member)*2+specification.sticks(member);
            if(s.total().woodUnits()>supply)throw new IllegalStateException("Commons stock creation");
            if(specification.rooms()==1&&CommonsStock.delivered(s.total(),s.bank(),s.crafted()))return finish("delivered");
        }
        return ticks>=plugin.horizon()&&finish("horizon");
    }
    private boolean finish(String end) {
        // Stop both members on this same region before capturing a coherent final stock.
        for(Npc actor:actors){actor.paused=true;actor.discardPending();actor.entity.setVelocity(new org.bukkit.util.Vector());}
        finished=true;plugin.completed(id,snapshot(end));return true;
    }
}
