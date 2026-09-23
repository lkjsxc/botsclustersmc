package org.botsclustersmc.core;

/** Body-relative, bounded control features; no curriculum labels or suggested actions. */
public final class GoalInputs {
    private GoalInputs() {}
    public static double forward(double x,double z,double yaw) {
        double a=Math.toRadians(yaw);return -Math.sin(a)*x+Math.cos(a)*z;
    }
    public static double right(double x,double z,double yaw) {
        double a=Math.toRadians(yaw);return Math.cos(a)*x+Math.sin(a)*z;
    }
    public static float unit(double value) { return (float)Math.max(-1,Math.min(1,value)); }
    public static void encode(float[] f,double dx,double dy,double dz,double yaw,
            double vx,double vz,int stillTicks,boolean water,boolean lava) {
        double distance=Math.hypot(dx,dz),fwd=forward(dx,dz,yaw),side=right(dx,dz,yaw);
        f[334]=unit(side/4);f[335]=unit(fwd/4);f[336]=unit(dy/4);f[337]=unit(distance/4);
        f[338]=(float)(1/(1+distance));
        f[339]=distance<1e-6?0:unit(side/distance);f[340]=distance<1e-6?1:unit(fwd/distance);
        f[341]=unit(forward(vx,vz,yaw)/.18);f[342]=unit(right(vx,vz,yaw)/.18);
        f[343]=unit(stillTicks/20.0);f[344]=water?1:0;f[345]=lava?1:0;
    }
}
