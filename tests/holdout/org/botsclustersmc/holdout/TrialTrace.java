package org.botsclustersmc.holdout;

import java.util.Locale;
import java.util.Arrays;
import org.botsclustersmc.core.Pocket;
import org.botsclustersmc.core.Policy;
import org.botsclustersmc.core.Stack;
import java.util.Map;
import org.botsclustersmc.plugin.Frame;
import org.botsclustersmc.plugin.Npc;

/** Owner-thread diagnostics of neural actions and real outcomes; never control gameplay. */
final class TrialTrace {
    private int observations,digDecisions,maxTargetMiningTicks;
    private double yaw,pitch,closest=Double.POSITIVE_INFINITY;
    private final int[] guiOperations=new int[6],clickedSlots=new int[64],menus=new int[5];
    private int openObservations,recipeObservations,maxGridUnits,maxCrafted;
    private int dropDecisions,useDecisions,minRawUnits=Integer.MAX_VALUE;
    private final StringBuilder firstClicks=new StringBuilder();
    private int recordedClicks;
    private CraftingTrace crafting;
    void observe(Npc npc,Npc.Applied previous,Frame next,Policy policy) {
        int task=npc.goal.task().ordinal();
        if(CraftingTrace.applies(task)) {
            if(crafting==null)crafting=new CraftingTrace();
            crafting.observe(policy,task,previous.frame().observation(),previous.frame().mask(),
                next.observation(),previous.result().actions(),previous.result().logProbability());
        }
        observations++;int[] action=previous.result().actions();
        if(action[4]==1)digDecisions++;
        if(action[4]==2)useDecisions++;
        if(action[4]==3)dropDecisions++;
        guiOperations[action[6]]++;
        if(action[6]>=1&&action[6]<=3)clickedSlots[action[7]]++;
        Pocket pocket=npc.pocket;menus[pocket.menu().ordinal()]++;
        if(pocket.menu()!=Pocket.Menu.CLOSED)openObservations++;
        int result=pocket.menu()==Pocket.Menu.INVENTORY?40:pocket.menu()==Pocket.Menu.WORKBENCH?45:-1;
        if(result>=0) {
            if(!pocket.get(result,Pocket.NONE).empty())recipeObservations++;
            int units=0;for(int i=36;i<result;i++)units+=pocket.get(i,Pocket.NONE).count();
            maxGridUnits=Math.max(maxGridUnits,units);
        }
        maxCrafted=Math.max(maxCrafted,(int)total(pocket.crafted));
        minRawUnits=Math.min(minRawUnits,pocket.countKind(npc.goal.task().ordinal()==8?2:3));
        if(action[6]!=0&&recordedClicks<24) {
            if(recordedClicks++>0)firstClicks.append(';');
            firstClicks.append(action[6]).append(':').append(action[7]).append(':')
                .append(pocket.menu().ordinal()).append(':').append(Stack.kind(pocket.cursor().item()))
                .append(':').append(pocket.cursor().count());
        }
        yaw+=Math.abs(next.yawError());pitch+=Math.abs(next.pitchError());closest=Math.min(closest,next.distance());
        String target=(int)Math.floor(npc.goal.x())+":"+(int)Math.floor(npc.goal.y())+":"+(int)Math.floor(npc.goal.z())+":";
        if(npc.mining!=null&&npc.mining.startsWith(target))maxTargetMiningTicks=Math.max(maxTargetMiningTicks,npc.miningTicks);
    }
    private static long total(Map<String,Long> counts) {
        long total=0;for(long value:counts.values())total+=value;return total;
    }
    String json(Npc npc) {
        if(observations==0)throw new IllegalStateException("A trial requires actual observations");
        String motor=String.format(Locale.ROOT,"{\"observations\":%d,\"dig_decisions\":%d,\"observed_max_target_mining_ticks\":%d,\"mean_abs_yaw_error\":%.6f,\"mean_abs_pitch_error\":%.6f,\"closest_distance\":%.6f,\"blocks_broken\":%d,\"items_collected\":%d}",
            observations,digDecisions,maxTargetMiningTicks,yaw/observations,pitch/observations,closest,total(npc.broken),total(npc.collected));
        return motor.substring(0,motor.length()-1)+String.format(Locale.ROOT,
            ",\"gui_operations\":%s,\"clicked_slots\":%s,\"menu_observations\":%s,\"menu_open_observations\":%d,\"recipe_visible_observations\":%d,\"max_grid_units\":%d,\"max_crafted_units\":%d,\"drop_decisions\":%d,\"use_decisions\":%d,\"minimum_raw_units\":%d,\"first_clicks_op_slot_menu_cursor_kind_count\":\"%s\"}",
            Arrays.toString(guiOperations),Arrays.toString(clickedSlots),Arrays.toString(menus),openObservations,recipeObservations,
            maxGridUnits,maxCrafted,dropDecisions,useDecisions,minRawUnits,firstClicks)
            .replaceFirst("}$",crafting==null?"}":",\"crafting\":"+crafting.json()+"}");
    }
}
