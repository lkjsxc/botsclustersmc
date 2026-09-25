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
            int[] buckets=new int[6],singleCell=new int[5];int operation=0;
            for(long seed=0;seed<2048;seed++) {
                Course.Lesson l=lesson(task,Course.Kind.PRACTICE,d,seed);Pocket p=stock(task);
                RandomSource rng=StationPractice.resetRandom(l);Pocket.Menu initial=StationPractice.initialMenu(l,rng);
                p.open(initial);
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
            check(operation==2048,"every assisted station start retains the product-completion goal");
            int frontier=(int)Math.ceil(d*5);
            for(int i=0;i<=frontier;i++)check(buckets[i]>40,"each earlier and frontier start is reachable");
            if(frontier>0) {
                check(buckets[frontier]>operation*.4&&buckets[frontier]<operation*.6,"frontier retains half of operation work");
                for(int n:singleCell)check(n>5,"all individual missing cells can be practiced, not one fixed prefix");
            }
            System.out.println("RESET "+task+" difficulty="+d+" operation="+operation+" missing="+Arrays.toString(buckets));
        }
        for(Task task:Task.values())for(Course.Kind kind:Course.Kind.values())for(double d:new double[]{.2,.9,1})for(long seed=0;seed<64;seed++) {
            Course.Lesson l=lesson(task,kind,d,seed);RandomSource rng=StationPractice.resetRandom(l);long before=rng.state();
            Pocket.Menu menu=StationPractice.initialMenu(l,rng);
            if(kind!=Course.Kind.PRACTICE||d==1) {
                check(menu==Pocket.Menu.CLOSED&&!StationPractice.operation(l),"full tasks retain closed unassisted reset");
                check(rng.state()==before,"full task consumes no assistance RNG");
            } else if(StationPractice.applies(task)) {
                check(menu==StationPractice.station(task),"all station types start usable in assisted practice");
                check(rng.state()==before,"station menu selection consumes no reset RNG");
            }
        }
        Pocket wrong=stock(Task.CRAFT_WOOD_PICK);wrong.open(Pocket.Menu.INVENTORY);
        String before=state(wrong);check(InitialCrafting.prepare(wrong,11,.2,new RandomSource(9))==-1&&state(wrong).equals(before),"no 3x3 reset in 2x2 inventory");
        Pocket absent=new Pocket();absent.open(Pocket.Menu.WORKBENCH);
        check(InitialCrafting.prepare(absent,11,0,new RandomSource(9))==5,"actual missing count when raw stock is absent");
        Pocket wrongStock=new Pocket();wrongStock.open(Pocket.Menu.WORKBENCH);wrongStock.setStorage(0,new Stack("DIRT",3));wrongStock.setStorage(1,new Stack("DIRT",2));
        check(InitialCrafting.prepare(wrongStock,11,0,new RandomSource(9))==5,"wrong stock never reports supplied ingredients");
    }
    private static void completionDifficulty() throws Exception {
        Course course=new Course(1,37);
        while(course.stage(0)<11){CourseTest.ready(course,0);CourseTest.exam(course,0,123,-1,0);}
        long beforeEpisodes=course.episodes(),beforeWins=course.successes();int operations=0,probes=0,wins=0;
        for(int i=0;i<500;i++) {
            Course.Lesson lesson=course.issue(0);double before=course.progress(0).practiceSuccess();
            boolean probe=lesson.kind()==Course.Kind.PROBE;
            if(probe)probes++;else {operations++;check(StationPractice.operation(lesson),"all assisted practice targets completion");}
            // Supplied test outcomes, not actions or learned success: probes deliberately fail.
            boolean completed=!probe&&i%3==0;if(completed)wins++;
            course.finish(0,lesson.serial(),completed);
            check(course.progress(0).practiceSuccess()==.95*before+.05*(completed?1:0),"every completion outcome updates difficulty");
            check(!course.needsExam(0)&&course.stage(0)==11,"assisted completions cannot replace full probes or unlock promotion");
        }
        check(operations==400&&probes==100,"unchanged 1-in-5 full-probe cadence");
        check(course.episodes()-beforeEpisodes==500&&course.successes()-beforeWins==wins,"exact completed-outcome counters");
        check(course.certifiedVersion(0,11)==-1,"no false wooden pick certificate");
        Course restored=Course.decode(course.encode(),1);check(Arrays.equals(course.encode(),restored.encode()),"unchanged checkpoint round trip");
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
        composedResets();completionDifficulty();outcomes();
        System.out.println("PASS composed crafting curriculum checks="+checks+"; not learned-Minecraft evidence");
    }
}
