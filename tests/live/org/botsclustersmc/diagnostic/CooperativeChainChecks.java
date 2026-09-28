package org.botsclustersmc.diagnostic;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.botsclustersmc.core.*;
import org.botsclustersmc.core.Stack;
import org.botsclustersmc.plugin.*;
import org.botsclustersmc.training.ArenaLayout;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.inventory.Inventory;

/** Two real bodies and continuous pockets. Scripted mechanics, NEVER learned cooperation. */
final class CooperativeChainChecks {
    private static final AtomicBoolean complete=new AtomicBoolean();
    private final Npc a,b;
    private final Block chest,bench,stone;
    private int stage,ticks,checks;
    static boolean complete(){return complete.get();}
    private void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private CooperativeChainChecks(RuntimePlugin plugin,ArenaLayout room,World world){
        chest=world.getBlockAt(room.x()+4,65,room.z()+10);
        bench=world.getBlockAt(room.x()+6,65,room.z()+10);
        stone=world.getBlockAt(room.x()+6,65,room.z()+12);
        for(Block block:List.of(chest,bench,stone))check(WorldActions.owned(block.getLocation())&&block.getType().isAir(),"chain reset area must be owned and empty");
        chest.setType(Material.CHEST,false);bench.setType(Material.CRAFTING_TABLE,false);stone.setType(Material.STONE,false);
        Goal goal=new Goal(Task.CRAFT_WOOD_PICK,bench.getX()+.5,65,bench.getZ()+.5,1,1,3000);
        Location first=new Location(world,room.x()+4.5,65,room.z()+12.5),second=new Location(world,room.x()+7.4,65,room.z()+12.5);
        a=new Npc(plugin,100_001,NpcBody.spawn(plugin,first,100_001),goal);
        b=new Npc(plugin,100_002,NpcBody.spawn(plugin,second,100_002),goal);
        a.begin(goal);b.begin(goal);
        // The only supplies and body positioning occur here, before the first action.
        a.pocket.setStorage(0,new Stack("OAK_PLANKS",3));b.pocket.setStorage(0,new Stack("STICK",2));
    }
    static void start(Npc fixture,ArenaLayout room){
        if(fixture.goal.task()!=Task.SUPPLY_CHEST)return;
        CooperativeChainChecks test=new CooperativeChainChecks(fixture.plugin,room,fixture.entity.getWorld());
        Bukkit.getRegionScheduler().runAtFixedRate(fixture.plugin,test.chest.getLocation(),task->{
            try{if(test.tick()){test.cleanup();complete.set(true);task.cancel();}}
            catch(Throwable failure){try{test.cleanup();}finally{fixture.plugin.fail(failure);task.cancel();}}
        },1,1);
    }
    private static int[] click(int op,int slot){int[] action=Schema.IDLE.clone();action[6]=op;action[7]=slot;return action;}
    private static void aim(Npc n,Block block){
        Location eye=n.entity.getEyeLocation(),target=block.getLocation().add(.5,.5,.5);
        double dx=target.getX()-eye.getX(),dy=target.getY()-eye.getY(),dz=target.getZ()-eye.getZ();
        n.entity.setRotation((float)Math.toDegrees(Math.atan2(-dx,dz)),(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz))));
    }
    private static int[] use(Npc n,Block block){aim(n,block);int[] action=Schema.IDLE.clone();action[4]=2;return action;}
    private Inventory stock(){return ((Chest)chest.getState()).getBlockInventory();}
    private int stock(String name){return Arrays.stream(stock().getContents()).filter(s->s!=null&&s.getType().name().equals(name)).mapToInt(s->s.getAmount()).sum();}
    private boolean tick(){
        check(Bukkit.isOwnedByCurrentRegion(a.entity)&&Bukkit.isOwnedByCurrentRegion(b.entity),"chain bodies left the owning region");
        check(++ticks<=160,"continuous resource chain timed out at stage "+stage);
        a.tick++;b.tick++;boolean advance=true;
        int[] aa=Schema.IDLE.clone(),bb=Schema.IDLE.clone();
        switch(stage){
            case 0->aa=use(a,bench);
            case 1->aa=click(1,0);
            case 2->aa=click(2,36);
            case 3->aa=click(2,37);
            case 4->aa=click(2,38);
            case 5->aa=click(3,45);
            case 6->{
                check(a.pocket.count("WOODEN_PICKAXE")==0&&a.pocket.crafted.isEmpty(),"missing partner ingredients fabricated a tool");
                check(a.pocket.count("OAK_PLANKS")==3&&b.pocket.count("STICK")==2,"failed craft consumed resources");
                aa=click(5,0);bb=use(b,chest);
            }
            case 7->bb=click(3,0);
            case 8->{check(stock("STICK")==2&&b.pocket.count("STICK")==0,"partner deposit failed");aa=use(a,chest);bb=click(5,0);}
            case 9->aa=click(3,36);
            case 10->{check(a.pocket.count("STICK")==2&&stock("STICK")==0,"ingredient transfer failed");aa=click(5,0);}
            case 11->aa=use(a,bench);
            case 12->aa=click(1,0);
            case 13->aa=click(2,40);
            case 14->aa=click(2,43);
            case 15->aa=click(3,45);
            case 16->{
                check(a.pocket.count("WOODEN_PICKAXE")==1&&a.pocket.crafted.getOrDefault("WOODEN_PICKAXE",0L)==1,"continuous ingredient combination failed");
                check(a.pocket.count("OAK_PLANKS")==0&&a.pocket.count("STICK")==0,"craft failed to consume exact inputs");aa=click(5,0);
            }
            case 17->aa=use(a,chest);
            case 18->aa=click(3,0);
            case 19->{check(stock("WOODEN_PICKAXE")==1&&a.pocket.count("WOODEN_PICKAXE")==0,"tool deposit failed");bb=use(b,chest);}
            case 20->bb=click(3,36);
            case 21->{check(b.pocket.held().item().equals("WOODEN_PICKAXE")&&b.pocket.crafted.isEmpty()&&stock("WOODEN_PICKAXE")==0,"tool handoff duplicated stock or crafting credit");bb=click(5,0);}
            case 22->{
                advance=false;
                if(b.broken.getOrDefault("STONE",0L)==1)stage++;
                else{aim(b,stone);bb[4]=1;}
            }
            case 23->{advance=false;if(b.pocket.count("COBBLESTONE")==1)stage++;}
            case 24->bb=use(b,chest);
            case 25->bb=click(3,1);
            case 26->{
                check(stone.getType().isAir()&&b.broken.getOrDefault("STONE",0L)==1&&b.collected.getOrDefault("COBBLESTONE",0L)==1,"handoff tool did not yield a real pickup");
                check(stock("COBBLESTONE")==1&&b.pocket.count("COBBLESTONE")==0,"useful shared stock not delivered");
                check(b.pocket.count("WOODEN_PICKAXE")==1&&a.pocket.count("WOODEN_PICKAXE")==0,"tool lost or duplicated");
                check(a.pocket.crafted.equals(Map.of("WOODEN_PICKAXE",1L))&&b.pocket.crafted.isEmpty(),"transfers fabricated crafting credit");
                check(a.collected.isEmpty()&&a.broken.isEmpty()&&a.pocket.extracted.isEmpty()&&b.pocket.extracted.isEmpty(),"transfers fabricated harvesting or smelting credit");
                check(a.pocket.cursor().empty()&&b.pocket.cursor().empty(),"stranded cursor resources");
                check(a.pocket.count("OAK_PLANKS")==0&&b.pocket.count("OAK_PLANKS")==0&&a.pocket.count("STICK")==0&&b.pocket.count("STICK")==0,"raw resources unexpectedly reappeared");
                check(Arrays.stream(stock().getContents()).filter(Objects::nonNull).mapToInt(s->s.getAmount()).sum()==1,"unexpected extra shared stock");
                System.out.println("COOPERATIVE CHAIN LIVE PASS actors=2 checks="+checks+" ticks="+ticks+" scripted=true new_learning_samples=0");
                return true;
            }
            default->throw new AssertionError("unknown chain stage");
        }
        // Exactly one actuator call per actor per real scheduled server tick. Action
        // selection AND aim are scripted; no mid-chain teleport/reset/injection or recipe API.
        WorldActions.tick(a,aa,true);WorldActions.tick(b,bb,true);if(advance)stage++;
        return false;
    }
    private void cleanup(){
        if(Bukkit.isOwnedByCurrentRegion(a.entity)&&a.entity.isValid())a.entity.remove();
        if(Bukkit.isOwnedByCurrentRegion(b.entity)&&b.entity.isValid())b.entity.remove();
        for(Block block:List.of(chest,bench,stone))if(WorldActions.owned(block.getLocation()))block.setType(Material.AIR,false);
    }
}
