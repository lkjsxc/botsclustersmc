package org.botsclustersmc.tests;

import java.util.*;
import org.botsclustersmc.commons.*;
import org.botsclustersmc.core.Pocket;
import org.botsclustersmc.core.Stack;

/** Independent finite recipe/drop graph, not an algebraic copy of CommonsLedger or learned actions. */
public final class CommonsLedgerTest {
    private static int checks,states,edges;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static boolean accepts(Runnable operation){try{operation.run();return true;}catch(IllegalArgumentException expected){return false;}}
    private record State(CommonsStock stock,int sticks,int picks){}
    private static List<State> steps(State s) {
        int p=s.stock.planks(),t=s.stock.sticks(),k=s.stock.picks();List<State> next=new ArrayList<>();
        if(p>0)next.add(new State(new CommonsStock(p-1,t,k),s.sticks,s.picks));
        if(t>0)next.add(new State(new CommonsStock(p,t-1,k),s.sticks,s.picks));
        if(k>0)next.add(new State(new CommonsStock(p,t,k-1),s.sticks,s.picks));
        if(p>=2)next.add(new State(new CommonsStock(p-2,t+4,k),s.sticks+4,s.picks));
        if(p>=3&&t>=2)next.add(new State(new CommonsStock(p-3,t-2,k+1),s.sticks,s.picks+1));
        return next;
    }
    private static Set<State> reachable(State start) {
        Set<State> seen=new LinkedHashSet<>();ArrayDeque<State> queue=new ArrayDeque<>();seen.add(start);queue.add(start);
        while(!queue.isEmpty())for(State next:steps(queue.remove()))if(seen.add(next))queue.add(next);
        return seen;
    }
    private static void exhaustive(CommonsStock source) {
        Set<State> all=reachable(new State(source,0,0));states+=all.size();
        for(int p=0;p<=4;p++)for(int t=0;t<=8;t++)for(int k=0;k<=2;k++)
            for(int sticks:new int[]{-1,0,1,2,3,4,5,8})for(int picks:new int[]{-1,0,1,2}) {
                State s=new State(new CommonsStock(p,t,k),sticks,picks);
                boolean ok=accepts(()->CommonsLedger.loss(source,s.stock,s.sticks,s.picks));
                check(ok==all.contains(s),"Endpoint disagrees with recipe/drop graph: "+source+" -> "+s);
            }
        for(State a:all) {
            Set<State> future=reachable(a);
            for(State b:all) {
                CommonsLedger ledger=new CommonsLedger(source);ledger.sample(a.stock,a.sticks,a.picks);
                boolean ok=accepts(()->ledger.sample(b.stock,b.sticks,b.picks));edges++;
                check(ok==future.contains(b),"Transition disagrees with independent graph: "+source+" / "+a+" -> "+b);
                if(!ok)check(accepts(()->ledger.sample(a.stock,a.sticks,a.picks)),"Rejected evidence poisoned the last valid ledger");
            }
        }
    }
    private static void actualRecipe() {
        Pocket p=new Pocket();p.setStorage(0,new Stack("OAK_PLANKS",3));p.setStorage(1,new Stack("STICK",2));
        CommonsLedger ledger=new CommonsLedger(CommonsStock.INITIAL);ledger.sample(CommonsStock.carried(p),0,0);
        p.open(Pocket.Menu.INVENTORY);p.click(1,0,Pocket.NONE);p.click(2,36,Pocket.NONE);p.click(2,38,Pocket.NONE);
        check(p.get(40,Pocket.NONE).equals(new Stack("STICK",4)),"Actual recipe preview");
        check(CommonsStock.carried(p).equals(CommonsStock.INITIAL),"Preview is not retained output");
        p.click(3,40,Pocket.NONE);
        check(p.crafted.getOrDefault("STICK",0L)==4,"Craft counter uses produced items, not clicks");
        check(CommonsStock.carried(p).equals(new CommonsStock(1,6,0)),"Actual stick recipe conserves material types");
        check(ledger.sample(CommonsStock.carried(p),4,0).equals(CommonsStock.EMPTY),"Real wrong recipe is conserved failure");
    }
    private static void immutableResult() {
        int[] inventory=new int[6];long[] ticks=new long[2],decisions=new long[2],gui=new long[12],chest=new long[2];
        CommonsResult r=new CommonsResult(0,40,"horizon",CommonsStock.INITIAL,CommonsStock.EMPTY,CommonsStock.EMPTY,0,0,CommonsStock.EMPTY,inventory,ticks,decisions,gui,chest);
        String before=r.json();inventory[0]=9;ticks[0]=9;decisions[0]=9;gui[0]=9;chest[0]=9;
        check(before.equals(r.json()),"Constructor aliases leaked mutable result inputs");
        r.memberCarried()[0]=8;r.actorTicks()[0]=8;r.decisions()[0]=8;r.guiSelections()[0]=8;r.chestObservations()[0]=8;
        check(before.equals(r.json()),"Accessors leaked mutable result arrays");
        check(r.total().equals(CommonsStock.INITIAL),"Detached result stock");
    }
    public static void main(String[] args) {
        for(CommonsStock source:List.of(CommonsStock.INITIAL,new CommonsStock(3,0,0),new CommonsStock(0,2,0)))exhaustive(source);
        check(!accepts(()->new CommonsLedger(new CommonsStock(0,0,1))),"Finished initial tools refused");
        check(!accepts(()->CommonsLedger.loss(CommonsStock.INITIAL,CommonsStock.EMPTY,Long.MAX_VALUE,0)),"Counter overflow refused");
        check(!accepts(()->CommonsLedger.loss(CommonsStock.INITIAL,CommonsStock.INITIAL,0,1)),"Retained ingredients plus craft credit refused");
        CommonsLedger lost=new CommonsLedger(CommonsStock.INITIAL);lost.sample(new CommonsStock(2,2,0),0,0);
        check(!accepts(()->lost.sample(CommonsStock.INITIAL,0,0)),"Reappearance below initial unit ceiling refused");
        actualRecipe();immutableResult();
        System.out.println("PASS commons material ledger checks="+checks+" reachable_states="+states+" ordered_transitions="+edges+"; finite software oracle only");
    }
}
