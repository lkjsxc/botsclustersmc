import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Real loopback HTTP checks against the compiled monitor; disposable metrics only. */
public final class MonitorTest {
    static int checks;
    static final List<String> failures=new ArrayList<>();
    static final String STATUS="{\"state\":\"running\",\"epoch_millis\":1700000000000,\"active_agents\":7}";
    static final String REPORT="{\"complete\":true,\"policy_updates\":123}";
    static final String HEARTBEAT="{\"state\":\"complete\",\"epoch_millis\":1700000000000}";
    static URI base;
    static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    static void check(boolean ok,String message){checks++;if(!ok)failures.add(message);}
    static HttpResponse<String> get(String path)throws Exception{
        return HTTP.send(HttpRequest.newBuilder(base.resolve(path)).timeout(Duration.ofSeconds(3)).build(),HttpResponse.BodyHandlers.ofString());
    }
    static void status(String path,int code,String expected)throws Exception{
        var response=get(path);check(response.statusCode()==code,path+" expected "+code+", got "+response.statusCode());
        if(expected!=null)check(response.body().equals(expected),path+" unexpected body: "+response.body().substring(0,Math.min(180,response.body().length())));
        check("no-store".equals(response.headers().firstValue("Cache-Control").orElse("")),path+" must not be cached");
    }
    static String evaluation(String report,String monitor){return "{\"result\":"+report+",\"monitor\":"+monitor+"}";}
    static void cycle()throws InterruptedException{Thread.sleep(5500);}
    static void oversized(Path path,int size)throws IOException{
        try(var file=new RandomAccessFile(path.toFile(),"rw")){file.setLength(size);}
    }
    public static void main(String[] args)throws Exception{
        Path folder=Files.createTempDirectory("bcmc-monitor-test-").toAbsolutePath(),data=folder.resolve("metrics");
        Files.createDirectory(data);
        Path status=data.resolve("status.json"),history=data.resolve("history.jsonl"),report=data.resolve("evaluation.json"),heartbeat=data.resolve("evaluation-status.json");
        Files.writeString(status,STATUS);Files.writeString(history,STATUS+"\n");Files.writeString(report,REPORT);Files.writeString(heartbeat,HEARTBEAT);
        int port;try(var socket=new ServerSocket(0,0,InetAddress.getByName("127.0.0.1"))){port=socket.getLocalPort();}
        base=URI.create("http://127.0.0.1:"+port);
        String java=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java").toString();
        Process child=new ProcessBuilder(java,"-Xmx96m","-cp",System.getProperty("java.class.path"),"Monitor",data.toString(),"127.0.0.1",Integer.toString(port))
                .redirectErrorStream(true).redirectOutput(folder.resolve("monitor.log").toFile()).start();
        boolean completed=false;
        try{
            boolean ready=false;long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
            while(System.nanoTime()<deadline&&child.isAlive()){
                try{if(get("/api/status").statusCode()==200){ready=true;break;}}catch(IOException ignored){}
                Thread.sleep(100);
            }
            if(!ready)throw new AssertionError("Monitor did not start: "+Files.readString(folder.resolve("monitor.log")));
            status("/api/status",200,STATUS);
            status("/api/history",200,"["+STATUS+"]");
            status("/api/evaluation",200,evaluation(REPORT,HEARTBEAT));
            check(get("/policy.bcmc").statusCode()==404,"No model download");
            check(get("/console").statusCode()==404,"No console");
            var post=HTTP.send(HttpRequest.newBuilder(base.resolve("/api/status")).POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString());
            check(post.statusCode()==405,"Read-only HTTP methods");

            oversized(history,9*1024*1024+1);cycle();
            status("/api/history",503,null);
            status("/api/status",200,STATUS);
            status("/api/evaluation",200,evaluation(REPORT,HEARTBEAT));
            Files.writeString(history,STATUS+"\n");
            Files.delete(status);cycle();
            status("/api/status",503,null);
            status("/api/history",200,"["+STATUS+"]");
            status("/api/evaluation",200,evaluation(REPORT,HEARTBEAT));

            // One broken optional report must not hide its valid sibling.
            Files.writeString(status,STATUS);Files.write(report,new byte[]{(byte)0xc3,(byte)0x28});cycle();
            status("/api/status",200,STATUS);
            status("/api/evaluation",200,evaluation("null",HEARTBEAT));

            Files.writeString(report,REPORT);Files.write(heartbeat,new byte[]{(byte)0xc3,(byte)0x28});
            oversized(status,131073);
            // A long rejected line is drained, not accumulated or mistaken for an object.
            Files.writeString(history,"x".repeat(100000)+"\n"+STATUS+"\r\n"+STATUS);cycle();
            status("/api/status",503,null);
            status("/api/history",200,"["+STATUS+","+STATUS+"]");
            status("/api/evaluation",200,evaluation(REPORT,"null"));

            // Recovery does not require a monitor restart; absence is not a broken history.
            Files.writeString(status,STATUS);Files.writeString(heartbeat,HEARTBEAT);Files.delete(history);cycle();
            status("/api/status",200,STATUS);
            status("/api/history",200,"[]");
            status("/api/evaluation",200,evaluation(REPORT,HEARTBEAT));
            check(child.isAlive(),"Reader and HTTP service survived transient failures");
            check(Files.readString(status).equals(STATUS),"Monitor did not rewrite source status");
            check(Files.readString(report).equals(REPORT),"Monitor did not rewrite source evaluation");
            check(Files.readString(heartbeat).equals(HEARTBEAT),"Monitor did not rewrite source heartbeat");
            try(var files=Files.list(data)){check(files.count()==3,"Monitor did not create metrics files");}
            completed=true;
        }finally{
            child.destroy();if(!child.waitFor(10,TimeUnit.SECONDS)){child.destroyForcibly();child.waitFor(5,TimeUnit.SECONDS);}
            // Keep failed evidence. A successful test owns and removes only this temp tree.
            if(completed&&failures.isEmpty()){
                try(var paths=Files.walk(folder)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}
            }else System.err.println("Retained monitor failure evidence: "+folder);
        }
        for(String failure:failures)System.err.println("FAIL "+failure);
        if(!failures.isEmpty())throw new AssertionError(failures.size()+" failures / "+checks+" checks");
        System.out.println("PASS monitor HTTP failure isolation, bounded files, recovery and read-only routes; "+checks+" checks");
    }
}
