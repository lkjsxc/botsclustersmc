package org.botsclustersmc.core;

import java.util.*;

/** NPC inventory mechanics. No action chooses a recipe, ingredient, tool, or destination. */
public final class Pocket {
    public enum Menu {CLOSED,INVENTORY,WORKBENCH,FURNACE,CHEST}
    public interface External {int size();Stack get(int slot);void set(int slot,Stack stack);boolean accepts(int slot,Stack stack);}
    public static final External NONE=new External(){public int size(){return 0;}public Stack get(int i){return Stack.EMPTY;}public void set(int i,Stack s){throw new IllegalArgumentException("no external slot");}public boolean accepts(int i,Stack s){return false;}};
    private final Stack[] storage=new Stack[36],grid=new Stack[9];
    private Stack cursor=Stack.EMPTY;private Menu menu=Menu.CLOSED;private int selected;
    public final Map<String,Long> crafted=new HashMap<>(),extracted=new HashMap<>();
    public Pocket(){clear();}
    public void clear(){Arrays.fill(storage,Stack.EMPTY);Arrays.fill(grid,Stack.EMPTY);cursor=Stack.EMPTY;menu=Menu.CLOSED;selected=0;crafted.clear();extracted.clear();}
    public Menu menu(){return menu;}public Stack cursor(){return cursor;}public int selected(){return selected;}
    public void select(int slot){if(slot<0||slot>=9)throw new IllegalArgumentException("hotbar");selected=slot;}
    public Stack held(){return storage[selected];}
    public void consumeHeld(int n){Stack s=held();if(n<0||n>s.count())throw new IllegalArgumentException("consume");storage[selected]=s.withCount(s.count()-n);}
    public void setStorage(int index,Stack s){if(index<0||index>=36)throw new IllegalArgumentException("storage");storage[index]=s;}
    public Stack storage(int i){return storage[i];}
    public void open(Menu target){menu=Objects.requireNonNull(target);}
    public void close(){menu=Menu.CLOSED;}
    public int slots(){return switch(menu){case CLOSED->0;case INVENTORY->41;case WORKBENCH->46;case FURNACE->39;case CHEST->63;};}
    public int count(String item){int n=cursor.item().equals(item)?cursor.count():0;for(Stack s:storage)if(s.item().equals(item))n+=s.count();for(Stack s:grid)if(s.item().equals(item))n+=s.count();return n;}
    public int countKind(int kind){int n=Stack.kind(cursor.item())==kind?cursor.count():0;for(Stack s:storage)if(Stack.kind(s.item())==kind)n+=s.count();for(Stack s:grid)if(Stack.kind(s.item())==kind)n+=s.count();return n;}
    public int capacity(Stack s){int n=0;for(Stack t:storage)if(t.empty())n+=s.maximum();else if(t.item().equals(s.item()))n+=Math.max(0,t.maximum()-t.count());return n;}
    public Stack insert(Stack s){int remaining=s.count();for(int pass=0;pass<2;pass++)for(int i=0;i<storage.length&&remaining>0;i++){
        Stack t=storage[i];if((pass==0&&!t.empty()&&t.item().equals(s.item()))||(pass==1&&t.empty())){int moved=Math.min(remaining,s.maximum()-t.count());storage[i]=new Stack(s.item(),t.count()+moved);remaining-=moved;}}
        return s.withCount(remaining);
    }
    private int gridIndex(int slot){int j=slot-36;return menu==Menu.INVENTORY?(j/2)*3+j%2:j;}
    private int resultSlot(){return menu==Menu.INVENTORY?40:menu==Menu.WORKBENCH?45:-1;}
    public Stack get(int slot,External external){
        if(slot<0||slot>=slots())return Stack.EMPTY;if(slot<36)return storage[slot];
        if(menu==Menu.INVENTORY||menu==Menu.WORKBENCH){if(slot==resultSlot()){Recipes.Recipe r=Recipes.match(grid,menu==Menu.INVENTORY?2:3);return r==null?Stack.EMPTY:r.output();}return grid[gridIndex(slot)];}
        return slot-36<external.size()?external.get(slot-36):Stack.EMPTY;
    }
    private boolean accepts(int slot,Stack stack,External external){if(slot<36)return true;if(slot==resultSlot())return false;if(menu==Menu.INVENTORY||menu==Menu.WORKBENCH)return true;return slot-36<external.size()&&external.accepts(slot-36,stack);}
    private void set(int slot,Stack s,External external){if(slot<36)storage[slot]=s;else if(menu==Menu.INVENTORY||menu==Menu.WORKBENCH)grid[gridIndex(slot)]=s;else external.set(slot-36,s);}
    public void click(int operation,int slot,External external){
        if(operation==0)return;if(operation==4){if(menu==Menu.CLOSED)open(Menu.INVENTORY);return;}if(operation==5){close();return;}
        if(operation<1||operation>3||slot<0||slot>=slots()||menu==Menu.CLOSED)return;
        if(slot==resultSlot()){craft(operation);return;}
        Stack original=get(slot,external),s=original;
        if(operation==3){
            if(slot>=36){Stack remaining=insert(s);set(slot,remaining,external);}
            else if(menu==Menu.CHEST||menu==Menu.FURNACE){
                int remaining=s.count();for(int i=0;i<external.size()&&remaining>0;i++){Stack t=external.get(i);if(external.accepts(i,s)&&(t.empty()||t.item().equals(s.item()))){int n=Math.min(remaining,s.maximum()-t.count());if(n>0){external.set(i,new Stack(s.item(),t.count()+n));remaining-=n;}}}storage[slot]=s.withCount(remaining);
            }else{
                int from=slot<9?9:0,to=slot<9?36:9,remaining=s.count();for(int i=from;i<to&&remaining>0;i++){Stack t=storage[i];if(t.empty()||t.item().equals(s.item())){int n=Math.min(remaining,s.maximum()-t.count());if(n>0){storage[i]=new Stack(s.item(),t.count()+n);remaining-=n;}}}storage[slot]=s.withCount(remaining);
            }
        }else if(cursor.empty()){
            int n=operation==2?(s.count()+1)/2:s.count();if(n>0){cursor=s.withCount(n);set(slot,s.withCount(s.count()-n),external);}
        }else if(accepts(slot,cursor,external)){
            if(s.empty()||s.item().equals(cursor.item())){int n=Math.min(operation==2?1:cursor.count(),cursor.maximum()-s.count());if(n>0){set(slot,new Stack(cursor.item(),s.count()+n),external);cursor=cursor.withCount(cursor.count()-n);}}
            else if(operation==1){set(slot,cursor,external);cursor=s;}
        }
        if(menu==Menu.FURNACE&&slot==38){int removed=original.count()-get(slot,external).count();if(removed>0)extracted.merge(original.item(),(long)removed,Long::sum);}
    }
    private void craft(int operation){
        Recipes.Recipe r=Recipes.match(grid,menu==Menu.INVENTORY?2:3);if(r==null)return;Stack out=r.output();
        if(operation==3){if(capacity(out)<out.count())return;insert(out);}
        else {if(!cursor.empty()&&(!cursor.item().equals(out.item())||cursor.count()+out.count()>out.maximum()))return;cursor=new Stack(out.item(),cursor.count()+out.count());}
        for(int i:r.consume())grid[i]=grid[i].withCount(grid[i].count()-1);crafted.merge(out.item(),(long)out.count(),Long::sum);
    }
}
