import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Read-only metrics HTTP service. Never provides a console or writes server data. */
public final class Monitor {
    record Snapshot(byte[] status,byte[] history,byte[] evaluation) {}
    private static String optionalReport(Path path)throws IOException {
        if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)||Files.size(path)>131072)return "null";
        String text=Files.readString(path).strip();
        return text.startsWith("{")&&text.endsWith("}")?text:"null";
    }
    private static byte[] evaluation(Path folder) {
        try {
            String result=optionalReport(folder.resolve("evaluation.json"));
            String monitor=optionalReport(folder.resolve("evaluation-status.json"));
            return ("{\"result\":"+result+",\"monitor\":"+monitor+"}").getBytes(StandardCharsets.UTF_8);
        }catch(IOException unavailable){return "{\"result\":null,\"monitor\":null}".getBytes(StandardCharsets.UTF_8);}
    }
    private static volatile Snapshot snapshot;
    private static final byte[] UNAVAILABLE="{\"error\":\"Training metrics are not available\"}".getBytes(StandardCharsets.UTF_8);
    private static byte[] history(Path folder)throws IOException {
        ArrayDeque<String> lines=new ArrayDeque<>();
        for(String name:List.of("history.previous.jsonl","history.jsonl")) {
            Path path=folder.resolve(name); if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))continue;
            if(Files.size(path)>9*1024*1024)throw new IOException("Metric history exceeds its bound");
            try(BufferedReader reader=Files.newBufferedReader(path)) {
                String line;while((line=reader.readLine())!=null) {
                    if(line.length()>32768||!line.startsWith("{")||!line.endsWith("}"))continue;
                    lines.addLast(line);if(lines.size()>720)lines.removeFirst();
                }
            }
        }
        return ("["+String.join(",",lines)+"]").getBytes(StandardCharsets.UTF_8);
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("Monitor <metrics directory> <private bind address> <port>");
        Path folder=Path.of(args[0]).toAbsolutePath().normalize();
        for(Path p=folder;p!=null;p=p.getParent())if(Files.isSymbolicLink(p))throw new IOException("Refusing symlinked metrics directory");
        InetAddress address=InetAddress.getByName(args[1]);byte[] a=address.getAddress();
        boolean tailscale=a.length==4&&(a[0]&255)==100&&(a[1]&255)>=64&&(a[1]&255)<=127;
        if(address.isAnyLocalAddress()||!(address.isLoopbackAddress()||address.isSiteLocalAddress()||tailscale))
            throw new IllegalArgumentException("Use an explicit loopback, LAN or Tailscale address; public/wildcard binds are refused.");
        int port=Integer.parseInt(args[2]);if(port<1||port>65535)throw new IllegalArgumentException("Port range");
        byte[] page=Files.readAllBytes(Path.of("host/monitor.html"));
        ScheduledExecutorService reader=Executors.newSingleThreadScheduledExecutor();
        reader.scheduleAtFixedRate(()->{
            try {
                Path status=folder.resolve("status.json");
                if(!Files.isRegularFile(status,LinkOption.NOFOLLOW_LINKS)||Files.size(status)>131072)throw new IOException("Invalid status file");
                snapshot=new Snapshot(Files.readAllBytes(status),history(folder),evaluation(folder));
            } catch(IOException failure) { snapshot=null; }
        },0,5,TimeUnit.SECONDS);
        HttpServer server=HttpServer.create(new InetSocketAddress(address,port),16);
        ExecutorService requests=new ThreadPoolExecutor(2,2,0L,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(16),new ThreadPoolExecutor.AbortPolicy());server.setExecutor(requests);
        server.createContext("/",exchange->{
            try(exchange) {
                if(!exchange.getRequestMethod().equals("GET")){exchange.sendResponseHeaders(405,-1);return;}
                String path=exchange.getRequestURI().getPath();Snapshot s=snapshot;byte[] body;String type;int code=200;
                if(path.equals("/")){body=page;type="text/html; charset=utf-8";}
                else if(path.equals("/api/status")||path.equals("/api/history")||path.equals("/api/evaluation")) {
                    type="application/json; charset=utf-8";body=s==null?UNAVAILABLE:path.endsWith("status")?s.status():path.endsWith("history")?s.history():s.evaluation();if(s==null)code=503;
                } else {exchange.sendResponseHeaders(404,-1);return;}
                Headers headers=exchange.getResponseHeaders();headers.set("Content-Type",type);
                headers.set("Cache-Control","no-store");headers.set("X-Content-Type-Options","nosniff");
                headers.set("Referrer-Policy","no-referrer");
                headers.set("Content-Security-Policy","default-src 'self'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'");
                exchange.sendResponseHeaders(code,body.length);exchange.getResponseBody().write(body);
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(()->{server.stop(0);reader.shutdownNow();requests.shutdownNow();}));
        server.start();System.out.println("Read-only training monitor: http://"+address.getHostAddress()+":"+port+"/");
        System.out.println("No remote console, CORS access, credentials or model files are exposed.");
    }
}
