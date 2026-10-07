package org.botsclustersmc.commonslearning;

import java.util.*;
import org.botsclustersmc.core.*;

/** Pure source/mechanics tests, not a learned-cooperation result. */
public final class SupplyRoomTest {
    private static long checks;
    private static void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
    private static void rejects(Runnable call){try{call.run();throw new AssertionError("invalid operation accepted");}catch(IllegalArgumentException expected){checks++;}}
    private static String state(SupplyRoom r){var o=r.snapshot();return r.step()+":"+Arrays.toString(o.serviceSteps())+Arrays.toString(o.bank())+Arrays.toString(o.remaining())+Arrays.toString(o.deposited())+Arrays.toString(o.withdrawn());}
    private static void masks(SupplyRoom r){
        Policy p=Policy.initialize(91);Policy.Workspace w=new Policy.Workspace();
        for(int i=0;i<r.members();i++){
            var a=r.view(i,true);var b=r.view(i,false);
            check(Arrays.equals(a.mask(),b.mask()),"demand-dependent mechanical mask");
            float[] x=a.observation(),y=b.observation();
            for(int j=0;j<x.length;j++)if(j<205||j>207)check(x[j]==y[j],"ablation changed another observation");
            p.forward(x,a.mask(),w);
            for(int draw=0;draw<8;draw++)SupplyRoom.code(Distribution.choose(w.probabilities,new RandomSource(draw),false).actions());
            x[0]=Float.NaN;a.mask()[0]=false;check(a.observation()[0]==1&&r.view(i,true).observation()[0]==1,"view array leakage");
        }
    }
    public static void main(String[] args){
        rejects(()->new SupplyRoom(0,new int[]{0}));rejects(()->new SupplyRoom(0,new int[]{0,1}));rejects(()->new SupplyRoom(2,new int[]{0,2}));rejects(()->SupplyRoom.action(4));
        // All 16 two-member primitive pairs and both execution orders, three steps deep.
        for(int first=0;first<2;first++)for(int slots=0;slots<4;slots++)for(int history=0;history<32768;history++){
            SupplyRoom r=new SupplyRoom(first,new int[]{slots&1,2+(slots>>1)});int bits=history;
            for(int t=0;t<3;t++){
                int pair=bits&15;bits>>>=4;int ordering=bits&1;bits>>>=1;
                int[][] actions={SupplyRoom.action(pair&3),SupplyRoom.action(pair>>2)};
                r.advance(actions,ordering==0?new int[]{0,1}:new int[]{1,0});r.verifyConservation();checks++;
            }
            var o=r.snapshot();check(o.consumed()[0]<=1&&o.consumed()[1]<=1,"circulation created stock");
        }
        for(int n:new int[]{2,8,64})for(int seed=0;seed<8;seed++){
            SupplyRoom r=SupplyScenario.room(55,seed,n);masks(r);
            var before=r.snapshot();int[] codes=before.initial();codes[0]=99;check(r.snapshot().initial()[0]!=99,"initial evidence leakage");
            for(int t=0;t<4;t++){
                int[][] a=new int[n][];for(int i=0;i<n;i++)a[i]=SupplyRoom.action((seed+t+i)%4);
                r.advance(a,SupplyScenario.order(57,seed,t,n));r.verifyConservation();
            }
            String s=state(r);rejects(()->r.advance(new int[n][],new int[n]));check(state(r).equals(s),"post-horizon mutation");
        }
        SupplyRoom r=new SupplyRoom(0,new int[]{0,2});
        SupplyRoom other=new SupplyRoom(0,new int[]{0,3});
        check(Arrays.equals(r.view(0,true).observation(),other.view(0,true).observation()),"partner private slot leaked");
        String before=state(r);rejects(()->r.advance(new int[][]{SupplyRoom.action(1),SupplyRoom.action(1)},new int[]{0,0}));check(state(r).equals(before),"invalid order mutated stock");
        int[] invalid=Schema.IDLE.clone();invalid[0]=1;
        rejects(()->r.advance(new int[][]{SupplyRoom.action(1),invalid},new int[]{0,1}));check(state(r).equals(before),"late invalid action partially mutated stock");
        // Depositing and withdrawing the wrong resource is never useful consumption.
        for(int t=0;t<4;t++){
            float reward=r.advance(new int[][]{SupplyRoom.action(0),SupplyRoom.action(t%2==0?1:3)},new int[]{0,1});
            check(reward==0,"circulation generated consumption reward");
        }
        check(Arrays.equals(r.snapshot().consumed(),new int[2]),"circulation consumed resources");
        // Stale same-bank withdrawals: one stack moves at most once.
        SupplyRoom race=new SupplyRoom(0,new int[]{0,2});race.advance(new int[][]{SupplyRoom.action(0),SupplyRoom.action(1)},new int[]{0,1});
        race.advance(new int[][]{SupplyRoom.action(3),SupplyRoom.action(3)},new int[]{0,1});
        check(Arrays.equals(race.snapshot().remaining(),new int[]{1,1,0,0}),"stale withdrawal duplicated material");
        for(int first=0;first<2;first++) {
            SupplyRoom left=new SupplyRoom(first,new int[]{0,3}),right=new SupplyRoom(first,new int[]{3,0});
            for(int t=0;t<4;t++) {
                check(Arrays.equals(left.view(0,true).observation(),right.view(1,true).observation()),"member permutation changed view");
                check(Arrays.equals(left.view(1,true).mask(),right.view(0,true).mask()),"member permutation changed mask");
                int[] a=SupplyRoom.action(t%4),b=SupplyRoom.action((t+1)%4);
                check(left.advance(new int[][]{a,b},new int[]{0,1})==right.advance(new int[][]{b,a},new int[]{1,0}),"member permutation changed reward");
                check(Arrays.equals(left.snapshot().consumed(),right.snapshot().consumed()),"member permutation changed consumed stock");
            }
        }
        for(int seed=0;seed<8;seed++) {
            var episode=SupplyEpisode.play(Policy.initialize(seed),true,seed,0,2,true);
            check(episode.trajectories().size()==2,"missing member trajectory");
            for(var path:episode.trajectories()) {
                check(path.steps().size()==4,"wrong horizon");
                for(int t=0;t<4;t++) {
                    var transition=path.steps().get(t);
                    check(transition.ticks()==4&&transition.terminal()==(t==3),"wrong clock or terminal");
                    check(transition.reward()==episode.trajectories().get(0).steps().get(t).reward(),"non-common reward");
                    check(transition.behaviorVersion()==0,"wrong behavior policy version");
                }
            }
        }
        System.out.println("PASS synthetic supply mechanics checks="+checks+" exhaustive_three_step_histories=262144; no learned/Minecraft claim");
    }
}
