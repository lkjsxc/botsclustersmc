package org.botsclustersmc.commonslearning;

import java.util.*;
import org.botsclustersmc.core.*;
import org.botsclustersmc.core.Stack;

/** Closed synthetic stock and a public consumer. No Bukkit, private-state oracle or export. */
public final class SupplyRoom {
    public static final String ID="synthetic-communal-supply-memory-only";
    public static final int HORIZON=4;
    private static final String[] MATERIAL={"OAK_PLANKS","STICK"};
    private final Pocket[] pockets;
    private final Bank bank=new Bank();
    private final int[] initial,served=new int[2],serviceSteps=new int[2],deposited,withdrawn;
    private final int first;
    private int step,phase,wrong;
    private static final class Bank implements Pocket.External {
        Stack stock=Stack.EMPTY;
        public int size(){return 1;}
        public Stack get(int slot){if(slot!=0)throw new IllegalArgumentException("bank slot");return stock;}
        public void set(int slot,Stack value){if(slot!=0||!accepts(slot,value))throw new IllegalArgumentException("bank stock");stock=value;}
        public boolean accepts(int slot,Stack value){return slot==0&&(value.empty()||kind(value)>=0)&&value.count()<=value.maximum();}
    }
    public record View(float[] observation,boolean[] mask) {
        public View {observation=observation.clone();mask=mask.clone();}
        @Override public float[] observation(){return observation.clone();}
        @Override public boolean[] mask(){return mask.clone();}
    }
    public record Outcome(int first,int[] initial,int[] serviceSteps,int[] consumed,int[] bank,
                          int[] remaining,int[] deposited,int[] withdrawn,int wrongDeposits) {
        public Outcome {
            initial=initial.clone();serviceSteps=serviceSteps.clone();consumed=consumed.clone();bank=bank.clone();
            remaining=remaining.clone();deposited=deposited.clone();withdrawn=withdrawn.clone();
        }
        @Override public int[] initial(){return initial.clone();}
        @Override public int[] serviceSteps(){return serviceSteps.clone();}
        @Override public int[] consumed(){return consumed.clone();}
        @Override public int[] bank(){return bank.clone();}
        @Override public int[] remaining(){return remaining.clone();}
        @Override public int[] deposited(){return deposited.clone();}
        @Override public int[] withdrawn(){return withdrawn.clone();}
    }
    /** Codes are material*2 + private slot; these codes are evidence, never policy input. */
    public SupplyRoom(int first,int[] initial){
        if(first<0||first>1||initial.length<2||initial.length>64||initial.length%2!=0)
            throw new IllegalArgumentException("room bounds");
        int[] counts=new int[2];for(int code:initial){if(code<0||code>3)throw new IllegalArgumentException("initial code");counts[code/2]++;}
        if(counts[0]!=initial.length/2||counts[1]!=initial.length/2)throw new IllegalArgumentException("complementary supplies");
        this.first=first;this.initial=initial.clone();pockets=new Pocket[initial.length];
        deposited=new int[initial.length*2];withdrawn=new int[deposited.length];
        for(int i=0;i<pockets.length;i++){
            pockets[i]=new Pocket();pockets[i].open(Pocket.Menu.CHEST);
            pockets[i].setStorage(initial[i]%2,new Stack(MATERIAL[initial[i]/2],1));
        }
        verifyConservation();
    }
    public int members(){return pockets.length;}
    public int step(){return step;}
    public int demand(){return phase==2?-1:first^phase;}
    private static int kind(Stack stock){return stock.item().equals(MATERIAL[0])?0:stock.item().equals(MATERIAL[1])?1:-1;}
    private Pocket member(int member){if(member<0||member>=pockets.length)throw new IllegalArgumentException("member");return pockets[member];}
    public View view(int member,boolean visible){
        Pocket p=member(member);float[] x=new float[Schema.INPUTS];x[0]=1;
        // Deliberately NOT a Minecraft observation. The task columns stay zero.
        for(int slot=0;slot<36;slot++){
            Stack s=p.storage(slot);int kind=kind(s);
            if(kind>=0){x[50+slot*3+kind]=1;x[52+slot*3]=s.count()/64f;}
        }
        int bankKind=kind(bank.stock);if(bankKind>=0){x[200+bankKind]=1;x[202]=bank.stock.count()/64f;}
        x[203]=members()/64f;x[204]=(HORIZON-step)/(float)HORIZON;x[205]=visible?1:0;
        if(visible&&demand()>=0)x[206+demand()]=1;
        boolean[] mask=new boolean[Schema.DISTRIBUTION];
        for(int head=0;head<7;head++)mask[Task.offset(head)+Schema.IDLE[head]]=true;
        for(int slot:new int[]{0,1,36})if(p.wouldChange(3,slot,bank)){
            mask[Task.offset(6)+3]=true;mask[Schema.slotOffset(3)+slot]=true;
        }
        return new View(x,mask);
    }
    public static int[] action(int code){
        if(code<0||code>3)throw new IllegalArgumentException("primitive code");
        int[] action=Schema.IDLE.clone();if(code>0){action[6]=3;action[7]=code==3?36:code-1;}return action;
    }
    public static int code(int[] action){
        Schema.checkAction(action);
        for(int h=0;h<6;h++)if(action[h]!=Schema.IDLE[h])throw new IllegalArgumentException("non-inventory primitive");
        if(action[6]==0)return 0;
        if(action[6]!=3||action[7]!=0&&action[7]!=1&&action[7]!=36)throw new IllegalArgumentException("non-shift primitive");
        return action[7]==36?3:action[7]+1;
    }
    /** All selections are already made; current Pocket.click rechecks stale feasibility. */
    public float advance(int[][] selected,int[] order){
        if(step>=HORIZON||selected.length!=members()||order.length!=members())throw new IllegalArgumentException("step boundary");
        boolean[] seen=new boolean[members()];
        for(int i:order){if(i<0||i>=members()||seen[i])throw new IllegalArgumentException("order permutation");seen[i]=true;}
        for(int[] a:selected)code(a); // Validate every request before any stock mutation.
        int demand=demand();
        for(int i:order){
            Pocket p=pockets[i];int[] before={p.count(MATERIAL[0]),p.count(MATERIAL[1])};
            p.click(selected[i][6],selected[i][7],bank);
            for(int k=0;k<2;k++){
                int delta=before[k]-p.count(MATERIAL[k]);
                if(delta>0){deposited[i*2+k]+=delta;if(k!=demand)wrong+=delta;}
                else withdrawn[i*2+k]-=delta;
            }
            verifyConservation();
        }
        step++;
        if(demand>=0&&kind(bank.stock)==demand&&bank.stock.count()==members()/2){
            served[demand]+=bank.stock.count();bank.stock=Stack.EMPTY;serviceSteps[demand]=step;phase++;
            verifyConservation();return 1;
        }
        verifyConservation();return 0;
    }
    public void verifyConservation(){
        for(int k=0;k<2;k++){
            int total=served[k]+(kind(bank.stock)==k?bank.stock.count():0);
            int deposit=0,withdraw=0;
            for(int i=0;i<members();i++){
                Pocket p=pockets[i];int own=p.count(MATERIAL[k]);total+=own;deposit+=deposited[i*2+k];withdraw+=withdrawn[i*2+k];
                if(own!=(initial[i]/2==k?1:0)-deposited[i*2+k]+withdrawn[i*2+k])throw new IllegalStateException("member material balance");
                if(!p.crafted.isEmpty()||!p.extracted.isEmpty()||!p.cursor().empty()||p.menu()!=Pocket.Menu.CHEST)throw new IllegalStateException("invented crafting/cursor/menu");
                for(int slot=0;slot<36;slot++)if(p.storage(slot).count()>p.storage(slot).maximum())throw new IllegalStateException("stack overflow");
            }
            if(total!=members()/2||deposit-withdraw-served[k]!=(kind(bank.stock)==k?bank.stock.count():0))throw new IllegalStateException("community material balance");
        }
    }
    /** Can inspect incomplete evidence, but only horizon-complete records are study trials. */
    public Outcome snapshot(){
        int[] remaining=new int[members()*2];for(int i=0;i<members();i++)for(int k=0;k<2;k++)remaining[i*2+k]=pockets[i].count(MATERIAL[k]);
        int[] stock=new int[2];if(kind(bank.stock)>=0)stock[kind(bank.stock)]=bank.stock.count();
        return new Outcome(first,initial,serviceSteps,served,stock,remaining,deposited,withdrawn,wrong);
    }
}
