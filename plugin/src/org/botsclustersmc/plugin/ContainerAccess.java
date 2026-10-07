package org.botsclustersmc.plugin;

import org.botsclustersmc.core.Pocket;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.Furnace;
import org.bukkit.inventory.Inventory;
import org.bukkit.loot.Lootable;

/** Current owner-thread access, not a reservation or a player-protection plugin. */
public final class ContainerAccess {
    public static final String CONTRACT="owned-unlocked-no-loot-local-v1";
    private ContainerAccess(){}

    private static boolean ordinary(Container container){
        // Inventory getters can expose live storage. Inspect protection first, and
        // never consume/clear a deferred native loot table to manufacture access.
        return container.isPlaced()&&!container.isLocked()
            &&!(container instanceof Lootable loot&&loot.getLootTable()!=null);
    }

    /** No cached block state, combined chest view, forced load, unlocking or loot generation. */
    public static Inventory inventory(Location at,Pocket.Menu menu){
        if(menu!=Pocket.Menu.CHEST&&menu!=Pocket.Menu.FURNACE)return null;
        if(at==null||!WorldActions.owned(at))return null;
        BlockState state=at.getBlock().getState();
        if(menu==Pocket.Menu.CHEST&&state instanceof Chest chest&&ordinary(chest))return chest.getBlockInventory();
        if(menu==Pocket.Menu.FURNACE&&state instanceof Furnace furnace&&ordinary(furnace))return furnace.getInventory();
        return null;
    }

    /** The simplified mining path cannot preserve protected metadata or spill stored items. */
    public static boolean mayBreak(Location at){
        if(at==null||!WorldActions.owned(at))return false;
        BlockState state=at.getBlock().getState();
        return !(state instanceof Container container)
            ||ordinary(container)&&container.getSnapshotInventory().isEmpty();
    }
}
