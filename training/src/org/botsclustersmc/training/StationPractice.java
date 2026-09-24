package org.botsclustersmc.training;

import org.botsclustersmc.core.Pocket;
import org.botsclustersmc.core.Task;

/** Training sublessons and bounded potentials; never chooses a gameplay action. */
public final class StationPractice {
    private StationPractice() {}
    public static Pocket.Menu station(Task task) {
        return switch(task) {
            case CRAFT_WOOD_PICK, CRAFT_STONE_PICK -> Pocket.Menu.WORKBENCH;
            case SMELT_IRON -> Pocket.Menu.FURNACE;
            case SUPPLY_CHEST -> Pocket.Menu.CHEST;
            default -> Pocket.Menu.CLOSED;
        };
    }
    public static boolean applies(Task task) {return station(task)!=Pocket.Menu.CLOSED;}
    /** Shorter practice completion is never used by probes or frozen exams. */
    public static boolean acquisition(Course.Lesson lesson) {
        return applies(lesson.task())&&lesson.kind()==Course.Kind.PRACTICE&&lesson.difficulty()<.55;
    }
    public static double potential(Task task,boolean stationOpen,double yawError,
            double pitchError,double distance,double progress) {
        if(!applies(task))return 0;
        if(!Double.isFinite(yawError)||!Double.isFinite(pitchError)||!Double.isFinite(distance)
                ||!Double.isFinite(progress)||distance<0||progress<0||progress>1)
            throw new IllegalArgumentException("Invalid station state");
        if(stationOpen)return .3+progress;
        return -.15*Math.min(1,Math.abs(yawError)/90)-.15*Math.min(1,Math.abs(pitchError)/45)
            -.1*Math.min(1,Math.max(0,distance-3)/8);
    }
}
