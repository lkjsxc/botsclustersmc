package org.botsclustersmc.holdout;

import org.botsclustersmc.core.Pocket;
import org.botsclustersmc.core.Stack;
import org.botsclustersmc.core.Task;

/** Test-only initial-state interventions. Never called after the first policy decision. */
public enum ResetIntervention {
    NONE("none"), WORKBENCH_OPEN("workbench-open"), PICKAXE_GRID("pickaxe-grid");
    private final String label;
    ResetIntervention(String label) { this.label=label; }
    public String label() { return label; }
    public static ResetIntervention parse(String value) {
        for(var intervention:values())if(intervention.label.equals(value))return intervention;
        throw new IllegalArgumentException("Unknown reset intervention: "+value);
    }
    public void requireTask(Task task) {
        if(this!=NONE&&task!=Task.CRAFT_WOOD_PICK&&task!=Task.CRAFT_STONE_PICK)
            throw new IllegalArgumentException("Workbench interventions require pickaxe tasks 11 or 13");
    }
    public void apply(Pocket pocket,Task task) {
        if(this==NONE)return;
        requireTask(task);
        String material=task==Task.CRAFT_WOOD_PICK?"OAK_PLANKS":"COBBLESTONE";
        if(pocket.menu()!=Pocket.Menu.CLOSED||!pocket.cursor().empty()
                ||!pocket.crafted.isEmpty()||!pocket.extracted.isEmpty()
                ||!pocket.storage(0).equals(new Stack(material,3))
                ||!pocket.storage(1).equals(new Stack("STICK",2)))
            throw new IllegalStateException("Intervention requires a fresh raw-stock reset");
        for(int slot=2;slot<36;slot++)if(!pocket.storage(slot).empty())
            throw new IllegalStateException("Unexpected reset storage");
        pocket.open(Pocket.Menu.WORKBENCH);
        for(int slot=36;slot<45;slot++)if(!pocket.get(slot,Pocket.NONE).empty()) {
            pocket.close();
            throw new IllegalStateException("Intervention requires an empty initial grid");
        }
        if(this==PICKAXE_GRID) {
            // Transfer the supplied raw units at reset, not a recipe action during gameplay.
            pocket.click(1,0,Pocket.NONE);
            for(int slot:new int[]{36,37,38})pocket.click(2,slot,Pocket.NONE);
            pocket.click(1,1,Pocket.NONE);
            for(int slot:new int[]{40,43})pocket.click(2,slot,Pocket.NONE);
        }
        String output=task==Task.CRAFT_WOOD_PICK?"WOODEN_PICKAXE":"STONE_PICKAXE";
        boolean preview=pocket.get(45,Pocket.NONE).item().equals(output);
        if(!pocket.cursor().empty()||pocket.count(material)!=3||pocket.count("STICK")!=2
                ||pocket.count(output)!=0||!pocket.crafted.isEmpty()
                ||preview!=(this==PICKAXE_GRID))
            throw new IllegalStateException("Invalid reset intervention result");
    }
}
