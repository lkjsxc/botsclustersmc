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
    private static Pocket mining() {Pocket p=new Pocket();p.setStorage(0,new Stack("WOODEN_PICKAXE",1));return p;}
    private static String state(Pocket p) {
        var menu=p.menu();StringBuilder s=new StringBuilder(menu+":"+p.cursor()+":"+p.selected());
        p.open(Pocket.Menu.WORKBENCH);
        for(int i=0;i<46;i++)s.append(':').append(p.get(i,Pocket.NONE));
        p.open(menu);return s+":"+p.crafted+":"+p.extracted;
    }
    private static void oneMissing() {
        String[] labels={"top-left","top-center","top-right","handle-upper","handle-lower"};
        int[] slots={36,37,38,40,43};
        for(Task task:List.of(Task.CRAFT_WOOD_PICK,Task.CRAFT_STONE_PICK))for(int missing=0;missing<5;missing++) {
            var mode=ResetIntervention.parse("pickaxe-missing-"+labels[missing]);
            String material=task==Task.CRAFT_WOOD_PICK?"OAK_PLANKS":"COBBLESTONE";
            String output=task==Task.CRAFT_WOOD_PICK?"WOODEN_PICKAXE":"STONE_PICKAXE";
            Pocket p=raw(task);p.select(8);mode.apply(p,task);
            check(p.menu()==Pocket.Menu.WORKBENCH&&p.selected()==8&&p.cursor().empty());
            check(p.count(material)==3&&p.count("STICK")==2&&p.count(output)==0);
            check(p.crafted.isEmpty()&&p.extracted.isEmpty()&&p.get(45,Pocket.NONE).empty());
            for(int slot=36;slot<45;slot++) {
                String expected=slot==slots[missing]?"AIR":slot<=38?material:slot==40||slot==43?"STICK":"AIR";
                check(expected.equals("AIR")?p.get(slot,Pocket.NONE).empty():p.get(slot,Pocket.NONE).equals(new Stack(expected,1)));
            }
            for(int slot=0;slot<36;slot++) {
                String expected=slot==(missing<3?0:1)?(missing<3?material:"STICK"):"AIR";
                check(expected.equals("AIR")?p.storage(slot).empty():p.storage(slot).equals(new Stack(expected,1)));
            }
            String before=state(p);rejects(()->mode.apply(p,task));check(state(p).equals(before));
            // A real policy must still pick up, place, and collect; these are mechanical tests only.
            p.click(1,missing<3?0:1,Pocket.NONE);check(p.cursor().count()==1);
            p.click(2,slots[missing],Pocket.NONE);check(p.cursor().empty());
            check(p.get(45,Pocket.NONE).item().equals(output));check(p.count(output)==0&&p.crafted.isEmpty());
            p.click(3,45,Pocket.NONE);check(p.count(output)==1&&p.crafted.getOrDefault(output,0L)==1);
            check(p.count(material)==0&&p.count("STICK")==0);
            for(Task other:Task.values())if(other!=Task.CRAFT_WOOD_PICK&&other!=Task.CRAFT_STONE_PICK) {
                Pocket raw=raw(task);String unchanged=state(raw);rejects(()->mode.apply(raw,other));check(state(raw).equals(unchanged));
            }
            for(int invalid=0;invalid<7;invalid++) {
                Pocket raw=raw(task);
                switch(invalid) {
                    case 0 -> raw.setStorage(0,new Stack(material,2));
                    case 1 -> raw.setStorage(1,new Stack("STICK",1));
                    case 2 -> raw.setStorage(35,new Stack("DIRT",1));
                    case 3 -> raw.crafted.put(output,1L);
                    case 4 -> raw.extracted.put("IRON_INGOT",1L);
                    case 5 -> {raw.open(Pocket.Menu.WORKBENCH);raw.click(1,0,Pocket.NONE);raw.close();}
                    case 6 -> {raw.open(Pocket.Menu.WORKBENCH);raw.click(1,0,Pocket.NONE);raw.click(2,36,Pocket.NONE);raw.click(1,0,Pocket.NONE);raw.setStorage(0,new Stack(material,3));raw.close();}
                }
                String unchanged=state(raw);rejects(()->mode.apply(raw,task));check(state(raw).equals(unchanged));
            }
        }
    }
    private static void miningFacing() {
        var mode=ResetIntervention.MINE_TARGET_FACING;Pocket p=mining();String before=state(p);
        mode.requireTask(Task.MINE_COBBLESTONE);mode.apply(p,Task.MINE_COBBLESTONE);
        check(state(p).equals(before));check(!mode.requiresWorkbench());
        for(Task task:Task.values())if(task!=Task.MINE_COBBLESTONE) {
            Pocket fresh=mining();String unchanged=state(fresh);rejects(()->mode.apply(fresh,task));check(state(fresh).equals(unchanged));
        }
        for(int invalid=0;invalid<8;invalid++) {
            Pocket q=mining();
            switch(invalid) {
                case 0 -> q.setStorage(0,Stack.EMPTY);
                case 1 -> q.setStorage(0,new Stack("STONE_PICKAXE",1));
                case 2 -> q.setStorage(1,new Stack("DIRT",1));
                case 3 -> q.select(1);
                case 4 -> q.open(Pocket.Menu.INVENTORY);
                case 5 -> {q.open(Pocket.Menu.INVENTORY);q.click(1,0,Pocket.NONE);q.close();}
                case 6 -> q.crafted.put("WOODEN_PICKAXE",1L);
                case 7 -> q.extracted.put("COBBLESTONE",1L);
            }
            String unchanged=state(q);rejects(()->mode.apply(q,Task.MINE_COBBLESTONE));check(state(q).equals(unchanged));
        }
        var north=ResetIntervention.targetFacing(0,1.6,0,0,.5,3);check(Math.abs(north.yaw())<1e-6&&north.pitch()>0);
        var east=ResetIntervention.targetFacing(0,1.6,0,3,.5,0);check(Math.abs(east.yaw()+90)<1e-6&&east.pitch()>0);
        var west=ResetIntervention.targetFacing(0,1.6,0,-3,.5,0);check(Math.abs(west.yaw()-90)<1e-6&&west.pitch()>0);
        var above=ResetIntervention.targetFacing(1,1,1,1,2,1);check(Math.abs(above.pitch()+90)<1e-6);
        for(double bad:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}) {
            rejects(()->ResetIntervention.targetFacing(bad,0,0,0,0,1));
            rejects(()->ResetIntervention.targetFacing(0,0,0,0,bad,1));
        }
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
        oneMissing();miningFacing();
        System.out.println("PASS reset intervention checks: "+checks);
    }
}
