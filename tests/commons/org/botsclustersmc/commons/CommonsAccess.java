package org.botsclustersmc.commons;

import java.util.List;
import org.botsclustersmc.plugin.ExternalInventory;
import org.botsclustersmc.plugin.WorldActions;
import org.bukkit.*;
import org.bukkit.entity.*;

/** Read boundaries for the test-only fixed rooms. Never query an entity before checking its owner. */
public final class CommonsAccess {
    private CommonsAccess(){}
    public static void requireOwner(Location origin,List<? extends Entity> actors) {
        if(!WorldActions.owned(origin))throw new IllegalStateException("Room is not loaded and owned");
        // Complete preflight before the caller reads pockets or pauses ANY of the room's actors.
        for(Entity actor:actors)if(!Bukkit.isOwnedByCurrentRegion(actor))throw new IllegalStateException("Foreign room actor");
        for(Entity actor:actors)if(!actor.isValid())throw new IllegalStateException("Room actor retired");
    }
    public static boolean contains(int x,int z,double px,double py,double pz) {
        return px>=x+1&&px<x+15&&pz>=z+1&&pz<z+15&&py>=64&&py<70;
    }
    public static CommonsStock dropped(Location origin) {
        requireOwner(origin,List.of());World world=origin.getWorld();
        int x=(origin.getBlockX()>>4)<<4,z=(origin.getBlockZ()>>4)<<4;
        CommonsStock stock=CommonsStock.EMPTY;
        for(Entity entity:world.getChunkAt(x>>4,z>>4).getEntities())if(entity instanceof Item item) {
            if(!Bukkit.isOwnedByCurrentRegion(item))throw new IllegalStateException("Foreign item in room chunk");
            if(!item.isValid())throw new IllegalStateException("Room item retired");
            Location at=item.getLocation();
            if(at.getWorld()!=world)throw new IllegalStateException("Room item changed world");
            if(!contains(x,z,at.getX(),at.getY(),at.getZ()))continue;
            var value=item.getItemStack();
            if(value==null||!ExternalInventory.supported(value))throw new IllegalStateException("Unrepresentable dropped item");
            stock=stock.plus(CommonsStock.item(value.getType().name(),value.getAmount()));
        }
        return stock;
    }
}
