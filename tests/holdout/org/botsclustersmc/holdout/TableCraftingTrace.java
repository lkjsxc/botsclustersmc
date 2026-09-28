package org.botsclustersmc.holdout;

import java.util.Arrays;
import org.botsclustersmc.core.*;

/** Read-only task-10 diagnostics. A visible sticks recipe is not a workbench. */
public final class TableCraftingTrace {
    public record State(boolean inventory,int correctMask,int wrongCells,int surplusUnits,
                        int planks,int sticks,int tables,int previewKind,int previewUnits) {
        public int correct(){return Integer.bitCount(correctMask);}
    }
    private int transitions,inventory,maximumCorrect,maximumSurplus,partialCloses,lowPlanks;
    private int targetPreviews,otherPreviews,targetClicks,otherClicks,stickGains,stickUnits,tableUnits;
    private final int[] patterns=new int[16],opportunities=new int[5],filled=new int[4],removed=new int[4];
    private final double[] fillMass=new double[5],singleMass=new double[5];
    private double targetMass,otherMass;
    private Policy.Workspace workspace;
    private static int integer(float value,int scale,int maximum) {
        double scaled=(double)value*scale;int result=(int)Math.round(scaled);
        if(!Float.isFinite(value)||result<0||result>maximum||Math.abs(scaled-result)>1e-4)
            throw new IllegalArgumentException("Invalid encoded table state");
        return result;
    }
    private static int kind(float[] x,int slot){return integer(x[200+2*slot],20,19);}
    private static int units(float[] x,int slot){return integer(x[201+2*slot],64,64);}
    private static void stack(int kind,int units) {
        if((kind==0)!=(units==0))throw new IllegalArgumentException("Inconsistent encoded stack");
    }
    public static State inspect(float[] x) {
        Schema.checkObservation(x);
        for(int task=0;task<18;task++)if(x[16+task]!=(task==10?1:0))
            throw new IllegalArgumentException("Table task/observation mismatch");
        int menu=integer(x[42],4,4);
        // Closed menus hide the retained grid. Never call that missing material.
        if(menu!=Pocket.Menu.INVENTORY.ordinal())return new State(false,0,0,0,0,0,0,0,0);
        int correct=0,wrong=0,surplus=0;int[] total=new int[20];
        for(int slot=0;slot<40;slot++) {
            int k=kind(x,slot),n=units(x,slot);stack(k,n);total[k]+=n;
            if(slot>=36&&n>0) {
                if(k==3){correct|=1<<(slot-36);surplus+=n-1;}else wrong++;
            }
        }
        int cursor=integer(x[328],20,19),count=integer(x[329],64,64);stack(cursor,count);total[cursor]+=count;
        int preview=kind(x,40),previewUnits=units(x,40);stack(preview,previewUnits);
        return new State(true,correct,wrong,surplus,total[3],total[4],total[5],preview,previewUnits);
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
    /** Recompute the applied frozen distribution without sampling or changing it. */
    public void observe(Policy policy,float[] before,boolean[] mask,float[] after,int[] action,double logProbability) {
        if(workspace==null)workspace=new Policy.Workspace();
        policy.forward(before,mask,workspace);
        double reproduced=Distribution.logProbability(workspace.probabilities,action);
        if(!Double.isFinite(logProbability)||!Double.isFinite(reproduced)||Math.abs(reproduced-logProbability)>1e-6)
            throw new IllegalStateException("Diagnostic distribution does not match the applied frozen action");
        observe(before,after,action,workspace.probabilities);
    }
    public void observe(float[] before,float[] after,int[] action,double[] probabilities) {
        State a=inspect(before),b=inspect(after);Schema.checkAction(action);distribution(probabilities);
        transitions++;
        maximumCorrect=Math.max(maximumCorrect,Math.max(a.correct(),b.correct()));
        maximumSurplus=Math.max(maximumSurplus,Math.max(a.surplusUnits(),b.surplusUnits()));
        if(!a.inventory())return;
        inventory++;patterns[a.correctMask()]++;
        if(a.planks()<4&&a.tables()==0)lowPlanks++;
        if(!b.inventory()&&a.correct()>0&&a.correct()<4)partialCloses++;
        if(b.inventory()) {
            // Observed gains, not a claim that a requested click succeeded.
            int gainedSticks=Math.max(0,b.sticks()-a.sticks());
            if(gainedSticks>0){stickGains++;stickUnits+=gainedSticks;}
            tableUnits+=Math.max(0,b.tables()-a.tables());
            // Output consumption is separate from dismantling a partial grid.
            if(gainedSticks==0&&b.tables()<=a.tables())for(int cell=0;cell<4;cell++) {
                int bit=1<<cell;
                if((a.correctMask()&bit)==0&&(b.correctMask()&bit)!=0)filled[cell]++;
                if((a.correctMask()&bit)!=0&&(b.correctMask()&bit)==0)removed[cell]++;
            }
        }
        int cursor=integer(before[328],20,19),count=integer(before[329],64,64);
        boolean compatible=false;double fill=0,single=0;
        if(cursor==3&&count>0)for(int cell=0;cell<4;cell++)if(units(before,36+cell)==0) {
            compatible=true;
            double left=pair(probabilities,1,36+cell),right=pair(probabilities,2,36+cell);
            fill+=left+right;single+=right+(count==1?left:0);
        }
        if(compatible){opportunities[a.correct()]++;fillMass[a.correct()]+=fill;singleMass[a.correct()]+=single;}
        if(a.previewUnits()>0) {
            double mass=0;for(int op=1;op<=3;op++)mass+=pair(probabilities,op,40);
            boolean click=action[6]>=1&&action[6]<=3&&action[7]==40;
            if(a.previewKind()==5){targetPreviews++;targetMass+=mass;if(click)targetClicks++;}
            else {otherPreviews++;otherMass+=mass;if(click)otherClicks++;}
        }
    }
    public String json() {
        return "{\"scope\":\"table-inventory-pre-action\",\"transitions\":"+transitions+
            ",\"inventory_states\":"+inventory+",\"correct_mask_states\":"+Arrays.toString(patterns)+
            ",\"max_correct_cells\":"+maximumCorrect+",\"max_surplus_units\":"+maximumSurplus+
            ",\"partial_inventory_exits\":"+partialCloses+",\"carried_planks_below_four_without_table_states\":"+lowPlanks+
            ",\"compatible_cursor_states_by_correct_cells\":"+Arrays.toString(opportunities)+
            ",\"fill_probability_sum_by_correct_cells\":"+Arrays.toString(fillMass)+
            ",\"single_unit_fill_probability_sum_by_correct_cells\":"+Arrays.toString(singleMass)+
            ",\"filled_cell_transitions\":"+Arrays.toString(filled)+",\"removed_cell_transitions\":"+Arrays.toString(removed)+
            ",\"target_preview_states\":"+targetPreviews+",\"other_preview_states\":"+otherPreviews+
            ",\"target_collection_probability_sum\":"+targetMass+",\"other_collection_probability_sum\":"+otherMass+
            ",\"chosen_target_result_clicks\":"+targetClicks+",\"chosen_other_result_clicks\":"+otherClicks+
            ",\"observed_stick_gain_transitions\":"+stickGains+",\"observed_stick_units_gained\":"+stickUnits+
            ",\"observed_table_units_gained\":"+tableUnits+"}";
    }
}
