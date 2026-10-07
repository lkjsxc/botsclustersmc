package org.botsclustersmc.holdout;

import java.util.Arrays;
import org.botsclustersmc.core.*;

/** Observation-only tool handoff and exact one-decision input mass; never predicts contact. */
public final class ToolUseTrace {
    public static final String SCOPE="tool-use-pre-action-not-contact";
    // States: closed-other, closed-pick, open-other, open-pick.
    public record State(int menu,int pickSlots,boolean selectedPick,boolean reservePick,
                        boolean cursorPick,boolean gridPick) {
        public boolean open(){return menu!=0;}
        public int index(){return (open()?2:0)+(selectedPick?1:0);}
    }
    private int transitions,initial=-1,last=-1,closedHotbar,closePickSelections;
    private final int[] visits=new int[4],flow=new int[16],locations=new int[5];
    private final int[] cascadeSelections=new int[4],guiSelections=new int[6];
    private final double[] cascadeMass=new double[4],guiMass=new double[6];
    private double closePickMass;
    private Policy.Workspace workspace;
    private static int integer(float value,int scale,int maximum) {
        double scaled=(double)value*scale;int result=(int)Math.round(scaled);
        if(!Float.isFinite(value)||result<0||result>maximum||Math.abs(scaled-result)>1e-4)
            throw new IllegalArgumentException("Invalid encoded tool state");
        return result;
    }
    private static boolean stack(float kind,float units) {
        int k=integer(kind,20,19),n=integer(units,64,64);
        if((k==0)!=(n==0))throw new IllegalArgumentException("Inconsistent encoded tool stack");
        return k==6||k==7;
    }
    public static State inspect(float[] x) {
        Schema.checkObservation(x);
        for(int task=0;task<18;task++)if(x[16+task]!=(task==12?1:0))
            throw new IllegalArgumentException("Tool task/observation mismatch");
        int menu=integer(x[42],4,4),selected=integer(x[43],8,8),hotbar=0;
        boolean reserve=false,grid=false;
        for(int slot=0;slot<36;slot++)if(stack(x[200+2*slot],x[201+2*slot])) {
            if(slot<9)hotbar|=1<<slot;else reserve=true;
        }
        // Preview outputs and external container stock are NOT carried tools. Closed
        // menus hide the retained grid: absence below means not visible, not destroyed.
        int end=menu==1?40:menu==2?45:36;
        for(int slot=36;slot<end;slot++)if(stack(x[200+2*slot],x[201+2*slot]))grid=true;
        boolean cursor=stack(x[328],x[329]);
        return new State(menu,hotbar,(hotbar&(1<<selected))!=0,reserve,cursor,grid);
    }
    private static void distribution(double[] p) {
        if(p.length!=Schema.DISTRIBUTION)throw new IllegalArgumentException("Distribution shape");
        for(double q:p)if(!Double.isFinite(q)||q<0||q>1)throw new IllegalArgumentException("Invalid probability");
        for(int head=0;head<7;head++)normalized(p,Task.offset(head),Schema.HEADS[head],true);
        for(int op=1;op<=3;op++)normalized(p,Schema.slotOffset(op),64,p[Task.offset(6)+op]>0);
    }
    private static void normalized(double[] p,int off,int size,boolean required) {
        double sum=0;for(int i=0;i<size;i++)sum+=p[off+i];
        // Legal conditional rows are computed even when their parent's softmax
        // underflows to zero. A zero-mass branch may therefore be normalized or empty.
        if(Math.abs(sum-1)>1e-8&&(required||sum!=0))
            throw new IllegalArgumentException("Unnormalized probability block");
    }
    /** Re-evaluation neither samples nor changes the actor's policy, inputs or random stream. */
    public void observe(Policy policy,float[] before,boolean[] mask,float[] after,int[] action,double logProbability) {
        if(workspace==null)workspace=new Policy.Workspace();
        policy.forward(before,mask,workspace);
        double reproduced=Distribution.logProbability(workspace.probabilities,action);
        if(!Double.isFinite(logProbability)||Math.abs(reproduced-logProbability)>1e-6)
            throw new IllegalStateException("Tool diagnostic disagrees with applied frozen likelihood");
        observe(before,after,action,workspace.probabilities);
    }
    public void observe(float[] before,float[] after,int[] action,double[] p) {
        State a=inspect(before),b=inspect(after);Schema.checkAction(action);distribution(p);
        Distribution.logProbability(p,action); // An impossible selected action is not evidence.
        if(transitions>0&&a.index()!=last)throw new IllegalStateException("Discontinuous tool observation stream");
        int op=Task.offset(6);
        // These are the existing task-12 masks, not newly imposed control restrictions.
        if(!a.open()&&(p[op+1]!=0||p[op+2]!=0||p[op+3]!=0||p[op+5]!=0))
            throw new IllegalArgumentException("Closed-menu operation support differs");
        if(a.open()&&p[op+4]!=0)throw new IllegalArgumentException("Open-menu operation support differs");
        if(transitions==0)initial=a.index();last=b.index();transitions++;
        visits[a.index()]++;flow[a.index()*4+b.index()]++;
        if(a.pickSlots()!=0)locations[0]++;if(a.reservePick())locations[1]++;
        if(a.cursorPick())locations[2]++;if(a.gridPick())locations[3]++;
        if(a.pickSlots()==0&&!a.reservePick()&&!a.cursorPick()&&!a.gridPick())locations[4]++;
        if(a.open()) {
            for(int i=0;i<6;i++)guiMass[i]+=p[op+i];guiSelections[action[6]]++;
            if(a.selectedPick()){closePickMass+=p[op+5];if(action[6]==5)closePickSelections++;}
            return;
        }
        if(a.pickSlots()!=0)closedHotbar++;
        double pick=0;for(int slot=0;slot<9;slot++)if((a.pickSlots()&(1<<slot))!=0)pick+=p[Task.offset(5)+slot];
        double mass=p[op];cascadeMass[0]+=mass;
        mass*=p[Task.offset(4)+1];cascadeMass[1]+=mass;
        mass*=pick;cascadeMass[2]+=mass;
        // Quiet INPUTS, not guaranteed stillness, a ray hit, useful damage or a future
        // sequence: zero movement/turn, no jump, dig, and a pick in the selected slot.
        mass*=p[Task.offset(0)]*p[Task.offset(1)+2]*p[Task.offset(2)+2]
            *(p[Task.offset(3)]+p[Task.offset(3)+2]);cascadeMass[3]+=mass;
        if(action[6]!=0)return;cascadeSelections[0]++;
        if(action[4]!=1)return;cascadeSelections[1]++;
        if((a.pickSlots()&(1<<action[5]))==0)return;cascadeSelections[2]++;
        if(action[0]==0&&action[1]==2&&action[2]==2&&action[3]!=1)cascadeSelections[3]++;
    }
    public String json() {
        if(transitions==0)throw new IllegalStateException("No tool observations");
        return "{\"scope\":\""+SCOPE+"\",\"transitions\":"+transitions
            +",\"initial_state\":"+initial+",\"final_state\":"+last
            +",\"state_visits\":"+Arrays.toString(visits)+",\"state_transitions\":"+Arrays.toString(flow)
            +",\"visible_pick_location_states\":"+Arrays.toString(locations)
            +",\"closed_hotbar_pick_states\":"+closedHotbar
            +",\"closed_cascade_probability_sums\":"+Arrays.toString(cascadeMass)
            +",\"closed_cascade_selections\":"+Arrays.toString(cascadeSelections)
            +",\"open_gui_probability_sums\":"+Arrays.toString(guiMass)
            +",\"open_gui_selections\":"+Arrays.toString(guiSelections)
            +",\"open_selected_pick_close_probability_sum\":"+closePickMass
            +",\"open_selected_pick_close_selections\":"+closePickSelections+"}";
    }
}
