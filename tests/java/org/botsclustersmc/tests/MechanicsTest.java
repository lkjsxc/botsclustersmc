package org.botsclustersmc.tests;

import org.botsclustersmc.core.*;
import org.botsclustersmc.core.Stack;
import java.util.*;

public final class MechanicsTest {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static void left(Pocket p,int slot){p.click(1,slot,Pocket.NONE);}
    static void right(Pocket p,int slot){p.click(2,slot,Pocket.NONE);}
    static final class External implements Pocket.External {
        final Stack[] slots;final boolean furnace;
        External(int n,boolean furnace){slots=new Stack[n];Arrays.fill(slots,Stack.EMPTY);this.furnace=furnace;}
        public int size(){return slots.length;}public Stack get(int i){return slots[i];}public void set(int i,Stack s){slots[i]=s;}
        public boolean accepts(int i,Stack s){return !furnace||i==0||i==1&&(s.item().equals("COAL")||s.empty());}
    }
    public static void main(String[] args){
        Pocket p=new Pocket();p.setStorage(0,new Stack("OAK_LOG",1));p.click(4,0,Pocket.NONE);left(p,0);left(p,36);check(p.get(40,Pocket.NONE).equals(new Stack("OAK_PLANKS",4)),"grid result preview");check(p.count("OAK_PLANKS")==0,"preview does not award output");left(p,40);check(p.count("OAK_PLANKS")==4&&p.count("OAK_LOG")==0,"craft real output and consume input");check(p.crafted.get("OAK_PLANKS")==4,"craft evidence");left(p,0);right(p,0);check(p.cursor().count()==2&&p.storage(0).count()==2,"half pickup");right(p,36);right(p,38);check(p.cursor().empty(),"one-item grid placement");p.click(3,40,Pocket.NONE);check(p.count("STICK")==4&&p.count("OAK_PLANKS")==2,"shift result one craft, conserved materials");
        p.clear();p.setStorage(0,new Stack("BIRCH_PLANKS",4));p.open(Pocket.Menu.INVENTORY);left(p,0);for(int i=36;i<40;i++)right(p,i);left(p,40);check(p.count("CRAFTING_TABLE")==1&&p.countKind(3)==0,"workbench recipe");
        for(String ingredient:List.of("OAK_PLANKS","COBBLESTONE")){
            p.clear();p.open(Pocket.Menu.WORKBENCH);p.setStorage(0,new Stack(ingredient,3));p.setStorage(1,new Stack("STICK",2));left(p,0);right(p,36);right(p,37);right(p,38);left(p,1);right(p,40);right(p,43);p.click(3,45,Pocket.NONE);
            check(p.count(ingredient.equals("COBBLESTONE")?"STONE_PICKAXE":"WOODEN_PICKAXE")==1,"pickaxe recipe");check(p.count(ingredient)==0&&p.count("STICK")==0,"pickaxe ingredients conserved");
        }
        p.clear();p.open(Pocket.Menu.INVENTORY);p.setStorage(0,new Stack("STRIPPED_OAK_LOG",1));left(p,0);left(p,39);left(p,40);check(p.count("OAK_PLANKS")==4,"stripped-log material name");
        p.clear();p.setStorage(0,new Stack("OAK_LOG",8));p.open(Pocket.Menu.CHEST);External chest=new External(27,false);p.click(3,0,chest);check(p.countKind(2)==0&&chest.get(0).count()==8,"shift deposit native-shaped chest");p.click(3,36,chest);check(p.countKind(2)==8&&chest.get(0).empty(),"shift withdrawal");
        p.clear();p.open(Pocket.Menu.FURNACE);External furnace=new External(3,true);p.setStorage(0,new Stack("RAW_IRON",1));p.setStorage(1,new Stack("COAL",1));p.click(1,0,furnace);p.click(1,36,furnace);p.click(1,1,furnace);p.click(1,37,furnace);check(furnace.get(0).item().equals("RAW_IRON")&&furnace.get(1).item().equals("COAL"),"furnace inputs are literal clicks");
        p.setStorage(0,new Stack("STONE",1));p.click(1,0,furnace);p.click(1,38,furnace);check(p.cursor().item().equals("STONE")&&furnace.get(2).empty(),"output slot rejects deposit");p.click(1,0,furnace);furnace.set(2,new Stack("IRON_INGOT",1));p.click(1,38,furnace);check(p.count("IRON_INGOT")==1&&p.extracted.get("IRON_INGOT")==1,"actual output extraction evidence");
        p.clear();for(int i=0;i<36;i++)p.setStorage(i,new Stack("COBBLESTONE",64));check(p.insert(new Stack("OAK_LOG",4)).count()==4,"full inventory does not delete drops");
        for(Task task:Task.values())for(boolean open:new boolean[]{false,true}){
            boolean[] mask=task.mask(open?41:0,open);float[] logits=new float[Schema.OUTPUTS];double[] probabilities=new double[Schema.LOGITS];Distribution.probabilities(logits,mask,probabilities);
            for(int i=0;i<50;i++){Distribution.Choice c=Distribution.choose(probabilities,new RandomSource(i),false);Schema.checkAction(c.actions());if(!open)check(!Schema.slotActive(c.actions()[6]),"closed-menu clicks masked");if(open)check(c.actions()[7]<41,"nonexistent slots masked");}
        }
        for(int task:new int[]{8,9,10,11,13})for(int seed=0;seed<500;seed++){
            Pocket initial=new Pocket();initial.open(task>=11?Pocket.Menu.WORKBENCH:Pocket.Menu.INVENTORY);
            String raw=task==8?"OAK_LOG":task==13?"COBBLESTONE":"OAK_PLANKS";int units=task==8?1:task==9?2:task==10?4:3;
            initial.setStorage(0,new Stack(raw,units));if(task>=11)initial.setStorage(1,new Stack("STICK",2));
            org.botsclustersmc.training.InitialCrafting.prepare(initial,task,(seed%5)/4.0,new RandomSource(seed));
            check(initial.count(raw)==units,"initial assistance conserves raw stock");check(initial.count("STICK")== (task>=11?2:0),"initial assistance conserves sticks");check(initial.crafted.isEmpty(),"initial assistance never awards output");
            if(seed%5==4){check(initial.cursor().empty(),"full difficulty has no cursor assistance");check(initial.storage(0).count()==units,"full difficulty has no grid assistance");}
        }
        System.out.println("PASS mechanics checks="+checks);
    }
}
