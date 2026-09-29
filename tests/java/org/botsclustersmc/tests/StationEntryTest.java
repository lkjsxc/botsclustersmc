package org.botsclustersmc.tests;

import org.botsclustersmc.core.*;
import org.botsclustersmc.core.Stack;
import org.botsclustersmc.training.*;

/** Reset-only station-entry bridge. No action selection or learned-skill claim. */
public final class StationEntryTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static Course.Lesson lesson(Task task,Course.Kind kind,double difficulty,long seed){
        return new Course.Lesson(1,task,difficulty,seed,kind);
    }
    private static String state(Pocket pocket){
        StringBuilder text=new StringBuilder(pocket.menu()+":"+pocket.cursor());
        for(int slot=0;slot<64;slot++)text.append('/').append(pocket.get(slot,Pocket.NONE));
        return text+"/"+pocket.crafted+"/"+pocket.extracted;
    }
    private static Pocket stock(Task task){
        Pocket p=new Pocket();
        if(task==Task.CRAFT_WOOD_PICK||task==Task.CRAFT_STONE_PICK){
            p.setStorage(0,new Stack(task==Task.CRAFT_WOOD_PICK?"OAK_PLANKS":"COBBLESTONE",3));
            p.setStorage(1,new Stack("STICK",2));
        }else if(task==Task.SMELT_IRON){p.setStorage(0,new Stack("RAW_IRON",1));p.setStorage(1,new Stack("COAL",1));}
        else p.setStorage(0,new Stack("OAK_LOG",8));
        return p;
    }
    private static Pocket.Menu oldMenu(Course.Lesson l,RandomSource rng){
        if(l.kind()!=Course.Kind.PRACTICE||l.difficulty()>=1)return Pocket.Menu.CLOSED;
        if(StationPractice.applies(l.task()))return StationPractice.station(l.task());
        return switch(l.task()){
            case CRAFT_PLANKS,CRAFT_STICKS,CRAFT_WORKBENCH -> rng.unit()>l.difficulty()?Pocket.Menu.INVENTORY:Pocket.Menu.CLOSED;
            default -> Pocket.Menu.CLOSED;
        };
    }
    private static void frequencies(){
        double[] difficulties={0,.1,.32,.54,.55,.7,.99,1};
        for(Task task:Task.values())if(StationPractice.applies(task)){
            int[] closed=new int[difficulties.length];
            for(long seed=0;seed<8192;seed++){
                boolean wasClosed=false;
                for(int i=0;i<difficulties.length;i++){
                    double d=difficulties[i];Course.Lesson l=lesson(task,Course.Kind.PRACTICE,d,seed);
                    RandomSource rng=StationPractice.resetRandom(l);long before=rng.state();
                    Pocket.Menu menu=StationPractice.initialMenu(l,rng);
                    boolean isClosed=menu==Pocket.Menu.CLOSED;
                    check(menu==StationPractice.station(task)||isClosed,"only closed or correct station");
                    check(!wasClosed||isClosed,"per-lesson assistance decreases monotonically");
                    check(StationPractice.operation(l)==!isClosed,"label reports actual initial assistance");
                    check(before==rng.state(),"menu choice leaves ingredient/cursor reset RNG unchanged");
                    check(menu==StationPractice.initialMenu(l,new RandomSource(91)),"choice independent of caller RNG state");
                    check(menu==StationPractice.initialMenu(new Course.Lesson(999,task,d,seed,Course.Kind.PRACTICE),rng),"choice depends on lesson seed, not serial");
                    if(isClosed)closed[i]++;wasClosed=isClosed;
                }
            }
            check(closed[0]==0&&closed[closed.length-1]==8192,"zero/full assistance endpoints");
            for(int i=1;i<difficulties.length-1;i++)
                check(Math.abs(closed[i]-8192*difficulties[i])<180,"observed closed-start fraction follows difficulty");
            System.out.println("STATION ENTRY "+task+" closed="+java.util.Arrays.toString(closed));
        }
    }
    private static void resetBoundary(){
        for(Task task:Task.values())for(Course.Kind kind:Course.Kind.values())for(double d:new double[]{0,.1,.55,.9,.99,1})for(long seed=0;seed<256;seed++){
            Course.Lesson l=lesson(task,kind,d,seed);
            RandomSource actualRng=StationPractice.resetRandom(l),oldRng=StationPractice.resetRandom(l);
            Pocket.Menu selected=StationPractice.initialMenu(l,actualRng),old=oldMenu(l,oldRng);
            if(!StationPractice.applies(task)||kind!=Course.Kind.PRACTICE||d==1){
                check(selected==old&&actualRng.state()==oldRng.state(),"all other tasks and full probe/exam behavior unchanged");
                check(!StationPractice.operation(l),"no false station assistance label");continue;
            }
            Pocket actual=stock(task),reference=stock(task);String raw=state(actual);
            actual.open(selected);int missing=-1,oldMissing=-1;
            if(selected==Pocket.Menu.WORKBENCH)missing=InitialCrafting.prepare(actual,task.ordinal(),d,actualRng);
            if(selected==Pocket.Menu.CLOSED){
                check(state(actual).equals(raw)&&actual.cursor().empty(),"closed start keeps exact raw stock and empty cursor");
                check(missing==-1&&actual.crafted.isEmpty()&&actual.extracted.isEmpty(),"no recipe or earned output supplied");
                long before=actualRng.state();
                check(InitialCrafting.prepare(actual,task.ordinal(),d,actualRng)==-1&&state(actual).equals(raw)&&before==actualRng.state(),"closed menu cannot receive hidden recipe assistance");
            }else{
                reference.open(old);
                if(old==Pocket.Menu.WORKBENCH)oldMissing=InitialCrafting.prepare(reference,task.ordinal(),d,oldRng);
                check(state(actual).equals(state(reference))&&missing==oldMissing,"remaining open resets exactly reproduce original composition");
                check(actualRng.state()==oldRng.state(),"remaining open resets retain exact final ingredient RNG");
            }
            for(String item:new String[]{"OAK_PLANKS","COBBLESTONE","STICK","RAW_IRON","COAL","OAK_LOG"})
                check(actual.count(item)==stock(task).count(item),"raw resource conservation");
            check(actual.crafted.isEmpty()&&actual.extracted.isEmpty(),"reset has no completed output");
        }
    }
    public static void main(String[] args){frequencies();resetBoundary();System.out.println("PASS station entry reset checks="+checks+"; not learned-gameplay evidence");}
}
