import com.google.gson.*;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import org.botsclustersmc.core.*;

/** One immutable artifact from the actual tested snapshot, never the live checkpoint. */
final class EvaluatedBundle {
    private EvaluatedBundle() {}
    static Path checkTarget(Path requested,Path academy)throws IOException {
        Path target=requested.toAbsolutePath().normalize(),owned=academy.toAbsolutePath().normalize();
        Host.safe(target);
        if(target.getFileName()==null||!target.getFileName().toString().endsWith(".zip")
                ||target.startsWith(owned)||owned.startsWith(target))
            throw new IOException("Choose a .zip file outside the Academy for the evaluated artifact");
        if(Files.exists(target,LinkOption.NOFOLLOW_LINKS))throw new FileAlreadyExistsException(target.toString());
        return target;
    }
    private static String digest(byte[] bytes)throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    static void write(Path requested,Path academy,byte[] policyBytes,byte[] inference,JsonObject report)throws Exception {
        Path target=checkTarget(requested,academy);
        if(inference.length<1||inference.length>8*1024*1024)throw new IOException("Inference artifact size bound");
        Policy policy=PolicyFile.decode(policyBytes);
        List<Integer> tasks=new ArrayList<>();
        for(JsonElement element:EvaluationChecks.array(report,"tasks"))tasks.add(Math.toIntExact(EvaluationChecks.integer(element.getAsJsonObject(),"task")));
        int cases=Math.toIntExact(EvaluationChecks.integer(report,"cases_per_task"));
        if(tasks.isEmpty()||tasks.size()>18||new HashSet<>(tasks).size()!=tasks.size()||cases<1||cases>64)
            throw new IOException("Evaluated artifact case specification");
        EvaluationChecks.validate(report.toString(),policy,tasks,cases,EvaluationChecks.integer(report,"seed"),0);
        if(!digest(policyBytes).equals(report.get("policy_sha256").getAsString())
                ||!digest(inference).equals(report.get("inference_jar_sha256").getAsString()))
            throw new IOException("The exported bytes differ from the tested artifact identities");
        JsonObject summary=report.deepCopy();summary.remove("trials");
        String readme="This contains the policy actually evaluated and the inference JAR from the same immutable build.\n"
            +"Policy update "+policy.updates()+", training samples "+policy.samples()+".\n"
            +"Completion does not mean mastery: read every success count and failed trial.\n"
            +"Only Academy rooms were tested, not general survival or cooperation.\n"
            +"Extract and copy the two plugins/ files to a stopped compatible Paper/Folia server.\n"
            +"Do not install training.jar. World edits default to false. NPCs are not logged-in players.\n"
            +"No live optimizer, Minecraft server distribution or private console credentials are included.\n";
        Files.createDirectories(target.getParent());Host.safe(target);
        Path temporary=Files.createTempFile(target.getParent(),".evaluated-",".tmp");
        try {
            try(ZipOutputStream zip=new ZipOutputStream(Files.newOutputStream(temporary))) {
                entry(zip,"README.txt",readme.getBytes(StandardCharsets.UTF_8));
                entry(zip,"evaluation.json",(summary+"\n").getBytes(StandardCharsets.UTF_8));
                entry(zip,"evaluation-details.json",(report+"\n").getBytes(StandardCharsets.UTF_8));
                entry(zip,"plugins/botsclustersmc.jar",inference);
                entry(zip,"plugins/BotsClustersMC/policy.bcmc",policyBytes);
            }
            try(FileChannel file=FileChannel.open(temporary,StandardOpenOption.WRITE)){file.force(true);}
            // Atomic create-without-replacement. Even a concurrent writer's target survives.
            // Unsupported filesystems fail closed instead of publishing a partial ZIP.
            Files.createLink(target,temporary);
        } finally {Files.deleteIfExists(temporary);}
    }
    private static void entry(ZipOutputStream zip,String name,byte[] bytes)throws IOException {
        ZipEntry entry=new ZipEntry(name);entry.setTime(0);zip.putNextEntry(entry);zip.write(bytes);zip.closeEntry();
    }
}
