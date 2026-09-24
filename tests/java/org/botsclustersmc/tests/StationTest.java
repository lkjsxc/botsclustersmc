package org.botsclustersmc.tests;

import java.util.*;
import org.botsclustersmc.core.*;
import org.botsclustersmc.core.Stack;
import org.botsclustersmc.training.*;

/** Mechanical and reward invariants, not learned policy evidence. */
public final class StationTest {
    private static int checks;
    private static void check(boolean ok) {checks++;if(!ok)throw new AssertionError("check "+checks);}
    private static void near(double a,double b) {check(Math.abs(a-b)<1e-9);}
    private static void put(Pocket p,int slot,String item,int count) {
        check(p.cursor().empty());p.setStorage(35,new Stack(item,count));
        p.click(1,35,Pocket.NONE);p.click(1,slot,Pocket.NONE);check(p.cursor().empty());
    }
    private static String state(Pocket p) {
        StringBuilder s=new StringBuilder(p.menu()+":"+p.cursor());
        for(int i=0;i<64;i++)s.append('/').append(p.get(i,Pocket.NONE));
        return s+"/"+p.crafted;
    }
    public static void main(String[] args) {
        Pocket inventory=new Pocket();inventory.open(Pocket.Menu.INVENTORY);
        for(int slot:new int[]{36,37,38})put(inventory,slot,"OAK_PLANKS",1);
        near(InitialCrafting.progress(inventory,11),0);
        near(InitialCrafting.progress(inventory,13),0);
        Pocket preview=new Pocket();preview.open(Pocket.Menu.INVENTORY);
        put(preview,36,"OAK_PLANKS",1);put(preview,38,"OAK_PLANKS",1);
        check(preview.get(40,Pocket.NONE).item().equals("STICK"));
        check(preview.count("STICK")==0);near(InitialCrafting.progress(preview,11),0);
        for(double difficulty:new double[]{.1,.2,.4,.6,.8,.99}) {
            Pocket p=new Pocket();p.open(Pocket.Menu.WORKBENCH);
            p.setStorage(0,new Stack("OAK_PLANKS",3));p.setStorage(1,new Stack("STICK",2));
            int missing=InitialCrafting.prepare(p,11,difficulty,new RandomSource(3));
            int grid=0;for(int slot=36;slot<45;slot++)grid+=p.get(slot,Pocket.NONE).count();
            check(grid==5-missing&&missing>=0&&missing<=(int)Math.ceil(5*difficulty));
            check(p.count("OAK_PLANKS")==3&&p.count("STICK")==2&&p.crafted.isEmpty());
        }

        for(Pocket.Menu menu:new Pocket.Menu[]{Pocket.Menu.CLOSED,Pocket.Menu.CHEST,Pocket.Menu.FURNACE}) {
            inventory.open(menu);near(InitialCrafting.progress(inventory,11),0);
        }
        for(int task:new int[]{8,9,10,11,13})for(Pocket.Menu menu:new Pocket.Menu[]{Pocket.Menu.INVENTORY,Pocket.Menu.WORKBENCH}) {
            if((task==11||task==13)&&menu==Pocket.Menu.INVENTORY)continue;
            int width=menu==Pocket.Menu.INVENTORY?2:3;
            for(int seed=0;seed<100;seed++) {
                Pocket p=new Pocket();p.open(menu);
                String ingredient=task==8?"OAK_LOG":task==13?"COBBLESTONE":"OAK_PLANKS";
                int count=task==8?1:task==9?2:task==10?4:3;
                p.setStorage(0,new Stack(ingredient,count));
                if(task==11||task==13)p.setStorage(1,new Stack("STICK",2));
                InitialCrafting.prepare(p,task,(seed%5)/4.0,new RandomSource(seed));
                check(p.count(ingredient)==count);
                check(p.count("STICK")==((task==11||task==13)?2:0));
                check(p.crafted.isEmpty());String before=state(p);
                double progress=InitialCrafting.progress(p,task);
                check(progress>=0&&progress<=1);check(before.equals(state(p)));
                if(progress==1)check(!p.get(width==2?40:45,Pocket.NONE).empty());
            }
        }
        for(int width:new int[]{2,3})for(int oy=0;oy<=width-2;oy++)for(int ox=0;ox<=width-2;ox++) {
            Pocket p=new Pocket();p.open(width==2?Pocket.Menu.INVENTORY:Pocket.Menu.WORKBENCH);
            for(int y=0;y<2;y++)for(int x=0;x<2;x++)put(p,36+(y+oy)*width+x+ox,"OAK_PLANKS",1);
            near(InitialCrafting.progress(p,10),1);
            check(p.get(width==2?40:45,Pocket.NONE).item().equals("CRAFTING_TABLE"));
        }
        Pocket pick=new Pocket();pick.open(Pocket.Menu.WORKBENCH);
        for(int slot:new int[]{36,37,38})put(pick,slot,"OAK_PLANKS",1);
        for(int slot:new int[]{40,43})put(pick,slot,"STICK",1);
        near(InitialCrafting.progress(pick,11),1);
        check(pick.get(45,Pocket.NONE).item().equals("WOODEN_PICKAXE"));
        put(pick,39,"DIRT",1);check(InitialCrafting.progress(pick,11)<1);check(pick.get(45,Pocket.NONE).empty());
        RandomSource untouched=new RandomSource(99);long before=untouched.state();
        InitialCrafting.prepare(pick,11,1,untouched);check(untouched.state()==before);
        for(Course.Kind kind:new Course.Kind[]{Course.Kind.PROBE,Course.Kind.EXAM}) {
            RandomSource rng=new RandomSource(73);before=rng.state();
            AimPractice.Pose pose=AimPractice.reset(kind,.3,121,-12,0,30,rng);
            near(pose.yaw(),121);near(pose.pitch(),-12);check(before==rng.state());
        }
        for(Task task:Task.values())for(double progress:new double[]{0,.5,1}) {
            double outside=StationPractice.potential(task,false,90,40,4,progress);
            double inside=StationPractice.potential(task,true,90,40,4,progress);
            check(outside>=-.4&&outside<=0&&inside>=0&&inside<=1.3);
            if(StationPractice.applies(task))check(inside>outside);else near(inside,0);
        }
        // Discounted shaping telescopes; opening/closing or ingredient rearrangement
        // cannot produce extra episode return when the final potential is zero.
        double[] potentials={-.4,.3,.7,1.3,.3,-.1,0};double discount=1,total=0;
        for(int i=0;i<potentials.length-1;i++) {
            double gamma=VTrace.discount(4+i,i==potentials.length-2);
            total+=discount*(gamma*potentials[i+1]-potentials[i]);discount*=gamma;
        }
        near(total,-potentials[0]);
        for(Task task:Task.values())for(Course.Kind kind:Course.Kind.values())for(double d:new double[]{0,.2,.549,.55,.8,1}) {
            Course.Lesson lesson=new Course.Lesson(1,task,d,3,kind);
            boolean assisted=StationPractice.applies(task)&&kind==Course.Kind.PRACTICE&&d<1;
            check((StationPractice.acquisition(lesson)||StationPractice.operation(lesson))==assisted);
            check(!(StationPractice.acquisition(lesson)&&StationPractice.operation(lesson)));
        }
        System.out.println("PASS station and menu-correct recipe checks="+checks);
    }
}
