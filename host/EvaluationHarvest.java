import com.google.gson.*;
import java.io.IOException;
import java.util.*;

/** Derive operator diagnostics from every completed trial, never from claimed aggregate counts. */
final class EvaluationHarvest {
    private EvaluationHarvest() {}
    static final String SCOPE="decision-boundary-not-every-tick";
    private static final List<String> SUMS=List.of("observations","menu_focused_selections",
        "world_dig_selections","held_pick_observations","target_pick_contact_observations",
        "target_other_contact_observations");
    static boolean applies(int task){return task==5||task==6||task==12;}
    private static void require(boolean condition,String message)throws IOException {
        EvaluationChecks.require(condition,"Harvest diagnostics: "+message);
    }
    private static JsonObject object(JsonObject parent,String key)throws IOException {
        require(parent.has(key)&&parent.get(key).isJsonObject(),"invalid "+key+" object");
        return parent.getAsJsonObject(key);
    }
    private static long count(JsonObject value,String key,long maximum)throws IOException {
        long n=EvaluationChecks.integer(value,key);
        require(n>=0&&n<=maximum,"out-of-range "+key);return n;
    }
    private static boolean text(JsonObject value,String key,String expected) {
        JsonElement item=value.get(key);
        return item!=null&&item.isJsonPrimitive()&&item.getAsJsonPrimitive().isString()
            &&item.getAsString().equals(expected);
    }
    /** Full coverage is required for an aggregate. Missing traces are not zero measurements. */
    static void enrich(JsonObject report)throws IOException {
        JsonArray summaries=EvaluationChecks.array(report,"tasks"),trials=EvaluationChecks.array(report,"trials");
        for(JsonElement element:summaries) {
            JsonObject task=element.getAsJsonObject();int id=Math.toIntExact(EvaluationChecks.integer(task,"task"));
            if(!applies(id)){require(!task.has("harvest"),"unexpected task aggregate");continue;}
            int cases=Math.toIntExact(EvaluationChecks.integer(task,"cases")),recorded=0,seen=0;
            long[] sums=new long[SUMS.size()],interactions=new long[4];
            long maxPick=0,maxOther=0,withPick=0,withContact=0,withBreak=0,withPickup=0;
            for(JsonElement entry:trials) {
                JsonObject trial=entry.getAsJsonObject();
                if(EvaluationChecks.integer(trial,"task")!=id)continue;
                seen++;
                if(!trial.has("diagnostics"))continue;
                JsonObject detail=object(trial,"diagnostics");
                if(!detail.has("harvest"))continue;
                JsonObject h=object(detail,"harvest");
                require(text(h,"scope",SCOPE),"unknown sampling scope");
                long elapsed=Math.min(Integer.MAX_VALUE,EvaluationChecks.integer(trial,"elapsed_ticks"));
                long n=count(h,"observations",elapsed);
                require(n>0&&count(detail,"observations",elapsed)==n,"observation denominator differs");
                long[] values=new long[SUMS.size()];
                for(int i=0;i<values.length;i++)values[i]=count(h,SUMS.get(i),n);
                long focused=values[1],dig=values[2],held=values[3],pick=values[4],other=values[5];
                require(dig<=n-focused&&pick<=held&&other<=n-held,"inconsistent sampled counts");
                long mp=count(h,"max_target_pick_ticks",elapsed),mo=count(h,"max_target_other_ticks",elapsed);
                require((pick==0)==(mp==0)&&(other==0)==(mo==0),"contact maxima disagree with counts");
                require(count(detail,"observed_max_target_mining_ticks",elapsed)==Math.max(mp,mo),
                    "target-contact maximum differs");
                JsonArray selected=EvaluationChecks.array(h,"interaction_selections");
                require(selected.size()==4,"interaction shape");
                long total=0;
                for(int i=0;i<4;i++) {
                    JsonObject field=new JsonObject();field.add("value",selected.get(i));
                    long value=count(field,"value",n);total=Math.addExact(total,value);
                    interactions[i]=Math.addExact(interactions[i],value);
                    if(i==1)require(count(detail,"dig_decisions",n)==value&&dig<=value,"dig selections differ");
                }
                require(total==n,"interaction denominator differs");
                long broken=count(detail,"blocks_broken",Long.MAX_VALUE);
                long collected=count(detail,"items_collected",Long.MAX_VALUE);
                for(int i=0;i<sums.length;i++)sums[i]=Math.addExact(sums[i],values[i]);
                maxPick=Math.max(maxPick,mp);maxOther=Math.max(maxOther,mo);
                if(pick>0)withPick++;if(pick+other>0)withContact++;
                if(broken>0)withBreak++;if(collected>0)withPickup++;
                recorded++;
            }
            require(seen==cases,"trial coverage differs");
            JsonObject derived=new JsonObject();
            derived.addProperty("state",recorded==cases?"recorded":recorded==0?"not-recorded":"partial");
            derived.addProperty("scope",SCOPE);derived.addProperty("recorded_trials",recorded);
            derived.addProperty("expected_trials",cases);
            if(recorded==cases) {
                for(int i=0;i<sums.length;i++)derived.addProperty(SUMS.get(i),sums[i]);
                JsonArray selected=new JsonArray();for(long value:interactions)selected.add(value);
                derived.add("interaction_selections",selected);
                derived.addProperty("max_target_pick_ticks",maxPick);
                derived.addProperty("max_target_other_ticks",maxOther);
                derived.addProperty("trials_with_pick_contact",withPick);
                derived.addProperty("trials_with_target_contact",withContact);
                derived.addProperty("trials_with_any_block_broken",withBreak);
                derived.addProperty("trials_with_any_item_collected",withPickup);
            }
            // A saved aggregate must agree too; do not silently repair a falsified report.
            if(task.has("harvest"))require(task.get("harvest").equals(derived),"aggregate differs from trials");
            task.add("harvest",derived);
        }
    }
}
