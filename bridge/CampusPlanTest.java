import org.botsclustersmc.lab.*;
import java.util.*;
public final class CampusPlanTest {
    private static Protocol.Request task(int id,int stage) {
        int x=CampusPlan.ox(id),z=CampusPlan.oz(id);
        return new Protocol.Request("r","0-0",id,stage,x+8,97,z+5,0,0,x+8.5,stage==2||stage==5?98.5:97,z+11.5);
    }
    public static void main(String[] args) {
        int positions=0,transitions=0;
        for(int id=0;id<32;id++) {
            Map<CampusPlan.Pos,String> base=new HashMap<>();
            for(var e:CampusPlan.base(id)) {
                var p=e.pos();
                if((p.x()>>4)!=(CampusPlan.ox(id)>>4)||(p.z()>>4)!=(CampusPlan.oz(id)>>4)) throw new AssertionError("cross chunk");
                if(base.put(p,e.material())!=null) throw new AssertionError("duplicate position");
                positions++;
            }
            if(base.size()!=2304) throw new AssertionError("size");
            for(int x=2;x<=13;x++) for(int z=2;z<=13;z++) for(int y=97;y<=103;y++)
                if(!base.get(new CampusPlan.Pos(CampusPlan.ox(id)+x,y,CampusPlan.oz(id)+z)).equals("AIR")) throw new AssertionError("interior blocked");
            for(int before=0;before<6;before++) for(int after=0;after<6;after++) {
                var a=task(id,before);var b=task(id,after);var result=new HashMap<>(base);result.putAll(CampusPlan.overlay(a));
                var edits=CampusPlan.reset(a,b);if(edits.size()>26) throw new AssertionError("reset bound");
                for(var e:edits) result.put(e.pos(),e.material());
                var expected=new HashMap<>(base);expected.putAll(CampusPlan.overlay(b));
                if(!result.equals(expected)) throw new AssertionError("reset mismatch");transitions++;
            }
        }
        if(!CampusPlan.manifest("r",32).endsWith("31 112 48\n")) throw new AssertionError("layout");
        System.out.println("PASS: actual pure planner: "+positions+" positions, "+transitions+" reset transitions. Not a Folia integration test.");
    }
}
