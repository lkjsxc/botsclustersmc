import com.google.gson.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import org.botsclustersmc.core.*;

/** Read only validated model bytes from a prior evaluation. Never extract or execute its JAR. */
final class ReplaySource {
    private ReplaySource() {}
    private static final int MIB=1024*1024;
    private static final Map<String,Integer> LIMITS=Map.of(
        "README.txt",16384,"evaluation.json",131072,"evaluation-details.json",2*MIB,
        "plugins/botsclustersmc.jar",8*MIB,"plugins/BotsClustersMC/policy.bcmc",4*MIB);
    record Snapshot(Policy policy,List<Integer> tasks,long seed,long evaluatedAt,String inferenceHash) {
        Snapshot {tasks=List.copyOf(tasks);}
        void describe(JsonObject report) {
            report.addProperty("policy_source","evaluated-bundle");
            report.addProperty("source_evaluation_seed",seed);
            report.addProperty("source_evaluation_epoch_millis",evaluatedAt);
            report.addProperty("source_inference_jar_sha256",inferenceHash);
            report.addProperty("same_inference_build_as_source",inferenceHash.equals(report.get("inference_jar_sha256").getAsString()));
        }
    }
    private static String digest(byte[] bytes)throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    private static JsonObject object(byte[] bytes)throws IOException {
        try {
            String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            return JsonParser.parseString(text).getAsJsonObject();
        }catch(CharacterCodingException|RuntimeException malformed){throw new IOException("Malformed source evaluation JSON",malformed);}
    }
    static Snapshot read(Path requested)throws Exception {
        Path source=requested.toAbsolutePath().normalize();Host.safe(source);
        if(!Files.isRegularFile(source,LinkOption.NOFOLLOW_LINKS)||Files.size(source)>16L*MIB)
            throw new IOException("Use a regular evaluated bundle of at most 16 MiB");
        Map<String,byte[]> files=new HashMap<>();
        try(ZipFile zip=new ZipFile(source.toFile())) {
            Enumeration<? extends ZipEntry> entries=zip.entries();
            while(entries.hasMoreElements()) {
                ZipEntry entry=entries.nextElement();Integer limit=LIMITS.get(entry.getName());
                if(limit==null||entry.isDirectory()||files.containsKey(entry.getName())||entry.getSize()>limit)
                    throw new IOException("Unexpected, duplicate or oversized evaluated-bundle entry");
                try(InputStream in=zip.getInputStream(entry)) {
                    byte[] bytes=in.readNBytes(limit+1);
                    if(bytes.length>limit||bytes.length!=entry.getSize())throw new IOException("Source entry exceeds its decoded size bound");
                    CRC32 crc=new CRC32();crc.update(bytes);
                    if(crc.getValue()!=entry.getCrc())throw new IOException("Source ZIP entry checksum mismatch");
                    files.put(entry.getName(),bytes);
                }
            }
        }
        if(!files.keySet().equals(LIMITS.keySet()))throw new IOException("Evaluated bundle is missing required entries");
        byte[] model=files.get("plugins/BotsClustersMC/policy.bcmc");Policy policy=PolicyFile.decode(model);
        JsonObject details=object(files.get("evaluation-details.json"));
        try {
            List<Integer> tasks=new ArrayList<>();
            for(JsonElement element:EvaluationChecks.array(details,"tasks")) {
                int task=Math.toIntExact(EvaluationChecks.integer(element.getAsJsonObject(),"task"));Task.at(task);tasks.add(task);
            }
            int cases=Math.toIntExact(EvaluationChecks.integer(details,"cases_per_task"));
            if(tasks.isEmpty()||tasks.size()>18||new HashSet<>(tasks).size()!=tasks.size()||cases<1||cases>64)
                throw new IOException("Source evaluated-bundle case specification");
            long seed=EvaluationChecks.integer(details,"seed");
            EvaluationChecks.validate(details.toString(),policy,tasks,cases,seed,0);
            String inference=digest(files.get("plugins/botsclustersmc.jar"));
            if(!digest(model).equals(details.get("policy_sha256").getAsString())
                    ||!inference.equals(details.get("inference_jar_sha256").getAsString()))
                throw new IOException("Source model/plugin bytes disagree with the evaluation report");
            JsonObject summary=details.deepCopy();summary.remove("trials");
            if(!summary.equals(object(files.get("evaluation.json"))))throw new IOException("Source evaluation summary disagrees with full trials");
            return new Snapshot(policy,tasks,seed,EvaluationChecks.integer(details,"epoch_millis"),inference);
        }catch(RuntimeException malformed){throw new IOException("Invalid evaluated-bundle content",malformed);}
    }
}
