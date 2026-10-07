package org.botsclustersmc.tests;

import com.google.gson.*;
import java.util.*;
import org.botsclustersmc.core.*;
import org.botsclustersmc.core.Stack;
import org.botsclustersmc.holdout.ToolUseTrace;

/** Independent primitive-tuple enumeration and real pocket fixtures, not learned behavior. */
public final class ToolUseTraceTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void near(double a,double b){check(Math.abs(a-b)<1e-9,"expected "+b+", got "+a);}
    private static void reject(Runnable r){boolean bad=false;try{r.run();}catch(IllegalArgumentException|IllegalStateException e){bad=true;}check(bad,"invalid input accepted");}
    private static JsonObject json(ToolUseTrace t){return JsonParser.parseString(t.json()).getAsJsonObject();}
    private static double at(JsonObject j,String name,int i){return j.getAsJsonArray(name).get(i).getAsDouble();}
    private static float[] encode(Pocket p) {
        float[] x=new float[Schema.INPUTS];x[0]=1;x[28]=1;x[42]=p.menu().ordinal()/4f;x[43]=p.selected()/8f;
        for(int i=0;i<64;i++){Stack s=i<36?p.storage(i):p.get(i,Pocket.NONE);x[200+2*i]=Stack.kind(s.item())/20f;x[201+2*i]=s.count()/64f;}
        x[328]=Stack.kind(p.cursor().item())/20f;x[329]=p.cursor().count()/64f;return x;
    }
    private static boolean[] mask(Pocket p){boolean[] m=Task.MINE_COBBLESTONE.mask(p.slots(),p.menu()!=Pocket.Menu.CLOSED);MenuInputs.restrict(p,Pocket.NONE,m);return m;}
    private static void states() {
        for(int bits=0;bits<512;bits++)for(int selected=0;selected<9;selected++)for(Pocket.Menu menu:Pocket.Menu.values()) {
            Pocket p=new Pocket();p.open(menu);p.select(selected);
            for(int i=0;i<9;i++)if((bits&(1<<i))!=0)p.setStorage(i,new Stack(i%2==0?"WOODEN_PICKAXE":"STONE_PICKAXE",1));
            float[] x=encode(p),copy=x.clone();ToolUseTrace.State s=ToolUseTrace.inspect(x);
            check(s.pickSlots()==bits&&s.selectedPick()==((bits&(1<<selected))!=0),"all hotbar subsets/indices");
            check(s.index()==(menu==Pocket.Menu.CLOSED?0:2)+(s.selectedPick()?1:0),"four-state partition");
            check(Arrays.equals(x,copy),"observation mutation");
        }
        Pocket p=new Pocket();p.setStorage(20,new Stack("WOODEN_PICKAXE",1));p.open(Pocket.Menu.INVENTORY);
        check(ToolUseTrace.inspect(encode(p)).reservePick(),"reserve tool");p.click(1,20,Pocket.NONE);
        check(ToolUseTrace.inspect(encode(p)).cursorPick()&&!ToolUseTrace.inspect(encode(p)).selectedPick(),"cursor is not held");
        p.click(1,36,Pocket.NONE);check(ToolUseTrace.inspect(encode(p)).gridPick(),"visible grid tool");
        p.close();check(!ToolUseTrace.inspect(encode(p)).gridPick()&&p.count("WOODEN_PICKAXE")==1,"hidden is not absent");
        float[] x=encode(p);x[280]=6/20f;x[281]=1/64f;
        check(ToolUseTrace.inspect(x).pickSlots()==0&&!ToolUseTrace.inspect(x).gridPick(),"hidden preview not carried");
        p.open(Pocket.Menu.WORKBENCH);x=encode(p);x[290]=6/20f;x[291]=1/64f;
        check(!ToolUseTrace.inspect(x).selectedPick(),"preview not held");
    }
    private static void enumeration() {
        for(int seed=0;seed<16;seed++) {
            Pocket pocket=new Pocket();pocket.setStorage(seed%9,new Stack("WOODEN_PICKAXE",1));
            if(seed%2==0)pocket.setStorage((seed+3)%9,new Stack("STONE_PICKAXE",1));
            float[] x=encode(pocket);Policy policy=Policy.initialize(70+seed);Policy.Workspace w=new Policy.Workspace();
            policy.forward(x,mask(pocket),w);double[] p=w.probabilities.clone(),expected=new double[4];double total=0;
            // Enumerate the original seven heads, NOT the observer's factored formula.
            int tuples=1;for(int h=0;h<7;h++)tuples*=Schema.HEADS[h];
            for(int code=0;code<tuples;code++) {
                int value=code;int[] a=new int[8];double mass=1;
                for(int h=0;h<7;h++){a[h]=value%Schema.HEADS[h];value/=Schema.HEADS[h];mass*=p[Task.offset(h)+a[h]];}
                if(mass==0)continue;total+=mass;
                if(a[6]!=0)continue;expected[0]+=mass;
                if(a[4]!=1)continue;expected[1]+=mass;
                if(pocket.storage(a[5]).empty())continue;expected[2]+=mass;
                if(a[0]==0&&a[1]==2&&a[2]==2&&a[3]!=1)expected[3]+=mass;
            }
            near(total,1);Distribution.Choice choice=Distribution.choose(p,new RandomSource(seed),false);
            ToolUseTrace t=new ToolUseTrace();t.observe(x,x,choice.actions(),p);
            for(int i=0;i<4;i++)near(at(json(t),"closed_cascade_probability_sums",i),expected[i]);
            check(t.json().length()<2000,"bounded trace");
        }
    }
    private static void closeHandoff() {
        Pocket pocket=new Pocket();pocket.setStorage(3,new Stack("WOODEN_PICKAXE",1));pocket.select(3);pocket.open(Pocket.Menu.INVENTORY);
        float[] before=encode(pocket);boolean[] m=mask(pocket);Task.only(m,6,5);
        // Disable now-unreachable slot branches, as a real distribution does.
        Policy.Workspace w=new Policy.Workspace();Policy.initialize(1).forward(before,m,w);
        int[] action=Schema.IDLE.clone();action[6]=5;pocket.close();float[] after=encode(pocket);
        ToolUseTrace t=new ToolUseTrace();t.observe(before,after,action,w.probabilities);JsonObject j=json(t);
        near(at(j,"state_visits",3),1);near(at(j,"state_transitions",13),1);
        near(j.get("open_selected_pick_close_probability_sum").getAsDouble(),1);
        near(j.get("open_selected_pick_close_selections").getAsDouble(),1);
        near(at(j,"open_gui_selections",5),1);
        // New hotbar selection is ignored on menu-close; this is measured, never chosen for the actor.
        check(pocket.selected()==3&&j.get("final_state").getAsInt()==1,"close retains existing selected slot");
        float[] absent=after.clone();absent[206]=0;absent[207]=0;String saved=t.json();
        reject(()->t.observe(absent,absent,action,w.probabilities));check(saved.equals(t.json()),"failed stream changed counters");
    }
    private static void noninterference() {
        Policy policy=Policy.initialize(917);float[] weights=policy.copyWeights();
        for(int i=0;i<128;i++) {
            Pocket p=new Pocket();p.setStorage(i%36,new Stack("WOODEN_PICKAXE",1));p.select(i%9);
            if(i%2==0)p.open(Pocket.Menu.INVENTORY);
            float[] x=encode(p),saved=x.clone();boolean[] m=mask(p),savedMask=m.clone();
            Policy.Workspace w=new Policy.Workspace();policy.forward(x,m,w);
            RandomSource a=new RandomSource(i),b=new RandomSource(i);Distribution.Choice chosen=Distribution.choose(w.probabilities,a,false);
            int[] savedAction=chosen.actions().clone();double[] savedP=w.probabilities.clone();ToolUseTrace t=new ToolUseTrace();
            t.observe(policy,x,m,x,chosen.actions(),chosen.logProbability());
            Distribution.Choice unchanged=Distribution.choose(w.probabilities,b,false);
            check(Arrays.equals(chosen.actions(),unchanged.actions())&&chosen.logProbability()==unchanged.logProbability()
                &&a.state()==b.state(),"sampled action/likelihood/RNG changed");
            check(Arrays.equals(x,saved)&&Arrays.equals(m,savedMask)&&Arrays.equals(chosen.actions(),savedAction)
                &&Arrays.equals(w.probabilities,savedP),"observer changed caller arrays");
            String report=t.json();reject(()->t.observe(policy,x,m,x,chosen.actions(),chosen.logProbability()+.01));
            check(t.json().equals(report),"mismatched likelihood partially recorded");
            for(int pos:new int[]{28,29,42,43,200,201,328,329})for(float bad:new float[]{Float.NaN,Float.POSITIVE_INFINITY,-1,.021f,8}) {
                float[] invalid=x.clone();invalid[pos]=bad;reject(()->t.observe(invalid,x,chosen.actions(),savedP));
                check(report.equals(t.json()),"invalid state partially recorded");
            }
            double[] invalid=savedP.clone();invalid[0]=Double.NaN;reject(()->t.observe(x,x,chosen.actions(),invalid));
            check(report.equals(t.json()),"invalid probability partially recorded");
        }
        check(Arrays.equals(weights,policy.copyWeights())&&policy.updates()==0&&policy.samples()==0,"policy changed");
    }
    private static void underflow() {
        Pocket pocket=new Pocket();pocket.setStorage(0,new Stack("WOODEN_PICKAXE",1));pocket.open(Pocket.Menu.INVENTORY);
        float[] x=encode(pocket),weights=new float[Policy.PARAMETERS];boolean[] m=mask(pocket);
        weights[Policy.B3+Task.offset(6)+1]=-1000;
        Policy policy=new Policy(weights,0,0);Policy.Workspace w=new Policy.Workspace();policy.forward(x,m,w);
        check(m[Task.offset(6)+1]&&w.probabilities[Task.offset(6)+1]==0,"legal parent underflows");
        double row=0;for(int i=0;i<64;i++)row+=w.probabilities[Schema.slotOffset(1)+i];near(row,1);
        Distribution.Choice chosen=Distribution.choose(w.probabilities,new RandomSource(0),false);
        ToolUseTrace trace=new ToolUseTrace();trace.observe(policy,x,m,x,chosen.actions(),chosen.logProbability());
        near(json(trace).get("transitions").getAsDouble(),1);
        String saved=trace.json();double[] bad=w.probabilities.clone();
        for(int i=0;i<64;i++)bad[Schema.slotOffset(1)+i]*=.5;
        reject(()->trace.observe(x,x,chosen.actions(),bad));check(trace.json().equals(saved),"malformed unreachable row recorded");
    }
    private static void artifacts()throws Exception {
        for(String file:List.of("dist/training.jar","dist/botsclustersmc.jar")) {
            try(var jar=new java.util.jar.JarFile(file)) {
                check(jar.stream().noneMatch(e->e.getName().contains("/holdout/")
                    ||e.getName().contains("ToolUse")),"diagnostic code leaked into plugin: "+file);
            }
        }
    }
    public static void main(String[] args)throws Exception {
        reject(()->new ToolUseTrace().json());states();enumeration();closeHandoff();noninterference();underflow();artifacts();
        System.out.println("PASS tool-use input enumeration, state and noninterference checks="+checks);
    }
}
