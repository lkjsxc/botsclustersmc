package org.botsclustersmc.tests;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.botsclustersmc.plugin.WorldActions;

/** Real API interfaces, deliberately throwing on any unowned world access. */
public final class OwnershipTest {
    private static boolean owned,loaded;
    private static int checks;
    private static final List<String> calls=new ArrayList<>();
    private static void check(boolean value,String message) {
        checks++;if(!value)throw new AssertionError(message+": "+calls);
    }
    public static void main(String[] args) throws ReflectiveOperationException {
        World world=(World)Proxy.newProxyInstance(World.class.getClassLoader(),new Class<?>[]{World.class},(proxy,method,values)->{
            if(method.getName().equals("toString"))return "ownership-test-world";
            if(method.getName().equals("hashCode"))return System.identityHashCode(proxy);
            if(method.getName().equals("equals"))return proxy==values[0];
            if(!owned)throw new AssertionError("Foreign world was queried: "+method.getName());
            calls.add(method.getName());
            return switch(method.getName()) {
                case "getMinHeight"->-64;
                case "getMaxHeight"->320;
                case "isChunkLoaded"->loaded;
                default->throw new AssertionError("Unexpected world call: "+method.getName());
            };
        });
        Server server=(Server)Proxy.newProxyInstance(Server.class.getClassLoader(),new Class<?>[]{Server.class},(proxy,method,values)->switch(method.getName()) {
            case "getLogger"->Logger.getLogger("OwnershipTest");
            case "getName","getVersion","getBukkitVersion"->"OwnershipTest";
            case "isOwnedByCurrentRegion"->{calls.add("ownership");yield owned;}
            default->throw new AssertionError("Unexpected server call: "+method.getName());
        });
        // Install the test double without Bukkit's real-server boot/version log.
        var field=Bukkit.class.getDeclaredField("server");field.setAccessible(true);field.set(null,server);
        for(int x:new int[]{-4097,-17,-1,0,15,16,4096}) {
            Location at=new Location(world,x,65,x);
            owned=false;loaded=true;calls.clear();
            check(!WorldActions.owned(at),"foreign locations must be unknown");
            check(calls.equals(List.of("ownership")),"ownership check precedes all world access");
            owned=true;loaded=false;calls.clear();
            check(!WorldActions.owned(at),"unloaded owned chunks must be unknown");
            check(calls.getFirst().equals("ownership"),"ownership first for unloaded chunks");
            loaded=true;calls.clear();check(WorldActions.owned(at),"loaded owned location is readable");
            check(calls.getFirst().equals("ownership"),"ownership first for loaded chunks");
        }
        for(int y:new int[]{-65,320}) {
            calls.clear();check(!WorldActions.owned(new Location(world,0,y,0)),"out-of-height location is unknown");
            check(!calls.contains("isChunkLoaded"),"out-of-height queries stop before touching chunks");
        }
        calls.clear();check(!WorldActions.owned(new Location(null,0,0,0)),"world-less location is unknown");
        check(calls.isEmpty(),"world-less location never queries server");
        System.out.println("PASS ownership checks="+checks);
    }
}
