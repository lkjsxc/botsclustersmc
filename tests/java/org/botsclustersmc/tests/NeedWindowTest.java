package org.botsclustersmc.tests;

import java.util.*;
import org.botsclustersmc.needs.*;
import org.botsclustersmc.needs.SharedNeed.*;
import org.botsclustersmc.training.VTrace;

public final class NeedWindowTest {
    private static long checks, partitions;
    private static final Stock PICK=new Stock(0,0,1);
    private static void check(boolean ok,String text){checks++;if(!ok)throw new AssertionError(text);}
    private static void near(double a,double b,String text){check(Math.abs(a-b)<2e-10,text+": "+a+" != "+b);}
    private static void reject(Runnable action,String text){
        try{action.run();}catch(IllegalArgumentException|IllegalStateException|NullPointerException|UnsupportedOperationException expected){checks++;return;}
        throw new AssertionError(text+" rejected");
    }
    private static Contract contract(int horizon,int members){return new Contract(17,horizon,members,PICK);}
    private static Frame frame(Contract c,int tick,boolean covered){return new Frame(c,tick,true,covered?PICK:Stock.EMPTY);}
    private static NeedWindow full(int ticks,int members,double gamma){
        Contract c=contract(ticks,members);NeedWindow w=new NeedWindow(c,ticks,gamma);
        for(int t=0;t<=ticks;t++)w.append(frame(c,t,true));return w;
    }
    private static void incomplete(){
        Contract c=contract(4,2);NeedWindow w=new NeedWindow(c,4,1);
        check(w.audit().score().isEmpty(),"uninitialized episode has no score");
        w.append(frame(c,0,true));w.append(frame(c,1,true));
        check(w.audit().score().isEmpty(),"unfinished episode has no score");
        check(!w.audit().outcomeComplete(),"unfinished episode incomplete");
    }
    private static void unknown(){
        Contract c=contract(4,2);NeedWindow w=new NeedWindow(c,4,1);
        w.append(frame(c,0,true));w.append(new Frame(c,1,false,Stock.EMPTY));w.append(frame(c,2,true));
        var before=w.audit();reject(()->w.claim(0,2),"unknown reward claim");
        check(w.audit().equals(before),"unknown rejection does not mutate cursors or evidence");
        w.claim(1,1);var skip=w.discardThrough(0,2);
        check(skip.fromTick()==0&&skip.toTick()==2&&skip.sequence()==0,"explicit unknown discontinuity");
        w.append(frame(c,3,true));w.append(frame(c,4,true));
        near(w.claim(0,4).memberReward(),.25,"known suffix after explicit discontinuity");
        reject(()->w.claim(1,4),"another member still sees unknown history");
        check(w.progress(1).cursor()==1,"failed interval retains earlier cursor");
        check(w.audit().knownTicks()==3&&w.audit().unknownTicks()==1,"unknown interval counted separately");
        check(!w.audit().outcomeComplete()&&w.audit().score().isEmpty(),"unknown history is not complete");
    }
    private static void cursor(){
        NeedWindow w=full(4,2,1);w.claim(0,2);var before=w.audit();
        reject(()->w.claim(0,2),"duplicate reward claim");
        check(w.audit().equals(before),"duplicate rejection unchanged");
        check(w.claim(0,4).sequence()==1,"member-local monotonic sequence");
        check(w.claim(1,4).sequence()==0,"members do not consume each other's history");
        check(w.progress(0).claimedTicks()==4,"exactly four claimed ticks");
        reject(()->w.discardThrough(0,4),"duplicate discontinuity");
        reject(()->w.claim(-1,4),"negative member");reject(()->w.claim(2,4),"outside member");
        reject(()->w.progress(2),"outside progress");
    }
    private static void terminal(){
        double gamma=Math.pow(.997,.25);NeedWindow w=full(8,1,gamma);
        var first=w.claim(0,3);near(first.bootstrapDiscount(),VTrace.discount(3,false),"actual-tick bootstrap");
        check(!first.terminal(),"nonterminal fragment");
        var last=w.claim(0,8);near(last.bootstrapDiscount(),0,"terminal bootstrap is zero");
        check(last.terminal()&&last.memberReward()>0,"terminal retains accrued service reward");
        near(w.audit().score().orElseThrow(),1,"complete constant coverage");
    }
    private static void timing(){
        Contract c=contract(16,1);NeedWindow empty=new NeedWindow(c,16,1);
        for(int t=0;t<=16;t++)empty.append(frame(c,t,false));
        near(empty.claim(0,16).memberReward(),0,"empty stock earns no service reward");
        NeedWindow slow=full(16,1,.97),fast=full(16,1,.97);
        double observed=0;for(int t=1;t<=16;t++){var r=fast.claim(0,t);observed+=Math.pow(.97,r.fromTick())*r.memberReward();}
        near(slow.claim(0,16).memberReward(),observed,"partition-invariant return");
        for(int pattern:new int[]{0xff00,0xaaaa}){
            NeedWindow w=new NeedWindow(c,16,1);for(int t=0;t<=16;t++)w.append(frame(c,t,t<16&&((pattern>>t)&1)!=0));
            near(w.claim(0,16).memberReward(),.5,"equal coverage-time despite different transfer frequency");
        }
    }
    private static void scale(){
        for(int n:new int[]{1,2,8,64,512}){
            NeedWindow w=full(48,n,1);double sum=0;
            for(int m=0;m<n;m++)for(int t=0;t<48;){int end=Math.min(48,t+1+m%17);var claim=w.claim(m,end);sum+=claim.memberReward();near(claim.teamReward(),claim.memberReward()*n,"team return separate from accounting share");t=end;}
            near(sum,1,"cohort-normalized reward");near(w.audit().score().orElseThrow(),1,"one team clock");
            for(var p:w.audit().members())check(p.cursor()==48&&p.claimedTicks()==48&&p.discardedTicks()==0,"complete member partition");
        }
    }
    private static void boundaries(){
        Contract c=contract(6,2);NeedWindow w=new NeedWindow(c,2,1);
        reject(()->w.claim(0,1),"uninitialized reward");w.append(frame(c,0,true));w.append(frame(c,1,true));
        var before=w.audit();reject(()->w.append(frame(c,1,false)),"duplicate frame");
        reject(()->w.append(frame(c,3,false)),"missing frame");
        reject(()->w.append(frame(new Contract(18,6,2,PICK),2,false)),"cross-episode frame");
        reject(()->w.append(frame(new Contract(17,6,1,PICK),2,false)),"changed population");
        reject(()->w.append(new Frame(new Contract(17,6,2,new Stock(1,0,0)),2,true,Stock.EMPTY)),"changed demand");
        reject(()->w.claim(0,2),"future interval");check(w.audit().equals(before),"failed operations are noninterfering");
        for(int t=2;t<=4;t++)w.append(frame(c,t,true));
        check(w.oldestTick()==2,"ring history boundary");before=w.audit();reject(()->w.claim(0,4),"slow actor overrun");
        check(w.audit().equals(before),"overrun leaves member state unchanged");
        w.discardThrough(0,2);near(w.claim(0,4).memberReward(),2/12.0,"recoverable retained suffix");
        check(w.progress(0).discardedTicks()==2,"lost samples explicitly counted");
        check(w.progress(1).cursor()==0,"other member is independent");
        for(int t=5;t<=6;t++)w.append(frame(c,t,true));w.claim(0,6);
        near(w.audit().score().orElseThrow(),1,"team outcome distinct from learner delivery");
        reject(()->w.audit().members().clear(),"immutable audit members");
        reject(()->new NeedWindow(c,0,1),"zero capacity");reject(()->new NeedWindow(c,7,1),"oversized capacity");
        for(double g:new double[]{-1,1.1,Double.NaN,Double.POSITIVE_INFINITY})reject(()->new NeedWindow(c,2,g),"invalid discount");
    }
    private static void materialEvidence(){
        Contract c=new Contract(27,8,2,new Stock(2,2,0));NeedWindow w=new NeedWindow(c,3,1);
        for(int t=0;t<=8;t++)w.append(new Frame(c,t,true,new Stock(2,0,4096)));
        var evidence=w.audit();near(evidence.score().orElseThrow(),.5,"aggregate score is not full coverage");
        check(evidence.fullyCoveredTicks()==0,"aggregate score cannot conceal complete stock failure");
        check(evidence.materials().size()==2,"unrequested surplus excluded from material outcomes");
        var planks=evidence.materials().get(0);var sticks=evidence.materials().get(1);
        check(planks.coveredTicks()==8&&planks.shortageTicks()==0,"fully provided material");
        check(sticks.coveredTicks()==0&&sticks.shortageTicks()==8&&sticks.longestConfirmedShortage()==8,"persistent missing material retained across ring eviction");
        reject(()->evidence.materials().clear(),"immutable material evidence");
        Contract u=contract(8,1);NeedWindow missing=new NeedWindow(u,8,1);
        for(int t=0;t<=8;t++)missing.append(new Frame(u,t,t!=3,Stock.EMPTY));
        check(missing.audit().materials().get(0).longestConfirmedShortage()==4,"unknown separates confirmed shortage runs");
        check(missing.audit().materials().get(0).shortageTicks()==7,"unknown is not a measured shortage");
        check(!missing.audit().outcomeComplete(),"component diagnostics do not make missing evidence complete");
    }
    private static void exhaustive(){
        Contract c=contract(8,1);
        for(double gamma:new double[]{0,.97,1})for(int bits=0;bits<256;bits++){
            double expected=0;for(int t=0;t<8;t++)expected+=Math.pow(gamma,t)*((bits>>t)&1)/8.0;
            for(int cuts=0;cuts<128;cuts++){
                NeedWindow w=new NeedWindow(c,8,gamma);
                for(int t=0;t<=8;t++)w.append(frame(c,t,t<8&&((bits>>t)&1)!=0));
                double found=0;
                for(int end=1;end<=8;end++)if(end==8||((cuts>>(end-1))&1)!=0){
                    var r=w.claim(0,end);found+=Math.pow(gamma,r.fromTick())*r.memberReward();
                    near(r.bootstrapDiscount(),end==8?0:Math.pow(gamma,r.toTick()-r.fromTick()),"partition discount");
                }
                near(found,expected,"independent per-tick oracle");
                near(w.audit().score().orElseThrow(),Integer.bitCount(bits)/8.0,"independent coverage score");partitions++;
            }
        }
    }
    private static void fractional(){
        Random rng=new Random(2026100721L);
        for(int trial=0;trial<200;trial++){
            Stock target=new Stock(rng.nextInt(64)+1,rng.nextInt(65),rng.nextInt(65));
            Contract c=new Contract(trial,37,8,target);NeedWindow w=new NeedWindow(c,37,.993);double expected=0,score=0;
            for(int t=0;t<=37;t++){
                Stock bank=new Stock(rng.nextInt(128),rng.nextInt(128),rng.nextInt(128));
                w.append(new Frame(c,t,true,bank));
                if(t==37)break;
                int dimensions=0;double value=0;
                for(int k=0;k<3;k++)if(target.at(k)>0){dimensions++;value+=(bank.at(k)>=target.at(k)?1:bank.at(k)/(double)target.at(k));}
                value/=dimensions;expected+=Math.pow(.993,t)*value/37;score+=value/37;
            }
            double found=0;
            for(int m=0;m<8;m++)for(int t=0;t<37;){int end=Math.min(37,t+1+rng.nextInt(11));var r=w.claim(m,end);found+=Math.pow(.993,t)*r.memberReward();t=end;}
            near(found,expected,"fractional independent reward oracle");near(w.audit().score().orElseThrow(),score,"fractional score oracle");
        }
    }
    private static void boundedCohort(){
        Contract c=contract(12000,512);NeedWindow w=new NeedWindow(c,128,1);w.append(frame(c,0,true));double reward=0;int covered=0;
        for(int t=1;t<=12000;t++){
            if((t-1)%7<4)covered++;w.append(frame(c,t,t%7<4));
            for(int m=0;m<512;m++)if(t%(1+m%31)==0||t==12000)reward+=w.claim(m,t).memberReward();
        }
        near(reward,covered/12000.0,"bounded 512-member asynchronous accounting");
        near(w.audit().score().orElseThrow(),covered/12000.0,"full finite-horizon outcome");
        for(var p:w.audit().members())check(p.claimedTicks()==12000&&p.discardedTicks()==0,"no stalled member in fixed stress schedule");
    }
    public static void main(String[] args){
        if(args.length>0){switch(args[0]){case "scale"->scale();case "unknown"->unknown();case "timing"->timing();case "cursor"->cursor();case "incomplete"->incomplete();case "terminal"->terminal();default->throw new IllegalArgumentException("test selection");}}
        else{incomplete();unknown();cursor();terminal();timing();scale();boundaries();materialEvidence();exhaustive();fractional();boundedCohort();}
        System.out.println("PASS need window "+checks+" checks, "+partitions+" exhaustive partitions; synthetic stock, zero Minecraft/learning trials");
    }
}
