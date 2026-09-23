import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.*;
import javax.tools.ToolProvider;
import org.botsclustersmc.core.*;

/** Disposable, loopback-only server using the same canonical Java holdout evaluator. */
final class EvaluationServer implements AutoCloseable {
    private final Host host;
    final Path root,server,data,log,runtime,inference,sourceRoot;
    private volatile Process process;
    private boolean stopRequested;
    private BufferedWriter input;
    EvaluationServer(Host host,Path tools)throws Exception {
        this.host=host;Host.safe(Host.ROOT.resolve(".build"));
        root=Files.createTempDirectory(Host.ROOT.resolve(".build"),"evaluation-");
        server=root.resolve("server");data=server.resolve("plugins/BotsClustersMC");log=root.resolve("server.log");
        runtime=tools.resolve("runtime.jar");inference=tools.resolve("inference.jar");sourceRoot=tools.resolve("holdout-src");
        Files.createDirectories(data);Host.text(root.resolve(".evaluation-owned"),"disposable-evaluation\n");
    }
    void prepare(Policy policy,List<Integer> tasks,int cases,long seed,int port)throws Exception {
        for(String name:List.of("libraries","versions","cache"))Host.copyCache(host.cache.resolve(name),server.resolve(name));
        PolicyFile.write(data.resolve("policy.bcmc"),policy);
        Host.text(server.resolve(".botsclustersmc-exam"),"Disposable fixed-policy evaluation\n");
        Host.text(server.resolve("eula.txt"),"eula=true\n");
        if(port==0)try(ServerSocket socket=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"))){port=socket.getLocalPort();}
        String flat="{\"layers\":[{\"block\":\"minecraft:bedrock\",\"height\":1},{\"block\":\"minecraft:dirt\",\"height\":2},{\"block\":\"minecraft:grass_block\",\"height\":1}],\"biome\":\"minecraft:plains\"}";
        Host.text(server.resolve("server.properties"),"server-ip=127.0.0.1\nserver-port="+port+"\nonline-mode=true\nmax-players=0\nwhite-list=true\nenforce-whitelist=true\nlevel-name=world\nlevel-type=minecraft:flat\ngenerator-settings="+flat+"\ngenerate-structures=false\nview-distance=2\nsimulation-distance=2\nspawn-protection=0\ndifficulty=normal\nallow-flight=true\n");
        Host.text(server.resolve("config/paper-global.yml"),"_version: 31\nthreaded-regions:\n  threads: 2\nchunk-system:\n  worker-threads: 1\n  io-threads: 1\n");
        int count=cases*tasks.size();
        Path resources=root.resolve("resources");Files.createDirectories(resources);
        Host.text(resources.resolve("plugin.yml"),"name: BotsClustersMC\nversion: 'evaluation'\nmain: org.botsclustersmc.holdout.FrozenPolicyExam\napi-version: '1.21'\nfolia-supported: true\ncommands:\n  bots:\n    description: Frozen evaluation diagnostics\npermissions:\n  botsclustersmc.observe:\n    default: op\n  botsclustersmc.admin:\n    default: op\n");
        Host.text(resources.resolve("config.yml"),"max-agents: "+Math.max(64,count)+"\nmax-loaded-chunks: "+Math.max(64,count)+"\ninference-threads: 1\nseed: "+seed+"\ncases-per-task: "+cases+"\ntasks: "+tasks+"\nworld-edits: false\n");
        Path classes=root.resolve("classes");Files.createDirectories(classes);
        List<String> compile=new ArrayList<>(List.of("--release","21","-encoding","UTF-8","-proc:none","-cp",runtime+File.pathSeparator+host.classpath(),"-d",classes.toString()));
        List<Path> sources;try(var paths=Files.walk(sourceRoot)){sources=paths.filter(p->p.toString().endsWith(".java")).sorted().toList();}if(sources.isEmpty())throw new IOException("Canonical holdout sources are missing");
        for(Path source:sources)compile.add(source.toString());
        if(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,compile.toArray(String[]::new))!=0)throw new IOException("Evaluation compilation failed");
        try(JarFile source=new JarFile(runtime.toFile());JarOutputStream dest=new JarOutputStream(Files.newOutputStream(server.resolve("plugins/exam.jar")))) {
            Set<String> names=new HashSet<>();
            for(JarEntry item:source.stream().sorted(Comparator.comparing(JarEntry::getName)).toList()) {
                if(item.isDirectory()||!item.getName().startsWith("org/"))continue;
                JarEntry entry=new JarEntry(item.getName());entry.setTime(0);dest.putNextEntry(entry);
                try(InputStream in=source.getInputStream(item)){in.transferTo(dest);}dest.closeEntry();names.add(item.getName());
            }
            try(var paths=Files.walk(classes)){for(Path file:paths.filter(Files::isRegularFile).sorted().toList())Host.entry(dest,file,classes.relativize(file).toString(),names);}
            Host.entry(dest,resources.resolve("plugin.yml"),"plugin.yml",names);
            Host.entry(dest,resources.resolve("config.yml"),"config.yml",names);
        }
    }
    void run(int heap,int count,Runnable heartbeat)throws Exception {
        List<String> command=List.of(host.java(),"-Xms256m","-Xmx"+heap+"G","-Dbcmc.holdout=true","-jar",host.cache.resolve("server.jar").toString(),"--nogui");
        synchronized(this){if(stopRequested)throw new InterruptedException("Evaluation was stopped before server startup");process=new ProcessBuilder(command).directory(server.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        input=new BufferedWriter(new OutputStreamWriter(process.getOutputStream(),StandardCharsets.UTF_8));}
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(300+count/4),nextHeartbeat=0;
        while(!process.waitFor(1,TimeUnit.SECONDS)) {
            long now=System.nanoTime();if(now>=nextHeartbeat){heartbeat.run();nextHeartbeat=now+TimeUnit.SECONDS.toNanos(5);}
            if(Files.exists(data.resolve("exam-failed.txt")))throw new IOException("Evaluation runtime failed; see evaluation.log");
            if(now>=deadline)throw new IOException("Evaluation timed out; no new result was published");
        }
        if(process.exitValue()!=0)throw new IOException("Evaluation server exited with code "+process.exitValue());
        if(Files.exists(data.resolve("exam-failed.txt")))throw new IOException("Evaluation runtime failed; see evaluation.log");
    }
    synchronized void requestStop()throws IOException,InterruptedException {
        stopRequested=true;Process child=process;if(child==null||!child.isAlive())return;
        if(input!=null){input.write("stop\n");input.flush();}
        if(!child.waitFor(40,TimeUnit.SECONDS))throw new IOException("Evaluation shutdown is still pending; preserving scratch files at "+root);
    }
    @Override public void close()throws Exception {
        requestStop();if(input!=null)input.close();Path marker=root.resolve(".evaluation-owned");Host.safe(marker);
        if(Files.isRegularFile(marker)&&Files.readString(marker).equals("disposable-evaluation\n"))Host.deleteTree(root);
    }
}
