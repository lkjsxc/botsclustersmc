package org.botsclustersmc.holdout;

import java.util.Arrays;
import org.botsclustersmc.core.*;

/** Read-only, bounded pickaxe diagnostics. Never supplied to a policy or optimizer. */
public final class CraftingTrace {
    private static final int[] SLOTS={36,37,38,40,43};
    public record State(boolean workbench,int correctMask,int wrongCells,int surplusUnits,boolean preview) {
        public int correct(){return Integer.bitCount(correctMask);}
    }
    private int transitions,workbench,maximumCorrect,maximumWrong,maximumSurplus,partialExits;
    private int cursorStates,emptyCursorStates,previewStates,closeActions;
    private final int[] patterns=new int[32],opportunities=new int[5],fills=new int[5],removals=new int[5];
    private final double[] fillMass=new double[5],singleMass=new double[5];
    private double pickupMass,collectMass,closeMass;
    private Policy.Workspace workspace;
    /** Recompute only the frozen distribution; never sample or retain a mutable frame. */
    public void observe(Policy policy,int task,float[] before,boolean[] mask,float[] after,int[] action,double logProbability) {
        if(workspace==null)workspace=new Policy.Workspace();
        policy.forward(before,mask,workspace);
        double reproduced=Distribution.logProbability(workspace.probabilities,action);
        if(!Double.isFinite(logProbability)||Math.abs(reproduced-logProbability)>1e-6)
            throw new IllegalStateException("Diagnostic distribution does not match the applied frozen action");
        observe(task,before,after,action,workspace.probabilities);
    }
    public static boolean applies(int task){return task==11||task==13;}
    private static int expected(int task,int cell){return cell<3?(task==13?8:3):4;}
    private static int integer(float value,int scale,int maximum) {
        double scaled=(double)value*scale;int result=(int)Math.round(scaled);
        if(!Float.isFinite(value)||result<0||result>maximum||Math.abs(scaled-result)>1e-4)
            throw new IllegalArgumentException("Invalid encoded pocket state");
        return result;
    }
    private static int kind(float[] x,int slot){return integer(x[200+2*slot],20,19);}
    private static int units(float[] x,int slot){return integer(x[201+2*slot],64,64);}
    private static int menu(float[] x){return integer(x[42],4,4);}
    private static void observation(float[] x,int task) {
        if(!applies(task)||x.length!=Schema.INPUTS||x[16+task]!=1)
            throw new IllegalArgumentException("Pickaxe task/observation mismatch");
    }
    public static State inspect(float[] x,int task) {
        observation(x,task);
        if(menu(x)!=Pocket.Menu.WORKBENCH.ordinal())return new State(false,0,0,0,false);
        int correct=0,wrong=0,surplus=0;
        for(int slot=36;slot<45;slot++) {
            int n=units(x,slot),k=kind(x,slot),cell=-1;
            if((n==0)!=(k==0))throw new IllegalArgumentException("Inconsistent encoded stack");
            for(int i=0;i<SLOTS.length;i++)if(SLOTS[i]==slot)cell=i;
            if(n==0)continue;
            if(cell>=0&&k==expected(task,cell)){correct|=1<<cell;surplus+=n-1;}else wrong++;
        }
        return new State(true,correct,wrong,surplus,kind(x,45)==(task==13?7:6)&&units(x,45)>0);
    }
    private static double pair(double[] p,int operation,int slot) {
        return p[Task.offset(6)+operation]*p[Schema.slotOffset(operation)+slot];
    }
    private static void distribution(double[] p) {
        if(p.length!=Schema.DISTRIBUTION)throw new IllegalArgumentException("Distribution shape");
        for(double v:p)if(!Double.isFinite(v)||v<0||v>1)throw new IllegalArgumentException("Invalid probability");
        int parent=Task.offset(6);double total=0;
        for(int op=0;op<6;op++)total+=p[parent+op];
        if(Math.abs(total-1)>1e-8)throw new IllegalArgumentException("Operation mass");
        for(int op=1;op<=3;op++)if(p[parent+op]>0) {
            total=0;for(int slot=0;slot<64;slot++)total+=p[Schema.slotOffset(op)+slot];
            if(Math.abs(total-1)>1e-8)throw new IllegalArgumentException("Conditional slot mass");
        }
    }
    /** Pairs pre-action state/probabilities with its actual next state. No world reads or random draws. */
    public void observe(int task,float[] before,float[] after,int[] action,double[] probabilities) {
        State a=inspect(before,task),b=inspect(after,task);Schema.checkAction(action);distribution(probabilities);
        transitions++;
        maximumCorrect=Math.max(maximumCorrect,Math.max(a.correct(),b.correct()));
        maximumWrong=Math.max(maximumWrong,Math.max(a.wrongCells(),b.wrongCells()));
        maximumSurplus=Math.max(maximumSurplus,Math.max(a.surplusUnits(),b.surplusUnits()));
        if(!a.workbench())return;
        workbench++;patterns[a.correctMask()]++;
        closeMass+=probabilities[Task.offset(6)+5];if(action[6]==5)closeActions++;
        if(!b.workbench()&&a.correct()>0&&a.correct()<5)partialExits++;
        if(b.workbench())for(int cell=0;cell<5;cell++) {
            int bit=1<<cell;
            if((a.correctMask()&bit)==0&&(b.correctMask()&bit)!=0)fills[cell]++;
            if((a.correctMask()&bit)!=0&&(b.correctMask()&bit)==0)removals[cell]++;
        }
        int cursorKind=integer(before[328],20,19),cursorUnits=integer(before[329],64,64);
        if((cursorUnits==0)!=(cursorKind==0))throw new IllegalArgumentException("Inconsistent cursor");
        boolean compatible=false;
        for(int cell=0;cell<5;cell++)if(units(before,SLOTS[cell])==0&&cursorUnits>0&&cursorKind==expected(task,cell)) {
            compatible=true;opportunities[cell]++;
            double left=pair(probabilities,1,SLOTS[cell]),right=pair(probabilities,2,SLOTS[cell]);
            fillMass[cell]+=left+right;singleMass[cell]+=right+(cursorUnits==1?left:0);
        }
        if(compatible)cursorStates++;
        if(cursorUnits==0) {
            emptyCursorStates++;
            for(int slot=0;slot<36;slot++)if(units(before,slot)>0) {
                boolean needed=false;
                for(int cell=0;cell<5;cell++)if(units(before,SLOTS[cell])==0&&kind(before,slot)==expected(task,cell))needed=true;
                if(needed)pickupMass+=pair(probabilities,1,slot)+pair(probabilities,2,slot);
            }
        }
        if(a.preview()) {
            previewStates++;
            for(int op=1;op<=3;op++)collectMass+=pair(probabilities,op,45);
        }
    }
    public String json() {
        return "{\"scope\":\"pickaxe-pre-action-observation\",\"transitions\":"+transitions+
            ",\"workbench_states\":"+workbench+",\"correct_mask_states\":"+Arrays.toString(patterns)+
            ",\"max_correct_cells\":"+maximumCorrect+",\"max_wrong_cells\":"+maximumWrong+
            ",\"max_surplus_units_in_correct_cells\":"+maximumSurplus+",\"partial_workbench_exits\":"+partialExits+
            ",\"compatible_cursor_states\":"+cursorStates+",\"compatible_cursor_states_by_cell\":"+Arrays.toString(opportunities)+
            ",\"compatible_fill_probability_sum_by_cell\":"+Arrays.toString(fillMass)+
            ",\"single_unit_fill_probability_sum_by_cell\":"+Arrays.toString(singleMass)+
            ",\"filled_cell_transitions\":"+Arrays.toString(fills)+",\"removed_cell_transitions\":"+Arrays.toString(removals)+
            ",\"empty_cursor_states\":"+emptyCursorStates+",\"needed_stock_pickup_probability_sum\":"+pickupMass+
            ",\"target_preview_states\":"+previewStates+",\"target_collection_probability_sum\":"+collectMass+
            ",\"close_probability_sum\":"+closeMass+",\"chosen_close_actions\":"+closeActions+"}";
    }
}
