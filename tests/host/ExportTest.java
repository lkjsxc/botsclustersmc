import org.botsclustersmc.core.*;
import org.botsclustersmc.training.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Exercise the actual source-launcher entrypoint, including its cross-process run lock. */
public final class ExportTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        Path root=Files.createTempDirectory("bcmc-export-").toRealPath(),academy=root.resolve("academy"),data=academy.resolve("server/plugins/BotsClustersMC"),target=root.resolve("deploy");
        try {
            Files.createDirectories(data);Files.writeString(academy.resolve(".botsclustersmc-academy"),"botsclustersmc-owned-training\n");
            Policy initial=Policy.initialize(7);float[] gradient=new float[Policy.PARAMETERS];gradient[0]=1;
            int[] counts=new int[Policy.EXPERTS];counts[0]=32;
            Adam.Update update=new Adam().update(initial,gradient,counts,.0003);
            TrainingState state=new TrainingState(update.policy(),update.optimizer(),new Course(2,7).encode());
            Path checkpoint=data.resolve("training.bcmc"),cached=data.resolve("policy.bcmc");state.write(checkpoint);PolicyFile.write(cached,initial);
            try(FileChannel channel=FileChannel.open(academy.resolve("run.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);FileLock lock=channel.lock()){
                execute(academy,target,root.resolve("locked.log"),false,"still running");check(!Files.exists(target),"running export writes no deployment");
            }
            execute(academy,target,root.resolve("export.log"),true,"Export completed from canonical training.bcmc");
            Path exported=target.resolve("plugins/BotsClustersMC/policy.bcmc");CheckpointTool.verifyExport(checkpoint,exported);checks++;
            check(PolicyFile.read(cached).updates()==0,"stale loose policy ignored, not rewritten");
            check(Files.isRegularFile(target.resolve("plugins/botsclustersmc.jar")),"deployment JAR written");
            byte[] complete=Files.readAllBytes(exported),source=Files.readAllBytes(checkpoint),bad=source.clone();bad[20]^=1;Files.write(checkpoint,bad);
            execute(academy,target,root.resolve("corrupt.log"),false,"checksum mismatch");
            check(Arrays.equals(complete,Files.readAllBytes(exported)),"failed export retains completed deployment");
            check(Arrays.equals(bad,Files.readAllBytes(checkpoint)),"failed export leaves damaged source untouched");
            Files.write(checkpoint,source);Files.write(cached,new byte[]{1,2,3});
            execute(academy,root.resolve("uncached"),root.resolve("derived.log"),true,"Export completed from canonical training.bcmc");
            CheckpointTool.verifyExport(checkpoint,root.resolve("uncached/plugins/BotsClustersMC/policy.bcmc"));checks++;
            check(Arrays.equals(Files.readAllBytes(cached),new byte[]{1,2,3}),"corrupt loose policy neither trusted nor overwritten");
            System.out.println("PASS source-launcher export checks="+checks);
        }finally{try(var paths=Files.walk(root)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}}
    }
    private static void execute(Path academy,Path output,Path log,boolean success,String expected)throws Exception {
        String java=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")?"java.exe":"java").toString();
        ProcessBuilder builder=new ProcessBuilder(java,"host/Host.java","export",output.toString());
        builder.environment().put("ACADEMY",academy.toString());builder.environment().put("JAVA_BIN",java);
        Process process=builder.redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(120,TimeUnit.SECONDS)){process.destroyForcibly();process.waitFor();throw new AssertionError("export timed out");}
        String text=Files.readString(log);check((process.exitValue()==0)==success,"export exit code: "+text);check(text.contains(expected),"export diagnostic: "+text);
    }
}
