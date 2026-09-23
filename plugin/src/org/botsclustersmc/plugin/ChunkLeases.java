package org.botsclustersmc.plugin;

import org.bukkit.*;
import java.util.*;

/** Ref-counted moving chunk tickets; no population-length work in an NPC tick. */
public final class ChunkLeases {
    private record Lease(Location at,int references){}
    private final RuntimePlugin plugin;private final int maximum;
    private final Map<String,Lease> chunks=new HashMap<>();private final Map<Long,String> agents=new HashMap<>();
    public ChunkLeases(RuntimePlugin plugin,int maximum){this.plugin=plugin;this.maximum=maximum;}
    public synchronized int size(){return chunks.size();}
    public void follow(long id,Location at){
        if(!WorldActions.owned(at))throw new IllegalStateException("chunk lease needs a loaded owned chunk");
        String key=at.getWorld().getUID()+":"+(at.getBlockX()>>4)+":"+(at.getBlockZ()>>4);Location release=null;boolean acquire;
        synchronized(this){String old=agents.get(id);if(key.equals(old))return;Lease target=chunks.get(key);if(target==null&&chunks.size()>=maximum)throw new IllegalStateException("loaded-chunk budget exceeded");
            acquire=target==null;chunks.put(key,new Lease(at.clone(),target==null?1:target.references()+1));agents.put(id,key);release=releaseReference(old);
        }
        if(acquire)at.getChunk().addPluginChunkTicket(plugin);releaseLater(release);
    }
    public void release(long id){Location at;synchronized(this){at=releaseReference(agents.remove(id));}releaseLater(at);}
    private Location releaseReference(String key){if(key==null)return null;Lease lease=chunks.get(key);if(lease.references()>1){chunks.put(key,new Lease(lease.at(),lease.references()-1));return null;}chunks.remove(key);return lease.at();}
    private void releaseLater(Location at){
        if(at==null||!plugin.isEnabled())return;String key=at.getWorld().getUID()+":"+(at.getBlockX()>>4)+":"+(at.getBlockZ()>>4);
        Bukkit.getRegionScheduler().run(plugin,at,t->{synchronized(this){if(chunks.containsKey(key))return;if(at.getWorld().isChunkLoaded(at.getBlockX()>>4,at.getBlockZ()>>4))at.getChunk().removePluginChunkTicket(plugin);}});
    }
}
