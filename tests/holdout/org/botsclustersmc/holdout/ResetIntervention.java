package org.botsclustersmc.holdout;

import org.botsclustersmc.core.Pocket;
import org.botsclustersmc.core.Stack;
import org.botsclustersmc.core.Task;

/** Test-only initial-state interventions. Never called after the first policy decision. */
public enum ResetIntervention {
    NONE("none"), WORKBENCH_OPEN("workbench-open"), PICKAXE_GRID("pickaxe-grid"),
    MISSING_TOP_LEFT("pickaxe-missing-top-left",36),
    MISSING_TOP_CENTER("pickaxe-missing-top-center",37),
    MISSING_TOP_RIGHT("pickaxe-missing-top-right",38),
    MISSING_HANDLE_UPPER("pickaxe-missing-handle-upper",40),
    MISSING_HANDLE_LOWER("pickaxe-missing-handle-lower",43);
    private final String label;
    private final int missingSlot;
    ResetIntervention(String label) { this(label,-1); }
    ResetIntervention(String label,int missingSlot) { this.label=label;this.missingSlot=missingSlot; }
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
        if(this!=WORKBENCH_OPEN) {
            // Transfer only supplied raw units. The missing unit stays in its original storage slot.
            // No output, cursor assistance or policy action is supplied.
            furnish(pocket,0,36,37,38);
            furnish(pocket,1,40,43);
        }
        for(int slot=36;slot<45;slot++) {
            String expected=this==WORKBENCH_OPEN||slot==missingSlot?"AIR":
                slot<=38?material:slot==40||slot==43?"STICK":"AIR";
            Stack stack=pocket.get(slot,Pocket.NONE);
            if(!(expected.equals("AIR")?stack.empty():stack.equals(new Stack(expected,1))))
                throw new IllegalStateException("Intervention grid differs from its declared missing cell");
        }
        String output=task==Task.CRAFT_WOOD_PICK?"WOODEN_PICKAXE":"STONE_PICKAXE";
        boolean preview=pocket.get(45,Pocket.NONE).item().equals(output);
        if(!pocket.cursor().empty()||pocket.count(material)!=3||pocket.count("STICK")!=2
                ||pocket.count(output)!=0||!pocket.crafted.isEmpty()
                ||preview!=(this==PICKAXE_GRID))
            throw new IllegalStateException("Invalid reset intervention result");
        int materialLeft=this==WORKBENCH_OPEN?3:missingSlot>=36&&missingSlot<=38?1:0;
        int sticksLeft=this==WORKBENCH_OPEN?2:missingSlot==40||missingSlot==43?1:0;
        if(pocket.storage(0).count()!=materialLeft||pocket.storage(1).count()!=sticksLeft)
            throw new IllegalStateException("Intervention did not preserve remaining raw stock in storage");
    }
    private void furnish(Pocket pocket,int source,int... slots) {
        pocket.click(1,source,Pocket.NONE);
        for(int slot:slots)if(slot!=missingSlot)pocket.click(2,slot,Pocket.NONE);
        if(!pocket.cursor().empty())pocket.click(1,source,Pocket.NONE);
    }
}
