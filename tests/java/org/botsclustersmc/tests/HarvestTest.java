package org.botsclustersmc.tests;

import org.botsclustersmc.core.*;
import org.botsclustersmc.training.*;

/** Resource curriculum invariants; not a claim of learned harvesting. */
public final class HarvestTest {
    private static int checks;
    private static void check(boolean yes){checks++;if(!yes)throw new AssertionError("check "+checks);}
    private static void near(double actual,double expected){check(Math.abs(actual-expected)<1e-9);}
    private static double reward(boolean broken,double yaw,double pitch,double distance,double speed,double progress,int ticks){
        return HarvestPractice.controlReward(Task.COLLECT_LOG,broken,yaw,pitch,distance,speed,progress,ticks);
    }
    public static void main(String[] args) {
        for(Task task:Task.values())check(HarvestPractice.applies(task)==(task==Task.BREAK_LOG||task==Task.COLLECT_LOG||task==Task.MINE_COBBLESTONE));
        for(Task task:Task.values())for(Course.Kind kind:Course.Kind.values())for(double difficulty:new double[]{0,.1,.5,.99,1}) {
            RandomSource rng=new RandomSource(761);long before=rng.state();
            var pose=HarvestPractice.reset(task,kind,difficulty,123,-24,0,0,rng);
            if(!HarvestPractice.applies(task)||kind!=Course.Kind.PRACTICE||difficulty==1) {
                check(pose.yaw()==123&&pose.pitch()==-24);check(rng.state()==before);
            }else {
                check(rng.state()!=before);check(Float.isFinite(pose.yaw()));check(pose.pitch()>=-89&&pose.pitch()<=89);
            }
        }
        near(reward(false,0,0,3,0,0,4),-.04);
        near(reward(false,0,0,3,0,1,4),0);
        check(reward(false,5,5,3,0,0,4)>reward(false,80,40,3,0,0,4));
        near(reward(false,-20,-10,3,0,0,4),reward(false,20,10,3,0,0,4));
        check(reward(false,0,0,3,.18,0,4)<reward(false,0,0,3,0,0,4));
        check(reward(false,0,0,8,0,0,4)<reward(false,0,0,3,0,0,4));
        check(reward(false,0,0,3,0,.8,4)>reward(false,0,0,3,0,.2,4));
        near(reward(false,7,3,4,.1,.4,8),2*reward(false,7,3,4,.1,.4,4));
        near(reward(true,90,45,.5,.18,0,4),0);
        check(reward(true,0,0,1,0,0,4)>reward(true,0,0,6,0,0,4));
        near(HarvestPractice.controlReward(Task.BREAK_LOG,true,180,90,10,.18,1,4),0);
        for(Task task:Task.values())if(!HarvestPractice.applies(task))
            near(HarvestPractice.controlReward(task,false,45,30,8,1,.5,4),0);
        RandomSource rng=new RandomSource(318);
        for(int i=0;i<10000;i++) {
            int ticks=1+rng.nextInt(20);
            double value=reward(false,rng.symmetric(180),rng.symmetric(90),rng.unit()*30,rng.unit(),rng.unit(),ticks);
            check(Double.isFinite(value));check(value<=0);check(value>=-.109*ticks/4-1e-12);
        }
        check(reward(false,0,0,3,0,.99,4)<0);
        check(reward(false,180,90,3,0,0,4)<reward(false,0,0,3,0,0,4));
        contactAndCollection();
        System.out.println("PASS harvesting practice invariants="+checks);
    }
    private static void rejects(Runnable action) {
        try { action.run();throw new AssertionError("Expected invalid harvesting state"); }
        catch(IllegalArgumentException expected) { checks++; }
    }
    private static void contactAndCollection() {
        for(Task task:Task.values()) {
            int expectedKind=task==Task.MINE_COBBLESTONE?9:task==Task.BREAK_LOG||task==Task.COLLECT_LOG?2:-1;
            check(HarvestPractice.targetBlockKind(task)==expectedKind);
            for(int held=0;held<=20;held++)for(int ticks:new int[]{0,1,4,39,40,59,60,299,300,Integer.MAX_VALUE}) {
                near(HarvestPractice.contactProgress(task,false,held,ticks),0);
                double expected=expectedKind<0||task==Task.MINE_COBBLESTONE&&held!=6&&held!=7?0
                    :Math.min(1,ticks/(task==Task.MINE_COBBLESTONE?40.0:60.0));
                near(HarvestPractice.contactProgress(task,true,held,ticks),expected);
            }
        }
        rejects(()->HarvestPractice.contactProgress(Task.MINE_COBBLESTONE,true,6,-1));
        near(HarvestPractice.contactProgress(Task.MINE_COBBLESTONE,true,6,39),.975);
        // More time without a suitable tool, or contact elsewhere, is not harvesting progress.
        near(HarvestPractice.contactProgress(Task.MINE_COBBLESTONE,true,0,299),0);
        near(HarvestPractice.contactProgress(Task.MINE_COBBLESTONE,false,6,39),0);
        RandomSource rng=new RandomSource(92026);
        for(int i=0;i<10000;i++) {
            double yaw=rng.symmetric(180),pitch=rng.symmetric(90),distance=rng.unit()*30,speed=rng.unit(),progress=rng.unit();
            int ticks=1+rng.nextInt(20);
            for(boolean broken:new boolean[]{false,true}) {
                double stone=HarvestPractice.controlReward(Task.MINE_COBBLESTONE,broken,yaw,pitch,distance,speed,progress,ticks);
                near(stone,reward(broken,yaw,pitch,distance,speed,progress,ticks));
                check(Double.isFinite(stone)&&stone<=0);
                check(stone>=-.109*ticks/4-1e-12);
            }
        }
        for(Task task:new Task[]{Task.BREAK_LOG,Task.COLLECT_LOG,Task.MINE_COBBLESTONE}) {
            rejects(()->HarvestPractice.controlReward(task,false,Double.NaN,0,3,0,0,4));
            rejects(()->HarvestPractice.controlReward(task,false,0,Double.POSITIVE_INFINITY,3,0,0,4));
            rejects(()->HarvestPractice.controlReward(task,false,0,0,-1,0,0,4));
            rejects(()->HarvestPractice.controlReward(task,false,0,0,3,-1,0,4));
            rejects(()->HarvestPractice.controlReward(task,false,0,0,3,0,1.01,4));
            rejects(()->HarvestPractice.controlReward(task,false,0,0,3,0,0,0));
        }
        // Target-contact potentials telescope, including contact lost before timeout.
        double gamma=VTrace.discount(4,false),discount=1,total=0;
        double[] potentials={0,.015,.09,.04,.14625,0};
        for(int i=1;i<potentials.length;i++) {
            total+=discount*(gamma*potentials[i]-potentials[i-1]);discount*=gamma;
        }
        near(total,0);
    }
}
