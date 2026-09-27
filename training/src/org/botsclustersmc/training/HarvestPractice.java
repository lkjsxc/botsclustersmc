package org.botsclustersmc.training;

import org.botsclustersmc.core.RandomSource;
import org.botsclustersmc.core.Task;

/** Reset assistance and measured state costs, never an in-episode action controller. */
public final class HarvestPractice {
    private HarvestPractice() {}
    public static boolean applies(Task task) {
        return task==Task.BREAK_LOG||task==Task.COLLECT_LOG||task==Task.MINE_COBBLESTONE;
    }
    public static AimPractice.Pose reset(Task task,Course.Kind kind,double difficulty,
            float yaw,float pitch,double targetYaw,double targetPitch,RandomSource rng) {
        if(!applies(task))return new AimPractice.Pose(yaw,pitch);
        // Full probes/exams retain their original pose AND random stream.
        return AimPractice.reset(kind,difficulty,yaw,pitch,targetYaw,targetPitch,rng);
    }
    /** Observed contact only. Stone without a pick cannot produce the required cobblestone. */
    public static double miningProgress(Task task,boolean targetContact,int heldKind,int ticks) {
        if(heldKind<0||ticks<0)throw new IllegalArgumentException("Invalid mining contact");
        if(!applies(task)||!targetContact)return 0;
        if(task==Task.MINE_COBBLESTONE) {
            if(heldKind!=6&&heldKind!=7)return 0;
            return Math.min(1,ticks/40.0);
        }
        return Math.min(1,ticks/60.0);
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
            return task==Task.COLLECT_LOG||task==Task.MINE_COBBLESTONE
                ?-.025*Math.min(1,Math.max(0,distance-.6)/6)*time:0;
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
