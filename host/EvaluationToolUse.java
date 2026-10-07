import com.google.gson.*;
import java.io.IOException;
import java.util.*;

/** Independent native verification of tool input probability/flow evidence, not competence. */
final class EvaluationToolUse {
    private EvaluationToolUse() {}
    private static final String SCOPE="tool-use-pre-action-not-contact";
    private static final Map<String,Integer> COUNTS=Map.of("state_visits",4,"state_transitions",16,
        "visible_pick_location_states",5,"closed_cascade_selections",4,"open_gui_selections",6);
    private static final Map<String,Integer> MASSES=Map.of("closed_cascade_probability_sums",4,"open_gui_probability_sums",6);
    private static void require(boolean ok,String why)throws IOException{EvaluationChecks.require(ok,"Tool diagnostics: "+why);}
    private static JsonObject object(JsonObject o,String key)throws IOException {
        require(o.has(key)&&o.get(key).isJsonObject(),key);return o.getAsJsonObject(key);
    }
    private static long count(JsonObject o,String key,long max)throws IOException {
        long n=EvaluationChecks.integer(o,key);require(n>=0&&n<=max,key);return n;
    }
    private static double mass(JsonObject o,String key,double max)throws IOException {
        JsonElement v=o.get(key);require(v!=null&&v.isJsonPrimitive()&&v.getAsJsonPrimitive().isNumber(),key);
        double n=v.getAsDouble();require(Double.isFinite(n)&&n>=0&&n<=max+1e-7,key);return n;
    }
    private static long[] counts(JsonObject o,String key,int size,long max)throws IOException {
        JsonArray a=EvaluationChecks.array(o,key);require(a.size()==size,key);long[] result=new long[size];
        for(int i=0;i<size;i++){JsonObject v=new JsonObject();v.add("n",a.get(i));result[i]=count(v,"n",max);}return result;
    }
    private static double[] masses(JsonObject o,String key,int size,double max)throws IOException {
        JsonArray a=EvaluationChecks.array(o,key);require(a.size()==size,key);double[] result=new double[size];
        for(int i=0;i<size;i++){JsonObject v=new JsonObject();v.add("n",a.get(i));result[i]=mass(v,"n",max);}return result;
    }
    static void validate(JsonObject t,long observations)throws IOException {
        JsonElement scope=t.get("scope");require(scope!=null&&scope.isJsonPrimitive()&&scope.getAsJsonPrimitive().isString()
            &&scope.getAsString().equals(SCOPE),"scope");
        require(observations>0&&observations<=Integer.MAX_VALUE,"observations");
        require(count(t,"transitions",observations)==observations,"transition denominator");
        int first=(int)count(t,"initial_state",3),last=(int)count(t,"final_state",3);
        long[] visits=counts(t,"state_visits",4,observations),flow=counts(t,"state_transitions",16,observations);
        require(Arrays.stream(visits).sum()==observations,"state denominator");
        for(int i=0;i<4;i++) {
            long out=0,in=0;for(int j=0;j<4;j++){out+=flow[4*i+j];in+=flow[4*j+i];}
            require(out==visits[i],"transition row");
            require(out-in==(first==i?1:0)-(last==i?1:0),"flow conservation");
        }
        boolean[] reached=new boolean[4];reached[first]=true;
        for(int pass=0;pass<4;pass++)for(int i=0;i<4;i++)if(reached[i])
            for(int j=0;j<4;j++)if(flow[i*4+j]>0)reached[j]=true;
        for(int i=0;i<4;i++)require(reached[i]||visits[i]==0,"disconnected flow");
        long[] location=counts(t,"visible_pick_location_states",5,observations);long visible=observations-location[4];
        long total=0;for(int i=0;i<4;i++){require(location[i]<=visible,"visible union");total+=location[i];}
        require(total>=visible&&location[0]>=visits[1]+visits[3],"visible/selected tool");
        long closed=visits[0]+visits[1],open=observations-closed;
        long hotbar=count(t,"closed_hotbar_pick_states",Math.min(closed,location[0]));
        require(hotbar>=visits[1],"closed selected tool");
        long[] selected=counts(t,"closed_cascade_selections",4,closed);
        double[] p=masses(t,"closed_cascade_probability_sums",4,closed);
        for(int i=1;i<4;i++)require(selected[i]<=selected[i-1]&&p[i]<=p[i-1]+1e-7,"nested closed inputs");
        require(selected[2]<=hotbar&&p[2]<=hotbar+1e-7,"tool input opportunity");
        long[] gui=counts(t,"open_gui_selections",6,open);double[] q=masses(t,"open_gui_probability_sums",6,open);
        require(Arrays.stream(gui).sum()==open&&Math.abs(Arrays.stream(q).sum()-open)<=1e-7,"open operation denominator");
        require(gui[4]==0&&q[4]==0,"already-open input");
        count(t,"open_selected_pick_close_selections",Math.min(visits[3],gui[5]));
        require(mass(t,"open_selected_pick_close_probability_sum",visits[3])<=q[5]+1e-7,"close subset");
    }
    private static JsonArray zeros(int size,boolean integral) {
        JsonArray result=new JsonArray();for(int i=0;i<size;i++){if(integral)result.add(0L);else result.add(0.0);}return result;
    }
    /** Missing historical diagnostic data stay unrecorded, never fabricated zeros. */
    static void enrich(JsonObject report)throws IOException {
        for(JsonElement entry:EvaluationChecks.array(report,"trials")) {
            JsonObject trial=entry.getAsJsonObject();
            if(EvaluationChecks.integer(trial,"task")!=12&&trial.has("diagnostics"))
                require(!object(trial,"diagnostics").has("tool_use"),"unexpected task trace");
        }
        for(JsonElement entry:EvaluationChecks.array(report,"tasks")) {
            JsonObject task=entry.getAsJsonObject();
            if(EvaluationChecks.integer(task,"task")!=12){require(!task.has("tool_use"),"unexpected task aggregate");continue;}
            long expected=EvaluationChecks.integer(task,"cases"),recorded=0;
            JsonObject sums=new JsonObject();
            COUNTS.forEach((k,n)->sums.add(k,zeros(n,true)));MASSES.forEach((k,n)->sums.add(k,zeros(n,false)));
            for(String k:List.of("transitions","closed_hotbar_pick_states","open_selected_pick_close_selections"))sums.addProperty(k,0L);
            sums.addProperty("open_selected_pick_close_probability_sum",0.0);
            for(JsonElement item:EvaluationChecks.array(report,"trials")) {
                JsonObject trial=item.getAsJsonObject();if(EvaluationChecks.integer(trial,"task")!=12||!trial.has("diagnostics"))continue;
                JsonObject detail=object(trial,"diagnostics");if(!detail.has("tool_use"))continue;
                JsonObject trace=object(detail,"tool_use");
                long n=count(detail,"observations",EvaluationChecks.integer(trial,"elapsed_ticks"));validate(trace,n);recorded++;
                for(var e:COUNTS.entrySet())for(int i=0;i<e.getValue();i++) {
                    JsonArray dest=sums.getAsJsonArray(e.getKey());
                    dest.set(i,new JsonPrimitive(Math.addExact(dest.get(i).getAsLong(),trace.getAsJsonArray(e.getKey()).get(i).getAsLong())));
                }
                for(var e:MASSES.entrySet())for(int i=0;i<e.getValue();i++) {
                    JsonArray dest=sums.getAsJsonArray(e.getKey());dest.set(i,new JsonPrimitive(dest.get(i).getAsDouble()+trace.getAsJsonArray(e.getKey()).get(i).getAsDouble()));
                }
                for(String k:List.of("transitions","closed_hotbar_pick_states","open_selected_pick_close_selections"))
                    sums.addProperty(k,Math.addExact(sums.get(k).getAsLong(),trace.get(k).getAsLong()));
                String key="open_selected_pick_close_probability_sum";sums.addProperty(key,sums.get(key).getAsDouble()+trace.get(key).getAsDouble());
            }
            JsonObject derived=new JsonObject();derived.addProperty("scope",SCOPE);
            derived.addProperty("state",recorded==expected?"recorded":recorded==0?"not-recorded":"partial");
            derived.addProperty("recorded_trials",recorded);derived.addProperty("expected_trials",expected);
            if(recorded==expected)for(var e:sums.entrySet())derived.add(e.getKey(),e.getValue());
            if(task.has("tool_use"))require(task.get("tool_use").equals(derived),"aggregate differs from trials");
            task.add("tool_use",derived);
        }
    }
}
