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
        for(Task task:Task.values())check(HarvestPractice.applies(task)==(task==Task.BREAK_LOG||task==Task.COLLECT_LOG));
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
        System.out.println("PASS harvesting practice invariants="+checks);
    }
}
