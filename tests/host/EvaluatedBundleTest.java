import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import org.botsclustersmc.core.*;

/** Synthetic integrity fixtures, never an assertion of measured Minecraft competence. */
public final class EvaluatedBundleTest {
    private static int checks;
    private static void check(boolean condition,String message) {
        checks++;if(!condition)throw new AssertionError(message);
    }
    interface Failing {void run()throws Exception;}
    private static void reject(Failing action)throws Exception {
        boolean failed=false;try{action.run();}catch(IOException|IllegalArgumentException expected){failed=true;}
        check(failed,"invalid or conflicting artifact must fail closed");
    }
    private static String hash(byte[] bytes)throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    public static void main(String[] args)throws Exception {
        byte[] model=PolicyFile.encode(new Policy(new float[Policy.PARAMETERS],7,64));
        byte[] jar="Synthetic bytes for identity checking, not a deployable plugin".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        JsonObject report=EvaluationTest.valid(23);report.addProperty("policy_sha256",hash(model));
        report.addProperty("inference_jar_sha256",hash(jar));String original=report.toString();
        Path directory=Files.createTempDirectory("bcmc-evaluated-artifact-");
        try {
            Path academy=directory.resolve("academy"),artifact=directory.resolve("measured.zip");Files.createDirectories(academy);
            EvaluatedBundle.write(artifact,academy,model,jar,report);
            check(original.equals(report.toString()),"packaging cannot change the validated trial report");
            Map<String,byte[]> contents=new HashMap<>();
            try(ZipInputStream zip=new ZipInputStream(Files.newInputStream(artifact))) {
                for(ZipEntry e;(e=zip.getNextEntry())!=null;)contents.put(e.getName(),zip.readAllBytes());
            }
            check(contents.keySet().equals(Set.of("README.txt","evaluation.json","evaluation-details.json",
                "plugins/botsclustersmc.jar","plugins/BotsClustersMC/policy.bcmc")),"only the five intended entries are exported");
            check(Arrays.equals(contents.get("plugins/BotsClustersMC/policy.bcmc"),model),"exact tested policy bytes");
            check(Arrays.equals(contents.get("plugins/botsclustersmc.jar"),jar),"exact tested inference bytes");
            JsonObject details=JsonParser.parseString(new String(contents.get("evaluation-details.json"),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            check(details.equals(report)&&details.getAsJsonArray("trials").size()==4,"all successes AND failures remain in the artifact");
            JsonObject summary=JsonParser.parseString(new String(contents.get("evaluation.json"),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            check(!summary.has("trials")&&summary.getAsJsonArray("tasks").equals(report.getAsJsonArray("tasks")),"summary denominators unchanged");
            byte[] existing=Files.readAllBytes(artifact);
            reject(()->EvaluatedBundle.write(artifact,academy,model,jar,report));
            check(Arrays.equals(existing,Files.readAllBytes(artifact)),"existing artifact never overwritten");
            reject(()->EvaluatedBundle.write(academy.resolve("inside.zip"),academy,model,jar,report));
            reject(()->EvaluatedBundle.write(directory.resolve("wrong.jar"),academy,model,jar,report));
            Path refused=directory.resolve("refused.zip");
            JsonObject wrong=report.deepCopy();wrong.addProperty("policy_sha256","wrong");
            reject(()->EvaluatedBundle.write(refused,academy,model,jar,wrong));
            wrong.addProperty("policy_sha256",hash(model));wrong.addProperty("inference_jar_sha256","wrong");
            reject(()->EvaluatedBundle.write(refused,academy,model,jar,wrong));
            JsonObject inconsistent=report.deepCopy();inconsistent.getAsJsonArray("tasks").get(0).getAsJsonObject().addProperty("passed",2);
            reject(()->EvaluatedBundle.write(refused,academy,model,jar,inconsistent));
            byte[] corrupt=model.clone();corrupt[corrupt.length-1]^=1;
            reject(()->EvaluatedBundle.write(refused,academy,corrupt,jar,report));
            check(!Files.exists(refused),"invalid data cannot leave an apparently usable artifact");
            try(var paths=Files.list(directory)){check(paths.noneMatch(p->p.getFileName().toString().startsWith(".evaluated-")),"temporary files cleaned");}
            Evaluate.Options options=Evaluate.Options.parse(new String[]{"--export",artifact.toString()});
            check(options.export().equals(artifact)&&!options.watch(),"explicit one-shot export option");
            reject(()->Evaluate.Options.parse(new String[]{"--watch","--export",artifact.toString()}));
            reject(()->Evaluate.Options.parse(new String[]{"--export",artifact.toString(),"--export",artifact.toString()}));
        } finally {Host.deleteTree(directory);}
        System.out.println("PASS exact evaluated artifact checks="+checks);
    }
}
