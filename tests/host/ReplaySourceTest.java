import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.*;
import org.botsclustersmc.core.*;

/** Archive fixtures and option checks only; no synthetic report is a learned-skill claim. */
public final class ReplaySourceTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    interface Attempt {void run()throws Exception;}
    private static void reject(Attempt action)throws Exception {
        boolean rejected=false;try{action.run();}catch(IOException|IllegalArgumentException expected){rejected=true;}
        check(rejected,"invalid replay source accepted");
    }
    private static byte[] text(String s){return s.getBytes(StandardCharsets.UTF_8);}
    private static String digest(byte[] b)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
    private static Map<String,byte[]> fixture()throws Exception {
        byte[] model=PolicyFile.encode(new Policy(new float[Policy.PARAMETERS],7,64)),jar=text("Not executable; never load me.");
        JsonObject detail=EvaluationTest.valid(23);detail.addProperty("policy_sha256",digest(model));detail.addProperty("inference_jar_sha256",digest(jar));
        JsonObject summary=detail.deepCopy();summary.remove("trials");
        return new LinkedHashMap<>(Map.of("README.txt",text("Synthetic fixture"),"evaluation.json",text(summary.toString()),
            "evaluation-details.json",text(detail.toString()),"plugins/BotsClustersMC/policy.bcmc",model,"plugins/botsclustersmc.jar",jar));
    }
    private static void write(Path path,Map<String,byte[]> files)throws IOException {
        try(ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(path))) {
            for(var entry:files.entrySet()){out.putNextEntry(new ZipEntry(entry.getKey()));out.write(entry.getValue());out.closeEntry();}
        }
    }
    private static void rejectedArchive(Path path,Consumer<Map<String,byte[]>> mutation)throws Exception {
        Map<String,byte[]> files=fixture();mutation.accept(files);write(path,files);byte[] before=Files.readAllBytes(path);
        reject(()->ReplaySource.read(path));check(Arrays.equals(before,Files.readAllBytes(path)),"rejection modified source archive");
    }
    private static void changedReport(Map<String,byte[]> files,Consumer<JsonObject> change) {
        JsonObject detail=JsonParser.parseString(new String(files.get("evaluation-details.json"),StandardCharsets.UTF_8)).getAsJsonObject();
        change.accept(detail);files.put("evaluation-details.json",text(detail.toString()));detail.remove("trials");files.put("evaluation.json",text(detail.toString()));
    }
    public static void main(String[] args)throws Exception {
        Path root=Files.createTempDirectory("bcmc-replay-source-");
        try {
            Path source=root.resolve("source.zip");Map<String,byte[]> files=fixture();write(source,files);byte[] original=Files.readAllBytes(source);
            ReplaySource.Snapshot read=ReplaySource.read(source);
            check(read.policy().updates()==7&&read.policy().samples()==64&&read.seed()==23,"source identity");
            check(read.tasks().equals(List.of(0,1))&&read.evaluatedAt()>0,"default tasks are the source task list, not fabricated course progress");
            check(Arrays.equals(PolicyFile.encode(read.policy()),files.get("plugins/BotsClustersMC/policy.bcmc")),"exact model round trip");
            check(Arrays.equals(original,Files.readAllBytes(source)),"source archive unchanged");
            boolean immutable=false;try{read.tasks().add(2);}catch(UnsupportedOperationException expected){immutable=true;}check(immutable,"task list immutable");
            JsonObject report=new JsonObject();report.addProperty("inference_jar_sha256",read.inferenceHash());read.describe(report);
            check(report.get("policy_source").getAsString().equals("evaluated-bundle")&&report.get("same_inference_build_as_source").getAsBoolean(),"provenance describes matching inference");
            report.addProperty("inference_jar_sha256","different-current-build");read.describe(report);
            check(!report.get("same_inference_build_as_source").getAsBoolean(),"current build difference disclosed, never execute archived code");
            try(var entries=Files.list(root)){check(entries.count()==1,"no archive extraction or side effects");}
            Path bad=root.resolve("bad.zip");
            for(String key:files.keySet())rejectedArchive(bad,f->f.remove(key));
            rejectedArchive(bad,f->f.put("../escaped",text("must never be written")));
            rejectedArchive(bad,f->f.put("extra/",new byte[0]));
            rejectedArchive(bad,f->f.put("README.txt",new byte[16385]));
            rejectedArchive(bad,f->f.put("evaluation-details.json",new byte[2*1024*1024+1]));
            rejectedArchive(bad,f->f.put("plugins/BotsClustersMC/policy.bcmc",new byte[4*1024*1024+1]));
            rejectedArchive(bad,f->{byte[] b=f.get("plugins/BotsClustersMC/policy.bcmc").clone();b[b.length-1]^=1;f.put("plugins/BotsClustersMC/policy.bcmc",b);});
            rejectedArchive(bad,f->f.put("plugins/botsclustersmc.jar",text("changed byte identity")));
            rejectedArchive(bad,f->f.put("evaluation.json",text("{}")));
            rejectedArchive(bad,f->f.put("evaluation-details.json",new byte[]{(byte)0xff}));
            rejectedArchive(bad,f->changedReport(f,d->d.addProperty("policy_updates",8)));
            rejectedArchive(bad,f->changedReport(f,d->d.addProperty("policy_sha256","wrong")));
            rejectedArchive(bad,f->changedReport(f,d->d.addProperty("cases_per_task",65)));
            rejectedArchive(bad,f->changedReport(f,d->d.addProperty("new_training_samples",1)));
            rejectedArchive(bad,f->changedReport(f,d->d.getAsJsonArray("trials").remove(0)));
            rejectedArchive(bad,f->changedReport(f,d->d.getAsJsonArray("tasks").get(0).getAsJsonObject().addProperty("passed",2)));
            rejectedArchive(bad,f->changedReport(f,d->d.addProperty("diagnostic_only",true)));
            rejectedArchive(bad,f->changedReport(f,d->d.addProperty("diagnostic_only","false")));
            rejectedArchive(bad,f->changedReport(f,d->d.addProperty("reset_intervention","open-workbench")));
            rejectedArchive(bad,f->changedReport(f,d->d.add("reset_intervention",JsonNull.INSTANCE)));
            // ZipOutputStream refuses duplicate names. Patch equal-length header names to construct the negative fixture.
            Map<String,byte[]> duplicate=fixture();duplicate.put("READM_.txt",text("duplicate"));write(bad,duplicate);
            byte[] bytes=Files.readAllBytes(bad),from=text("READM_.txt"),to=text("README.txt");int replacements=0;
            for(int i=0;i<=bytes.length-from.length;i++)if(Arrays.equals(Arrays.copyOfRange(bytes,i,i+from.length),from)) {
                System.arraycopy(to,0,bytes,i,to.length);replacements++;
            }
            check(replacements==2,"patched central and local duplicate names");Files.write(bad,bytes);reject(()->ReplaySource.read(bad));
            Files.write(bad,text("not a zip"));reject(()->ReplaySource.read(bad));
            try(RandomAccessFile large=new RandomAccessFile(bad.toFile(),"rw")){large.setLength(16L*1024*1024+1);}
            reject(()->ReplaySource.read(bad));reject(()->ReplaySource.read(root));reject(()->ReplaySource.read(root.resolve("missing.zip")));
            try {
                Path link=root.resolve("linked.zip");Files.createSymbolicLink(link,source);
                reject(()->ReplaySource.read(link));Files.delete(link);
                Path ancestor=root.resolve("linked-parent");Files.createSymbolicLink(ancestor,root);
                reject(()->ReplaySource.read(ancestor.resolve("source.zip")));Files.delete(ancestor);
            }catch(UnsupportedOperationException|FileSystemException unavailable){System.out.println("Symlink creation unavailable on this platform; archive tests still executed.");}
            Evaluate.Options options=Evaluate.Options.parse(new String[]{"--from",source.toString(),"--seed","29","--tasks","11","--export",root.resolve("new.zip").toString()});
            check(options.from().equals(source)&&options.tasks().equals(List.of(11))&&options.seed()==29&&!options.watch(),"explicit replay options");
            check(Evaluate.Options.parse(new String[]{"--from",source.toString()}).tasks().isEmpty(),"source tasks selected later");
            reject(()->Evaluate.Options.parse(new String[]{"--from",source.toString(),"--watch"}));
            reject(()->Evaluate.Options.parse(new String[]{"--from",source.toString(),"--from",source.toString()}));
            reject(()->Evaluate.Options.parse(new String[]{"--from"}));
            check(Arrays.equals(original,Files.readAllBytes(source)),"all tests preserve original source");
        }finally {Host.deleteTree(root);}
        System.out.println("PASS frozen bundle replay integrity checks="+checks+"; synthetic source only");
    }
}
