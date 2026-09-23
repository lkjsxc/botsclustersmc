package org.botsclustersmc.plugin;

import java.util.Arrays;
import java.util.UUID;

/** Immutable observation for operators, copied only by the owning entity thread. */
public record AgentSnapshot(long id, String body, UUID world, double x, double y, double z,
        float yaw, float pitch, double goalX, double goalY, double goalZ,
        String task, double distance, double speed, double health, int fireTicks,
        boolean burnsInSunlight, long decisions, long policy, String action,
        double logProbability, long capturedNanos) {
    public static AgentSnapshot capture(Npc npc, Frame frame, Npc.Applied applied) {
        return new AgentSnapshot(npc.id, npc.entity.getType().name(), npc.entity.getWorld().getUID(), frame.x(),
            frame.y(), frame.z(), frame.yaw(), frame.pitch(), npc.goal.x(),
            npc.goal.y(), npc.goal.z(), npc.goal.task().label(), frame.distance(),
            Math.hypot(frame.vx(), frame.vz()), npc.entity.getHealth(),
            npc.entity.getFireTicks(), npc.entity instanceof org.bukkit.entity.Zombie zombie && zombie.shouldBurnInDay(), npc.decisions,
            applied.result().policyVersion(), Arrays.toString(applied.result().actions()),
            applied.result().logProbability(), System.nanoTime());
    }
}
