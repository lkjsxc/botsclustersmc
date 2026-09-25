package org.botsclustersmc.training;

import org.botsclustersmc.core.Pocket;
import org.botsclustersmc.core.RandomSource;
import org.botsclustersmc.core.Task;

/** Reset-only scaffolding with one task-completion goal; never chooses a gameplay action. */
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
    /** Assistance changes the initial state, never the task's success predicate. */
    public static boolean operation(Course.Lesson lesson) {
        return applies(lesson.task())&&lesson.kind()==Course.Kind.PRACTICE&&lesson.difficulty()<1;
    }
    /** Isolated from world/pose RNG so each lesson has reproducible pocket reset diagnostics. */
    public static RandomSource resetRandom(Course.Lesson lesson) {
        return new RandomSource(lesson.seed()^0x706f636b6574L);
    }
    /** Reset-only menu selection. Full probes/exams consume no assistance RNG. */
    public static Pocket.Menu initialMenu(Course.Lesson lesson,RandomSource rng) {
        if(lesson.kind()!=Course.Kind.PRACTICE||lesson.difficulty()>=1)return Pocket.Menu.CLOSED;
        if(applies(lesson.task()))return station(lesson.task());
        return switch(lesson.task()) {
            case CRAFT_PLANKS,CRAFT_STICKS,CRAFT_WORKBENCH ->
                rng.unit()>lesson.difficulty()?Pocket.Menu.INVENTORY:Pocket.Menu.CLOSED;
            default -> Pocket.Menu.CLOSED;
        };
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
