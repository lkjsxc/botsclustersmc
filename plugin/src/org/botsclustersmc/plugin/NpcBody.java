package org.botsclustersmc.plugin;

import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;

/** A neutral citizen body, with no undead sunlight or hostile-mob classification. */
public final class NpcBody {
    private NpcBody() {}
    public static Villager spawn(RuntimePlugin plugin,Location at,long id) {
        return at.getWorld().spawn(at,Villager.class,CreatureSpawnEvent.SpawnReason.CUSTOM,false,body->{
            body.setAdult();body.setAgeLock(true);body.setProfession(Villager.Profession.NITWIT);
            body.setPersistent(false);body.setRemoveWhenFarAway(false);
            body.setSilent(true);body.setInvulnerable(plugin.training());
            body.setCanPickupItems(false);body.setCollidable(false);
            body.setAI(true);body.setAware(false);Bukkit.getMobGoals().removeAllGoals(body);
            body.setTarget(null);body.setFireTicks(0);
            body.customName(Component.text("bcmc"+id));body.setCustomNameVisible(false);
            body.getPersistentDataContainer().set(plugin.provenance,PersistentDataType.STRING,plugin.run);
        });
    }
}
