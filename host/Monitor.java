import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Read-only metrics HTTP service. Never provides a console or writes server data. */
public final class Monitor {
    record Snapshot(byte[] status,byte[] history,byte[] evaluation) {}
    static final int OBJECT_BYTES=131072,HISTORY_BYTES=9*1024*1024,LINE_CHARS=32768,HISTORY_ROWS=720;
    /** Enforce the byte limit while reading, even if the file grows after its size check. */
    static final class BoundedInput extends FilterInputStream {
        private int remaining;
        BoundedInput(InputStream input,int limit){super(input);if(limit<0||limit==Integer.MAX_VALUE)throw new IllegalArgumentException("read bound");remaining=limit;}
        @Override public int read()throws IOException{
            if(remaining<0)throw new IOException("Metric read bound was exceeded");
            int value=in.read();if(value>=0&&--remaining<0)throw new IOException("Metric file exceeds its read bound");return value;
        }
        @Override public int read(byte[] bytes,int offset,int length)throws IOException{
            Objects.checkFromIndexSize(offset,length,bytes.length);if(length==0)return 0;
            if(remaining<0)throw new IOException("Metric read bound was exceeded");
            int n=in.read(bytes,offset,Math.min(length,remaining+1));
            if(n>0&&(remaining-=n)<0)throw new IOException("Metric file exceeds its read bound");return n;
        }
    }
    private static InputStream input(Path path,int limit)throws IOException {
        for(Path p=path.toAbsolutePath();p!=null;p=p.getParent())if(Files.isSymbolicLink(p))throw new IOException("Symlinked metric path");
        if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)||Files.size(path)>limit)throw new IOException("Invalid metric file");
        return new BoundedInput(Files.newInputStream(path,LinkOption.NOFOLLOW_LINKS),limit);
    }
    @FunctionalInterface interface Read {byte[] get()throws IOException;}
    private static byte[] available(Read read){try{return read.get();}catch(IOException|RuntimeException unavailable){return null;}}
    private static byte[] object(Path path) {
        return available(()->{
            byte[] bytes;try(InputStream in=input(path,OBJECT_BYTES)){bytes=in.readAllBytes();}
            String text=StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString().strip();
            if(!text.startsWith("{")||!text.endsWith("}"))throw new IOException("Invalid metric object envelope");
            return bytes;
        });
    }
    private static String optionalReport(Path path) {
        byte[] bytes=object(path);return bytes==null?"null":new String(bytes,StandardCharsets.UTF_8).strip();
    }
    private static byte[] evaluation(Path folder) {
        String result=optionalReport(folder.resolve("evaluation.json"));
        String monitor=optionalReport(folder.resolve("evaluation-status.json"));
        return ("{\"result\":"+result+",\"monitor\":"+monitor+"}").getBytes(StandardCharsets.UTF_8);
    }
    private static volatile Snapshot snapshot;
    private static final byte[] UNAVAILABLE="{\"error\":\"Requested metrics are not available\"}".getBytes(StandardCharsets.UTF_8);
    private static void keepLine(ArrayDeque<String> lines,StringBuilder line,boolean tooLong){
        if(tooLong)return;
        int n=line.length();if(n>0&&line.charAt(n-1)=='\r')line.setLength(--n);
        if(n>LINE_CHARS||n<2||line.charAt(0)!='{'||line.charAt(n-1)!='}')return;
        lines.addLast(line.toString());if(lines.size()>HISTORY_ROWS)lines.removeFirst();
    }
    private static byte[] history(Path folder)throws IOException {
        ArrayDeque<String> lines=new ArrayDeque<>();
        for(String name:List.of("history.previous.jsonl","history.jsonl")) {
            Path path=folder.resolve(name);if(Files.notExists(path,LinkOption.NOFOLLOW_LINKS))continue;
            try(Reader reader=new InputStreamReader(input(path,HISTORY_BYTES),StandardCharsets.UTF_8.newDecoder())) {
                char[] buffer=new char[8192];StringBuilder line=new StringBuilder();boolean tooLong=false;int count;
                while((count=reader.read(buffer))!=-1)for(int i=0;i<count;i++){
                    char c=buffer[i];
                    if(c=='\n'){keepLine(lines,line,tooLong);line.setLength(0);tooLong=false;}
                    else if(line.length()<=LINE_CHARS)line.append(c);else tooLong=true;
                }
                keepLine(lines,line,tooLong);
            }
        }
        return ("["+String.join(",",lines)+"]").getBytes(StandardCharsets.UTF_8);
    }
    static Snapshot load(Path folder){
        return new Snapshot(object(folder.resolve("status.json")),available(()->history(folder)),evaluation(folder));
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
        reader.scheduleAtFixedRate(()->snapshot=load(folder),0,5,TimeUnit.SECONDS);
        HttpServer server=HttpServer.create(new InetSocketAddress(address,port),16);
        ExecutorService requests=new ThreadPoolExecutor(2,2,0L,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(16),new ThreadPoolExecutor.AbortPolicy());server.setExecutor(requests);
        server.createContext("/",exchange->{
            try(exchange) {
                if(!exchange.getRequestMethod().equals("GET")){exchange.sendResponseHeaders(405,-1);return;}
                String path=exchange.getRequestURI().getPath();Snapshot s=snapshot;byte[] body;String type;int code=200;
                if(path.equals("/")){body=page;type="text/html; charset=utf-8";}
                else if(path.equals("/api/status")||path.equals("/api/history")||path.equals("/api/evaluation")) {
                    type="application/json; charset=utf-8";body=s==null?null:path.endsWith("status")?s.status():path.endsWith("history")?s.history():s.evaluation();
                    if(body==null){body=UNAVAILABLE;code=503;}
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
