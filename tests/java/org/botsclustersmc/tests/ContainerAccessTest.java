package org.botsclustersmc.tests;

import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;
import org.botsclustersmc.core.Pocket;
import org.botsclustersmc.plugin.ContainerAccess;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.inventory.*;
import org.bukkit.loot.LootTable;

/** Real API proxies enforce read order; these checks are not a physical-server result. */
public final class ContainerAccessTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    @SuppressWarnings("unchecked") private static <T> T proxy(Class<T> type,InvocationHandler handler){
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(self,method,args)->switch(method.getName()){
            case "toString"->"fixture-"+type.getSimpleName();
            case "hashCode"->System.identityHashCode(self);
            case "equals"->self==args[0];
            default->handler.invoke(self,method,args);
        });
    }
    private static final class Fixture {
        boolean owned=true,loaded=true,placed=true,locked,loot,empty=true,throwLock;
        final List<String> calls=new ArrayList<>();
        final boolean chest,container;
        final Inventory inventory,snapshot;
        // Bukkit Location keeps a weak world reference. A real server owns worlds;
        // this fixture must keep that ownership alive through its final assertion.
        final World world;
        final Location at;
        Fixture(int kind)throws ReflectiveOperationException{
            chest=kind==0;container=kind!=2;
            inventory=chest?proxy(Inventory.class,this::inventoryCall):proxy(FurnaceInventory.class,this::inventoryCall);
            snapshot=proxy(Inventory.class,(p,m,a)->{
                if(!m.getName().equals("isEmpty"))throw new AssertionError("unexpected snapshot call "+m.getName());
                calls.add("isEmpty");return empty;
            });
            LootTable table=proxy(LootTable.class,(p,m,a)->{throw new AssertionError("loot table must not be evaluated");});
            InvocationHandler stateCall=(p,m,a)->{
                String name=m.getName();calls.add(name);
                return switch(name){
                    case "isPlaced"->placed;
                    case "isLocked"->{if(throwLock)throw new IllegalStateException("fixture API failure");yield locked;}
                    case "getLootTable"->loot?table:null;
                    case "getBlockInventory"->{check(chest,"block inventory called on wrong container");readable();yield inventory;}
                    case "getInventory"->{check(!chest,"combined chest inventory must not be read");readable();yield inventory;}
                    case "getSnapshotInventory"->{readable();yield snapshot;}
                    default->throw new AssertionError("unexpected block state read "+name);
                };
            };
            BlockState state=kind==0?proxy(Chest.class,stateCall):kind==1?proxy(Furnace.class,stateCall):proxy(BlockState.class,stateCall);
            Block block=proxy(Block.class,(p,m,a)->{
                if(!m.getName().equals("getState"))throw new AssertionError("unexpected block read "+m.getName());
                check(owned&&loaded,"unavailable block state was read");calls.add("getState");return state;
            });
            world=proxy(World.class,(p,m,a)->{
                check(owned,"foreign world read "+m.getName());calls.add(m.getName());
                return switch(m.getName()){
                    case "getMinHeight"->-64;
                    case "getMaxHeight"->320;
                    case "isChunkLoaded"->{check((int)a[0]==1&&(int)a[1]==-2,"wrong chunk coordinates");yield loaded;}
                    case "getBlockAt"->{check(loaded,"unloaded block was queried");yield block;}
                    default->throw new AssertionError("unexpected world read "+m.getName());
                };
            });
            Server server=proxy(Server.class,(p,m,a)->switch(m.getName()){
                case "getLogger"->Logger.getLogger("ContainerAccessTest");
                case "getName","getVersion","getBukkitVersion"->"ContainerAccessTest";
                case "isOwnedByCurrentRegion"->{calls.add("owned");yield owned;}
                default->throw new AssertionError("unexpected server read "+m.getName());
            });
            var field=Bukkit.class.getDeclaredField("server");field.setAccessible(true);field.set(null,server);
            at=new Location(world,31,65,-17);
        }
        private Object inventoryCall(Object p,Method m,Object[] a){throw new AssertionError("opening must not inspect/change slots: "+m.getName());}
        private void readable(){
            check(owned&&loaded&&placed&&!locked&&!(chest&&loot),"protected inventory getter called: "+calls);
            check(calls.indexOf("owned")<calls.indexOf("getState"),"ownership must precede state");
            check(calls.indexOf("isLocked")>=0&&(!chest||calls.indexOf("getLootTable")>=0),"inventory read precedes protection");
        }
        boolean ordinary(){return owned&&loaded&&placed&&!locked&&!(chest&&loot);}
        void order(){
            if(!owned)check(calls.equals(List.of("owned")),"foreign access must stop at ownership: "+calls);
            if(!loaded)check(!calls.contains("getState"),"unloaded state accessed: "+calls);
            if(!placed)check(!calls.contains("isLocked"),"unplaced state accessed beyond placement: "+calls);
            if(locked)check(!calls.contains("getLootTable"),"locked storage need not inspect loot: "+calls);
        }
    }
    public static void main(String[] args)throws Exception{
        for(int kind=0;kind<3;kind++)for(int flags=0;flags<64;flags++){
            Fixture f=new Fixture(kind);f.owned=(flags&1)!=0;f.loaded=(flags&2)!=0;f.placed=(flags&4)!=0;
            f.locked=(flags&8)!=0;f.loot=(flags&16)!=0;f.empty=(flags&32)!=0;
            for(Pocket.Menu menu:Pocket.Menu.values()){
                f.calls.clear();Inventory got=ContainerAccess.inventory(f.at,menu);
                boolean matching=kind==0&&menu==Pocket.Menu.CHEST||kind==1&&menu==Pocket.Menu.FURNACE;
                boolean expected=matching&&f.ordinary();
                check((got==f.inventory)==expected&&((got==null)==!expected),"inventory availability kind="+kind+" flags="+flags+" menu="+menu);
                check(!f.calls.contains("getSnapshotInventory")&&!f.calls.contains("isEmpty"),"opening read contents");
                if(menu==Pocket.Menu.CHEST||menu==Pocket.Menu.FURNACE)f.order();
                else check(f.calls.isEmpty(),"non-container menu queried world");
            }
            f.calls.clear();boolean mayBreak=ContainerAccess.mayBreak(f.at);
            check(mayBreak==(f.owned&&f.loaded&&(!f.container||f.ordinary()&&f.empty)),"break availability kind="+kind+" flags="+flags);
            check(!f.calls.contains("getInventory")&&!f.calls.contains("getBlockInventory"),"mining inspected live inventory");f.order();java.lang.ref.Reference.reachabilityFence(f.world);
        }
        Fixture f=new Fixture(0);f.calls.clear();
        check(ContainerAccess.inventory(null,Pocket.Menu.CHEST)==null&&!ContainerAccess.mayBreak(null)&&f.calls.isEmpty(),"null location touched server");
        for(int y:new int[]{-65,320}){
            Location at=f.at.clone();at.setY(y);f.calls.clear();
            check(ContainerAccess.inventory(at,Pocket.Menu.CHEST)==null&&!ContainerAccess.mayBreak(at),"height bounds ignored");
            check(!f.calls.contains("isChunkLoaded")&&!f.calls.contains("getState"),"invalid height queried chunks");
        }
        for(int repetition=0;repetition<8;repetition++){
            System.gc();check(ContainerAccess.inventory(f.at,Pocket.Menu.CHEST)==f.inventory,"live fixture world lost across GC");
            java.lang.ref.Reference.reachabilityFence(f.world);
        }
        for(int repetition=0;repetition<100;repetition++){
            f.locked=false;check(ContainerAccess.inventory(f.at,Pocket.Menu.CHEST)==f.inventory,"ordinary access unavailable");
            f.locked=true;check(ContainerAccess.inventory(f.at,Pocket.Menu.CHEST)==null&&!ContainerAccess.mayBreak(f.at),"stale protection was cached");
        }
        f.locked=false;f.throwLock=true;
        for(boolean mining:new boolean[]{false,true})try{
            if(mining)ContainerAccess.mayBreak(f.at);else ContainerAccess.inventory(f.at,Pocket.Menu.CHEST);
            throw new AssertionError("unexpected API exception was swallowed");
        }catch(IllegalStateException expected){check(expected.getMessage().equals("fixture API failure"),"wrong exception");}
        java.lang.ref.Reference.reachabilityFence(f.world);
        System.out.println("PASS container access checks="+checks+" state_combinations=192 stale_lock_cycles=100 retained_world_gc_cycles=8");
    }
}
