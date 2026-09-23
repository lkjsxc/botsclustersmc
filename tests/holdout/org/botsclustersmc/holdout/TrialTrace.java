package org.botsclustersmc.holdout;

import java.util.Locale;
import java.util.Map;
import org.botsclustersmc.plugin.Frame;
import org.botsclustersmc.plugin.Npc;

/** Owner-thread diagnostics of neural actions and real outcomes; never control gameplay. */
final class TrialTrace {
    private int observations,digDecisions,maxTargetMiningTicks;
    private double yaw,pitch,closest=Double.POSITIVE_INFINITY;
    void observe(Npc npc,Npc.Applied previous,Frame next) {
        observations++;if(previous.result().actions()[4]==1)digDecisions++;
        yaw+=Math.abs(next.yawError());pitch+=Math.abs(next.pitchError());closest=Math.min(closest,next.distance());
        String target=(int)Math.floor(npc.goal.x())+":"+(int)Math.floor(npc.goal.y())+":"+(int)Math.floor(npc.goal.z())+":";
        if(npc.mining!=null&&npc.mining.startsWith(target))maxTargetMiningTicks=Math.max(maxTargetMiningTicks,npc.miningTicks);
    }
    private static long total(Map<String,Long> counts) {
        long total=0;for(long value:counts.values())total+=value;return total;
    }
    String json(Npc npc) {
        if(observations==0)throw new IllegalStateException("A trial requires actual observations");
        return String.format(Locale.ROOT,"{\"observations\":%d,\"dig_decisions\":%d,\"observed_max_target_mining_ticks\":%d,\"mean_abs_yaw_error\":%.6f,\"mean_abs_pitch_error\":%.6f,\"closest_distance\":%.6f,\"blocks_broken\":%d,\"items_collected\":%d}",
            observations,digDecisions,maxTargetMiningTicks,yaw/observations,pitch/observations,closest,total(npc.broken),total(npc.collected));
    }
}
