package org.botsclustersmc.tests;

import java.util.Arrays;
import org.botsclustersmc.core.*;
import org.botsclustersmc.core.Stack;

/** Differential tests use actual literal clicks, not a second preferred recipe. */
public final class MenuInputsTest {
    private static int checks;
    private static void check(boolean ok,String message) {
        checks++;if(!ok)throw new AssertionError(message);
    }
    private static final class External implements Pocket.External {
        private final Stack[] contents;
        private final boolean furnace;
        External(int size,boolean furnace) {
            contents=new Stack[size];Arrays.fill(contents,Stack.EMPTY);this.furnace=furnace;
        }
        public int size(){return contents.length;}
        public Stack get(int i){return contents[i];}
        public void set(int i,Stack s){contents[i]=s;}
        public boolean accepts(int i,Stack s) {
            return !furnace||i==0||i==1&&(s.empty()||s.item().equals("COAL"));
        }
    }
    private static String state(Pocket pocket,Pocket.External external) {
        StringBuilder out=new StringBuilder().append(pocket.menu()).append(pocket.selected()).append(pocket.cursor());
        for(int i=0;i<36;i++)out.append(pocket.storage(i));
        for(int i=36;i<pocket.slots();i++)out.append(pocket.get(i,external));
        for(int i=0;i<external.size();i++)out.append(external.get(i));
        return out.append(pocket.crafted).append(pocket.extracted).toString();
    }
    private static void click(Pocket pocket,int op,int slot,Pocket.External external) {
        String before=state(pocket,external);
        boolean predicted=pocket.wouldChange(op,slot,external);
        check(state(pocket,external).equals(before),"affordance query changed state");
        pocket.click(op,slot,external);
        check(predicted!=state(pocket,external).equals(before),"effect prediction differs: "+op+":"+slot);
    }
    private static boolean[] mask(Pocket pocket,Pocket.External external,Task task) {
        boolean[] mask=task.mask(pocket.slots(),pocket.menu()!=Pocket.Menu.CLOSED);
        MenuInputs.restrict(pocket,external,mask);return mask;
    }
    private static void maskChecks(Pocket pocket,Pocket.External external) {
        int operation=Task.offset(6);
        String before=state(pocket,external);
        boolean[] result=mask(pocket,external,Task.CRAFT_WORKBENCH);
        check(state(pocket,external).equals(before),"mask construction mutated the inventory");
        if(pocket.menu()==Pocket.Menu.CLOSED) {
            check(Arrays.equals(result,Task.CRAFT_WORKBENCH.mask(0,false)),"closed input meanings unchanged");return;
        }
        check(result[operation]&&result[operation+5]&&!result[operation+4],"wait/close allowed; duplicate open excluded");
        for(int op=1;op<=3;op++) {
            boolean possible=false;
            for(int i=0;i<64;i++) {
                boolean useful=pocket.wouldChange(op,i,external);
                check(result[Schema.slotOffset(op)+i]==useful,"exact operation-conditioned mechanical support");
                possible|=useful;
            }
            check(result[operation+op]==possible,"empty branches disable only their parent");
        }
        for(Task task:Task.values()) {
            boolean[] other=mask(pocket,external,task);
            for(int i=operation;i<Schema.DISTRIBUTION;i++)check(other[i]==result[i],"menu availability must be goal-independent");
        }
        double[] probabilities=new double[Schema.DISTRIBUTION];
        Distribution.probabilities(new float[Schema.OUTPUTS],result,probabilities);
        for(int i=0;i<8;i++) {
            Distribution.Choice choice=Distribution.choose(probabilities,new RandomSource(checks+i),i==0);
            check(Double.isFinite(choice.logProbability()),"conditional likelihood stays finite");
            Schema.checkAction(choice.actions());
            int op=choice.actions()[6],slot=choice.actions()[7];
            check(!Schema.slotActive(op)||pocket.wouldChange(op,slot,external),"sampled click must have a mechanical effect");
        }
    }
    private static void preexistingRestrictions() {
        Pocket pocket=new Pocket();pocket.open(Pocket.Menu.INVENTORY);
        pocket.setStorage(0,new Stack("OAK_PLANKS",4));
        pocket.click(1,0,Pocket.NONE);pocket.click(2,36,Pocket.NONE);
        boolean[] mask=Task.CRAFT_WORKBENCH.mask(pocket.slots(),true);
        Task.only(mask,7,0,36,37);mask[Task.offset(6)+1]=false;
        mask[Schema.slotOffset(2)+36]=false;
        boolean[] before=mask.clone();
        MenuInputs.restrict(pocket,Pocket.NONE,mask);
        for(int i=0;i<mask.length;i++)check(!mask[i]||before[i],"restriction cannot enable a forbidden control");
        for(int op=1;op<=3;op++)for(int slot=0;slot<64;slot++) {
            boolean expected=before[Task.offset(6)+op]&&before[Schema.slotOffset(op)+slot]
                &&pocket.wouldChange(op,slot,Pocket.NONE);
            check(mask[Schema.slotOffset(op)+slot]==expected,"input restrictions survive mechanical filtering");
        }
        boolean[] once=mask.clone();MenuInputs.restrict(pocket,Pocket.NONE,mask);
        check(Arrays.equals(once,mask),"mechanical filtering is idempotent");
    }
    public static void main(String[] args) {
        preexistingRestrictions();
        Pocket p=new Pocket();Pocket.External none=Pocket.NONE;
        p.open(Pocket.Menu.INVENTORY);maskChecks(p,none);
        p.setStorage(17,new Stack("OAK_LOG",1));
        boolean[] m=mask(p,none,Task.CRAFT_STICKS);
        check(m[Task.offset(7)+17]&&!m[Task.offset(7)+0],"any occupied slot, not a fixed ingredient position");
        click(p,1,17,none);m=mask(p,none,Task.CRAFT_STICKS);
        for(int i=0;i<40;i++)check(m[Task.offset(7)+i],"carried item may be placed in EVERY accepting slot");
        click(p,1,39,none);maskChecks(p,none);
        check(mask(p,none,Task.CRAFT_STICKS)[Task.offset(7)+40],"even an output irrelevant to the goal remains available");
        click(p,1,40,none);click(p,1,17,none);
        p.clear();p.open(Pocket.Menu.INVENTORY);p.setStorage(0,new Stack("OAK_LOG",1));
        click(p,1,0,none);click(p,1,36,none);
        for(int i=0;i<36;i++)p.setStorage(i,new Stack("COBBLESTONE",64));
        check(!p.wouldChange(3,40,none)&&p.wouldChange(1,40,none),"full inventory forbids shift craft but allows cursor extraction");
        click(p,3,40,none);click(p,1,0,none);
        check(!p.wouldChange(1,40,none),"incompatible cursor cannot extract output");
        maskChecks(p,none);
        RandomSource rng=new RandomSource(924603);
        String[] items={"OAK_LOG","OAK_PLANKS","BIRCH_PLANKS","STICK","COBBLESTONE","COAL","RAW_IRON","IRON_INGOT"};
        for(Pocket.Menu menu:Pocket.Menu.values())for(int seed=0;seed<120;seed++) {
            p=new Pocket();External external=new External(menu==Pocket.Menu.CHEST?27:menu==Pocket.Menu.FURNACE?3:0,menu==Pocket.Menu.FURNACE);
            p.open(menu);
            for(int i=0;i<36;i++)if(rng.nextInt(3)==0)p.setStorage(i,new Stack(items[rng.nextInt(items.length)],1+rng.nextInt(64)));
            for(int i=0;i<external.size();i++)if(rng.nextInt(3)==0)external.set(i,new Stack(items[rng.nextInt(items.length)],1+rng.nextInt(64)));
            for(int n=0;n<60;n++) {
                int op=rng.nextInt(6),slot=rng.nextInt(68)-2;
                click(p,op,slot,external);
                if(n%20==0)maskChecks(p,external);
            }
        }
        System.out.println("PASS goal-independent menu affordance checks="+checks);
    }
}
