package org.botsclustersmc.training;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.botsclustersmc.core.Task;

/** Bounded, read-only accounting of the first actually issued lesson per actor. */
public final class StartupCoverage {
    private final boolean[] seen;
    private final boolean restored;
    private final int[] tasks=new int[Task.values().length],trainingTasks=new int[Task.values().length];
    private int observed,foundation,frontier,review,exams;

    public StartupCoverage(int actors,boolean restored) {
        if(actors<1||actors>10000)throw new IllegalArgumentException("startup actor count");
        seen=new boolean[actors];this.restored=restored;
    }

    /** Records issuance, not a successful reset, transition, completed trial or learned skill. */
    public synchronized boolean record(long actor,int stage,Course.Lesson lesson) {
        if(actor<0||actor>=seen.length||stage<0||stage>=tasks.length||lesson==null
                ||lesson.task()==null||lesson.kind()==null||lesson.serial()<1
                ||lesson.task().ordinal()>stage)
            throw new IllegalArgumentException("invalid first lesson");
        if(seen[(int)actor])return false;
        int task=lesson.task().ordinal();
        seen[(int)actor]=true;observed++;tasks[task]++;
        if(lesson.kind()==Course.Kind.EXAM)exams++;
        else {
            trainingTasks[task]++;
            if(stage==0)foundation++;
            else if(task<stage)review++;
            else frontier++;
        }
        return true;
    }

    /** Only immutable scalars/strings escape; all counts share one consistent snapshot. */
    public synchronized Map<String,Object> status() {
        Map<String,Object> values=new LinkedHashMap<>();
        values.put("startup_coverage_scope","first-issued-lesson-this-process");
        values.put("startup_restored_checkpoint",restored);
        values.put("startup_expected_agents",seen.length);
        values.put("startup_observed_agents",observed);
        values.put("startup_unobserved_agents",seen.length-observed);
        values.put("startup_foundation_agents",foundation);
        values.put("startup_frontier_agents",frontier);
        values.put("startup_review_agents",review);
        values.put("startup_exam_agents",exams);
        values.put("startup_task_population",Arrays.toString(tasks));
        values.put("startup_training_task_population",Arrays.toString(trainingTasks));
        return Map.copyOf(values);
    }
}
