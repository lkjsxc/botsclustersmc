package org.botsclustersmc.tests;

import java.util.*;
import org.botsclustersmc.core.*;
import org.botsclustersmc.core.Stack;

/** Serialized owner-thread interleavings, not a learned-cooperation benchmark. */
public final class SharedInventoryTest {
    private static int checks;
    private static void check(boolean value,String message) {
        checks++;if(!value)throw new AssertionError(message);
    }
    private static final class Chest implements Pocket.External {
        final Stack[] items=new Stack[27];
        final boolean[] blocked=new boolean[27];
        boolean available=true;
        Chest(){Arrays.fill(items,Stack.EMPTY);}
        public int size(){return available?items.length:0;}
        public boolean accessible(int slot){return slot>=0&&slot<size()&&!blocked[slot];}
        public Stack get(int slot) {
            if(!accessible(slot))throw new AssertionError("read inaccessible external slot");
            return items[slot];
        }
        public void set(int slot,Stack value) {
            if(!accessible(slot))throw new AssertionError("write inaccessible external slot");
            items[slot]=value;
        }
        public boolean accepts(int slot,Stack value){return accessible(slot);}
    }
    private static Pocket pocket() {Pocket p=new Pocket();p.open(Pocket.Menu.CHEST);return p;}
    private static Map<String,Integer> stock(Chest chest,Pocket... actors) {
        Map<String,Integer> total=new TreeMap<>();
        for(Stack item:chest.items)add(total,item);
        for(Pocket actor:actors) {
            add(total,actor.cursor());
            for(int slot=0;slot<36;slot++)add(total,actor.storage(slot));
        }
        return total;
    }
    private static void add(Map<String,Integer> total,Stack item) {
        check(item.count()<=item.maximum(),"stack overflow");
        if(!item.empty())total.merge(item.item(),item.count(),Integer::sum);
    }
    private static String actorState(Pocket p) {
        StringBuilder state=new StringBuilder().append(p.menu()).append(p.cursor());
        for(int i=0;i<36;i++)state.append(p.storage(i));
        return state.append(p.crafted).append(p.extracted).toString();
    }
    private static void disappeared() {
        for(Pocket.Menu menu:List.of(Pocket.Menu.CHEST,Pocket.Menu.FURNACE))
            for(int operation=1;operation<=3;operation++) {
                Pocket p=new Pocket();p.open(menu);Chest chest=new Chest();
                chest.items[0]=new Stack("OAK_LOG",8);
                check(p.wouldChange(operation,36,chest),"original action was feasible");
                chest.available=false;
                String before=actorState(p);
                p.click(operation,36,chest);
                check(before.equals(actorState(p)),"missing container mutated pocket");
                check(chest.items[0].count()==8,"missing container mutated external stock");
                check(!p.wouldChange(operation,36,chest),"missing slot remains unavailable");
                p.click(operation,36,Pocket.NONE);
                check(before.equals(actorState(p)),"NONE external mutated pocket");
            }
    }
    private static void blocked() {
        for(int operation=1;operation<=3;operation++) {
            Pocket p=pocket();Chest chest=new Chest();chest.items[0]=new Stack("OAK_LOG",8);
            check(p.wouldChange(operation,36,chest),"pre-block action feasible");
            chest.blocked[0]=true;
            String before=actorState(p);
            check(!p.wouldChange(operation,36,chest),"blocked source offered");
            p.click(operation,36,chest);
            check(before.equals(actorState(p))&&chest.items[0].count()==8,"blocked withdrawal mutated state");
            p.setStorage(0,new Stack("OAK_PLANKS",4));p.click(1,0,chest);
            before=actorState(p);p.click(operation,36,chest);
            check(before.equals(actorState(p)),"blocked deposit/swap mutated state");
        }
        Pocket p=pocket();Chest chest=new Chest();Arrays.fill(chest.blocked,true);
        chest.items[0]=new Stack("OAK_LOG",63);p.setStorage(0,new Stack("OAK_LOG",8));
        check(!p.wouldChange(3,0,chest),"blocked shift destination offered");
        p.click(3,0,chest);
        check(p.storage(0).count()==8&&chest.items[0].count()==63,"blocked shift lost resources");
        chest.blocked[1]=false;p.click(3,0,chest);
        check(p.storage(0).empty()&&chest.items[1].count()==8&&chest.items[0].count()==63,
            "shift must skip blocked destination, not treat it as empty");
    }
    private static void interleavings() {
        Pocket a=pocket(),b=pocket();Chest chest=new Chest();chest.items[0]=new Stack("OAK_LOG",8);
        Map<String,Integer> initial=stock(chest,a,b);
        check(a.wouldChange(3,36,chest)&&b.wouldChange(3,36,chest),"both actors planned withdrawal");
        a.click(3,36,chest);b.click(3,36,chest);
        check(a.count("OAK_LOG")==8&&b.count("OAK_LOG")==0,"stale second withdrawal duplicated stock");
        check(initial.equals(stock(chest,a,b)),"competing withdrawals changed total");
        RandomSource rng=new RandomSource(2026092861L);
        String[] names={"OAK_LOG","OAK_PLANKS","COBBLESTONE","STICK","WOODEN_PICKAXE"};
        for(int seed=0;seed<128;seed++) {
            a=pocket();b=pocket();chest=new Chest();
            for(int slot=0;slot<27;slot++)if(rng.nextInt(3)==0) {
                String name=names[rng.nextInt(names.length)];
                chest.items[slot]=new Stack(name,name.endsWith("_PICKAXE")?1:1+rng.nextInt(64));
            }
            for(Pocket p:List.of(a,b))for(int slot=0;slot<36;slot++)if(rng.nextInt(5)==0) {
                String name=names[rng.nextInt(names.length)];
                p.setStorage(slot,new Stack(name,name.endsWith("_PICKAXE")?1:1+rng.nextInt(64)));
            }
            initial=stock(chest,a,b);
            for(int step=0;step<256;step++) {
                int operation=1+rng.nextInt(3),slot=rng.nextInt(63);
                boolean feasible=a.wouldChange(operation,slot,chest);
                // A second actor may change the inventory after A's observation.
                b.click(1+rng.nextInt(3),rng.nextInt(63),chest);
                a.click(operation,slot,chest);
                check(initial.equals(stock(chest,a,b)),"interleaving lost/duplicated stock: "+seed+":"+step);
                check(a.crafted.isEmpty()&&b.crafted.isEmpty(),"transfer fabricated crafting credit");
                check(a.extracted.isEmpty()&&b.extracted.isEmpty(),"chest transfer fabricated furnace credit");
                if(feasible)check(a.menu()==Pocket.Menu.CHEST,"click changed menu");
            }
        }
    }
    public static void main(String[] args) {
        disappeared();blocked();interleavings();
        System.out.println("PASS shared inventory checks="+checks+" randomized_interleavings=32768");
    }
}
