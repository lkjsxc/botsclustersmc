package org.botsclustersmc.tests;

import java.util.*;
import org.botsclustersmc.core.Pocket;
import org.botsclustersmc.core.Stack;
import org.botsclustersmc.core.Task;
import org.botsclustersmc.holdout.ResetIntervention;

public final class ResetInterventionTest {
    private static int checks;
    private static void check(boolean value) {checks++;if(!value)throw new AssertionError("reset check "+checks);}
    private static void rejects(Runnable operation) {
        checks++;
        try {operation.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}
        throw new AssertionError("reset accepted invalid input");
    }
    private static Pocket raw(Task task) {
        Pocket p=new Pocket();
        p.setStorage(0,new Stack(task==Task.CRAFT_WOOD_PICK?"OAK_PLANKS":"COBBLESTONE",3));
        p.setStorage(1,new Stack("STICK",2));return p;
    }
    private static String state(Pocket p) {
        var menu=p.menu();StringBuilder s=new StringBuilder(menu+":"+p.cursor()+":"+p.selected());
        p.open(Pocket.Menu.WORKBENCH);
        for(int i=0;i<46;i++)s.append(':').append(p.get(i,Pocket.NONE));
        p.open(menu);return s+":"+p.crafted+":"+p.extracted;
    }
    public static void main(String[] args) {
        rejects(()->ResetIntervention.parse("open"));rejects(()->ResetIntervention.parse(null));
        for(var intervention:ResetIntervention.values())check(ResetIntervention.parse(intervention.label())==intervention);
        for(Task task:Task.values()) {
            Pocket p=raw(Task.CRAFT_WOOD_PICK);String before=state(p);
            ResetIntervention.NONE.requireTask(task);ResetIntervention.NONE.apply(p,task);
            check(state(p).equals(before));
            if(task!=Task.CRAFT_WOOD_PICK&&task!=Task.CRAFT_STONE_PICK) {
                rejects(()->ResetIntervention.WORKBENCH_OPEN.apply(p,task));
                rejects(()->ResetIntervention.PICKAXE_GRID.apply(p,task));check(state(p).equals(before));
            }
        }
        for(Task task:List.of(Task.CRAFT_WOOD_PICK,Task.CRAFT_STONE_PICK)) {
            String material=task==Task.CRAFT_WOOD_PICK?"OAK_PLANKS":"COBBLESTONE";
            String output=task==Task.CRAFT_WOOD_PICK?"WOODEN_PICKAXE":"STONE_PICKAXE";
            for(var mode:List.of(ResetIntervention.WORKBENCH_OPEN,ResetIntervention.PICKAXE_GRID)) {
                Pocket p=raw(task);p.select(8);mode.apply(p,task);
                check(p.menu()==Pocket.Menu.WORKBENCH);check(p.selected()==8);
                check(p.cursor().empty());check(p.count(material)==3);check(p.count("STICK")==2);
                check(p.count(output)==0);check(p.crafted.isEmpty());check(p.extracted.isEmpty());
                for(int i=36;i<45;i++) {
                    String expected=mode==ResetIntervention.WORKBENCH_OPEN?"AIR":
                        i==36||i==37||i==38?material:i==40||i==43?"STICK":"AIR";
                    Stack actual=p.get(i,Pocket.NONE);
                    check(expected.equals("AIR")?actual.empty():actual.equals(new Stack(expected,1)));
                }
                check(p.get(45,Pocket.NONE).item().equals(output)==(mode==ResetIntervention.PICKAXE_GRID));
                if(mode==ResetIntervention.WORKBENCH_OPEN) {
                    check(p.storage(0).equals(new Stack(material,3)));check(p.storage(1).equals(new Stack("STICK",2)));
                } else {
                    for(int i=0;i<36;i++)check(p.storage(i).empty());
                }
                String before=state(p);rejects(()->mode.apply(p,task));check(before.equals(state(p)));
                if(mode==ResetIntervention.PICKAXE_GRID) {
                    // Only an ordinary subsequent output click creates the crafted and owned item.
                    p.click(3,45,Pocket.NONE);
                    check(p.count(output)==1);check(p.crafted.getOrDefault(output,0L)==1);
                }
            }
            for(int kind=0;kind<7;kind++) {
                Pocket p=raw(task);
                switch(kind) {
                    case 0 -> p.setStorage(0,new Stack(material,2));
                    case 1 -> p.setStorage(1,new Stack("STICK",1));
                    case 2 -> p.setStorage(35,new Stack("DIRT",1));
                    case 3 -> p.crafted.put(output,1L);
                    case 4 -> p.extracted.put("IRON_INGOT",1L);
                    case 5 -> {p.open(Pocket.Menu.WORKBENCH);p.click(1,0,Pocket.NONE);p.close();}
                    case 6 -> {p.open(Pocket.Menu.WORKBENCH);p.click(1,0,Pocket.NONE);p.click(2,36,Pocket.NONE);p.click(1,0,Pocket.NONE);p.setStorage(0,new Stack(material,3));p.close();}
                }
                String before=state(p);rejects(()->ResetIntervention.PICKAXE_GRID.apply(p,task));check(state(p).equals(before));
            }
        }
        System.out.println("PASS reset intervention checks: "+checks);
    }
}
