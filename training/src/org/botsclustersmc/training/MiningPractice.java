package org.botsclustersmc.training;

import org.botsclustersmc.core.Task;

/** State-only mining shaping. It never chooses a tool, menu, look direction or dig action. */
public final class MiningPractice {
    private MiningPractice() {}
    public static boolean applies(Task task) { return task==Task.MINE_COBBLESTONE; }
    public static double potential(Task task,boolean menuClosed,boolean pickHeld,double targetProgress) {
        if(!applies(task))return 0;
        if(!Double.isFinite(targetProgress)||targetProgress<0||targetProgress>1)
            throw new IllegalArgumentException("Invalid mining progress");
        double ready=menuClosed&&pickHeld?.10:0;
        return ready+.15*targetProgress;
    }
}
