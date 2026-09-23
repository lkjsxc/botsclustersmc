package org.botsclustersmc.plugin;

public record Frame(float[] observation,boolean[] mask,long tick,double x,double y,double z,float yaw,float pitch,
                    double vx,double vy,double vz,boolean ground,double distance,double yawError,double pitchError) {}
