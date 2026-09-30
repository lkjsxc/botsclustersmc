package org.botsclustersmc.tests;

import org.botsclustersmc.core.Task;
import org.botsclustersmc.training.*;

/** Potential-shaping invariants only; not learned mining evidence. */
public final class MiningPracticeTest {
    private static int checks;
    private static void check(boolean ok){checks++;if(!ok)throw new AssertionError("check "+checks);}
    private static void near(double a,double b){check(Math.abs(a-b)<1e-12);}
    private static void rejects(double value) {
        checks++;
        try {MiningPractice.potential(Task.MINE_COBBLESTONE,true,true,value);}
        catch(IllegalArgumentException expected){return;}
        throw new AssertionError("invalid progress accepted");
    }
    public static void main(String[] args) {
        for(Task task:Task.values())check(MiningPractice.applies(task)==(task==Task.MINE_COBBLESTONE));
        for(Task task:Task.values())if(task!=Task.MINE_COBBLESTONE)
            near(MiningPractice.potential(task,true,true,Double.NaN),0);
        near(MiningPractice.potential(Task.MINE_COBBLESTONE,false,false,0),0);
        near(MiningPractice.potential(Task.MINE_COBBLESTONE,true,false,0),0);
        near(MiningPractice.potential(Task.MINE_COBBLESTONE,false,true,0),0);
        near(MiningPractice.potential(Task.MINE_COBBLESTONE,true,true,0),.10);
        near(MiningPractice.potential(Task.MINE_COBBLESTONE,false,false,1),.15);
        near(MiningPractice.potential(Task.MINE_COBBLESTONE,true,true,1),.25);
        near(MiningPractice.potential(Task.MINE_COBBLESTONE,true,true,.4),.16);
        rejects(-Double.MIN_VALUE);rejects(Math.nextUp(1.0));rejects(Double.NaN);
        rejects(Double.POSITIVE_INFINITY);rejects(Double.NEGATIVE_INFINITY);
        // Potential shaping telescopes. Dropping readiness and recovering it cannot farm return.
        double[] phi={.10,0,.10,.16,.25,0};
        double discount=1,total=0;
        for(int i=0;i<phi.length-1;i++) {
            boolean terminal=i==phi.length-2;
            double gamma=VTrace.discount(4,terminal);
            total+=discount*(gamma*phi[i+1]-phi[i]);discount*=gamma;
        }
        near(total,-phi[0]);
        // More real target progress is strictly better at a fixed readiness state.
        for(int i=0;i<100;i++) {
            double a=i/100.0,b=(i+1)/100.0;
            check(MiningPractice.potential(Task.MINE_COBBLESTONE,true,true,b)
                > MiningPractice.potential(Task.MINE_COBBLESTONE,true,true,a));
            check(MiningPractice.potential(Task.MINE_COBBLESTONE,false,false,b)
                > MiningPractice.potential(Task.MINE_COBBLESTONE,false,false,a));
        }
        System.out.println("PASS mining readiness potential checks="+checks);
    }
}
