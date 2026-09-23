package org.botsclustersmc.diagnostic;

import org.botsclustersmc.core.Pocket;
import org.botsclustersmc.core.Schema;
import org.botsclustersmc.core.Task;
import org.botsclustersmc.plugin.Sensors;
import org.botsclustersmc.core.Stack;
import org.botsclustersmc.plugin.Npc;
import org.botsclustersmc.plugin.WorldActions;

/** Adversarial mechanical checks, isolated from training and public artifacts. */
final class InputChecks {
    private InputChecks() {}
    static void verify(Npc npc) {
        if(npc.pocket.menu()!=Pocket.Menu.CLOSED)throw new IllegalStateException("Fixture must start with a closed menu");
        var position=npc.entity.getLocation();var velocity=npc.entity.getVelocity();
        int selected=npc.pocket.selected();Stack original=npc.pocket.storage(5);
        npc.pocket.setStorage(5,new Stack("OAK_LOG",8));npc.pocket.select(5);
        try {
            for(int phase=0;phase<3;phase++) {
                int[] action=Schema.IDLE.clone();action[0]=1;action[1]=4;action[2]=4;action[3]=1;
                action[4]=phase==1?2:3;action[5]=phase==1?2:5;action[6]=phase==0?4:phase==2?5:0;
                WorldActions.tick(npc,action,true);
                if(phase<2) {
                    boolean[] mask=Sensors.capture(npc).mask();int slots=Task.offset(7),operations=Task.offset(6);
                    if(!mask[slots+5]||mask[slots+35]||mask[slots+40]||mask[operations+4])
                        throw new IllegalStateException("Real sensor pipeline lost menu affordances");
                }
                var actual=npc.entity.getLocation();var speed=npc.entity.getVelocity();
                if(npc.pocket.storage(5).count()!=8||npc.pocket.selected()!=5)
                    throw new IllegalStateException("Menu focus lost items or changed the selected hotbar");
                if(actual.getYaw()!=position.getYaw()||actual.getPitch()!=position.getPitch()
                        ||speed.getX()!=0||speed.getZ()!=0||speed.getY()!=velocity.getY())
                    throw new IllegalStateException("Menu focus allowed a world motor input");
                if(npc.pocket.menu()!=(phase==2?Pocket.Menu.CLOSED:Pocket.Menu.INVENTORY))
                    throw new IllegalStateException("Menu focus blocked a literal open/close operation");
            }
            if(!WorldActions.owned(position)||WorldActions.owned(position.clone().add(1000000,0,1000000)))
                throw new IllegalStateException("Owned/unavailable location guard failed");
        } finally {
            npc.pocket.close();npc.pocket.setStorage(5,original);npc.pocket.select(selected);
            npc.entity.setRotation(position.getYaw(),position.getPitch());npc.entity.setVelocity(velocity);
        }
    }
}
