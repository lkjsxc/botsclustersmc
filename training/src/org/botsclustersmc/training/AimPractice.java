package org.botsclustersmc.training;

import org.botsclustersmc.core.RandomSource;

/** Reset-only orientation curriculum. Never chooses an in-episode action. */
public final class AimPractice {
    private AimPractice() {}
    public record Pose(float yaw,float pitch) {}
    public static Pose reset(Course.Kind kind,double difficulty,float yaw,float pitch,
            double targetYaw,double targetPitch,RandomSource rng) {
        if(kind!=Course.Kind.PRACTICE||difficulty>=1)return new Pose(yaw,pitch);
        return pose(targetYaw,targetPitch,difficulty,rng);
    }
    public static Pose pose(double targetYaw,double targetPitch,double difficulty,RandomSource rng) {
        if(!Double.isFinite(targetYaw)||!Double.isFinite(targetPitch)||!Double.isFinite(difficulty)
            ||difficulty<0||difficulty>1)throw new IllegalArgumentException("Invalid aiming reset");
        double spread=difficulty*difficulty;
        double yaw=targetYaw+rng.symmetric(5+175*spread);
        double pitch=targetPitch+rng.symmetric(3+80*spread);
        return new Pose((float)(yaw%360),(float)Math.max(-89,Math.min(89,pitch)));
    }
    /** Practice grows from one settled decision to the unchanged full 20-tick exam. */
    public static int requiredHold(Course.Kind kind,double difficulty) {
        if(!Double.isFinite(difficulty)||difficulty<0||difficulty>1)throw new IllegalArgumentException("Invalid practice difficulty");
        return kind==Course.Kind.PRACTICE?4+(int)Math.ceil(16*difficulty*difficulty):20;
    }
    /** Dense state cost, deliberately not only a telescoping potential difference. */
    public static double controlReward(double yawError,double pitchError,double angularSpeed,int elapsedTicks) {
        if(!Double.isFinite(yawError)||!Double.isFinite(pitchError)||!Double.isFinite(angularSpeed)
            ||angularSpeed<0||elapsedTicks<1)throw new IllegalArgumentException("Invalid control reward state");
        double alignment=.04*Math.min(1,Math.abs(yawError)/180)+.04*Math.min(1,Math.abs(pitchError)/90);
        double motion=.002*Math.min(1,angularSpeed/8);
        return -(alignment+motion)*(elapsedTicks/4.0);
    }
    public static double potential(double yawError,double pitchError,int settledTicks) {
        if(!Double.isFinite(yawError)||!Double.isFinite(pitchError)||settledTicks<0)
            throw new IllegalArgumentException("Invalid aiming state");
        return -(Math.abs(yawError)+Math.abs(pitchError))/180+.3*Math.min(1,settledTicks/20.0);
    }
}
