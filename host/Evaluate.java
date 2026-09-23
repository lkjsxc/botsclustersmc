import com.google.gson.*;
import java.io.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.botsclustersmc.core.*;
import org.botsclustersmc.training.*;

/** Operator-facing fixed-policy evaluation. It never changes the live Academy. */
public final class Evaluate {
    static final String HELP="evaluate [--tasks 0,1,2] [--cases 32] [--seed N] [--heap-gb 2] [--port 0] [--watch --interval 600] [--export FILE.zip]\nDefault tasks cover every reached stage. A completed test is not a guarantee of mastery.";
    record Options(List<Integer> tasks,int cases,long seed,boolean fixedSeed,int heap,int port,boolean watch,int interval,Path export) {
        static Options parse(String[] args) {
            List<Integer> tasks=List.of();int cases=32,heap=2,port=0,interval=600;long seed=0;boolean fixed=false,watch=false;Path export=null;
            Set<String> seen=new HashSet<>();
            for(int i=0;i<args.length;i++) {
                String key=args[i];if(!seen.add(key))throw new IllegalArgumentException("Duplicate option: "+key);
                if(key.equals("--watch")){watch=true;continue;}
                if(i+1>=args.length)throw new IllegalArgumentException("Missing value: "+key);String value=args[++i];
                switch(key) {
                    case "--tasks"->{List<Integer> chosen=new ArrayList<>();for(String part:value.split(",",-1)){int task=Integer.parseInt(part);Task.at(task);if(chosen.contains(task))throw new IllegalArgumentException("Duplicate task");chosen.add(task);}tasks=List.copyOf(chosen);}
                    case "--cases"->cases=Integer.parseInt(value);
                    case "--heap-gb"->heap=Integer.parseInt(value);
                    case "--port"->port=Integer.parseInt(value);
                    case "--interval"->interval=Integer.parseInt(value);
                    case "--seed"->{seed=Long.parseLong(value);fixed=true;}
                    case "--export"->export=Path.of(value);
                    default->throw new IllegalArgumentException("Unknown option: "+key);
                }
            }
            if(cases<1||cases>64||heap<1||heap>8||interval<60||interval>86400||(port!=0&&(port<1024||port>65535||port==25565)))throw new IllegalArgumentException("Options exceed the documented bounds");
            if(!watch&&seen.contains("--interval"))throw new IllegalArgumentException("--interval requires --watch");
            if(export!=null&&watch)throw new IllegalArgumentException("--export is a one-shot immutable artifact; omit --watch");
            return new Options(tasks,cases,seed,fixed,heap,port,watch,interval,export);
        }
    }
    private final Host host;private final Options options;private final Path tools,folder;
    private final AtomicBoolean stopping=new AtomicBoolean();private final AtomicReference<EvaluationServer> active=new AtomicReference<>();
    private volatile long started;private volatile Policy current;private volatile List<Integer> selected=List.of();
    private Evaluate(Host host,Path tools,Options options){this.host=host;this.tools=tools;this.options=options;folder=host.academy().resolve("server/plugins/BotsClustersMC");}
    static List<Integer> reached(TrainingState state)throws IOException {
        byte[] bytes=state.course();int actors;
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))){in.readUTF();actors=in.readInt();}
        int maximum=Course.decode(bytes,actors).maximumTask();List<Integer> tasks=new ArrayList<>();for(int i=0;i<=maximum;i++)tasks.add(i);return List.copyOf(tasks);
    }
    private void status(String state,String detail)throws IOException {
        JsonObject data=new JsonObject();data.addProperty("state",state);data.addProperty("detail",detail);data.addProperty("epoch_millis",System.currentTimeMillis());
        data.addProperty("started_epoch_millis",started);data.addProperty("watch",options.watch());data.addProperty("interval_seconds",options.interval());
        data.add("tasks",new Gson().toJsonTree(selected));data.addProperty("cases_per_task",options.cases());
        Policy p=current;if(p!=null){data.addProperty("policy_updates",p.updates());data.addProperty("policy_trained_samples",p.samples());}
        EvaluationServer experiment=active.get();
        if(experiment!=null&&state.equals("running")) {
            Path progress=experiment.data.resolve("status.json");
            try {
                if(Files.isRegularFile(progress,LinkOption.NOFOLLOW_LINKS)&&Files.size(progress)<=131072) {
                    JsonObject update=JsonParser.parseString(Files.readString(progress)).getAsJsonObject();
                    long total=EvaluationChecks.integer(update,"evaluation_trials_total");
                    long done=EvaluationChecks.integer(update,"evaluation_trials_completed");
                    if(total==selected.size()*options.cases()&&done>=0&&done<=total
                            &&EvaluationChecks.integer(update,"policy_updates")==current.updates()) {
                        data.addProperty("trials_total",total);data.addProperty("trials_completed",done);
                    }
                }
            }catch(IOException|RuntimeException unavailable){/* A missing heartbeat is not a result. */}
        }
        Host.text(folder.resolve("evaluation-status.json"),data+"\n");
    }
    private void heartbeat(){try{status("running","A fixed policy is being tested; training continues independently.");}catch(IOException e){throw new UncheckedIOException(e);}}
    private void checkOwned()throws IOException {
        Path marker=host.academy().resolve(".botsclustersmc-academy");Host.safe(marker);Host.safe(folder);
        if(!Files.isRegularFile(marker,LinkOption.NOFOLLOW_LINKS)||!Files.readString(marker).equals("botsclustersmc-owned-training\n"))throw new IOException("Evaluation requires an existing owned Academy");
        Host.safe(folder.resolve("training.bcmc"));if(!Files.isRegularFile(folder.resolve("training.bcmc"),LinkOption.NOFOLLOW_LINKS))throw new IOException("A complete training checkpoint is required");
    }
    private JsonObject once(TrainingState snapshot)throws Exception {
        current=snapshot.policy();selected=options.tasks().isEmpty()?reached(snapshot):options.tasks();started=System.currentTimeMillis();
        long seed=options.fixedSeed()?options.seed():new java.security.SecureRandom().nextLong();
        status("preparing","Preparing an isolated evaluation of a canonical checkpoint snapshot.");
        EvaluationServer experiment=new EvaluationServer(host,tools);active.set(experiment);
        try {
            if(stopping.get())throw new InterruptedException("Evaluation was stopped during preparation");
            experiment.prepare(current,selected,options.cases(),seed,options.port());
            byte[] policyBytes=Files.readAllBytes(experiment.data.resolve("policy.bcmc"));
            heartbeat();System.out.println("Evaluating frozen policy "+current.updates()+", tasks="+selected+", cases/task="+options.cases());
            experiment.run(options.heap(),selected.size()*options.cases(),this::heartbeat);
            if(stopping.get())throw new InterruptedException("Evaluation was stopped");
            Path result=experiment.data.resolve("exam-result.json");Host.safe(result);
            if(!Files.isRegularFile(result)||Files.size(result)>2*1024*1024)throw new IOException("Missing or oversized evaluation report");
            JsonObject report=EvaluationChecks.validate(Files.readString(result),current,selected,options.cases(),seed,started);
            if(!Arrays.equals(policyBytes,Files.readAllBytes(experiment.data.resolve("policy.bcmc"))))throw new IOException("Evaluation changed its policy file");
            try(var files=Files.newDirectoryStream(experiment.data,"training*.bcmc")){if(files.iterator().hasNext())throw new IOException("Evaluation created a training checkpoint");}
            report.addProperty("policy_sha256",Host.hash(experiment.data.resolve("policy.bcmc")));
            report.addProperty("runtime_jar_sha256",Host.hash(experiment.runtime));report.addProperty("inference_jar_sha256",Host.hash(experiment.inference));
            report.addProperty("exam_jar_sha256",Host.hash(experiment.server.resolve("plugins/exam.jar")));
            report.addProperty("server_version",host.pin.getProperty("version"));report.addProperty("server_build",host.pin.getProperty("build"));
            report.addProperty("scope","Fixed policy in full-difficulty Academy rooms; not open-world survival or a certificate for later policies.");
            if(options.export()!=null) {
                EvaluatedBundle.write(options.export(),host.academy(),policyBytes,Files.readAllBytes(experiment.inference),report);
                System.out.println("Tested policy and matching inference build: "+options.export().toAbsolutePath().normalize());
            }
            publish(report);
            status("completed","The frozen-policy test completed. Read the measured success counts, not only this state.");
            for(JsonElement task:report.getAsJsonArray("tasks"))System.out.println(task);
            return report;
        }finally{
            try {
                if(Files.isRegularFile(experiment.log)) {
                    try(RandomAccessFile log=new RandomAccessFile(experiment.log.toFile(),"r")) {
                        log.seek(Math.max(0,log.length()-65536));
                        byte[] tail=new byte[(int)(log.length()-log.getFilePointer())];log.readFully(tail);
                        Host.atomic(folder.resolve("evaluation.log"),tail);
                    }
                }
            }finally{
                experiment.close();active.compareAndSet(experiment,null);
            }
        }
    }
    private void publish(JsonObject report)throws IOException {
        Host.text(folder.resolve("evaluation-details.json"),report+"\n");
        JsonObject summary=report.deepCopy();summary.remove("trials");
        Host.text(folder.resolve("evaluation.json"),summary+"\n");
        System.out.println("Measured results: "+folder.resolve("evaluation.json"));
        System.out.println(options.export()==null
            ?"To retain exact tested weights during live learning, use a one-shot --export FILE.zip."
            :"The exported ZIP contains this tested snapshot, not the continually updated live weights. Read all success counts.");
    }
    private static String identity(Policy policy)throws Exception {
        float[] weights=policy.copyWeights();java.nio.ByteBuffer bytes=java.nio.ByteBuffer.allocate(weights.length*4);
        for(float value:weights)bytes.putFloat(value);
        byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(bytes.array());
        return policy.updates()+":"+policy.samples()+":"+HexFormat.of().formatHex(digest);
    }
    private void shutdown() {
        stopping.set(true);EvaluationServer server=active.get();
        try {
            if(server!=null)server.requestStop();
            status("stopped","Evaluation monitoring stopped; previous completed results remain available.");
        }catch(Exception failure){System.err.println("Evaluation shutdown: "+failure.getMessage());}
    }
    private void run()throws Exception {
        checkOwned();if(options.export()!=null)EvaluatedBundle.checkTarget(options.export(),host.academy());Path lockPath=host.academy().resolve("evaluation.lock");Host.safe(lockPath);
        try(FileChannel channel=FileChannel.open(lockPath,StandardOpenOption.CREATE,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS);FileLock lock=channel.tryLock()) {
            if(lock==null)throw new IOException("An evaluation already owns this Academy");
            Thread hook=new Thread(this::shutdown,"bcmc-evaluation-stop");Runtime.getRuntime().addShutdownHook(hook);
            if(options.watch())Thread.ofPlatform().daemon().name("bcmc-evaluation-input").start(()->{
                try(BufferedReader reader=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8))){
                    String line;while(!stopping.get()&&(line=reader.readLine())!=null){
                        if(line.strip().equals("stop")){shutdown();break;}
                    }
                }catch(IOException failure){System.err.println("Evaluation input closed: "+failure.getMessage());}
            });
            try {
                String last="";
                while(!stopping.get()) {
                    checkOwned();TrainingState snapshot=TrainingState.read(folder.resolve("training.bcmc"));
                    List<Integer> available=reached(snapshot); // Validate course bytes even for explicitly selected tasks.
                    List<Integer> tasks=options.tasks().isEmpty()?available:options.tasks();
                    String key=identity(snapshot.policy())+tasks;
                    if(!key.equals(last)){once(snapshot);last=key;}
                    if(!options.watch())break;
                    long next=System.nanoTime()+options.interval()*1_000_000_000L;
                    while(!stopping.get()&&System.nanoTime()<next) {
                        status("waiting","Waiting for the next check. Completed results belong to their recorded frozen policy.");
                        Thread.sleep(Math.max(1,Math.min(5000,(next-System.nanoTime())/1_000_000)));
                    }
                }
            }catch(Exception failure){if(stopping.get()){status("stopped","Evaluation was stopped; earlier completed results were retained.");return;}status("failed",failure.getMessage()==null?failure.getClass().getSimpleName():failure.getMessage());throw failure;}
            finally{Runtime.getRuntime().removeShutdownHook(hook);}
        }
    }
    public static void main(String[] args)throws Exception {
        try {
            if(args.length<1)throw new IllegalArgumentException("Internal tools directory is missing");
            Host host=new Host();if(!host.bool("EULA",false))throw new IOException("Explicit Minecraft EULA consent is required");
            Options options=Options.parse(Arrays.copyOfRange(args,1,args.length));
            new Evaluate(host,Path.of(args[0]).toAbsolutePath().normalize(),options).run();
        }catch(Exception failure){System.err.println("Evaluation failed: "+failure.getMessage());throw failure;}
    }
}
