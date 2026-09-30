import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.*;
import java.util.regex.*;
import javax.tools.*;

/** Source-launched JDK-only build and foreground supervisor. No shell config evaluation. */
public final class Host {
    static final Path ROOT=Path.of("").toAbsolutePath().normalize();
    static final Set<String> KEYS=Set.of("EULA","BOTS","HEAP_GB","REGION_THREADS","INFERENCE_THREADS","LEARNER_THREADS","PORT","BIND_ADDRESS","ONLINE_MODE","OFFLINE_ACCESS_ACK","SEED","ACADEMY","JAVA_BIN");
    final Map<String,String> config=new HashMap<>();
    final Properties pin=new Properties();
    final Path cache;
    Host()throws Exception{
        if(Runtime.version().feature()<21||ToolProvider.getSystemJavaCompiler()==null)throw new IOException("Install a Java 21+ JDK (java AND javac), then retry.");
        if(!Files.isRegularFile(ROOT.resolve("host/server.properties")))throw new IOException("Run from the repository root.");
        if(Files.exists(ROOT.resolve(".env"))){
            for(String line:Files.readAllLines(ROOT.resolve(".env"))){line=line.strip();if(line.isEmpty()||line.startsWith("#"))continue;int eq=line.indexOf('=');if(eq<1)throw new IOException("Malformed .env line");String key=line.substring(0,eq).strip(),value=line.substring(eq+1).strip();
                if(!KEYS.contains(key))throw new IOException("Unknown .env key: "+key+"; use the current .env.example, not a legacy config.");
                if(value.length()>1&&((value.startsWith("\"")&&value.endsWith("\""))||(value.startsWith("'")&&value.endsWith("'"))))value=value.substring(1,value.length()-1);
                if(config.put(key,value)!=null)throw new IOException("Duplicate .env key: "+key);
            }
        }
        for(String key:KEYS)if(System.getenv(key)!=null)config.put(key,System.getenv(key));
        for(String value:config.values())if(value.contains("\n")||value.contains("\r"))throw new IOException("Newlines are not valid configuration values");
        try(var input=Files.newInputStream(ROOT.resolve("host/server.properties"))){pin.load(input);}
        cache=Path.of(System.getenv().getOrDefault("BCMC_SERVER_CACHE",ROOT.resolve(".cache/server").toString())).toAbsolutePath().normalize();
    }
    String value(String key,String fallback){return config.getOrDefault(key,fallback);}
    boolean bool(String key,boolean fallback)throws IOException{String s=value(key,Boolean.toString(fallback));if(!s.equals("true")&&!s.equals("false"))throw new IOException(key+" must be true or false");return Boolean.parseBoolean(s);}
    int number(String key,int fallback,int min,int max)throws IOException{try{String s=value(key,"auto");int n=s.equals("auto")?fallback:Integer.parseInt(s);if(n<min||n>max)throw new NumberFormatException();return n;}catch(NumberFormatException e){throw new IOException(key+" must be auto or "+min+".."+max);}}
    String java(){return value("JAVA_BIN",Path.of(System.getProperty("java.home"),"bin",isWindows()?"java.exe":"java").toString());}
    static boolean isWindows(){return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");}
    static void safe(Path path)throws IOException{for(Path p=path.toAbsolutePath().normalize();p!=null;p=p.getParent())if(Files.isSymbolicLink(p))throw new IOException("Refusing symlinked managed path: "+p);}
    static void atomic(Path path,byte[] data)throws IOException{
        safe(path);Files.createDirectories(path.toAbsolutePath().getParent());Path tmp=Files.createTempFile(path.toAbsolutePath().getParent(),".write-",".tmp");
        try{Files.write(tmp,data);Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(tmp);}
    }
    static void text(Path path,String text)throws IOException{atomic(path,text.getBytes(StandardCharsets.UTF_8));}
    static String hash(Path path)throws Exception{MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream in=Files.newInputStream(path)){byte[] b=new byte[65536];int n;while((n=in.read(b))>=0)digest.update(b,0,n);}return HexFormat.of().formatHex(digest.digest());}
    void prepareCache()throws Exception{
        safe(cache);Files.createDirectories(cache);Path server=cache.resolve("server.jar");safe(server);
        if(Files.exists(server)&&!hash(server).equals(pin.getProperty("sha256")))throw new IOException("Cached server JAR does not match the official pin; remove only "+server+" and retry.");
        if(!Files.exists(server)){
            System.out.println("Downloading the pinned official "+pin.getProperty("project")+" server "+pin.getProperty("version")+" build "+pin.getProperty("build"));
            HttpClient client=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(30)).build();Path partial=Files.createTempFile(cache,"download-",".tmp");
            try{
                HttpRequest request=HttpRequest.newBuilder(URI.create(pin.getProperty("url"))).timeout(Duration.ofMinutes(5)).header("User-Agent","botsclustersmc-source-build/0.6.1 (https://github.com/lkjsxc/botsclustersmc)").GET().build();
                HttpResponse<Path> response=client.send(request,HttpResponse.BodyHandlers.ofFile(partial));if(response.statusCode()!=200)throw new IOException("Server download HTTP "+response.statusCode());
                if(!hash(partial).equals(pin.getProperty("sha256")))throw new IOException("Official server download checksum mismatch");Files.move(partial,server,StandardCopyOption.ATOMIC_MOVE);
            }finally{Files.deleteIfExists(partial);}
        }
        execute(List.of(java(),"-Xmx1G","-Dpaperclip.patchonly=true","-jar",server.toString()),cache);
    }
    static List<Path> sources(String... directories)throws IOException{List<Path> files=new ArrayList<>();for(String directory:directories){Path path=ROOT.resolve(directory);if(Files.exists(path))try(var stream=Files.walk(path)){files.addAll(stream.filter(p->p.toString().endsWith(".java")).sorted().toList());}}return files;}
    String classpath()throws IOException{List<String> jars=new ArrayList<>();for(String name:List.of("libraries","versions")){Path path=cache.resolve(name);if(Files.exists(path))try(var files=Files.walk(path)){files.filter(p->p.toString().endsWith(".jar")).sorted().forEach(p->jars.add(p.toString()));}}if(jars.isEmpty())throw new IOException("Real server API libraries missing");return String.join(File.pathSeparator,jars);}
    void build()throws Exception{
        safe(ROOT.resolve(".build"));Files.createDirectories(ROOT.resolve(".build"));
        try(FileChannel ch=FileChannel.open(ROOT.resolve(".build/build.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);FileLock lock=ch.tryLock()){
            if(lock==null)throw new IOException("Another build owns this checkout");prepareCache();Path classes=ROOT.resolve(".build/classes");deleteTree(classes);Files.createDirectories(classes);
            List<String> args=new ArrayList<>(List.of("--release","21","-encoding","UTF-8","-proc:none","-cp",classpath(),"-d",classes.toString()));for(Path source:sources("core/src","plugin/src","training/src"))args.add(source.toString());
            int exit=ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,args.toArray(String[]::new));if(exit!=0)throw new IOException("Java compilation failed: "+exit);
            safe(ROOT.resolve("dist"));Files.createDirectories(ROOT.resolve("dist"));jar(ROOT.resolve("dist/botsclustersmc.jar"),classes,List.of("org/botsclustersmc/core","org/botsclustersmc/plugin"),ROOT.resolve("plugin/resources"));
            jar(ROOT.resolve("dist/training.jar"),classes,List.of("org"),ROOT.resolve("training/resources"));
            System.out.println("Built dist/botsclustersmc.jar (inference only) and dist/training.jar (isolated training).");
        }
    }
    static void deleteTree(Path path)throws IOException{if(!Files.exists(path))return;safe(path);try(var files=Files.walk(path)){for(Path p:files.sorted(Comparator.reverseOrder()).toList()){safe(p);Files.delete(p);}}}
    static void jar(Path dest,Path classes,List<String> roots,Path resources)throws IOException{
        safe(dest);Path temporary=Files.createTempFile(dest.getParent(),"jar-",".tmp");try(JarOutputStream jar=new JarOutputStream(Files.newOutputStream(temporary))){
            Set<String> seen=new HashSet<>();entry(jar,ROOT.resolve("LICENSE"),"META-INF/LICENSE",seen);entry(jar,ROOT.resolve("NOTICE"),"META-INF/NOTICE",seen);for(String root:roots){try(var stream=Files.walk(classes.resolve(root))){for(Path file:stream.filter(Files::isRegularFile).sorted().toList())entry(jar,file,classes.relativize(file).toString(),seen);}}
            try(var stream=Files.walk(resources)){for(Path file:stream.filter(Files::isRegularFile).sorted().toList())entry(jar,file,resources.relativize(file).toString(),seen);}
        }catch(Throwable e){Files.deleteIfExists(temporary);throw e;}Files.move(temporary,dest,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
    }
    static void entry(JarOutputStream out,Path file,String name,Set<String> seen)throws IOException{name=name.replace(File.separatorChar,'/');if(!seen.add(name))return;JarEntry entry=new JarEntry(name);entry.setTime(0);out.putNextEntry(entry);Files.copy(file,out);out.closeEntry();}
    static void execute(List<String> command,Path directory)throws Exception{int exit=new ProcessBuilder(command).directory(directory.toFile()).inheritIO().start().waitFor();if(exit!=0)throw new IOException("Command failed ("+exit+"): "+command.get(0));}
    Path academy(){return ROOT.resolve(value("ACADEMY","academy")).toAbsolutePath().normalize();}
    void ownAcademy()throws Exception{
        Path dir=academy();safe(dir);if(dir.equals(ROOT))throw new IOException("The checkout itself cannot be an Academy");
        Path marker=dir.resolve(".botsclustersmc-academy");safe(marker);
        if(!Files.exists(marker)){
            if(Files.exists(dir))try(var children=Files.list(dir)){if(children.findAny().isPresent())throw new IOException("Refusing a nonempty, unowned academy directory: "+dir+". Start in a new clone or select an empty ACADEMY.");}
            Files.createDirectories(dir);text(marker,"botsclustersmc-owned-training\n");
        }else if(!Files.readString(marker).equals("botsclustersmc-owned-training\n"))throw new IOException("Academy ownership marker does not match");
        // Academy names are configurable: a root .gitignore cannot enumerate their paths.
        // Ignore directories too, so descendant negations cannot expose checkpoints/world data.
        Path ignored=dir.resolve(".gitignore");safe(ignored);
        if(Files.exists(ignored,LinkOption.NOFOLLOW_LINKS)) {
            if(!Files.isRegularFile(ignored,LinkOption.NOFOLLOW_LINKS)||Files.size(ignored)!=2||!Files.readString(ignored).equals("*\n"))
                throw new IOException("Owned Academy .gitignore must contain exactly '*\\n'; refusing to overwrite custom rules: "+ignored);
        }else Files.writeString(ignored,"*\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS);
    }
    static void copyCache(Path from,Path to)throws IOException{
        if(!Files.exists(from))return;try(var stream=Files.walk(from)){for(Path p:stream.sorted().toList()){Path dest=to.resolve(from.relativize(p));safe(dest);if(Files.isDirectory(p)){Files.createDirectories(dest);continue;}if(!Files.exists(dest)){try{Files.createLink(dest,p);}catch(IOException|UnsupportedOperationException e){Files.copy(p,dest);}}}}
    }
    void start()throws Exception{
        if(!bool("EULA",false))throw new IOException("Read the Minecraft EULA at https://aka.ms/MinecraftEULA. After personally accepting it, set EULA=true in .env or run EULA=true ./start.sh.");
        ownAcademy();Path dir=academy();safe(dir.resolve("run.lock"));
        try(FileChannel channel=FileChannel.open(dir.resolve("run.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);FileLock lock=channel.tryLock()){
            if(lock==null)throw new IOException("This Academy is already running");build();
            int cpus=Runtime.getRuntime().availableProcessors();long total=4L<<30;var os=java.lang.management.ManagementFactory.getOperatingSystemMXBean();if(os instanceof com.sun.management.OperatingSystemMXBean m)total=m.getTotalMemorySize();
            int heap=number("HEAP_GB",Math.max(2,Math.min(12,(int)(total/(1L<<30)*2/3))),1,256);
            int bots=number("BOTS",Math.min(2048,Math.min(cpus*64,heap*128)),1,10000);
            int available=Math.max(1,cpus-1),infer=number("INFERENCE_THREADS",Math.max(1,available/8),1,128),learner=number("LEARNER_THREADS",Math.max(1,available/3),1,128),regions=number("REGION_THREADS",Math.max(1,available-infer-learner),1,128);
            int port=number("PORT",25565,1,65535);String bind=value("BIND_ADDRESS","0.0.0.0");if(!bind.matches("[0-9a-fA-F:.]+"))throw new IOException("BIND_ADDRESS must be a literal IP address");
            boolean online=bool("ONLINE_MODE",true);if(!online&&!bool("OFFLINE_ACCESS_ACK",false))throw new IOException("Offline mode allows identity spoofing. Set OFFLINE_ACCESS_ACK=true only on a properly isolated server.");
            long seed;try{seed=Long.parseLong(value("SEED","7"));}catch(NumberFormatException e){throw new IOException("SEED must be a signed integer");}
            try(ServerSocket check=new ServerSocket()){check.setReuseAddress(false);check.bind(new InetSocketAddress(bind,port));}
            Path server=dir.resolve("server"),data=server.resolve("plugins/BotsClustersMC");safe(data);Files.createDirectories(data);Files.createDirectories(server.resolve("config"));
            for(String name:List.of("libraries","versions","cache"))copyCache(cache.resolve(name),server.resolve(name));
            atomic(server.resolve("plugins/training.jar"),Files.readAllBytes(ROOT.resolve("dist/training.jar")));
            text(server.resolve(".botsclustersmc-training"),"launcher-owned training server\n");text(server.resolve("eula.txt"),"eula=true\n");
            String props="server-port="+port+"\nserver-ip="+bind+"\nonline-mode="+online+"\nspawn-protection=0\nmax-players=32\nview-distance=2\nsimulation-distance=2\nlevel-name=world\nlevel-type=minecraft:flat\nlevel-seed="+seed+"\ngenerate-structures=false\ngenerator-settings={\"layers\":[{\"block\":\"minecraft:bedrock\",\"height\":1},{\"block\":\"minecraft:dirt\",\"height\":2},{\"block\":\"minecraft:grass_block\",\"height\":1}],\"biome\":\"minecraft:plains\"}\ndifficulty=normal\nallow-flight=true\nsync-chunk-writes=false\nmotd=BotsClustersMC training - NPCs, not logged-in players\n";
            text(server.resolve("server.properties"),props);
            text(server.resolve("config/paper-global.yml"),"_version: 31\nthreaded-regions:\n  threads: "+regions+"\nchunk-system:\n  io-threads: 1\n  worker-threads: "+Math.max(1,Math.min(4,cpus/8))+"\n");
            text(data.resolve("config.yml"),"count: "+bots+"\nmax-agents: "+Math.max(4096,bots)+"\nmax-loaded-chunks: "+Math.max(4096,bots)+"\nseed: "+seed+"\ninference-threads: "+infer+"\nlearner-threads: "+learner+"\nregion-threads: "+regions+"\nrollout-queue: "+Math.min(16384,Math.max(128,bots*2))+"\nbatch-samples: 512\nmax-policy-lag: "+Math.max(64,bots/4)+"\nworld-edits: true\n");
            System.out.printf(Locale.ROOT,"Starting %d NPC learners; effective CPUs=%d, region/inference/learner threads=%d/%d/%d, heap=%d GiB, port=%d%n",bots,cpus,regions,infer,learner,heap,port);
            if(regions+infer+learner>cpus+1)System.out.println("Warning: selected thread budget exceeds effective CPUs. Compare real samples/s rather than CPU% alone.");
            supervise(server,dir,List.of(java(),"-Xms512m","-Xmx"+heap+"G","-Dbcmc.training=true","-jar",cache.resolve("server.jar").toString(),"--nogui"));
        }
    }
    /** Operational deadlines only; never a lesson, reward, or checkpoint setting. */
    record Supervision(long startupMillis,long staleMillis,long stopMillis,long terminateMillis,long pollMillis){
        Supervision{if(startupMillis<1||staleMillis<1||stopMillis<1||terminateMillis<1||pollMillis<1)throw new IllegalArgumentException("supervision deadlines");}
    }
    static final Supervision SUPERVISION=new Supervision(180000,60000,35000,5000,1000);
    static final int CONSOLE_BYTES=8*1024*1024,STATUS_BYTES=64*1024;

    /** Two bounded byte segments, not an unbounded run archive. Never follow a log symlink. */
    static final class ConsoleLog extends OutputStream{
        final Path current,previous;final int limit;OutputStream output;long size;boolean closed;
        ConsoleLog(Path dir,int limit)throws IOException{
            if(limit<1||limit>CONSOLE_BYTES)throw new IllegalArgumentException("console bound");
            this.limit=limit;current=dir.resolve("console.log");previous=dir.resolve("console.previous.log");
            validate();if(Files.exists(current)&&Files.size(current)>0)archive();open();
        }
        void validate()throws IOException{
            for(Path path:List.of(current,previous)){
                safe(path);
                if(Files.exists(path,LinkOption.NOFOLLOW_LINKS)&&(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)||Files.size(path)>limit))
                    throw new IOException("Archive the existing non-regular/oversized console path explicitly before restart: "+path);
            }
        }
        void archive()throws IOException{
            validate();Files.move(current,previous,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }
        void open()throws IOException{
            validate();
            output=Files.exists(current,LinkOption.NOFOLLOW_LINKS)
                ?Files.newOutputStream(current,StandardOpenOption.WRITE,StandardOpenOption.APPEND,LinkOption.NOFOLLOW_LINKS)
                :Files.newOutputStream(current,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS);
            privateFile(current);size=Files.size(current);
        }
        @Override public void write(int value)throws IOException{write(new byte[]{(byte)value},0,1);}
        @Override public void write(byte[] bytes,int offset,int length)throws IOException{
            Objects.checkFromIndexSize(offset,length,bytes.length);
            if(closed)throw new IOException("Console log is closed");
            while(length>0){
                if(size==limit){output.close();output=null;archive();open();}
                int count=(int)Math.min(length,limit-size);output.write(bytes,offset,count);
                size+=count;offset+=count;length-=count;
            }
        }
        @Override public void flush()throws IOException{if(output!=null)output.flush();}
        @Override public void close()throws IOException{closed=true;if(output!=null){OutputStream closing=output;output=null;closing.close();}}
    }

    /** A failed sink is disabled once; the child pipe continues to be drained into the other sink. */
    static final class ConsoleCapture implements Runnable{
        final InputStream source;final OutputStream mirror;final OutputStream local;
        volatile IOException failure;
        ConsoleCapture(InputStream source,OutputStream mirror,OutputStream local,IOException openingFailure){
            this.source=source;this.mirror=mirror;this.local=local;failure=openingFailure;
        }
        void failed(String stage,IOException error){if(failure==null)failure=new IOException(stage+": "+error.getMessage(),error);}
        void closeLocal(OutputStream stream){try{stream.close();}catch(IOException error){failed("Closing local console log failed",error);}}
        @Override public void run(){
            OutputStream screen=mirror,file=local;byte[] bytes=new byte[8192];
            try(source){int count;while((count=source.read(bytes))!=-1){
                if(screen!=null)try{
                    screen.write(bytes,0,count);screen.flush();
                    if(screen instanceof PrintStream print&&print.checkError())throw new IOException("Parent console stream reports an error");
                }catch(IOException error){failed("Forwarding console output failed",error);screen=null;}
                if(file!=null)try{file.write(bytes,0,count);file.flush();}
                catch(IOException error){failed("Writing local console log failed",error);closeLocal(file);file=null;}
            }}catch(IOException error){failed("Reading child console output failed",error);}
            finally{if(file!=null)closeLocal(file);}
        }
    }

    /** Monotonic silence deadlines; missing/ambiguous timing fields and replayed/future stamps are not heartbeats. */
    static final class SupervisionHealth{
        final long started;final Supervision timing;long latestEpoch=-1,lastFresh;boolean seen;
        static final Pattern EPOCH=Pattern.compile("\"epoch_millis\"\\s*:\\s*([0-9]{1,19})(?=\\s*[,}])");
        static final Pattern STATE=Pattern.compile("\"state\"\\s*:\\s*\"(running|paused|failed)\"");
        SupervisionHealth(long started,Supervision timing){this.started=started;this.timing=timing;}
        String observe(String json,long elapsed,long wallNow){
            if(json!=null){
                Matcher time=EPOCH.matcher(json),state=STATE.matcher(json);
                if(time.find()&&state.find()){
                    String stamp=time.group(1),mode=state.group(1);
                    if(!time.find()&&!state.find())try{
                        long epoch=Long.parseLong(stamp);
                        if(epoch>=started&&epoch<=wallNow+5000&&wallNow-epoch<=timing.staleMillis()){
                            if(epoch>latestEpoch){latestEpoch=epoch;lastFresh=elapsed;seen=true;}
                            if(epoch==latestEpoch&&mode.equals("failed"))return "Training reported a failure";
                        }
                    }catch(NumberFormatException ignored){/* Invalid reports do not extend a deadline. */}
                }
            }
            if(!seen&&elapsed>=timing.startupMillis())return "No fresh training status after startup";
            if(seen&&elapsed-lastFresh>=timing.staleMillis())return "Training status stopped advancing";
            return null;
        }
    }
    static String supervisedStatus(Path path)throws IOException{
        safe(path);if(!Files.exists(path,LinkOption.NOFOLLOW_LINKS))return null;
        if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))throw new IOException("Training status is not a regular file");
        try(InputStream input=Files.newInputStream(path,LinkOption.NOFOLLOW_LINKS)){
            byte[] bytes=input.readNBytes(STATUS_BYTES+1);if(bytes.length>STATUS_BYTES)throw new IOException("Training status exceeds its read bound");
            return new String(bytes,StandardCharsets.UTF_8);
        }
    }
    static void stopChild(Process child,java.util.function.Consumer<String> send,Supervision timing)throws InterruptedException{
        if(!child.isAlive())return;send.accept("stop");
        if(child.waitFor(timing.stopMillis(),TimeUnit.MILLISECONDS))return;
        child.destroy();if(child.waitFor(timing.terminateMillis(),TimeUnit.MILLISECONDS))return;
        child.destroyForcibly();child.waitFor(timing.terminateMillis(),TimeUnit.MILLISECONDS);
    }
    static void supervise(Path server,Path dir,List<String> command)throws Exception{supervise(server,dir,command,SUPERVISION);}
    static void supervise(Path server,Path dir,List<String> command,Supervision timing)throws Exception{
        Path controlFile=dir.resolve("control.properties");safe(controlFile);
        ConsoleLog log=new ConsoleLog(dir,CONSOLE_BYTES);
        long started=System.currentTimeMillis(),begin=System.nanoTime();Process child;
        try{child=new ProcessBuilder(command).directory(server.toFile()).redirectErrorStream(true).start();}
        catch(IOException|RuntimeException error){try{log.close();}catch(IOException closing){error.addSuppressed(closing);}throw error;}
        BufferedWriter input=new BufferedWriter(new OutputStreamWriter(child.getOutputStream(),StandardCharsets.UTF_8));Object inputLock=new Object();
        java.util.function.Consumer<String> send=s->{try{synchronized(inputLock){input.write(s);input.newLine();input.flush();}}catch(IOException ignored){}};
        Thread hook=new Thread(()->{try{stopChild(child,send,timing);}catch(InterruptedException e){Thread.currentThread().interrupt();}},"bcmc-stop");Runtime.getRuntime().addShutdownHook(hook);
        ConsoleCapture capture=new ConsoleCapture(child.getInputStream(),System.out,log,null);
        Thread output=Thread.ofPlatform().daemon().name("bcmc-output").start(capture);
        try(ServerSocket control=new ServerSocket(0,16,InetAddress.getLoopbackAddress())){
            String token=UUID.randomUUID().toString();Properties info=new Properties();info.setProperty("pid",Long.toString(child.pid()));info.setProperty("port",Integer.toString(control.getLocalPort()));info.setProperty("address",control.getInetAddress().getHostAddress());info.setProperty("token",token);info.setProperty("started",Long.toString(started));ByteArrayOutputStream bytes=new ByteArrayOutputStream();info.store(bytes,"Local authenticated console; do not share this file");atomic(controlFile,bytes.toByteArray());privateFile(controlFile);
            Thread.ofPlatform().daemon().name("bcmc-console").start(()->{try(var reader=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8))){String line;while((line=reader.readLine())!=null)send.accept(line);}catch(IOException ignored){}});
            Thread.ofPlatform().daemon().name("bcmc-control").start(()->{while(!control.isClosed()){try(Socket socket=control.accept()){socket.setSoTimeout(2000);DataInputStream in=new DataInputStream(socket.getInputStream());DataOutputStream out=new DataOutputStream(socket.getOutputStream());String provided=in.readUTF(),cmd=in.readUTF();if(!MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8),token.getBytes(StandardCharsets.UTF_8))||cmd.length()>4096||cmd.contains("\n")||cmd.contains("\r")){out.writeUTF("rejected");continue;}send.accept(cmd);out.writeUTF("sent");}catch(IOException ignored){}}});
            SupervisionHealth health=new SupervisionHealth(started,timing);String reason=null;long stopAt=0;
            while(!child.waitFor(timing.pollMillis(),TimeUnit.MILLISECONDS)){
                long elapsed=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-begin);
                if(reason==null){
                    if(capture.failure!=null)reason=capture.failure.getMessage();
                    else try{reason=health.observe(supervisedStatus(server.resolve("plugins/BotsClustersMC/status.json")),elapsed,System.currentTimeMillis());}
                    catch(IOException error){reason="Cannot read training status: "+error.getMessage();}
                    if(reason!=null){System.err.println(reason+"; requesting a clean stop.");send.accept("stop");stopAt=elapsed;}
                }else if(elapsed-stopAt>=timing.stopMillis()){
                    System.err.println("Training did not stop within its grace period; terminating the child. Final checkpoint is not guaranteed.");
                    child.destroy();if(!child.waitFor(timing.terminateMillis(),TimeUnit.MILLISECONDS)){child.destroyForcibly();child.waitFor(timing.terminateMillis(),TimeUnit.MILLISECONDS);}
                    break;
                }
            }
            output.join(3000);
            if(output.isAlive())throw new IOException("Console drain did not finish after child exit; captured output may be incomplete");
            if(child.isAlive()||child.exitValue()!=0||reason!=null||capture.failure!=null)
                throw new IOException("Training server stopped unsuccessfully"+(reason==null?"":": "+reason)+"; inspect console.log, console.previous.log and the parent console/journal",capture.failure);
        }finally{
            try{stopChild(child,send,timing);output.join(3000);}
            finally{try{Files.deleteIfExists(controlFile);}finally{Runtime.getRuntime().removeShutdownHook(hook);}}
        }
    }
    static void privateFile(Path p){try{Files.setPosixFilePermissions(p,PosixFilePermissions.fromString("rw-------"));}catch(IOException|UnsupportedOperationException ignored){}}
    void console(String command)throws Exception{
        Path path=academy().resolve("control.properties");safe(path);Properties p=new Properties();try(var in=Files.newInputStream(path)){p.load(in);}
        InetAddress address=InetAddress.getByName(p.getProperty("address"));if(!address.isLoopbackAddress())throw new IOException("Control endpoint must be loopback");
        try(Socket socket=new Socket()){socket.connect(new InetSocketAddress(address,Integer.parseInt(p.getProperty("port"))),2000);socket.setSoTimeout(3000);DataOutputStream out=new DataOutputStream(socket.getOutputStream());out.writeUTF(p.getProperty("token"));out.writeUTF(command);out.flush();String response=new DataInputStream(socket.getInputStream()).readUTF();if(!response.equals("sent"))throw new IOException("Console command rejected");System.out.println("Sent: "+command);}
    }
    static double metric(String json,String key,double fallback){Matcher m=Pattern.compile("\""+Pattern.quote(key)+"\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9Ee+\\-]+)?)").matcher(json);return m.find()?Double.parseDouble(m.group(1)):fallback;}
    void evaluate(String[] options)throws Exception {
        if(Arrays.asList(options).contains("--help")){System.out.println("evaluate [--tasks 0,1,2] [--cases 32] [--seed N] [--heap-gb 2] [--port 0] [--watch --interval 600] [--from EVALUATED.zip] [--export FILE.zip]");return;}
        if(!bool("EULA",false))throw new IOException("Read and accept the Minecraft EULA before setting EULA=true");
        Path marker=academy().resolve(".botsclustersmc-academy"),checkpoint=academy().resolve("server/plugins/BotsClustersMC/training.bcmc");safe(marker);safe(checkpoint);
        if(!Files.isRegularFile(marker)||!Files.readString(marker).equals("botsclustersmc-owned-training\n"))throw new IOException("Evaluation requires an owned Academy");
        if(!Arrays.asList(options).contains("--from")&&!Files.isRegularFile(checkpoint))throw new IOException("A complete training checkpoint is required without --from");
        build();Path tools=Files.createTempDirectory(ROOT.resolve(".build"),"evaluation-tools-");
        try {
            try(FileChannel guard=FileChannel.open(ROOT.resolve(".build/build.lock"),StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS);FileLock lock=guard.tryLock()){
                if(lock==null)throw new IOException("Another build is publishing artifacts; retry evaluation");
            Files.copy(ROOT.resolve("dist/training.jar"),tools.resolve("runtime.jar"));Files.copy(ROOT.resolve("dist/botsclustersmc.jar"),tools.resolve("inference.jar"));
            Path src=ROOT.resolve("tests/holdout");
            for(Path file:sources("tests/holdout")){safe(file);Path dest=tools.resolve("holdout-src").resolve(src.relativize(file));Files.createDirectories(dest.getParent());Files.copy(file,dest);}
            }
            String cp=tools.resolve("runtime.jar")+File.pathSeparator+classpath();
            List<String> compile=new ArrayList<>(List.of("--release","21","-encoding","UTF-8","-proc:none","-cp",cp,"-d",tools.toString()));
            for(Path file:sources("host"))compile.add(file.toString());
            if(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,compile.toArray(String[]::new))!=0)throw new IOException("Evaluation tools compilation failed");
            List<URL> urls=new ArrayList<>();urls.add(tools.toUri().toURL());for(String path:cp.split(Pattern.quote(File.pathSeparator)))urls.add(Path.of(path).toUri().toURL());
            try(URLClassLoader loader=new URLClassLoader(urls.toArray(URL[]::new),ClassLoader.getPlatformClassLoader())) {
                String[] args=new String[options.length+1];args[0]=tools.toString();System.arraycopy(options,0,args,1,options.length);
                try{loader.loadClass("Evaluate").getMethod("main",String[].class).invoke(null,(Object)args);}
                catch(java.lang.reflect.InvocationTargetException failure){if(failure.getCause() instanceof Exception e)throw e;throw failure;}
            }
        }finally{deleteTree(tools);}
    }
    void status()throws Exception{
        Path path=academy().resolve("server/plugins/BotsClustersMC/status.json");String json=Files.readString(path);System.out.println(json);double age=(System.currentTimeMillis()-metric(json,"epoch_millis",0))/1000;System.out.printf(Locale.ROOT,"Status age: %.1f seconds. Process CPU fraction is normalized over available CPUs, not one thread.%n",age);
        if(!Files.exists(academy().resolve("control.properties")))System.out.println("Supervisor is not running; this is the last saved status, not a live health claim.");
    }
    void export(Path destination)throws Exception{
        Path dir=academy(),marker=dir.resolve(".botsclustersmc-academy"),runLock=dir.resolve("run.lock");
        safe(marker);safe(runLock);
        if(!Files.isRegularFile(marker,LinkOption.NOFOLLOW_LINKS)||!Files.readString(marker).equals("botsclustersmc-owned-training\n"))
            throw new IOException("Export requires an existing owned Academy and a complete training checkpoint");
        Path target=destination.toAbsolutePath().normalize();safe(target);
        if(target.startsWith(dir)||dir.startsWith(target))throw new IOException("Export destination must be separate from the Academy");
        try(FileChannel ch=FileChannel.open(runLock,StandardOpenOption.CREATE,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS);FileLock lock=ch.tryLock()){
            if(lock==null)throw new IOException("Stop training cleanly before export; this Academy is still running");
            build(); // Never pair weights with stale JARs left behind by a source update.
            Path staging=Files.createTempDirectory(ROOT.resolve(".build"),"export-");
            try {
                Path checkpoint=dir.resolve("server/plugins/BotsClustersMC/training.bcmc"),policy=staging.resolve("policy.bcmc");
                execute(List.of(java(),"-cp",ROOT.resolve("dist/training.jar").toString(),"org.botsclustersmc.training.CheckpointTool","export",checkpoint.toString(),policy.toString()),ROOT);
                Path jarTarget=target.resolve("plugins/botsclustersmc.jar"),policyTarget=target.resolve("plugins/BotsClustersMC/policy.bcmc"),readme=target.resolve("README.txt");
                safe(jarTarget);safe(policyTarget);safe(readme);
                byte[] model=Files.readAllBytes(policy),jar=Files.readAllBytes(ROOT.resolve("dist/botsclustersmc.jar"));
                atomic(jarTarget,jar);atomic(policyTarget,model);
                text(readme,"Copy plugins/botsclustersmc.jar and plugins/BotsClustersMC/policy.bcmc to a STOPPED tested Paper/Folia server only after export succeeds. The policy was derived from the canonical training.bcmc, not a loose cached policy. No external process or native libraries are required. NPCs are ephemeral bodies, not logged-in players. World edits default to false. Read the repository README before enabling them.\n");
                System.out.println("Export completed from canonical training.bcmc: "+target);
            }finally{deleteTree(staging);}
        }
    }
    void test()throws Exception{
        build();Path out=ROOT.resolve(".build/tests");Files.createDirectories(out);String cp=ROOT.resolve(".build/classes")+File.pathSeparator+classpath();
        List<String> args=new ArrayList<>(List.of("--release","21","-proc:none","-cp",cp,"-d",out.toString()));for(Path p:sources("tests/java","tests/host","tests/live","tests/holdout","tests/lifecycle","tests/transfer","host"))args.add(p.toString());
        if(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,args.toArray(String[]::new))!=0)throw new IOException("Test compilation failed");
        System.out.println("PASS real-API compilation of live diagnostic fixtures; not executed by source tests.");
        for(String test:List.of("CoreTest","GoalTransferTest","MechanicsTest","SharedInventoryTest","OwnershipTest","MenuFocusTest","ControlTest","PocketViewTest","AimTest","HarvestTest","HarvestTraceTest","StationTest","ResetInterventionTest","CraftingCurriculumTest","CraftingTraceTest","TableCraftingTraceTest","BalanceTest","UpdateTest","CriticFeaturesTest","LearningContextsTest","MiningControlTest","CourseTest","ProbePoliciesTest","LearningTest","PersistenceTest","ConcurrencyTest"))execute(List.of(java(),"-cp",out+File.pathSeparator+cp,"org.botsclustersmc.tests."+test),ROOT);
        execute(List.of(java(),"-cp",out+File.pathSeparator+cp,"SupervisorTest"),ROOT);
        execute(List.of(java(),"-cp",out+File.pathSeparator+cp,"MonitorTest"),ROOT);
        execute(List.of(java(),"-cp",out+File.pathSeparator+cp,"MonitorReadTest"),ROOT);
        execute(List.of(java(),"-cp",out+File.pathSeparator+cp,"AcademyBoundaryTest"),ROOT);
        execute(List.of(java(),"-cp",out+File.pathSeparator+cp,"ExportTest"),ROOT);
        execute(List.of(java(),"-cp",out+File.pathSeparator+cp,"EvaluationTest"),ROOT);
        try(JarFile jar=new JarFile(ROOT.resolve("dist/botsclustersmc.jar").toFile())){if(jar.stream().anyMatch(e->e.getName().contains("/training/")||e.getName().contains("TrainingEnvironment")))throw new IOException("Inference artifact contains training/reset code");}
        System.out.println("PASS inference artifact separation; all tests completed.");
    }
    public static void main(String[] args){try{Host host=new Host();String op=args.length==0?"help":args[0];switch(op){case "build"->host.build();case "start"->host.start();case "stop"->host.console("stop");case "console"->{if(args.length<2)throw new IOException("console <Minecraft command>");host.console(String.join(" ",Arrays.copyOfRange(args,1,args.length)));}case "monitor"->{if(args.length>3)throw new IOException("monitor [private bind address] [port]");execute(List.of(host.java(),"-Xmx128m","host/Monitor.java",host.academy().resolve("server/plugins/BotsClustersMC").toString(),args.length>1?args[1]:"127.0.0.1",args.length>2?args[2]:"8765"),ROOT);}case "evaluate"->host.evaluate(Arrays.copyOfRange(args,1,args.length));case "status"->host.status();case "export"->host.export(args.length>1?Path.of(args[1]):ROOT.resolve("dist/deploy"));case "test"->host.test();default->System.out.println("Commands: build | start | status | evaluate [--help] | monitor [private address] [port] | console <command> | stop | export [directory] | test");}}catch(Exception e){System.err.println("ERROR: "+e.getMessage());System.exit(1);}}
}
