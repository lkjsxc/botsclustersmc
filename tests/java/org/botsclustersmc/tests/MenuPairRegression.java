package org.botsclustersmc.tests;

import org.botsclustersmc.core.*;
import org.botsclustersmc.core.Stack;

/** Same regression runs against the old union and the conditional implementation. */
public final class MenuPairRegression {
    public static double ineffectiveMass() {
        Pocket pocket=new Pocket();
        pocket.open(Pocket.Menu.INVENTORY);
        pocket.setStorage(0,new Stack("OAK_PLANKS",4));
        pocket.click(1,0,Pocket.NONE);
        pocket.click(2,36,Pocket.NONE);
        boolean[] mask=Task.CRAFT_WORKBENCH.mask(pocket.slots(),true);
        MenuInputs.restrict(pocket,Pocket.NONE,mask);
        double[] probabilities=new Policy.Workspace().probabilities;
        Distribution.probabilities(new float[Schema.OUTPUTS],mask,probabilities);
        double ineffective=0;
        for(int operation=1;operation<=3;operation++)for(int slot=0;slot<64;slot++) {
            int[] action=Schema.IDLE.clone();action[6]=operation;action[7]=slot;
            double probability;
            try {probability=Math.exp(Distribution.logProbability(probabilities,action));}
            catch(IllegalArgumentException impossible) {continue;}
            if(!pocket.wouldChange(operation,slot,Pocket.NONE))ineffective+=probability;
        }
        return ineffective;
    }
    public static void main(String[] args) {
        double mass=ineffectiveMass();
        System.out.println("Ineffective click-pair probability mass="+mass);
        if(mass>1e-12)throw new AssertionError("A selected click/slot pair can be effect-free");
        System.out.println("PASS operation-conditioned mechanical support");
    }
}
