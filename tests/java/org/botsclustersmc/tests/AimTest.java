package org.botsclustersmc.tests;

import org.botsclustersmc.core.RandomSource;
import org.botsclustersmc.training.*;

/** Reset curriculum invariants, not learned-skill evidence. */
public final class AimTest {
    private static int checks;
    private static void check(boolean ok){checks++;if(!ok)throw new AssertionError("check "+checks);}
    public static void main(String[] args) {
        for(Course.Kind kind:Course.Kind.values())for(double difficulty:new double[]{0,.1,.5,1}) {
            if(kind==Course.Kind.PRACTICE&&difficulty<1)continue;
            RandomSource rng=new RandomSource(947);long before=rng.state();
            var pose=AimPractice.reset(kind,difficulty,127,-19,0,0,rng);
            check(pose.yaw()==127&&pose.pitch()==-19);check(rng.state()==before);
        }
        for(int i=0;i<1000;i++) {
            RandomSource rng=new RandomSource(i);var pose=AimPractice.pose(50,-20,.1,rng);
            check(Math.abs(pose.yaw()-50)<=6.751);check(Math.abs(pose.pitch()+20)<=3.801);
            check(Float.isFinite(pose.yaw())&&Float.isFinite(pose.pitch()));
            var extreme=AimPractice.pose(179,89,.99,rng);
            check(extreme.pitch()>=-89&&extreme.pitch()<=89);
        }
        check(AimPractice.potential(0,0,20)==.3);check(AimPractice.potential(0,0,40)==.3);
        check(AimPractice.potential(0,0,16)<AimPractice.potential(0,0,20));
        check(AimPractice.potential(12,8,0)==AimPractice.potential(-12,-8,0));
        check(AimPractice.controlReward(0,0,0,4)==0);
        check(AimPractice.controlReward(10,5,0,4)>AimPractice.controlReward(100,50,0,4));
        check(AimPractice.controlReward(10,5,0,4)==AimPractice.controlReward(-10,-5,0,4));
        check(AimPractice.controlReward(0,0,2,4)<AimPractice.controlReward(0,0,0,4));
        check(AimPractice.controlReward(30,10,2,8)==2*AimPractice.controlReward(30,10,2,4));
        for(Course.Kind kind:new Course.Kind[]{Course.Kind.PROBE,Course.Kind.EXAM})
            for(double difficulty:new double[]{0,.1,.5,1})check(AimPractice.requiredHold(kind,difficulty)==20);
        check(AimPractice.requiredHold(Course.Kind.PRACTICE,0)==4);
        check(AimPractice.requiredHold(Course.Kind.PRACTICE,.1)==5);
        check(AimPractice.requiredHold(Course.Kind.PRACTICE,1)==20);
        int last=4;for(int i=0;i<=100;i++){int hold=AimPractice.requiredHold(Course.Kind.PRACTICE,i/100.0);check(hold>=last&&hold<=20);last=hold;}
        System.out.println("PASS aiming reset curriculum checks="+checks);
    }
}
