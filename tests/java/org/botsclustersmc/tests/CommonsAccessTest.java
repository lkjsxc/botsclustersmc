package org.botsclustersmc.tests;

import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;
import org.botsclustersmc.commons.*;
import org.bukkit.*;
import org.bukkit.entity.*;

/** Actual Bukkit API proxies throw on any foreign read; no Minecraft server is started. */
public final class CommonsAccessTest {
    private static boolean roomOwned,loaded;
    private static int checks;
    private static final List<String> calls=new ArrayList<>();
    private static final Map<Entity,Boolean> owners=new IdentityHashMap<>(),valid=new IdentityHashMap<>();
    private static Location itemAt;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message+": "+calls);}
    private static void reject(Runnable operation){try{operation.run();throw new AssertionError("Foreign/corrupt evidence accepted");}catch(IllegalStateException expected){checks++;}}
    @SuppressWarnings("unchecked") private static <T>T proxy(Class<T> type,InvocationHandler handler){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},handler);}
    private static <T extends Entity>T entity(Class<T> type,String name) {
        T e=proxy(type,(p,m,v)->{
            if(m.getName().equals("toString"))return name;
            if(m.getName().equals("hashCode"))return System.identityHashCode(p);
            if(m.getName().equals("equals"))return p==v[0];
            if(!owners.getOrDefault(p,false))throw new AssertionError("Foreign entity read before ownership: "+name+"."+m.getName());
            calls.add(name+"."+m.getName());
            return switch(m.getName()){
                case "isValid"->valid.getOrDefault(p,true);
                case "getLocation"->itemAt;
                case "getItemStack"->null; // Deliberately corrupt input: still must only be read by its owner.
                default->throw new AssertionError("Unexpected entity operation: "+m.getName());
            };
        });owners.put(e,true);return e;
    }
    public static void main(String[] args)throws Exception {
        Entity first=entity(Entity.class,"first"),second=entity(Entity.class,"second");Item item=entity(Item.class,"item");
        Chunk chunk=proxy(Chunk.class,(p,m,v)->{
            if(!roomOwned||!loaded)throw new AssertionError("Unowned/unloaded chunk read");
            if(!m.getName().equals("getEntities"))throw new AssertionError("Unexpected chunk read: "+m.getName());
            calls.add("chunk.entities");return new Entity[]{item};
        });
        World world=proxy(World.class,(p,m,v)->{
            if(!roomOwned)throw new AssertionError("Foreign world read: "+m.getName());
            calls.add("world."+m.getName());
            return switch(m.getName()){
                case "getMinHeight"->-64;case "getMaxHeight"->320;case "isChunkLoaded"->loaded;
                case "getChunkAt"->{if(!loaded)throw new AssertionError("Attempted to load a chunk");yield chunk;}
                default->throw new AssertionError("Unexpected world call: "+m.getName());
            };
        });
        Server server=proxy(Server.class,(p,m,v)->switch(m.getName()){
            case "getLogger"->Logger.getLogger("CommonsAccessTest");
            case "getName","getVersion","getBukkitVersion"->"CommonsAccessTest";
            case "isOwnedByCurrentRegion"->{
                if(v[0] instanceof Entity e){calls.add("owner.entity");yield owners.getOrDefault(e,false);}
                calls.add("owner.room");yield roomOwned;
            }
            default->throw new AssertionError("Unexpected server operation: "+m.getName());
        });
        Field field=Bukkit.class.getDeclaredField("server");field.setAccessible(true);Object previous=field.get(null);field.set(null,server);
        try {
            Location origin=new Location(world,8,65,8);itemAt=new Location(world,8,65,8);
            roomOwned=false;loaded=true;calls.clear();reject(()->CommonsAccess.dropped(origin));
            check(calls.equals(List.of("owner.room")),"Room ownership precedes all world access");
            roomOwned=true;loaded=false;calls.clear();reject(()->CommonsAccess.dropped(origin));
            check(!calls.contains("world.getChunkAt"),"Unavailable chunks are not loaded");
            loaded=true;owners.put(second,false);calls.clear();reject(()->CommonsAccess.requireOwner(origin,List.of(first,second)));
            check(calls.stream().noneMatch(c->c.endsWith("isValid")),"All actor owners checked before reading even the first actor");
            owners.put(second,true);valid.put(second,false);reject(()->CommonsAccess.requireOwner(origin,List.of(first,second)));
            valid.put(second,true);CommonsAccess.requireOwner(origin,List.of(first,second));checks++;
            owners.put(item,false);calls.clear();reject(()->CommonsAccess.dropped(origin));
            check(!calls.contains("item.getLocation")&&!calls.contains("item.getItemStack"),"Foreign item location is not read");
            owners.put(item,true);valid.put(item,false);calls.clear();reject(()->CommonsAccess.dropped(origin));
            check(!calls.contains("item.getLocation"),"Retired item location is not read");
            valid.put(item,true);itemAt=new Location(world,15,65,8);calls.clear();
            check(CommonsAccess.dropped(origin).equals(CommonsStock.EMPTY),"Owned out-of-room item is excluded");
            check(!calls.contains("item.getItemStack"),"Out-of-room contents are not inspected");
            itemAt=new Location(world,8,65,8);calls.clear();reject(()->CommonsAccess.dropped(origin));
            check(calls.indexOf("owner.entity")<calls.indexOf("item.getLocation"),"Ownership precedes location");
            check(calls.indexOf("item.getLocation")<calls.indexOf("item.getItemStack"),"Geometry precedes contents");
            for(int offset:new int[]{-4096,-16,0,16,4096}) {
                check(CommonsAccess.contains(offset,offset,offset+1,64,offset+1),"Inclusive lower room bound");
                check(!CommonsAccess.contains(offset,offset,offset+15,65,offset+8),"Exclusive horizontal boundary");
                check(!CommonsAccess.contains(offset,offset,offset+8,70,offset+8),"Exclusive ceiling");
            }
        }finally{field.set(null,previous);}
        System.out.println("PASS commons ownership and read-order checks="+checks+"; API proxies only");
    }
}
