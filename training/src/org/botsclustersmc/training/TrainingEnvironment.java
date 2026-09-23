package org.botsclustersmc.training;

import org.botsclustersmc.core.*;
import org.botsclustersmc.core.Stack;
import org.botsclustersmc.plugin.*;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.Item;
import java.util.*;

/** Actual server environments for the shared NPC body, not an offline surrogate simulator. */
public final class TrainingEnvironment {
    private TrainingEnvironment(){}
    public static final class Session {
        public final ArenaLayout arena;public final List<Transition> fragment=new ArrayList<>(32);
        public Course.Lesson lesson;public Policy examPolicy;public long sequence;public int hold;public double potential;
        public Session(ArenaLayout arena){this.arena=arena;}
    }
    public static void build(World world,ArenaLayout a){
        for(int x=0;x<16;x++)for(int z=0;z<16;z++){
            world.getBlockAt(a.x()+x,64,a.z()+z).setType((x%15==0&&z%15==0)?Material.SEA_LANTERN:Material.STONE,false);
            for(int y=65;y<=70;y++)world.getBlockAt(a.x()+x,y,a.z()+z).setType(x==0||x==15||z==0||z==15||y==70?Material.GLASS:Material.AIR,false);
        }
    }
    public static void reset(RuntimePlugin plugin,Npc npc,Session session,Course.Lesson lesson){
        ArenaLayout a=session.arena;World world=npc.anchor.getWorld();npc.resetting=true;
        Location center=new Location(world,a.x()+8,65,a.z()+8);
        Bukkit.getRegionScheduler().run(plugin,center,t->{try{
            for(int x=1;x<15;x++)for(int z=1;z<15;z++)for(int y=65;y<70;y++){Block b=world.getBlockAt(a.x()+x,y,a.z()+z);if(!b.getType().isAir())b.setType(Material.AIR,false);}
            // Training rooms are owned, but entity removal still requires entity ownership.
            for(var entity:world.getNearbyEntities(center,7.5,5,7.5,e->e instanceof Item))if(Bukkit.isOwnedByCurrentRegion(entity))entity.remove();
            RandomSource rng=new RandomSource(lesson.seed()^(lesson.kind()==Course.Kind.EXAM?0x677d815fab35L:lesson.kind()==Course.Kind.PROBE?0x173a6ae017a1L:0x79b1L));
            double d=lesson.difficulty(),sx=a.x()+7.5,sz=a.z()+3.5,gx=a.x()+7.5,gz=sz+1.2+d*(2+rng.unit()*3),gy=65;
            float yaw=0,pitch=0;int task=lesson.task().ordinal();
            if(task==1||task==3){sx=a.x()+7.5;sz=a.z()+7.5;double angle=rng.unit()*Math.PI*2,distance=1.2+d*(2+rng.unit()*3);gx=sx+Math.sin(angle)*distance;gz=sz+Math.cos(angle)*distance;yaw=(float)(rng.unit()*360-180);}
            if(task==2){sx=a.x()+7.5;sz=a.z()+7.5;gx=a.x()+2.5+rng.unit()*10;gz=a.z()+2.5+rng.unit()*10;gy=66+rng.unit()*2;world.getBlockAt((int)gx,(int)gy,(int)gz).setType(Material.OAK_LOG,false);yaw=(float)(rng.unit()*360-180);}
            if(task==4){gx=a.x()+7.5;gz=a.z()+10.5;for(int x=2;x<14;x++)world.getBlockAt(a.x()+x,65,a.z()+7).setType(Material.STONE,false);}
            if(task>=5){sx=a.x()+8.5;sz=a.z()+5.5;gx=a.x()+8.5;gz=a.z()+8.5;gy=65;
                if(task==5||task==6||task==17){gy=66;world.getBlockAt(a.x()+8,66,a.z()+8).setType(Material.OAK_LOG,false);}
                if(task==12)world.getBlockAt(a.x()+8,65,a.z()+8).setType(Material.STONE,false);
                if(task==11||task==13)world.getBlockAt(a.x()+8,65,a.z()+8).setType(Material.CRAFTING_TABLE,false);
                if(task==14)world.getBlockAt(a.x()+8,65,a.z()+8).setType(Material.FURNACE,false);
                if(task==15)world.getBlockAt(a.x()+8,65,a.z()+8).setType(Material.CHEST,false);
                // Reset-only aim assistance fades out; full probes/exams have random initial facing.
                yaw=d<.5?0:(float)(rng.unit()*360-180);pitch=d<.5?12:0;
            }
            Goal goal=new Goal(lesson.task(),gx,gy,gz,lesson.serial(),d,lesson.task().horizon());
            Location spawn=new Location(world,sx,65,sz,yaw,pitch);
            npc.reset(goal,spawn,()->{
                supplies(npc,task);
                // Only an easier initial menu, never a generated output or teacher action during play.
                if(d<1&&lesson.kind()==Course.Kind.PRACTICE&&rng.unit()>d){
                    if(task==8||task==9||task==10)npc.pocket.open(Pocket.Menu.INVENTORY);
                    if(task==11||task==13||task==14||task==15){npc.container=new Location(world,a.x()+8,65,a.z()+8);npc.pocket.open(task==14?Pocket.Menu.FURNACE:task==15?Pocket.Menu.CHEST:Pocket.Menu.WORKBENCH);}
                }
                if(lesson.kind()==Course.Kind.PRACTICE&&(npc.pocket.menu()==Pocket.Menu.INVENTORY||npc.pocket.menu()==Pocket.Menu.WORKBENCH))InitialCrafting.prepare(npc.pocket,task,d,rng);
                session.hold=0;session.potential=potential(npc,session);
            });
        }catch(Throwable e){plugin.fail(e);}});
    }
    private static void supplies(Npc npc,int task){
        switch(task){
            case 7->npc.pocket.setStorage(0,new Stack("OAK_PLANKS",8));
            case 8->npc.pocket.setStorage(0,new Stack("OAK_LOG",1));
            case 9->npc.pocket.setStorage(0,new Stack("OAK_PLANKS",2));
            case 10->npc.pocket.setStorage(0,new Stack("OAK_PLANKS",4));
            case 11->{npc.pocket.setStorage(0,new Stack("OAK_PLANKS",3));npc.pocket.setStorage(1,new Stack("STICK",2));}
            case 12->npc.pocket.setStorage(0,new Stack("WOODEN_PICKAXE",1));
            case 13->{npc.pocket.setStorage(0,new Stack("COBBLESTONE",3));npc.pocket.setStorage(1,new Stack("STICK",2));}
            case 14->{npc.pocket.setStorage(0,new Stack("RAW_IRON",1));npc.pocket.setStorage(1,new Stack("COAL",1));}
            case 15->npc.pocket.setStorage(0,new Stack("OAK_LOG",8));
            case 16->npc.pocket.setStorage(0,new Stack("OAK_PLANKS",16));
            default->{}
        }
    }
    private static double targetMining(Npc npc,Session s){String target=(s.arena.x()+8)+":"+(npc.goal.task().ordinal()==12?65:66)+":"+(s.arena.z()+8)+":";return npc.mining!=null&&npc.mining.startsWith(target)?Math.min(1,npc.miningTicks/60.0):0;}
    private static int kindAt(Npc npc,int x,int y,int z){Location p=new Location(npc.anchor.getWorld(),x,y,z);return WorldActions.owned(p)?Stack.kind(p.getBlock().getType().name()):-1;}
    private static int placedCells(Npc npc,Session s){ArenaLayout a=s.arena;int n=0;for(int x=7;x<=9;x++)if(kindAt(npc,a.x()+x,65,a.z()+8)==3)n++;return n;}
    public static int chestLogs(Npc npc,Session s){
        Location p=new Location(npc.anchor.getWorld(),s.arena.x()+8,65,s.arena.z()+8);if(!WorldActions.owned(p))return 0;
        if(!(p.getBlock().getState() instanceof Chest chest))return 0;int n=0;for(var item:chest.getBlockInventory().getContents())if(item!=null&&Stack.kind(item.getType().name())==2)n+=item.getAmount();return n;
    }
    private static long count(Map<String,Long> counts,int kind){return counts.entrySet().stream().filter(e->Stack.kind(e.getKey())==kind).mapToLong(Map.Entry::getValue).sum();}
    public static double potential(Npc npc,Session s){
        var at=npc.entity.getLocation();Goal goal=npc.goal;int task=goal.task().ordinal();
        double distance=Math.hypot(goal.x()-at.getX(),goal.z()-at.getZ());
        if(task==2){double dx=goal.x()-at.getX(),dz=goal.z()-at.getZ();double yaw=Sensors.angle(Math.toDegrees(Math.atan2(-dx,dz))-at.getYaw());double pitch=Sensors.angle(-Math.toDegrees(Math.atan2(goal.y()+.5-(at.getY()+npc.entity.getEyeHeight()),Math.hypot(dx,dz)))-at.getPitch());return -(Math.abs(yaw)+Math.abs(pitch))/180;}
        if(task<5)return -Math.min(16,distance)/8;
        return switch(task){
            case 5->Math.min(1,count(npc.broken,2))*.8+targetMining(npc,s)*.2;
            case 6->Math.min(1,count(npc.broken,2))*.3+Math.min(1,count(npc.collected,2))*.5+targetMining(npc,s)*.15;
            case 7->kindAt(npc,s.arena.x()+8,65,s.arena.z()+8)==3?.8:0;
            case 8->Math.min(4,count(npc.pocket.crafted,3))*.2+InitialCrafting.progress(npc.pocket,task)*.2;
            case 9->Math.min(4,npc.pocket.crafted.getOrDefault("STICK",0L))*.2+InitialCrafting.progress(npc.pocket,task)*.2;
            case 10->Math.min(1,npc.pocket.crafted.getOrDefault("CRAFTING_TABLE",0L))*.8+InitialCrafting.progress(npc.pocket,task)*.2;
            case 11->Math.min(1,npc.pocket.crafted.getOrDefault("WOODEN_PICKAXE",0L))*.8+InitialCrafting.progress(npc.pocket,task)*.2;
            case 12->Math.min(1,count(npc.broken,9))*.3+Math.min(1,count(npc.collected,8))*.5;
            case 13->Math.min(1,npc.pocket.crafted.getOrDefault("STONE_PICKAXE",0L))*.8+InitialCrafting.progress(npc.pocket,task)*.2;
            case 14->Math.min(1,npc.pocket.extracted.getOrDefault("IRON_INGOT",0L))*.8;
            case 15->Math.min(4,chestLogs(npc,s))*.2;
            case 16->placedCells(npc,s)*(.8/3);
            case 17->Math.min(1,count(npc.broken,2))*.1+Math.min(1,count(npc.collected,2))*.1+Math.min(4,count(npc.pocket.crafted,3))*.05+Math.min(1,npc.pocket.crafted.getOrDefault("CRAFTING_TABLE",0L))*.4;
            default->0;
        };
    }
    public static boolean success(Npc npc,Session s,Npc.Applied previous,Frame next){
        int task=npc.goal.task().ordinal();int ticks=(int)(next.tick()-previous.frame().tick());
        if(task<5){
            double speed=Math.hypot(next.x()-previous.frame().x(),next.z()-previous.frame().z())/ticks;
            double angular=Math.max(Math.abs(Sensors.angle(next.yaw()-previous.frame().yaw())),Math.abs(next.pitch()-previous.frame().pitch()))/ticks;
            boolean meets=task==2?Math.abs(next.yawError())<=8&&Math.abs(next.pitchError())<=8:Math.hypot(npc.goal.x()-next.x(),npc.goal.z()-next.z())<=.65&&speed<=.025&&next.ground();
            if(meets&&angular<=.15&&ticks<=8)s.hold+=ticks;else s.hold=0;return s.hold>=20;
        }
        return switch(task){
            case 5->count(npc.broken,2)>=1&&kindAt(npc,s.arena.x()+8,66,s.arena.z()+8)==0;
            case 6->count(npc.collected,2)>=1&&npc.pocket.countKind(2)>=1;
            case 7->kindAt(npc,s.arena.x()+8,65,s.arena.z()+8)==3&&count(npc.placed,3)>=1;
            case 8->count(npc.pocket.crafted,3)>=4&&npc.pocket.countKind(3)>=4+InitialCrafting.progress(npc.pocket,task)*.2;
            case 9->npc.pocket.crafted.getOrDefault("STICK",0L)>=4&&npc.pocket.count("STICK")>=4+InitialCrafting.progress(npc.pocket,task)*.2;
            case 10,17->npc.pocket.crafted.getOrDefault("CRAFTING_TABLE",0L)>=1&&npc.pocket.count("CRAFTING_TABLE")>=1;
            case 11->npc.pocket.crafted.getOrDefault("WOODEN_PICKAXE",0L)>=1&&npc.pocket.count("WOODEN_PICKAXE")>=1+InitialCrafting.progress(npc.pocket,task)*.2;
            case 12->count(npc.collected,8)>=1&&npc.pocket.count("COBBLESTONE")>=1;
            case 13->npc.pocket.crafted.getOrDefault("STONE_PICKAXE",0L)>=1&&npc.pocket.count("STONE_PICKAXE")>=1+InitialCrafting.progress(npc.pocket,task)*.2;
            case 14->npc.pocket.extracted.getOrDefault("IRON_INGOT",0L)>=1&&npc.pocket.count("IRON_INGOT")>=1;
            case 15->chestLogs(npc,s)>=4&&npc.pocket.countKind(2)<=4;
            case 16->placedCells(npc,s)==3&&count(npc.placed,3)>=3;
            default->false;
        };
    }
}
