package org.botsclustersmc.tests;

import java.util.*;
import org.botsclustersmc.commons.*;
import org.botsclustersmc.core.Pocket;
import org.botsclustersmc.core.Stack;

/** Pure mechanics/accounting tests. None of these scripted clicks is learned behavior. */
public final class CommonsTest {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static void reject(Runnable r){try{r.run();throw new AssertionError("Accepted impossible evidence");}catch(IllegalArgumentException expected){checks++;}}
    static final class Bank implements Pocket.External {
        final Stack[] slots=new Stack[27];Bank(){Arrays.fill(slots,Stack.EMPTY);}
        public int size(){return slots.length;}public Stack get(int i){return slots[i];}
        public void set(int i,Stack s){slots[i]=s;}public boolean accepts(int i,Stack s){return i>=0&&i<slots.length;}
        CommonsStock stock(){CommonsStock s=CommonsStock.EMPTY;for(Stack x:slots)s=s.plus(CommonsStock.item(x.item(),x.count()));return s;}
    }
    static CommonsStock stock(Pocket p){return new CommonsStock(p.count("OAK_PLANKS"),p.count("STICK"),p.count("WOODEN_PICKAXE"));}
    static void click(Pocket p,int operation,int slot,Bank b){p.click(operation,slot,b);}
    static Pocket[] supplies(CommonsCase c){Pocket[] p={new Pocket(),new Pocket()};for(int m=0;m<2;m++){
        if(c.planks(m)>0)p[m].setStorage(0,new Stack("OAK_PLANKS",3));if(c.sticks(m)>0)p[m].setStorage(1,new Stack("STICK",2));}return p;}
    static void assemble(Pocket p,Bank b){p.open(Pocket.Menu.WORKBENCH);
        click(p,1,0,b);for(int slot:new int[]{36,37,38})click(p,2,slot,b);
        click(p,1,1,b);click(p,2,40,b);click(p,2,43,b);click(p,3,45,b);}
    public static void main(String[] args)throws Exception{
        for(int i=0;i<32;i++)for(var condition:CommonsCase.Condition.values()){
            CommonsCase c=CommonsCase.of(i,condition,2026100711L);Pocket[] p=supplies(c);
            check(stock(p[0]).plus(stock(p[1])).equals(CommonsStock.INITIAL),"matched total supplies");
            check(c.owner()==i%2&&c.mirror()==((i/2)%2!=0),"balanced exchange and mirror");
            check(c.seed()==2026100711L+i*104729L,"common seed schedule");
            check(c.actorSeed(0)!=c.actorSeed(1),"independent actor streams");
            check(c.rooms()==(condition==CommonsCase.Condition.SPLIT_ISOLATED?2:1),"isolation boundary");
        }
        for(int swap=0;swap<2;swap++){
            CommonsCase c=CommonsCase.of(swap,CommonsCase.Condition.SPLIT_SHARED,17);Pocket[] p=supplies(c);Pocket maker=p[swap],partner=p[1-swap];Bank b=new Bank();
            // The missing sticks must remain a real dependency, even with perfect clicks.
            assemble(maker,b);check(maker.count("WOODEN_PICKAXE")==0,"cannot fabricate absent partner ingredients");
            // Recover the placed planks into the cursor/storage through literal clicks.
            for(int slot:new int[]{36,37,38})click(maker,3,slot,b);
            partner.open(Pocket.Menu.CHEST);click(partner,3,1,b);maker.open(Pocket.Menu.CHEST);click(maker,3,36,b);
            check(stock(maker).plus(stock(partner)).plus(b.stock()).woodUnits()==8,"handoff conserves resources");
            // Shift withdrawal chooses first available storage (slot 1 after the planks).
            assemble(maker,b);CommonsStock carried=stock(maker).plus(stock(partner));
            check(carried.equals(new CommonsStock(0,0,1)),"exact crafted tool");
            check(!CommonsStock.delivered(carried,b.stock(),1),"private tool is not shared delivery");
            maker.open(Pocket.Menu.CHEST);click(maker,3,0,b);
            check(CommonsStock.delivered(stock(maker).plus(stock(partner)).plus(b.stock()),b.stock(),1),"shared final product");
            for(int repeat=0;repeat<100;repeat++){
                click(partner,3,36,b);click(partner,3,0,b);
                CommonsStock total=stock(maker).plus(stock(partner)).plus(b.stock());
                check(total.woodUnits()==8&&maker.crafted.getOrDefault("WOODEN_PICKAXE",0L)==1&&partner.crafted.isEmpty(),"recirculation cannot create output or credit");
            }
        }
        for(int swap=0;swap<2;swap++){
            Pocket[] p=supplies(CommonsCase.of(swap,CommonsCase.Condition.SPLIT_ISOLATED,19));
            for(Pocket member:p){Bank own=new Bank();assemble(member,own);check(member.count("WOODEN_PICKAXE")==0,"isolated complementary actors cannot assemble");}
        }
        reject(()->CommonsStock.delivered(new CommonsStock(0,0,2),new CommonsStock(0,0,1),1));
        reject(()->CommonsStock.delivered(CommonsStock.EMPTY,new CommonsStock(0,0,1),0));
        reject(()->CommonsStock.delivered(new CommonsStock(0,0,1),new CommonsStock(0,0,1),2));
        reject(()->new CommonsStock(-1,0,0));reject(()->CommonsStock.item("DIAMOND",1));
        reject(()->new CommonsCase(32,CommonsCase.Condition.SPLIT_SHARED,0));
        try{CommonsCase.of(1,CommonsCase.Condition.SPLIT_SHARED,Long.MAX_VALUE);throw new AssertionError("seed overflow accepted");}catch(ArithmeticException expected){checks++;}
        check(!CommonsStock.delivered(CommonsStock.EMPTY,CommonsStock.EMPTY,0),"loss is a valid failure, not success");
        check(!CommonsStock.delivered(new CommonsStock(0,8,0),CommonsStock.EMPTY,0),"irreversible wrong recipe is a valid failure");
        reject(()->CommonsStock.delivered(new CommonsStock(0,0,1),new CommonsStock(0,0,1),0));
        Pocket contaminated=new Pocket();contaminated.setStorage(0,new Stack("CRAFTING_TABLE",1));reject(()->CommonsStock.carried(contaminated));
        contaminated.setStorage(0,new Stack("BIRCH_PLANKS",1));reject(()->CommonsStock.carried(contaminated));
        for(String name:List.of("botsclustersmc.jar","training.jar"))try(var jar=new java.util.jar.JarFile("dist/"+name)){
            check(jar.stream().noneMatch(e->e.getName().contains("/commons/")),"commons evaluator leaked into public runtime");
        }
        System.out.println("PASS commons cases, source conservation and completion: "+checks+" checks; scripted software only");
    }
}
