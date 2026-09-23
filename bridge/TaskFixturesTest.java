import org.botsclustersmc.lab.*;
public final class TaskFixturesTest{
    private static void check(boolean value){if(!value)throw new AssertionError();}
    private static Protocol.Request request(int stage,double d,boolean full){return new Protocol.Request("run","1-63-1",63,stage,120,97,120,0,0,120.5,97.5,122.5,42,d,full);}
    public static void main(String[] args){
        check(TaskFixtures.MATERIALS.length==13&&TaskFixtures.NAMES.length==18);
        for(int stage=0;stage<18;stage++){
            check(TaskFixtures.assistance(request(stage,1,true))==0);check(!TaskFixtures.mayOpenAtReset(request(stage,1,true)));
            check(TaskFixtures.assistance(request(stage,1,false))==0);check(!TaskFixtures.mayOpenAtReset(request(stage,1,false)));
            var supplies=TaskFixtures.supplies(stage);String output=TaskFixtures.craftOutput(stage);
            for(var s:supplies){check(s.amount()>0&&s.amount()<=64);check(TaskFixtures.itemIndex(s.material())>=0);check(!s.material().equals(output));}
            double last=100;for(double d:new double[]{0.2,0.25,0.5,0.7,0.85,1}){int count=TaskFixtures.assistance(request(stage,d,false));check(count<=last);check(count<=TaskFixtures.recipe(stage).size());last=count;}
        }
        check(TaskFixtures.supplies(17).isEmpty());check(TaskFixtures.assistance(request(17,0.2,false))==0);
        check(TaskFixtures.assistance(request(11,0.25,false))==5);check(TaskFixtures.assistance(request(8,0.25,false))==1);
        check(TaskFixtures.supplies(14).stream().noneMatch(s->s.material().equals("IRON_INGOT")));
        System.out.println("Fixtures: 18 tasks, raw-only supplies, diminishing assistance, no full-probe/exam assistance passed");
    }
}
