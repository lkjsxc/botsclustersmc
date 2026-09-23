import java.util.*;
import org.botsclustersmc.lab.*;
public final class CampusPlanTest{
    private static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        int resetPositions=0;
        for(int id=0;id<64;id++){
            int ox=CampusPlan.ox(id),oz=CampusPlan.oz(id);var base=CampusPlan.base(id);check(base.size()==2304);
            Set<CampusPlan.Pos> seen=new HashSet<>();for(var e:base){check(seen.add(e.pos()));check(e.pos().x()>=ox&&e.pos().x()<ox+16&&e.pos().z()>=oz&&e.pos().z()<oz+16);if(e.pos().y()==104)check(e.material().equals("GLASS"));}
            Protocol.Request previous=null;
            for(int stage=0;stage<18;stage++){
                var next=new Protocol.Request("run","1-"+id+"-"+stage,id,stage,ox+7.5,97,oz+7.5,0,0,ox+8.5,stage==5||stage==6||stage==12||stage==17?98.5:97.5,oz+10.5,stage,1,true);
                var overlay=CampusPlan.overlay(next);var edits=CampusPlan.reset(previous,next);check(edits.size()==2304);Set<CampusPlan.Pos> reset=new HashSet<>();
                for(var e:edits){check(reset.add(e.pos()));check(e.material().equals(overlay.getOrDefault(e.pos(),CampusPlan.baseMaterial(id,e.pos().x(),e.pos().y(),e.pos().z()))));}
                resetPositions+=edits.size();previous=next;
            }
            check(CampusPlan.placeable(id,ox+4,98,oz+4));check(!CampusPlan.placeable(id,ox+1,98,oz+4));check(!CampusPlan.placeable(id,ox+4,96,oz+4));
        }
        String manifest=CampusPlan.manifest("run",64);check(manifest.lines().count()==65);check(manifest.endsWith("63 112 112\n"));
        for(int bad:new int[]{-1,64})try{CampusPlan.base(bad);throw new AssertionError();}catch(IllegalArgumentException expected){}
        System.out.println("Campus: 64 rooms x 18 tasks, "+resetPositions+" bounded reset/readback positions passed");
    }
}
