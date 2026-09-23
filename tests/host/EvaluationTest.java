import com.google.gson.*;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import org.botsclustersmc.core.*;

/** Result-integrity tests. Synthetic reports are never published as measured skill. */
public final class EvaluationTest {
    private static int checks;
    private static final Policy POLICY=new Policy(new float[Policy.PARAMETERS],7,64);
    private static final List<Integer> TASKS=List.of(0,1);
    private static void check(boolean condition){checks++;if(!condition)throw new AssertionError("check "+checks);}
    static JsonObject valid(long seed) {
        JsonObject report=new JsonObject();report.addProperty("complete",true);report.addProperty("stochastic",true);
        report.addProperty("schema",Schema.ID);report.addProperty("policy_updates",7);report.addProperty("policy_trained_samples",64);
        report.addProperty("new_training_samples",0);report.addProperty("cases_per_task",2);report.addProperty("seed",seed);
        report.addProperty("epoch_millis",System.currentTimeMillis());JsonArray tasks=new JsonArray(),trials=new JsonArray();
        for(int task:TASKS) {
            JsonObject summary=new JsonObject();summary.addProperty("task",task);summary.addProperty("label",Task.at(task).label());summary.addProperty("cases",2);summary.addProperty("passed",1);tasks.add(summary);
            for(int i=0;i<2;i++) {
                JsonObject trial=new JsonObject();trial.addProperty("actor",task*2+i);trial.addProperty("task",task);
                trial.addProperty("seed",seed+task*1000003L+i*104729L);trial.addProperty("elapsed_ticks",i==0?25:600);
                trial.addProperty("distance",i==0?.25:3.0);trial.addProperty("success",i==0);trials.add(trial);
            }
        }
        report.add("tasks",tasks);report.add("trials",trials);return report;
    }
    private static void reject(Consumer<JsonObject> change)throws Exception {
        JsonObject report=valid(23);change.accept(report);
        try{EvaluationChecks.validate(report.toString(),POLICY,TASKS,2,23,1);throw new AssertionError("Invalid report accepted");}catch(IOException expected){checks++;}
    }
    public static void main(String[] args)throws Exception {
        EvaluatedBundleTest.main(args);
        for(long seed:new long[]{0,23,Long.MAX_VALUE,Long.MIN_VALUE}) {
            JsonObject accepted=EvaluationChecks.validate(valid(seed).toString(),POLICY,TASKS,2,seed,1);
            check(accepted.getAsJsonArray("trials").size()==4);
        }
        reject(r->r.addProperty("complete",false));reject(r->r.addProperty("complete","true"));
        reject(r->r.addProperty("stochastic",false));reject(r->r.addProperty("schema","foreign"));
        reject(r->r.addProperty("policy_updates",8));reject(r->r.addProperty("policy_updates","7"));
        reject(r->r.addProperty("policy_trained_samples",65));reject(r->r.addProperty("new_training_samples",1));
        reject(r->r.addProperty("seed",24));reject(r->r.addProperty("cases_per_task",3));
        reject(r->r.addProperty("epoch_millis",0));reject(r->r.addProperty("epoch_millis",Long.MAX_VALUE));
        reject(r->r.remove("trials"));reject(r->r.getAsJsonArray("trials").remove(0));
        reject(r->r.getAsJsonArray("trials").get(1).getAsJsonObject().addProperty("actor",0));
        reject(r->r.getAsJsonArray("trials").get(0).getAsJsonObject().addProperty("actor",-1));
        reject(r->r.getAsJsonArray("trials").get(0).getAsJsonObject().addProperty("task",1));
        reject(r->r.getAsJsonArray("trials").get(0).getAsJsonObject().addProperty("seed",0));
        reject(r->r.getAsJsonArray("trials").get(0).getAsJsonObject().addProperty("elapsed_ticks",0));
        reject(r->r.getAsJsonArray("trials").get(0).getAsJsonObject().addProperty("success","true"));
        reject(r->r.getAsJsonArray("trials").get(0).getAsJsonObject().addProperty("distance",-1));
        reject(r->r.getAsJsonArray("trials").get(0).getAsJsonObject().addProperty("distance","1"));
        reject(r->r.getAsJsonArray("tasks").get(0).getAsJsonObject().addProperty("passed",2));
        reject(r->r.getAsJsonArray("tasks").get(0).getAsJsonObject().addProperty("cases",1));
        reject(r->r.getAsJsonArray("tasks").get(0).getAsJsonObject().addProperty("label","completed-survival"));
        reject(r->r.getAsJsonArray("tasks").remove(0));
        check(Evaluate.Options.parse(new String[]{}).tasks().isEmpty());
        check(Evaluate.Options.parse(new String[]{"--tasks","0,5","--cases","64"}).tasks().equals(List.of(0,5)));
        check(Evaluate.Options.parse(new String[]{"--watch","--interval","60","--seed","-1"}).watch());
        try {
            Evaluate.Options.parse(new String[]{"--cases","0"});
            throw new AssertionError("Zero cases accepted");
        }catch(IllegalArgumentException expected){checks++;}
        try {
            EvaluationChecks.validate("{}",POLICY,TASKS,2,23,1);
            throw new AssertionError("Empty report accepted");
        }catch(IOException expected){checks++;}
        org.botsclustersmc.training.TrainingState fresh=new org.botsclustersmc.training.TrainingState(Policy.initialize(1),new org.botsclustersmc.training.Adam(),new org.botsclustersmc.training.Course(2,1).encode());
        check(Evaluate.reached(fresh).equals(List.of(0)));
        try {
            Evaluate.reached(new org.botsclustersmc.training.TrainingState(Policy.initialize(1),new org.botsclustersmc.training.Adam(),new byte[]{0}));
            throw new AssertionError("Invalid course accepted");
        }catch(IOException expected){checks++;}
        check(POLICY.updates()==7&&POLICY.samples()==64);
        System.out.println("PASS operator evaluation integrity checks="+checks);
    }
}
