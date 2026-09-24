package org.botsclustersmc.diagnostic;

import org.botsclustersmc.core.*;
import org.botsclustersmc.plugin.*;
import org.botsclustersmc.training.*;
import org.bukkit.Location;

/** Explicit diagnostic mutations, restored before the full scripted fixture. */
final class StationChecks {
    private StationChecks() {}
    private static Course.Lesson practice(Task task,boolean acquisition) {
        for(long seed=0;seed<1024;seed++) {
            Course.Lesson lesson=new Course.Lesson(1,task,.2,seed,Course.Kind.PRACTICE);
            if(StationPractice.acquisition(lesson)==acquisition)return lesson;
        }
        throw new IllegalStateException("Practice phase unavailable");
    }
    static void verify(Npc npc,TrainingEnvironment.Session session) {
        if(!StationPractice.applies(npc.goal.task()))return;
        Course.Lesson original=session.lesson;Location container=npc.container;
        if(npc.pocket.menu()!=Pocket.Menu.CLOSED)throw new IllegalStateException("Expected fresh closed-menu fixture");
        Frame frame=Sensors.capture(npc);
        Npc.Applied previous=new Npc.Applied(frame,new InferencePool.Result(0,Schema.IDLE.clone(),0,0,0,0,0));
        try {
            session.lesson=practice(npc.goal.task(),true);
            npc.container=new Location(npc.anchor.getWorld(),npc.goal.x(),npc.goal.y(),npc.goal.z());
            npc.pocket.open(Pocket.Menu.INVENTORY);
            if(TrainingEnvironment.success(npc,session,previous,frame))throw new IllegalStateException("Wrong menu completed acquisition");
            npc.pocket.open(StationPractice.station(npc.goal.task()));npc.container.add(1,0,0);
            if(TrainingEnvironment.success(npc,session,previous,frame))throw new IllegalStateException("Wrong station completed acquisition");
            npc.container.subtract(1,0,0);
            if(!TrainingEnvironment.success(npc,session,previous,frame))throw new IllegalStateException("Target station acquisition not recognized");
            session.lesson=practice(npc.goal.task(),false);
            if(TrainingEnvironment.success(npc,session,previous,frame))
                throw new IllegalStateException("Opening a station falsely completed operation practice");
            for(Course.Kind kind:new Course.Kind[]{Course.Kind.PROBE,Course.Kind.EXAM}) {
                session.lesson=new Course.Lesson(1,npc.goal.task(),1,7,kind);
                if(TrainingEnvironment.success(npc,session,previous,frame))
                    throw new IllegalStateException("Opening a station falsely completed a full recipe trial");
            }
        } finally {
            session.lesson=original;npc.container=container;npc.pocket.close();
        }
    }
}
