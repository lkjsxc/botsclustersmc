import com.google.gson.*;
import java.io.IOException;
import java.util.*;
import org.botsclustersmc.core.*;

/** Validate every trial before an operator-facing result can replace the last report. */
final class EvaluationChecks {
    private EvaluationChecks() {}
    static long integer(JsonObject o,String name)throws IOException {
        JsonElement value=o.get(name);
        if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IOException("Missing numeric field: "+name);
        try{return value.getAsBigDecimal().longValueExact();}catch(ArithmeticException|NumberFormatException e){throw new IOException("Invalid integer: "+name,e);}
    }
    static boolean bool(JsonObject o,String name)throws IOException {
        JsonElement value=o.get(name);
        if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isBoolean())throw new IOException("Missing boolean: "+name);
        return value.getAsBoolean();
    }
    static JsonArray array(JsonObject o,String name)throws IOException {
        JsonElement value=o.get(name);if(value==null||!value.isJsonArray())throw new IOException("Missing array: "+name);return value.getAsJsonArray();
    }
    static void require(boolean condition,String message)throws IOException{if(!condition)throw new IOException(message);}
    static JsonObject validate(String text,Policy policy,List<Integer> tasks,int cases,long seed,long started)throws IOException {
        require(text.length()<=2*1024*1024,"Evaluation report exceeds the size bound");
        try {
            JsonObject report=JsonParser.parseString(text).getAsJsonObject();
            require(bool(report,"complete")&&bool(report,"stochastic"),"Incomplete or non-stochastic evaluation");
            require(Schema.ID.equals(report.get("schema").getAsString()),"Evaluation schema differs");
            require(integer(report,"new_training_samples")==0,"Evaluation performed training");
            require(integer(report,"policy_updates")==policy.updates()&&integer(report,"policy_trained_samples")==policy.samples(),"Evaluation used a different policy identity");
            require(integer(report,"cases_per_task")==cases&&integer(report,"seed")==seed,"Evaluation case specification differs");
            long ended=integer(report,"epoch_millis");require(ended>=started&&ended<=System.currentTimeMillis()+10000,"Evaluation timestamp is invalid");
            JsonArray summaries=array(report,"tasks"),trials=array(report,"trials");
            int count=Math.multiplyExact(cases,tasks.size());
            require(summaries.size()==tasks.size()&&trials.size()==count,"Evaluation omitted trials");
            BitSet seen=new BitSet(count);int[] successes=new int[tasks.size()];
            for(JsonElement element:trials) {
                JsonObject trial=element.getAsJsonObject();long actor=integer(trial,"actor");
                require(actor>=0&&actor<count&&!seen.get((int)actor),"Duplicate or invalid trial identity");seen.set((int)actor);
                int group=(int)actor/cases,task=tasks.get(group);
                require(integer(trial,"task")==task,"Trial belongs to the wrong task");
                require(integer(trial,"seed")==seed+task*1000003L+(actor%cases)*104729L,"Trial seed differs");
                require(integer(trial,"elapsed_ticks")>0,"A trial did not run");
                JsonElement d=trial.get("distance");require(d!=null&&d.isJsonPrimitive()&&d.getAsJsonPrimitive().isNumber(),"Invalid distance type");double distance=d.getAsDouble();require(Double.isFinite(distance)&&distance>=0,"Invalid final distance");
                if(bool(trial,"success"))successes[group]++;
            }
            for(int i=0;i<tasks.size();i++) {
                JsonObject summary=summaries.get(i).getAsJsonObject();
                require(integer(summary,"task")==tasks.get(i)&&integer(summary,"cases")==cases,"Task denominator differs");
                require(integer(summary,"passed")==successes[i],"Summary disagrees with actual trials");
                require(Task.at(tasks.get(i)).label().equals(summary.get("label").getAsString()),"Task label differs");
            }
            return report;
        }catch(JsonParseException|IllegalStateException|UnsupportedOperationException|NullPointerException|ArithmeticException e){throw new IOException("Malformed evaluation report",e);}
    }
}
