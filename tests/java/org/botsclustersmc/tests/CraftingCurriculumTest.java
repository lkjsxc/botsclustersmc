package org.botsclustersmc.tests;

import java.util.Arrays;
import org.botsclustersmc.core.*;
import org.botsclustersmc.core.Stack;
import org.botsclustersmc.training.*;

/** Composed reset/curriculum and diagnostic tests. No learned-Minecraft claim. */
public final class CraftingCurriculumTest {
    private static int checks;
    private static final int[] GRID={36,37,38,40,43};
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Course.Lesson lesson(Task task,Course.Kind kind,double d,long seed) {
        return new Course.Lesson(1,task,d,seed,kind);
    }
    private static Pocket stock(Task task) {
        Pocket p=new Pocket();p.setStorage(0,new Stack(task==Task.CRAFT_STONE_PICK?"COBBLESTONE":"OAK_PLANKS",3));
        p.setStorage(1,new Stack("STICK",2));return p;
    }
    private static String state(Pocket p) {
        StringBuilder text=new StringBuilder(p.menu()+":"+p.cursor());
        for(int i=0;i<64;i++)text.append('/').append(p.get(i,Pocket.NONE));return text+"/"+p.crafted;
    }
    private static void composedResets() {
        for(Task task:new Task[]{Task.CRAFT_WOOD_PICK,Task.CRAFT_STONE_PICK})for(double d:new double[]{0,.1,.32,.54,.55,.7,.99}) {
            int[] buckets=new int[6],singleCell=new int[5];int opening=0,operation=0;
            for(long seed=0;seed<2048;seed++) {
                Course.Lesson l=lesson(task,Course.Kind.PRACTICE,d,seed);Pocket p=stock(task);
                RandomSource rng=StationPractice.resetRandom(l);Pocket.Menu initial=StationPractice.initialMenu(l,rng);
                p.open(initial);
                if(StationPractice.acquisition(l)) {
                    opening++;check(initial==Pocket.Menu.CLOSED,"opening begins closed at EVERY difficulty");
                    check(p.storage(0).count()==3&&p.storage(1).count()==2&&p.cursor().empty(),"opening stock stays raw");
                    continue;
                }
                operation++;check(StationPractice.operation(l)&&initial==Pocket.Menu.WORKBENCH,"operation starts at usable workbench");
                int missing=InitialCrafting.prepare(p,task.ordinal(),d,rng),actual=0;
                for(int i=0;i<GRID.length;i++)if(p.get(GRID[i],Pocket.NONE).empty())actual++;
                check(missing==actual&&missing<=(int)Math.ceil(d*5),"reported and actual initial cell counts");buckets[missing]++;
                if(missing==1)for(int i=0;i<GRID.length;i++)if(p.get(GRID[i],Pocket.NONE).empty())singleCell[i]++;
                String raw=task==Task.CRAFT_STONE_PICK?"COBBLESTONE":"OAK_PLANKS",output=task==Task.CRAFT_STONE_PICK?"STONE_PICKAXE":"WOODEN_PICKAXE";
                check(p.count(raw)==3&&p.count("STICK")==2,"no raw materials created or destroyed");
                check(p.crafted.isEmpty()&&p.count(output)==0,"reset never grants owned output or earned crafting");
                if(missing==0) {
                    check(p.get(45,Pocket.NONE).item().equals(output),"collection-only reset has actual preview");
                    p.click(3,45,Pocket.NONE); // Explicit mechanical assertion, not a policy demonstration.
                    check(p.count(output)==1&&p.crafted.getOrDefault(output,0L)==1,"a real click is needed to own the result");
                }
                Pocket replay=stock(task);RandomSource again=StationPractice.resetRandom(l);replay.open(StationPractice.initialMenu(l,again));
                int replayMissing=InitialCrafting.prepare(replay,task.ordinal(),d,again);
                if(missing==0)replay.click(3,45,Pocket.NONE);
                check(replayMissing==missing&&state(p).equals(state(replay)),"same lesson reproduces pocket independently of pose RNG");
            }
            check(opening>400&&opening<640&&operation==2048-opening,"bounded deterministic opening/operation mixture");
            int frontier=(int)Math.ceil(d*5);
            for(int i=0;i<=frontier;i++)check(buckets[i]>40,"each earlier and frontier start is reachable");
            if(frontier>0) {
                check(buckets[frontier]>operation*.4&&buckets[frontier]<operation*.6,"frontier retains half of operation work");
                for(int n:singleCell)check(n>5,"all individual missing cells can be practiced, not one fixed prefix");
            }
            System.out.println("RESET "+task+" difficulty="+d+" opening="+opening+" operation="+operation+" missing="+Arrays.toString(buckets));
        }
        for(Task task:Task.values())for(Course.Kind kind:Course.Kind.values())for(double d:new double[]{.2,.9,1})for(long seed=0;seed<64;seed++) {
            Course.Lesson l=lesson(task,kind,d,seed);RandomSource rng=StationPractice.resetRandom(l);long before=rng.state();
            Pocket.Menu menu=StationPractice.initialMenu(l,rng);
            if(kind!=Course.Kind.PRACTICE||d==1) {
                check(menu==Pocket.Menu.CLOSED&&!StationPractice.acquisition(l)&&!StationPractice.operation(l),"full tasks retain closed unassisted reset");
                check(rng.state()==before,"full task consumes no assistance RNG");
            } else if(StationPractice.applies(task)) {
                check(menu==(StationPractice.acquisition(l)?Pocket.Menu.CLOSED:StationPractice.station(task)),"all station types are usable in operation practice");
            }
        }
        Pocket wrong=stock(Task.CRAFT_WOOD_PICK);wrong.open(Pocket.Menu.INVENTORY);
        String before=state(wrong);check(InitialCrafting.prepare(wrong,11,.2,new RandomSource(9))==-1&&state(wrong).equals(before),"no 3x3 reset in 2x2 inventory");
        Pocket absent=new Pocket();absent.open(Pocket.Menu.WORKBENCH);
        check(InitialCrafting.prepare(absent,11,0,new RandomSource(9))==5,"actual missing count when raw stock is absent");
        Pocket wrongStock=new Pocket();wrongStock.open(Pocket.Menu.WORKBENCH);wrongStock.setStorage(0,new Stack("DIRT",3));wrongStock.setStorage(1,new Stack("DIRT",2));
        check(InitialCrafting.prepare(wrongStock,11,0,new RandomSource(9))==5,"wrong stock never reports supplied ingredients");
    }
    private static void independentDifficulty() throws Exception {
        Course base=new Course(1,37);
        while(base.stage(0)<11){CourseTest.ready(base,0);CourseTest.exam(base,0,123,-1,0);}
        byte[] saved=base.encode();Course success=Course.decode(saved,1),failure=Course.decode(saved,1);int openings=0,operations=0,probes=0;
        for(int i=0;i<500;i++) {
            Course.Lesson a=success.issue(0),b=failure.issue(0);
            check(a.equals(b),"opening success cannot change next seed, phase, difficulty or task");
            boolean opening=StationPractice.acquisition(a);double before=success.progress(0).practiceSuccess();
            if(opening)openings++;else if(a.kind()==Course.Kind.PROBE)probes++;else operations++;
            success.finish(0,a.serial(),opening);failure.finish(0,b.serial(),false);
            double after=success.progress(0).practiceSuccess();
            check(after==failure.progress(0).practiceSuccess(),"opening wins excluded from completion EMA");
            check(after==(opening?before:.95*before),"completion failures still lower difficulty");
            check(!success.needsExam(0)&&!failure.needsExam(0)&&success.stage(0)==11,"opening successes never unlock exam or promotion");
        }
        check(openings>50&&operations>100&&probes==100,"both practice phases and unchanged 1-in-5 probe cadence");
        check(success.episodes()==failure.episodes()&&success.successes()-failure.successes()==openings,"actual successes/counters are retained, not erased");
        check(success.certifiedVersion(0,11)==-1,"no false wooden pick certificate");
        Course restored=Course.decode(success.encode(),1);check(Arrays.equals(success.encode(),restored.encode()),"unchanged checkpoint round trip");
    }
    private static void outcomes() throws Exception {
        CraftingOutcomes c=new CraftingOutcomes();Thread[] writers=new Thread[4];
        for(int t=0;t<4;t++) {writers[t]=new Thread(()->{for(int i=0;i<6000;i++)c.record(Task.CRAFT_WOOD_PICK,i%6,(i/6)%2==0);});writers[t].start();}
        for(Thread thread:writers)thread.join();CraftingOutcomes.Totals totals=c.snapshot();
        check(totals.trials().length==18*6&&totals.successes().length==18*6,"fixed bounded diagnostic shape");
        for(int i=0;i<108;i++)check(totals.trials()[i]==(i>=66&&i<72?4000:0)&&totals.successes()[i]==(i>=66&&i<72?2000:0),"exact concurrent per-bucket counts");
        totals.trials()[66]=999;totals.successes()[66]=999;check(c.snapshot().trials()[66]==4000&&c.snapshot().successes()[66]==2000,"snapshots do not expose internal arrays");
        for(int invalid:new int[]{-1,6}){boolean rejected=false;try{c.record(Task.CRAFT_WOOD_PICK,invalid,true);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"invalid bucket rejected");}
        boolean rejected=false;try{c.record(Task.AIM_HOLD,0,true);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"noncrafting task rejected");
        check(Arrays.stream(new CraftingOutcomes().snapshot().trials()).sum()==0,"new process has no borrowed historical bucket claims");
    }
    public static void main(String[] args)throws Exception {
        composedResets();independentDifficulty();outcomes();
        System.out.println("PASS composed crafting curriculum checks="+checks+"; not learned-Minecraft evidence");
    }
}
