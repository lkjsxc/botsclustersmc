package org.botsclustersmc.training;

import org.botsclustersmc.core.Task;

/** Nonpositive task-12 control cost from measured state; never an in-episode controller. */
public final class MiningControl {
    private MiningControl() {}
    public static boolean applies(Task task) { return task==Task.MINE_COBBLESTONE; }
    public static double reward(Task task,boolean targetBroken,double yawError,
            double pitchError,double distance,double speed,double targetProgress,int ticks) {
        if(!applies(task))return 0;
        if(!Double.isFinite(yawError)||!Double.isFinite(pitchError)||!Double.isFinite(distance)
                ||!Double.isFinite(speed)||!Double.isFinite(targetProgress)||distance<0||speed<0
                ||targetProgress<0||targetProgress>1||ticks<1)
            throw new IllegalArgumentException("Invalid mining control state");
        if(targetBroken)return 0; // Do not penalize post-break pickup/recovery.
        double time=ticks/4.0;
        double alignment=.025*Math.min(1,Math.abs(yawError)/90)
            +.025*Math.min(1,Math.abs(pitchError)/45);
        double reach=.015*Math.min(1,Math.max(0,distance-3)/8);
        double linedUp=Math.max(0,1-(Math.abs(yawError)+Math.abs(pitchError))/30);
        double motion=.004*Math.min(1,speed/.18)*linedUp*Math.min(1,3/(distance+.01));
        // Progress is the runtime's actual designated-target mining state. Selecting
        // dig in empty air leaves it zero. This remains a cost until real contact grows.
        double unfinished=.04*(1-targetProgress);
        return -(unfinished+alignment+reach+motion)*time;
    }
}
