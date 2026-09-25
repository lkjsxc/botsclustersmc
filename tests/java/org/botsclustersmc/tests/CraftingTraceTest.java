package org.botsclustersmc.tests;

import com.google.gson.*;
import java.util.*;
import org.botsclustersmc.core.*;
import org.botsclustersmc.core.Stack;
import org.botsclustersmc.holdout.CraftingTrace;

/** Synthetic mechanical/measurement checks, never learned-gameplay evidence. */
public final class CraftingTraceTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void near(double a,double b){check(Math.abs(a-b)<1e-9,"expected "+b+", got "+a);}
    private static void rejected(Runnable action){boolean rejected=false;try{action.run();}catch(IllegalArgumentException|IllegalStateException e){rejected=true;}check(rejected,"malformed diagnostic input must fail");}
    private static JsonObject report(CraftingTrace trace){return JsonParser.parseString(trace.json()).getAsJsonObject();}
    private static int count(JsonObject r,String key){return r.get(key).getAsInt();}
    private static double cell(JsonObject r,String key,int cell){return r.getAsJsonArray(key).get(cell).getAsDouble();}
    private static float[] encode(Pocket pocket,int task) {
        float[] x=new float[Schema.INPUTS];x[0]=1;x[16+task]=1;x[42]=pocket.menu().ordinal()/4f;
        for(int i=0;i<64;i++){Stack s=i<36?pocket.storage(i):pocket.get(i,Pocket.NONE);x[200+2*i]=Stack.kind(s.item())/20f;x[201+2*i]=s.count()/64f;}
        x[328]=Stack.kind(pocket.cursor().item())/20f;x[329]=pocket.cursor().count()/64f;return x;
    }
    private static void put(Pocket p,int slot,String item,int units) {
        p.setStorage(35,new Stack(item,units));p.click(1,35,Pocket.NONE);p.click(1,slot,Pocket.NONE);
        check(p.cursor().empty(),"mechanical fixture placement");
    }
    private static Pocket pocket(){Pocket p=new Pocket();p.open(Pocket.Menu.WORKBENCH);return p;}
    private static double[] only(int op,int slot){double[] p=new double[Schema.DISTRIBUTION];p[Task.offset(6)+op]=1;if(Schema.slotActive(op))p[Schema.slotOffset(op)+slot]=1;return p;}
    private static int[] action(int op,int slot){int[] a=Schema.IDLE.clone();a[6]=op;a[7]=Schema.slotActive(op)?slot:0;return a;}
    private static void states() {
        // Independent row-major recipe oracle, including four cells that must stay empty.
        for(int task:new int[]{11,13})for(int ternary=0;ternary<243;ternary++)for(int extras=0;extras<16;extras++) {
            int[] recipe={1,1,1,0,2,0,0,2,0};Pocket p=pocket();int code=ternary,bit=0,extra=0,mask=0,wrong=0,surplus=0;
            for(int pos=0;pos<9;pos++) {
                if(recipe[pos]==0){if((extras&(1<<extra))!=0){put(p,36+pos,"DIRT",1);wrong++;}extra++;continue;}
                int state=code%3;code/=3;
                if(state!=0){int n=1+(ternary+pos)%3;String item=state==2?"DIRT":recipe[pos]==2?"STICK":task==13?"COBBLESTONE":"OAK_PLANKS";put(p,36+pos,item,n);
                    if(state==1){mask|=1<<bit;surplus+=n-1;}else wrong++;}
                bit++;
            }
            float[] x=encode(p,task),copy=x.clone();CraftingTrace.State s=CraftingTrace.inspect(x,task);
            check(s.workbench()&&s.correctMask()==mask&&s.wrongCells()==wrong&&s.surplusUnits()==surplus,"exact cells, not grid units or preview");
            check(s.preview()==(mask==31&&wrong==0),"actual recipe preview");
            check(Arrays.equals(x,copy),"inspection cannot alter observations");
        }
        for(Pocket.Menu menu:Pocket.Menu.values())if(menu!=Pocket.Menu.WORKBENCH) {
            Pocket p=new Pocket();p.open(menu);float[] x=encode(p,11);x[280]=.2f;x[281]=1/64f;
            CraftingTrace.State s=CraftingTrace.inspect(x,11);check(!s.workbench()&&s.correct()==0&&!s.preview(),"2x2 result or other menu is not a pickaxe cell");
        }
        Pocket p=pocket();put(p,36,"OAK_PLANKS",1);check(CraftingTrace.inspect(encode(p,13),13).correct()==0,"wood material is wrong for stone pickaxe");
    }
    private static void probabilitiesAndTransitions() {
        Pocket p=pocket();p.setStorage(0,new Stack("OAK_PLANKS",3));p.click(1,0,Pocket.NONE);
        float[] before=encode(p,11);p.click(2,36,Pocket.NONE);float[] after=encode(p,11);
        double[] probabilities=only(5,0);int parent=Task.offset(6);probabilities[parent+5]=.25;
        probabilities[parent+1]=.25;probabilities[parent+2]=.5;
        probabilities[Schema.slotOffset(1)+36]=1;probabilities[Schema.slotOffset(2)+36]=1;
        CraftingTrace trace=new CraftingTrace();trace.observe(11,before,after,action(2,36),probabilities);
        JsonObject r=report(trace);check(count(r,"max_correct_cells")==1&&count(r,"compatible_cursor_states")==1,"actual next state, pre-action opportunities");
        for(int i=0;i<3;i++)near(cell(r,"compatible_cursor_states_by_cell",i),1);
        for(int i=3;i<5;i++)near(cell(r,"compatible_cursor_states_by_cell",i),0);
        near(cell(r,"compatible_fill_probability_sum_by_cell",0),.75);
        near(cell(r,"single_unit_fill_probability_sum_by_cell",0),.5);
        near(cell(r,"filled_cell_transitions",0),1);near(cell(r,"correct_mask_states",0),1);
        trace.observe(11,after,before,action(1,36),only(1,36));r=report(trace);
        near(cell(r,"removed_cell_transitions",0),1);
        p.close();trace.observe(11,after,encode(p,11),action(5,0),only(5,0));r=report(trace);
        check(count(r,"partial_workbench_exits")==1&&count(r,"chosen_close_actions")==1,"menu exit is separate from grid removal");
        // Preview consumption removes ingredients legitimately; it is not classified as failure.
        p=pocket();for(int s:new int[]{36,37,38})put(p,s,"OAK_PLANKS",1);for(int s:new int[]{40,43})put(p,s,"STICK",1);
        before=encode(p,11);p.click(3,45,Pocket.NONE);after=encode(p,11);
        trace=new CraftingTrace();trace.observe(11,before,after,action(3,45),only(3,45));r=report(trace);
        check(count(r,"max_correct_cells")==5&&count(r,"target_preview_states")==1&&p.count("WOODEN_PICKAXE")==1,"preview collection is actual mechanical output");
        near(r.get("target_collection_probability_sum").getAsDouble(),1);
        check(trace.json().length()<2500,"bounded report size");
        p=pocket();p.setStorage(0,new Stack("OAK_PLANKS",3));p.setStorage(1,new Stack("STICK",2));
        before=encode(p,11);p.click(1,0,Pocket.NONE);trace=new CraftingTrace();trace.observe(11,before,encode(p,11),action(1,0),only(1,0));
        r=report(trace);check(count(r,"empty_cursor_states")==1,"empty cursor denominator");near(r.get("needed_stock_pickup_probability_sum").getAsDouble(),1);
        check(count(report(new CraftingTrace()),"workbench_states")==0,"unobserved is not a fabricated rate");
    }
    private static void nonInterference() {
        Policy policy=Policy.initialize(73);float[] weights=policy.copyWeights();CraftingTrace trace=new CraftingTrace();
        for(int seed=0;seed<256;seed++) {
            Pocket p=pocket();p.setStorage(0,new Stack("OAK_PLANKS",3));p.setStorage(1,new Stack("STICK",2));
            if(seed%2==0)p.click(1,seed%3==0?1:0,Pocket.NONE);
            float[] x=encode(p,11);boolean[] mask=Task.CRAFT_WOOD_PICK.mask(p.slots(),true);MenuInputs.restrict(p,Pocket.NONE,mask);
            Policy.Workspace w=new Policy.Workspace();policy.forward(x,mask,w);RandomSource a=new RandomSource(seed),b=new RandomSource(seed);
            Distribution.Choice chosen=Distribution.choose(w.probabilities,a,false);
            float[] saved=x.clone();boolean[] savedMask=mask.clone();int[] savedAction=chosen.actions().clone();double[] savedP=w.probabilities.clone();
            String old=report(trace).toString();trace.observe(policy,11,x,mask,x,chosen.actions(),chosen.logProbability());
            policy.forward(x,mask,w);Distribution.Choice again=Distribution.choose(w.probabilities,b,false);
            check(Arrays.equals(chosen.actions(),again.actions())&&a.state()==b.state()&&chosen.logProbability()==again.logProbability(),"diagnosis cannot sample or change action/RNG/likelihood");
            check(Arrays.equals(x,saved)&&Arrays.equals(mask,savedMask)&&Arrays.equals(savedAction,chosen.actions())&&Arrays.equals(savedP,w.probabilities),"all input arrays and frozen probabilities unchanged");
            check(!old.equals(report(trace).toString()),"diagnostics actually accumulated");
            rejected(()->new CraftingTrace().observe(policy,11,x,mask,x,chosen.actions(),chosen.logProbability()+.01));
        }
        check(Arrays.equals(weights,policy.copyWeights())&&policy.updates()==0&&policy.samples()==0,"policy weights and counters unchanged");
    }
    private static void invalid() {
        float[] x=encode(pocket(),11);rejected(()->CraftingTrace.inspect(x,10));rejected(()->CraftingTrace.inspect(new float[4],11));
        for(float bad:new float[]{Float.NaN,Float.POSITIVE_INFINITY,-.1f,.023f,2}){float[] a=x.clone();a[272]=bad;rejected(()->CraftingTrace.inspect(a,11));}
        float[] a=x.clone();a[272]=3/20f;rejected(()->CraftingTrace.inspect(a,11));
        double[] p=only(5,0);p[Task.offset(6)+5]=.5;rejected(()->new CraftingTrace().observe(11,x,x,action(5,0),p));
        double[] child=only(1,36);child[Schema.slotOffset(1)+36]=.5;rejected(()->new CraftingTrace().observe(11,x,x,action(1,36),child));
    }
    public static void main(String[] args){states();probabilitiesAndTransitions();nonInterference();invalid();System.out.println("PASS crafting transition/probability/noninterference checks="+checks+"; synthetic measurement evidence only");}
}
