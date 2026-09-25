package org.botsclustersmc.diagnostic;

import org.botsclustersmc.core.*;
import org.botsclustersmc.plugin.*;
import org.botsclustersmc.training.*;
import org.bukkit.Location;

/** Explicit diagnostic mutations, restored before the full scripted fixture. */
final class StationChecks {
    private StationChecks() {}
    static void verify(Npc npc,TrainingEnvironment.Session session) {
        if(!StationPractice.applies(npc.goal.task()))return;
        Course.Lesson original=session.lesson;Location container=npc.container;
        if(npc.pocket.menu()!=Pocket.Menu.CLOSED)throw new IllegalStateException("Expected fresh closed-menu fixture");
        Frame frame=Sensors.capture(npc);
        Npc.Applied previous=new Npc.Applied(frame,new InferencePool.Result(0,Schema.IDLE.clone(),0,0,0,0,0));
        try {
            npc.container=new Location(npc.anchor.getWorld(),npc.goal.x(),npc.goal.y(),npc.goal.z());
            if(TrainingEnvironment.stationOpen(npc))throw new IllegalStateException("Closed station marked open");
            npc.pocket.open(Pocket.Menu.INVENTORY);
            if(TrainingEnvironment.stationOpen(npc))throw new IllegalStateException("Wrong menu marked target station open");
            npc.pocket.open(StationPractice.station(npc.goal.task()));npc.container.add(1,0,0);
            if(TrainingEnvironment.stationOpen(npc))throw new IllegalStateException("Wrong station marked target station open");
            npc.container.subtract(1,0,0);
            if(!TrainingEnvironment.stationOpen(npc))throw new IllegalStateException("Target station opening not recognized");
            // Hold the physical state and observed Frame fixed. An unobserved lesson
            // seed/kind must never turn this non-completion into a successful terminal.
            int cases=0;double potential=TrainingEnvironment.potential(npc,session);
            for(Course.Kind kind:Course.Kind.values())for(double difficulty:new double[]{.2,.54,.99,1})for(long seed=0;seed<64;seed++) {
                session.lesson=new Course.Lesson(1,npc.goal.task(),difficulty,seed,kind);
                if(TrainingEnvironment.success(npc,session,previous,frame))
                    throw new IllegalStateException("Station goal alias: opening alone completed "+session.lesson);
                if(TrainingEnvironment.potential(npc,session)!=potential)
                    throw new IllegalStateException("Unobserved lesson changed station potential");
                cases++;
            }
            System.out.println("STATION GOAL PASS task="+npc.goal.task().ordinal()+" cases="+cases);
        } finally {
            session.lesson=original;npc.container=container;npc.pocket.close();
        }
    }
}
