package org.botsclustersmc.tests;

import org.botsclustersmc.core.*;
import org.botsclustersmc.training.*;

/** Mining-control reward invariants only; not evidence of learned Minecraft mining. */
public final class MiningControlTest {
    private static int checks;
    private static void check(boolean ok){checks++;if(!ok)throw new AssertionError("check "+checks);}
    private static void near(double a,double b){check(Math.abs(a-b)<1e-12);}
    private static double r(boolean broken,double yaw,double pitch,double distance,double speed,double progress,int ticks) {
        return MiningControl.reward(Task.MINE_COBBLESTONE,broken,yaw,pitch,distance,speed,progress,ticks);
    }
    private static void reject(double yaw,double pitch,double distance,double speed,double progress,int ticks) {
        checks++;
        try {r(false,yaw,pitch,distance,speed,progress,ticks);}
        catch(IllegalArgumentException expected){return;}
        throw new AssertionError("invalid mining control state accepted");
    }
    public static void main(String[] args) {
        for(Task task:Task.values())check(MiningControl.applies(task)==(task==Task.MINE_COBBLESTONE));
        for(Task task:Task.values())if(task!=Task.MINE_COBBLESTONE)
            near(MiningControl.reward(task,false,Double.NaN,Double.NaN,-1,-1,Double.NaN,0),0);
        near(r(false,0,0,3,0,0,4),-.04);
        near(r(false,0,0,3,0,1,4),0);
        check(r(false,5,5,3,0,0,4)>r(false,90,45,3,0,0,4));
        near(r(false,-20,-10,3,0,0,4),r(false,20,10,3,0,0,4));
        check(r(false,0,0,3,.18,0,4)<r(false,0,0,3,0,0,4));
        check(r(false,0,0,8,0,0,4)<r(false,0,0,3,0,0,4));
        check(r(false,0,0,3,0,.8,4)>r(false,0,0,3,0,.2,4));
        near(r(false,7,3,4,.1,.4,8),2*r(false,7,3,4,.1,.4,4));
        for(double yaw:new double[]{-180,-45,0,45,180})
            for(double pitch:new double[]{-90,-20,0,20,90})
                for(double distance:new double[]{0,3,8,30})
                    for(double speed:new double[]{0,.1,.18,1})
                        for(double progress:new double[]{0,.2,.8,1}) {
                            double value=r(false,yaw,pitch,distance,speed,progress,4);
                            check(Double.isFinite(value)&&value<=0&&value>=-.1090000001);
                            near(r(true,yaw,pitch,distance,speed,progress,4),0);
                        }
        reject(Double.NaN,0,3,0,0,4);reject(0,Double.NaN,3,0,0,4);
        reject(0,0,Double.NaN,0,0,4);reject(0,0,-1,0,0,4);
        reject(0,0,3,Double.NaN,0,4);reject(0,0,3,-1,0,4);
        reject(0,0,3,0,Double.NaN,4);reject(0,0,3,0,-Double.MIN_VALUE,4);
        reject(0,0,3,0,Math.nextUp(1.0),4);reject(0,0,3,0,0,0);
        RandomSource random=new RandomSource(1206);
        for(int i=0;i<10000;i++) {
            int ticks=1+random.nextInt(20);
            double value=r(false,random.symmetric(180),random.symmetric(90),random.unit()*30,
                random.unit(),random.unit(),ticks);
            check(Double.isFinite(value)&&value<=0&&value>=-.109*ticks/4-1e-12);
        }
        System.out.println("PASS mining spatial control cost checks="+checks);
    }
}
