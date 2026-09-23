package org.botsclustersmc.tests;

import org.botsclustersmc.core.*;
import java.util.*;

/** Pure coordinate/observation regressions, not demonstrations or Minecraft skill tests. */
public final class ControlTest {
    private static int checks;
    private static void equal(double actual,double expected){checks++;if(Math.abs(actual-expected)>1e-5)throw new AssertionError(actual+" != "+expected);}
    private static void check(boolean condition){checks++;if(!condition)throw new AssertionError();}
    public static void main(String[] args){
        equal(GoalInputs.forward(0,4,0),4);equal(GoalInputs.right(3,0,0),3);
        equal(GoalInputs.forward(-4,0,90),4);equal(GoalInputs.right(0,3,90),3);
        Random random=new Random(817);
        for(int i=0;i<1000;i++){
            double x=random.nextDouble()*20-10,z=random.nextDouble()*20-10,yaw=random.nextDouble()*360;
            double angle=Math.toRadians(yaw),forward=GoalInputs.forward(x,z,yaw),right=GoalInputs.right(x,z,yaw);
            equal(-Math.sin(angle)*forward+Math.cos(angle)*right,x);
            equal(Math.cos(angle)*forward+Math.sin(angle)*right,z);
            equal(forward*forward+right*right,x*x+z*z);
        }
        float[] f=new float[Schema.INPUTS];
        GoalInputs.encode(f,0,0,0,17,0,0,25,false,false);
        for(float value:f)check(Float.isFinite(value));equal(f[338],1);equal(f[339],0);equal(f[340],1);equal(f[343],1);
        GoalInputs.encode(f,-8,8,0,90,-.18,0,10,true,false);
        equal(f[334],0);equal(f[335],1);equal(f[336],1);equal(f[337],1);equal(f[341],1);equal(f[343],.5);equal(f[344],1);equal(f[345],0);
        GoalInputs.encode(f,0,0,1,0,0,0,0,false,true);equal(f[345],1);equal(f[344],0);
        boolean[] mask=Task.FORWARD_STOP.mask(0,false);
        check(mask[0]&&mask[1]&&mask[2]);for(int i=3;i<9;i++)check(!mask[i]);
        // No direction is selected by a target position: the entire lesson shares one mask.
        check(Arrays.equals(mask,Task.FORWARD_STOP.mask(0,false)));
        check(Schema.INPUTS==512);check(Schema.HIDDEN==96);
        int[] input=Schema.IDLE.clone();String idle=ActionText.describe(input);
        check(idle.contains("move=stop")&&idle.contains("interact=none")&&!idle.contains("slot(index)"));
        input[0]=5;input[1]=4;input[2]=0;input[3]=1;input[4]=3;input[5]=8;input[6]=2;input[7]=38;
        String description=ActionText.describe(input);
        check(description.contains("move=forward-left")&&description.contains("yaw=8")&&description.contains("pitch=-4"));
        check(description.contains("jump-pulse")&&description.contains("drop-one")&&description.contains("hotbar=9"));
        check(description.contains("right-click")&&description.contains("slot(index)=38"));
        check(input[0]==5&&input[4]==3&&input[7]==38); // Text rendering must not alter gameplay.
        System.out.println("PASS control observation checks="+checks);
    }
}
