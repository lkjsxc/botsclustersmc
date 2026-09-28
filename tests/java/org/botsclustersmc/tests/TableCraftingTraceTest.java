package org.botsclustersmc.tests;

import com.google.gson.*;
import java.util.Arrays;
import org.botsclustersmc.core.*;
import org.botsclustersmc.core.Stack;
import org.botsclustersmc.holdout.TableCraftingTrace;

/** Independent pocket fixtures; these are measurement tests, not learned skill. */
public final class TableCraftingTraceTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void near(double a,double b){check(Math.abs(a-b)<1e-9,"expected "+b+", got "+a);}
    private static void rejected(Runnable action){boolean rejected=false;try{action.run();}catch(IllegalArgumentException|IllegalStateException e){rejected=true;}check(rejected,"malformed input must fail");}
    private static JsonObject report(TableCraftingTrace t){return JsonParser.parseString(t.json()).getAsJsonObject();}
    private static double n(JsonObject r,String key){return r.get(key).getAsDouble();}
    private static double cell(JsonObject r,String key,int i){return r.getAsJsonArray(key).get(i).getAsDouble();}
    private static float[] encode(Pocket p) {
        float[] x=new float[Schema.INPUTS];x[0]=1;x[26]=1;x[42]=p.menu().ordinal()/4f;
        for(int i=0;i<64;i++){Stack s=i<36?p.storage(i):p.get(i,Pocket.NONE);x[200+2*i]=Stack.kind(s.item())/20f;x[201+2*i]=s.count()/64f;}
        x[328]=Stack.kind(p.cursor().item())/20f;x[329]=p.cursor().count()/64f;return x;
    }
    private static Pocket pocket(){Pocket p=new Pocket();p.open(Pocket.Menu.INVENTORY);return p;}
    private static void put(Pocket p,int slot,String item,int units){p.setStorage(35,new Stack(item,units));p.click(1,35,Pocket.NONE);p.click(1,slot,Pocket.NONE);check(p.cursor().empty(),"fixture placement");}
    private static int[] action(int op,int slot){int[] a=Schema.IDLE.clone();a[6]=op;a[7]=Schema.slotActive(op)?slot:0;return a;}
    private static double[] only(int op,int slot){double[] p=new double[Schema.DISTRIBUTION];p[Task.offset(6)+op]=1;if(Schema.slotActive(op))p[Schema.slotOffset(op)+slot]=1;return p;}
    private static void states() {
        for(int code=0;code<81;code++) {
            Pocket p=pocket();int value=code,mask=0,wrong=0,surplus=0,planks=0;
            for(int i=0;i<4;i++){int state=value%3;value/=3;if(state==0)continue;int units=1+(code+i)%4;
                put(p,36+i,state==1?"OAK_PLANKS":"DIRT",units);
                if(state==1){mask|=1<<i;surplus+=units-1;planks+=units;}else wrong++;
            }
            float[] x=encode(p),copy=x.clone();TableCraftingTrace.State s=TableCraftingTrace.inspect(x);
            check(s.inventory()&&s.correctMask()==mask&&s.wrongCells()==wrong&&s.surplusUnits()==surplus,"physical 2x2 cells");
            check(s.planks()==planks&&s.sticks()==0&&s.tables()==0,"preview is not carried output");
            check(Arrays.equals(x,copy),"read only");
        }
        for(int mask=0;mask<16;mask++) {
            Pocket p=pocket();for(int i=0;i<4;i++)if((mask&(1<<i))!=0)put(p,36+i,"OAK_PLANKS",1);
            TableCraftingTrace.State s=TableCraftingTrace.inspect(encode(p));
            int output=mask==15?5:mask==5||mask==10?4:0;
            check(s.previewKind()==output&&s.previewUnits()==(output==5?1:output==4?4:0),"sticks and table previews differ");
        }
        Pocket p=pocket();put(p,36,"OAK_PLANKS",4);TableCraftingTrace.State s=TableCraftingTrace.inspect(encode(p));
        check(s.correct()==1&&s.surplusUnits()==3&&s.planks()==4&&s.previewKind()==0,"four units in one cell is not four recipe cells");
        for(Pocket.Menu menu:Pocket.Menu.values())if(menu!=Pocket.Menu.INVENTORY){p.open(menu);s=TableCraftingTrace.inspect(encode(p));check(!s.inventory()&&s.planks()==0,"hidden or different grid is outside measurement scope");}
    }
    private static void effects() {
        // The real two-plank recipe consumes scarce stock but cannot count as the target.
        Pocket p=pocket();p.setStorage(0,new Stack("OAK_PLANKS",2));put(p,36,"OAK_PLANKS",1);put(p,38,"OAK_PLANKS",1);
        float[] before=encode(p);p.click(3,40,Pocket.NONE);float[] after=encode(p);TableCraftingTrace t=new TableCraftingTrace();
        t.observe(before,after,action(3,40),only(3,40));JsonObject r=report(t);
        near(n(r,"other_preview_states"),1);near(n(r,"target_preview_states"),0);near(n(r,"other_collection_probability_sum"),1);
        near(n(r,"observed_stick_units_gained"),4);near(n(r,"observed_stick_gain_transitions"),1);near(n(r,"observed_table_units_gained"),0);
        near(n(r,"chosen_other_result_clicks"),1);near(n(r,"carried_planks_below_four_without_table_states"),0);
        for(int i=0;i<4;i++)near(cell(r,"removed_cell_transitions",i),0);
        t.observe(after,after,action(0,0),only(0,0));near(n(report(t),"carried_planks_below_four_without_table_states"),1);
        p.close();t.observe(encode(p),encode(p),action(0,0),only(0,0));near(n(report(t),"carried_planks_below_four_without_table_states"),1);
        // Recovering a dropped unit raises observed stock again, not the crafted counter.
        p=pocket();p.setStorage(0,new Stack("OAK_PLANKS",2));put(p,36,"OAK_PLANKS",1);put(p,38,"OAK_PLANKS",1);
        before=encode(p);p.click(3,40,Pocket.NONE);after=encode(p);t=new TableCraftingTrace();
        t.observe(before,after,action(3,40),only(3,40));
        p.setStorage(1,new Stack("STICK",3));before=encode(p);t.observe(after,before,action(0,0),only(0,0));
        p.setStorage(1,new Stack("STICK",4));t.observe(before,encode(p),action(0,0),only(0,0));
        near(n(report(t),"observed_stick_units_gained"),5);check(p.crafted.get("STICK")==4,"carried gains must not masquerade as manufactured units");
        for(int op=1;op<=3;op++) {
            p=pocket();for(int i=0;i<4;i++)put(p,36+i,"OAK_PLANKS",1);before=encode(p);p.click(op,40,Pocket.NONE);
            t=new TableCraftingTrace();t.observe(before,encode(p),action(op,40),only(op,40));r=report(t);
            near(n(r,"target_preview_states"),1);near(n(r,"target_collection_probability_sum"),1);near(n(r,"observed_table_units_gained"),1);
            near(n(r,"observed_stick_units_gained"),0);near(n(r,"carried_planks_below_four_without_table_states"),0);
            for(int i=0;i<4;i++)near(cell(r,"removed_cell_transitions",i),0);
        }
        p=pocket();put(p,36,"OAK_PLANKS",1);before=encode(p);p.click(1,36,Pocket.NONE);after=encode(p);
        t=new TableCraftingTrace();t.observe(before,after,action(1,36),only(1,36));near(cell(report(t),"removed_cell_transitions",0),1);
        p.click(2,36,Pocket.NONE);t.observe(after,encode(p),action(2,36),only(2,36));near(cell(report(t),"filled_cell_transitions",0),1);
        before=encode(p);p.close();t.observe(before,encode(p),action(5,0),only(5,0));near(n(report(t),"partial_inventory_exits"),1);
    }
    private static void probabilities() {
        for(int count=1;count<=4;count++) {
            Pocket p=pocket();p.setStorage(0,new Stack("OAK_PLANKS",count));p.click(1,0,Pocket.NONE);float[] x=encode(p);
            double[] q=only(5,0);int o=Task.offset(6);q[o+5]=.25;q[o+1]=.25;q[o+2]=.5;
            q[Schema.slotOffset(1)+36]=1;q[Schema.slotOffset(2)+37]=1;
            TableCraftingTrace t=new TableCraftingTrace();t.observe(x,x,action(5,0),q);JsonObject r=report(t);
            near(cell(r,"compatible_cursor_states_by_correct_cells",0),1);near(cell(r,"fill_probability_sum_by_correct_cells",0),.75);
            near(cell(r,"single_unit_fill_probability_sum_by_correct_cells",0),count==1?.75:.5);
            for(int i=1;i<5;i++)near(cell(r,"compatible_cursor_states_by_correct_cells",i),0);
        }
    }
    private static void nonInterference() {
        Policy policy=Policy.initialize(73);float[] weights=policy.copyWeights();TableCraftingTrace t=new TableCraftingTrace();
        for(int seed=0;seed<256;seed++) {
            Pocket p=pocket();p.setStorage(0,new Stack("OAK_PLANKS",4));if(seed%2==0)p.click(1,0,Pocket.NONE);
            float[] x=encode(p),saved=x.clone();boolean[] mask=Task.CRAFT_WORKBENCH.mask(p.slots(),true);MenuInputs.restrict(p,Pocket.NONE,mask);boolean[] savedMask=mask.clone();
            Policy.Workspace w=new Policy.Workspace();policy.forward(x,mask,w);RandomSource a=new RandomSource(seed),b=new RandomSource(seed);
            Distribution.Choice chosen=Distribution.choose(w.probabilities,a,false);int[] savedAction=chosen.actions().clone();double[] savedP=w.probabilities.clone();
            t.observe(policy,x,mask,x,chosen.actions(),chosen.logProbability());policy.forward(x,mask,w);Distribution.Choice again=Distribution.choose(w.probabilities,b,false);
            check(Arrays.equals(chosen.actions(),again.actions())&&a.state()==b.state()&&chosen.logProbability()==again.logProbability(),"action, likelihood and random stream unchanged");
            check(Arrays.equals(x,saved)&&Arrays.equals(mask,savedMask)&&Arrays.equals(chosen.actions(),savedAction)&&Arrays.equals(w.probabilities,savedP),"input arrays unchanged");
            rejected(()->new TableCraftingTrace().observe(policy,x,mask,x,chosen.actions(),chosen.logProbability()+.01));
        }
        check(Arrays.equals(weights,policy.copyWeights())&&policy.updates()==0&&policy.samples()==0,"model unchanged");
        check(t.json().length()<3000,"bounded report");
        near(n(report(t),"inventory_states"),256);
    }
    private static void invalid() {
        float[] x=encode(pocket());rejected(()->TableCraftingTrace.inspect(new float[4]));
        for(int index:new int[]{26,27,42,272,273,280,281,328,329})for(float bad:new float[]{Float.NaN,Float.POSITIVE_INFINITY,-.1f,.023f,2}) {
            float[] changed=x.clone();changed[index]=bad;rejected(()->TableCraftingTrace.inspect(changed));
        }
        float[] inconsistent=x.clone();inconsistent[272]=3/20f;rejected(()->TableCraftingTrace.inspect(inconsistent));
        double[] q=only(0,0);q[Task.offset(6)]=.5;TableCraftingTrace t=new TableCraftingTrace();String before=t.json();rejected(()->t.observe(x,x,action(0,0),q));check(before.equals(t.json()),"rejection cannot add partial counts");
        double[] child=only(1,36);child[Schema.slotOffset(1)+36]=.5;rejected(()->new TableCraftingTrace().observe(x,x,action(1,36),child));
    }
    public static void main(String[] args){states();effects();probabilities();nonInterference();invalid();System.out.println("PASS table crafting measurement checks="+checks+"; no learned skill claim");}
}
