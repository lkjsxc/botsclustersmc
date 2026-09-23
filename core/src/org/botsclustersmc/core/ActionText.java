package org.botsclustersmc.core;

/** Human-readable descriptions of already selected primitive inputs; never a controller. */
public final class ActionText {
    private ActionText() {}
    private static final String[] MOVE={"stop","forward","backward","left","right",
        "forward-left","forward-right","backward-left","backward-right"};
    private static final int[] YAW={-8,-2,0,2,8},PITCH={-4,-1,0,1,4};
    private static final String[] POSTURE={"walk","jump-pulse","crouch"};
    private static final String[] INTERACT={"none","dig","use","drop-one"};
    private static final String[] GUI={"none","left-click","right-click","shift-click","open-inventory","close-menu"};
    public static String describe(int[] action) {
        Schema.checkAction(action);
        return "move="+MOVE[action[0]]+" yaw="+YAW[action[1]]+" pitch="+PITCH[action[2]]
            +" deg/tick posture="+POSTURE[action[3]]+" interact="+INTERACT[action[4]]
            +" hotbar="+(action[5]+1)+" menu="+GUI[action[6]]
            +(Schema.slotActive(action[6])?" slot(index)="+action[7]:"");
    }
}
