import com.google.gson.*;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import org.botsclustersmc.core.*;
import org.botsclustersmc.holdout.HarvestTrace;

/** Synthetic integrity fixtures, never gameplay or learned-skill evidence. */
public final class EvaluationHarvestTest {
    private static int checks;
    private static final List<Integer> TASKS=List.of(12,5,6,1);
    private static final Policy POLICY=new Policy(new float[Policy.PARAMETERS],7,64);
    private static void check(boolean value){checks++;if(!value)throw new AssertionError("harvest check "+checks);}
    private static JsonObject fixture() {
        JsonObject report=EvaluationTest.valid(23);JsonArray tasks=new JsonArray(),trials=new JsonArray();
        for(int group=0;group<TASKS.size();group++) {
            int task=TASKS.get(group);JsonObject summary=new JsonObject();
            summary.addProperty("task",task);summary.addProperty("label",Task.at(task).label());
            summary.addProperty("cases",2);summary.addProperty("passed",0);tasks.add(summary);
            for(int i=0;i<2;i++) {
                JsonObject trial=new JsonObject();trial.addProperty("actor",group*2+i);trial.addProperty("task",task);
                trial.addProperty("seed",23+task*1000003L+i*104729L);trial.addProperty("elapsed_ticks",100);
                trial.addProperty("distance",3);trial.addProperty("success",false);
                if(EvaluationHarvest.applies(task)) {
                    HarvestTrace trace=new HarvestTrace();
                    if(i==0) {
                        trace.observe(true,0,0,false,0);trace.observe(false,1,6,true,3);
                        for(int j=0;j<8;j++)trace.observe(false,0,0,false,0);
                    }else {trace.observe(false,1,0,true,3);trace.observe(false,1,0,true,5);}
                    JsonObject detail=new JsonObject();detail.addProperty("observations",i==0?10:2);
                    detail.addProperty("dig_decisions",i==0?1:2);
                    detail.addProperty("observed_max_target_mining_ticks",i==0?3:5);
                    detail.addProperty("blocks_broken",i==0?1:0);detail.addProperty("items_collected",0);
                    detail.add("harvest",JsonParser.parseString(trace.json()));trial.add("diagnostics",detail);
                }
                trials.add(trial);
            }
        }
        report.add("tasks",tasks);report.add("trials",trials);return report;
    }
    private static JsonObject trial(JsonObject r,int i){return r.getAsJsonArray("trials").get(i).getAsJsonObject();}
    private static JsonObject detail(JsonObject r){return trial(r,0).getAsJsonObject("diagnostics");}
    private static JsonObject trace(JsonObject r){return detail(r).getAsJsonObject("harvest");}
    private static JsonObject aggregate(JsonObject r){return r.getAsJsonArray("tasks").get(0).getAsJsonObject().getAsJsonObject("harvest");}
    private static JsonObject validate(JsonObject r)throws IOException{return EvaluationChecks.validate(r.toString(),POLICY,TASKS,2,23,1);}
    private static void reject(Consumer<JsonObject> change)throws Exception {
        JsonObject report=fixture();change.accept(report);
        try{validate(report);throw new AssertionError("Invalid harvest report accepted");}catch(IOException expected){checks++;}
    }
    public static void main(String[] args)throws Exception {
        JsonObject result=validate(fixture()),h=aggregate(result);
        check(h.get("state").getAsString().equals("recorded"));check(h.get("observations").getAsLong()==12);
        check(h.get("menu_focused_selections").getAsLong()==1);check(h.get("world_dig_selections").getAsLong()==3);
        check(h.get("held_pick_observations").getAsLong()==1);check(h.get("target_pick_contact_observations").getAsLong()==1);
        check(h.get("target_other_contact_observations").getAsLong()==2);check(h.get("max_target_pick_ticks").getAsLong()==3);
        check(h.get("max_target_other_ticks").getAsLong()==5);check(h.get("trials_with_target_contact").getAsLong()==2);
        check(h.get("trials_with_pick_contact").getAsLong()==1);check(h.get("trials_with_any_block_broken").getAsLong()==1);
        check(h.get("trials_with_any_item_collected").getAsLong()==0);check(validate(result).equals(result));
        check(result.getAsJsonArray("tasks").get(3).getAsJsonObject().get("harvest")==null);
        check(result.getAsJsonArray("trials").equals(fixture().getAsJsonArray("trials")));
        check(result.getAsJsonArray("tasks").get(0).getAsJsonObject().get("passed").getAsInt()==0);
        JsonObject missing=fixture();trial(missing,0).remove("diagnostics");
        JsonObject partial=aggregate(validate(missing));check(partial.get("state").getAsString().equals("partial"));
        check(partial.get("recorded_trials").getAsInt()==1);check(!partial.has("observations"));
        trial(missing,1).getAsJsonObject("diagnostics").remove("harvest");
        JsonObject absent=aggregate(validate(missing));check(absent.get("state").getAsString().equals("not-recorded"));
        check(absent.get("recorded_trials").getAsInt()==0);check(!absent.has("trials_with_target_contact"));
        for(String key:List.of("observations","menu_focused_selections","world_dig_selections","held_pick_observations",
                "target_pick_contact_observations","target_other_contact_observations","max_target_pick_ticks","max_target_other_ticks")) {
            reject(r->trace(r).remove(key));reject(r->trace(r).addProperty(key,-1));
            reject(r->trace(r).addProperty(key,"1"));reject(r->trace(r).addProperty(key,.5));
            reject(r->trace(r).addProperty(key,Long.MAX_VALUE));reject(r->trace(r).add(key,JsonNull.INSTANCE));
        }
        reject(r->trace(r).addProperty("observations",0));reject(r->detail(r).addProperty("observations",9));
        reject(r->trace(r).addProperty("scope","every-tick"));reject(r->trace(r).addProperty("scope",true));
        reject(r->trace(r).addProperty("held_pick_observations",0));
        reject(r->trace(r).addProperty("menu_focused_selections",10));
        reject(r->trace(r).addProperty("world_dig_selections",2));
        reject(r->trace(r).addProperty("max_target_pick_ticks",0));
        reject(r->trace(r).addProperty("max_target_other_ticks",1));
        reject(r->detail(r).addProperty("observed_max_target_mining_ticks",2));
        reject(r->trace(r).getAsJsonArray("interaction_selections").remove(0));
        reject(r->trace(r).getAsJsonArray("interaction_selections").set(0,new JsonPrimitive(8)));
        reject(r->trace(r).getAsJsonArray("interaction_selections").set(0,new JsonPrimitive("9")));
        reject(r->detail(r).addProperty("dig_decisions",2));reject(r->detail(r).addProperty("blocks_broken",-1));
        reject(r->detail(r).addProperty("items_collected","0"));
        reject(r->trial(r,0).add("diagnostics",JsonNull.INSTANCE));
        reject(r->detail(r).add("harvest",new JsonArray()));
        reject(r->{trial(r,1).remove("diagnostics");trace(r).addProperty("observations",-1);});
        reject(r->r.getAsJsonArray("tasks").get(0).getAsJsonObject().add("harvest",new JsonObject()));
        JsonObject forged=result.deepCopy();aggregate(forged).addProperty("trials_with_any_item_collected",1);
        try{validate(forged);throw new AssertionError("Falsified aggregate accepted");}catch(IOException expected){checks++;}
        check(POLICY.updates()==7&&POLICY.samples()==64);
        System.out.println("PASS harvest report derivation and integrity checks="+checks);
    }
}
