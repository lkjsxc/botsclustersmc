package org.botsclustersmc.training;

import org.botsclustersmc.core.RandomSource;
import org.botsclustersmc.core.Task;

/** Reset assistance and measured state costs, never an in-episode action controller. */
public final class HarvestPractice {
    private HarvestPractice() {}
    public static boolean applies(Task task) {
        return task==Task.BREAK_LOG||task==Task.COLLECT_LOG||task==Task.MINE_COBBLESTONE;
    }
    public static int targetBlockKind(Task task) {
        return task==Task.MINE_COBBLESTONE?9:task==Task.BREAK_LOG||task==Task.COLLECT_LOG?2:-1;
    }
    /** Harvestable contact, not merely a dig request or progress on an unrelated block.
     * Durations match the current primitive actuator: log 60 ticks, stone with pick 40.
     * Bare-hand stone breaking cannot yield the cobblestone required by this goal. */
    public static double contactProgress(Task task,boolean targetContact,int heldKind,int ticks) {
        if(ticks<0)throw new IllegalArgumentException("Negative mining ticks");
        if(!applies(task)||!targetContact)return 0;
        if(task==Task.MINE_COBBLESTONE&&heldKind!=6&&heldKind!=7)return 0;
        return Math.min(1,ticks/(task==Task.MINE_COBBLESTONE?40.0:60.0));
    }
    public static AimPractice.Pose reset(Task task,Course.Kind kind,double difficulty,
            float yaw,float pitch,double targetYaw,double targetPitch,RandomSource rng) {
        if(!applies(task))return new AimPractice.Pose(yaw,pitch);
        // Full probes/exams retain their original pose AND random stream.
        return AimPractice.reset(kind,difficulty,yaw,pitch,targetYaw,targetPitch,rng);
    }
    public static double controlReward(Task task,boolean targetBroken,double yawError,
            double pitchError,double distance,double speed,double targetProgress,int ticks) {
        if(!applies(task))return 0;
        if(!Double.isFinite(yawError)||!Double.isFinite(pitchError)||!Double.isFinite(distance)
                ||!Double.isFinite(speed)||!Double.isFinite(targetProgress)||distance<0||speed<0
                ||targetProgress<0||targetProgress>1||ticks<1)
            throw new IllegalArgumentException("Invalid harvesting state");
        double time=ticks/4.0;
        if(targetBroken) {
            // A collected drop remains a real, provenance-checked inventory outcome.
            return task==Task.BREAK_LOG?0:-.025*Math.min(1,Math.max(0,distance-.6)/6)*time;
        }
        double alignment=.025*Math.min(1,Math.abs(yawError)/90)
            +.025*Math.min(1,Math.abs(pitchError)/45);
        double reach=.015*Math.min(1,Math.max(0,distance-3)/8);
        double linedUp=Math.max(0,1-(Math.abs(yawError)+Math.abs(pitchError))/30);
        double motion=.004*Math.min(1,speed/.18)*linedUp*Math.min(1,3/(distance+.01));
        // Nonpositive cost in every pre-break state: unfinished contact cannot farm
        // positive reward. Progress is measured against the actual target block;
        // selecting dig in empty air does not reduce this cost.
        double unfinished=.04*(1-targetProgress);
        return -(unfinished+alignment+reach+motion)*time;
    }
}
