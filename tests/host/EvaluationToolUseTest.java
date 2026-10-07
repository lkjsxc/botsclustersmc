import com.google.gson.*;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/** Native integrity/adversarial checks use a hand-counted flow independent of the observer. */
public final class EvaluationToolUseTest {
    private static int checks;
    static JsonObject fixture() {
        return JsonParser.parseString("""
            {"scope":"tool-use-pre-action-not-contact","transitions":4,"initial_state":1,"final_state":1,
             "state_visits":[0,2,0,2],"state_transitions":[0,0,0,0,0,1,0,1,0,0,0,0,0,1,0,1],
             "visible_pick_location_states":[4,0,0,0,0],"closed_hotbar_pick_states":2,
             "closed_cascade_probability_sums":[1.5,0.5,0.2,0.1],"closed_cascade_selections":[1,1,1,0],
             "open_gui_probability_sums":[0.2,0.3,0.4,0.1,0,1.0],"open_gui_selections":[1,0,0,0,0,1],
             "open_selected_pick_close_probability_sum":1.0,"open_selected_pick_close_selections":1}
            """).getAsJsonObject();
    }
    private static void check(boolean ok){checks++;if(!ok)throw new AssertionError("tool check "+checks);}
    private static void reject(Consumer<JsonObject> change)throws Exception {
        JsonObject j=fixture();change.accept(j);try{EvaluationToolUse.validate(j,4);throw new AssertionError("corrupt tool trace accepted");}
        catch(IOException expected){checks++;}
    }
    static JsonObject report() {
        JsonObject r=EvaluationTest.valid(23),task=r.getAsJsonArray("tasks").get(0).getAsJsonObject();
        JsonArray tasks=new JsonArray();task.addProperty("task",12);task.addProperty("label","mine-cobblestone");task.addProperty("cases",2);task.addProperty("passed",0);tasks.add(task);r.add("tasks",tasks);
        JsonArray trials=new JsonArray();
        for(int i=0;i<2;i++) {
            JsonObject t=new JsonObject();t.addProperty("actor",i);t.addProperty("task",12);t.addProperty("seed",23+12000036L+i*104729L);
            t.addProperty("elapsed_ticks",20);t.addProperty("success",false);t.addProperty("distance",1);
            JsonObject d=new JsonObject();d.addProperty("observations",4);d.add("tool_use",fixture());t.add("diagnostics",d);trials.add(t);
        }
        r.add("trials",trials);return r;
    }
    private static JsonObject aggregate(JsonObject r){return r.getAsJsonArray("tasks").get(0).getAsJsonObject().getAsJsonObject("tool_use");}
    public static void main(String[] args)throws Exception {
        EvaluationToolUse.validate(fixture(),4);checks++;
        for(String key:fixture().keySet())reject(j->j.remove(key));
        for(String key:List.of("transitions","initial_state","final_state","closed_hotbar_pick_states","open_selected_pick_close_selections")) {
            reject(j->j.addProperty(key,-1));reject(j->j.addProperty(key,true));reject(j->j.addProperty(key,"1"));
            reject(j->j.addProperty(key,.5));reject(j->j.addProperty(key,Long.MAX_VALUE));
        }
        for(String key:List.of("state_visits","state_transitions","visible_pick_location_states","closed_cascade_selections","open_gui_selections","closed_cascade_probability_sums","open_gui_probability_sums")) {
            reject(j->j.add(key,new JsonArray()));reject(j->j.getAsJsonArray(key).set(0,new JsonPrimitive(-1)));
            reject(j->j.getAsJsonArray(key).set(0,new JsonPrimitive("1")));reject(j->j.getAsJsonArray(key).set(0,new JsonPrimitive(true)));
        }
        reject(j->j.add("state_transitions",JsonParser.parseString("[0,0,0,0,0,2,0,0,0,0,0,0,0,0,0,2]")));
        reject(j->j.addProperty("scope","contact-probability"));reject(j->j.addProperty("initial_state",0));
        reject(j->j.getAsJsonArray("state_transitions").set(5,new JsonPrimitive(0)));
        reject(j->j.getAsJsonArray("visible_pick_location_states").set(4,new JsonPrimitive(1)));
        reject(j->j.addProperty("closed_hotbar_pick_states",1));
        reject(j->j.getAsJsonArray("closed_cascade_probability_sums").set(3,new JsonPrimitive(.21)));
        reject(j->j.getAsJsonArray("closed_cascade_selections").set(3,new JsonPrimitive(2)));
        reject(j->j.getAsJsonArray("open_gui_probability_sums").set(4,new JsonPrimitive(.1)));
        reject(j->j.getAsJsonArray("open_gui_selections").set(5,new JsonPrimitive(0)));
        reject(j->j.addProperty("open_selected_pick_close_probability_sum",1.1));
        reject(j->j.addProperty("open_selected_pick_close_probability_sum",Double.NaN));
        JsonObject report=report(),trials=report.deepCopy();EvaluationToolUse.enrich(report);
        check(aggregate(report).get("transitions").getAsInt()==8);check(aggregate(report).get("state").getAsString().equals("recorded"));
        check(report.get("trials").equals(trials.get("trials")));JsonObject saved=report.deepCopy();EvaluationToolUse.enrich(report);check(saved.equals(report));
        aggregate(report).addProperty("transitions",7);
        try{EvaluationToolUse.enrich(report);throw new AssertionError("forged aggregate accepted");}catch(IOException expected){checks++;}
        report=report();report.getAsJsonArray("trials").get(0).getAsJsonObject().getAsJsonObject("diagnostics").remove("tool_use");EvaluationToolUse.enrich(report);
        check(aggregate(report).get("state").getAsString().equals("partial")&&!aggregate(report).has("transitions"));
        report=report();for(JsonElement t:report.getAsJsonArray("trials"))t.getAsJsonObject().remove("diagnostics");EvaluationToolUse.enrich(report);
        check(aggregate(report).get("state").getAsString().equals("not-recorded")&&!aggregate(report).has("transitions"));
        report=report();report.getAsJsonArray("trials").get(0).getAsJsonObject().addProperty("task",11);
        try{EvaluationToolUse.enrich(report);throw new AssertionError("wrong task accepted");}catch(IOException expected){checks++;}
        System.out.println("PASS native tool-use evidence checks="+checks);
    }
}
