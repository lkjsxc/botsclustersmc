import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Host supervision only. No gameplay actions, rewards or policy updates. */
public final class HostTools {
    static String read(Path p,int limit) throws IOException {
        if(Files.size(p)>limit) throw new IOException("Oversized file: "+p);
        return Files.readString(p,StandardCharsets.UTF_8);
    }
    static int vi(InputStream in) throws IOException {
        int n=0;
        for(int i=0;i<5;i++) { int b=in.read(); if(b<0) throw new EOFException(); n|=(b&127)<<(7*i); if((b&128)==0) return n; }
        throw new IOException("Oversized VarInt");
    }
    static void vi(OutputStream out,int n) throws IOException {
        do {int b=n&127;n>>>=7;out.write(n==0?b:b|128);} while(n!=0);
    }
    static void packet(OutputStream out,byte[] bytes) throws IOException {vi(out,bytes.length);out.write(bytes);out.flush();}
    static JsonObject probe(String host,int port) throws Exception {
        try(Socket socket=new Socket()) {
            socket.connect(new InetSocketAddress(host,port),1000);socket.setSoTimeout(2000);
            ByteArrayOutputStream b=new ByteArrayOutputStream();vi(b,0);vi(b,774);
            byte[] name=host.getBytes(StandardCharsets.UTF_8);vi(b,name.length);b.write(name);
            b.write(port>>8);b.write(port&255);vi(b,1);packet(socket.getOutputStream(),b.toByteArray());
            packet(socket.getOutputStream(),new byte[]{0});
            InputStream in=socket.getInputStream();int length=vi(in);
            if(length<3||length>1048576) throw new IOException("Invalid status packet size");
            byte[] bytes=in.readNBytes(length);if(bytes.length!=length) throw new EOFException();
            ByteArrayInputStream body=new ByteArrayInputStream(bytes);
            if(vi(body)!=0) throw new IOException("Not a status response");
            int size=vi(body);if(size<0||size!=body.available()) throw new IOException("Invalid status string length");
            JsonObject j=JsonParser.parseString(new String(body.readNBytes(size),StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject version=j.getAsJsonObject("version");
            if(version.get("protocol").getAsInt()!=774||!version.get("name").getAsString().contains("1.21.11"))
                throw new IOException("Expected Minecraft 1.21.11 / protocol 774, got "+version);
            return j;
        }
    }
    static boolean campus(Path root,String run,int bots) throws Exception {
        Path lab=root.resolve(".runtime/lab"),fatal=lab.resolve("fatal.txt");
        if(Files.exists(fatal)) {
            String error=read(fatal,8192);
            if(error.startsWith(run+" ")) throw new IOException("Academy failed: "+error.strip());
            throw new IOException("Stale fatal receipt");
        }
        Path ready=lab.resolve("bridge.ready"),manifest=lab.resolve("campus.ready");
        if(!Files.exists(ready)||!Files.exists(manifest)) return false;
        if(!read(ready,1024).equals("BCMCLAB2 "+run+" ready\n")) throw new IOException("Stale bridge receipt");
        StringBuilder expected=new StringBuilder("BCMCCAMPUS1 "+run+" "+bots+" 8 16 96\n");
        for(int i=0;i<bots;i++) expected.append(i).append(' ').append(i%8*16).append(' ').append(i/8*16).append('\n');
        if(!read(manifest,8192).equals(expected.toString())) throw new IOException("Incomplete or incompatible campus manifest");
        return true;
    }
    static boolean status(Path root,String run,int bots,String prefix,boolean enforce) throws Exception {
        Path file=root.resolve("state/status.json");if(!Files.exists(file)) return false;
        JsonObject j=JsonParser.parseString(read(file,1048576)).getAsJsonObject();
        long now=System.currentTimeMillis()/1000,updated=j.get("updated").getAsLong();
        if(!j.get("run_id").getAsString().equals(run)) throw new IOException("Stale learner run ID");
        if(now-updated>120||updated>now+60) throw new IOException("Learner heartbeat is stale or in the future");
        String error=j.get("error").getAsString();if(!error.isEmpty()) throw new IOException("Learner failed: "+error);
        JsonArray agents=j.getAsJsonArray("agents");if(agents.size()!=bots) throw new IOException("Wrong learner population");
        int online=0,acted=0;long min=Long.MAX_VALUE,max=0;
        for(int id=0;id<bots;id++) {
            JsonObject a=agents.get(id).getAsJsonObject();
            if(a.get("id").getAsInt()!=id||!a.get("name").getAsString().equals(prefix+String.format(Locale.ROOT,"%02d",id)))
                throw new IOException("Missing, duplicate or wrong bot identity at "+id);
            long last=a.get("last_seen").getAsLong(),steps=a.get("steps").getAsLong();
            if(last>now+60) throw new IOException("Future bot heartbeat");
            if(enforce&&now-last>240) throw new IOException("Bot "+id+" has been stale for over 240 seconds");
            boolean live=a.get("online").getAsBoolean();if(live) online++;
            if(live&&steps>0&&a.get("spawns").getAsLong()>0&&now-last<=120) acted++;
            min=Math.min(min,steps);max=Math.max(max,steps);
        }
        JsonObject out=new JsonObject();out.addProperty("run_id",run);out.addProperty("updated",now);
        out.addProperty("online",online);out.addProperty("acted",acted);out.addProperty("required",bots);
        out.addProperty("min_steps",min);out.addProperty("max_steps",max);
        out.addProperty("policy_version",j.get("version").getAsLong());out.addProperty("trained_samples",j.get("trained_samples").getAsLong());
        System.out.println(out);return acted==bots;
    }
    static void rotate(Path file) throws IOException {
        Files.deleteIfExists(Path.of(file+".4"));
        for(int i=3;i>=1;i--) {Path p=Path.of(file+"."+i);if(Files.exists(p)) Files.move(p,Path.of(file+"."+(i+1)),StandardCopyOption.REPLACE_EXISTING);}
        if(Files.exists(file)) Files.move(file,Path.of(file+".1"),StandardCopyOption.REPLACE_EXISTING);
    }
    static void log(Path file) throws IOException {
        Files.createDirectories(file.getParent());long size=Files.exists(file)?Files.size(file):0;
        OutputStream out=Files.newOutputStream(file,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        try {byte[] b=new byte[8192];int n;while((n=System.in.read(b))>=0) {
            if(size+n>8*1024*1024) {out.close();rotate(file);out=Files.newOutputStream(file);size=0;}
            out.write(b,0,n);out.flush();size+=n;
        }} finally {out.close();}
    }
    public static void main(String[] a) {
        try {
            switch(a[0]) {
                case "available" -> {try(ServerSocket s=new ServerSocket()) {s.setReuseAddress(false);s.bind(new InetSocketAddress(a[1],Integer.parseInt(a[2])));}}
                case "probe" -> System.out.println(probe(a[1],Integer.parseInt(a[2])));
                case "campus" -> {if(!campus(Path.of(a[1]),a[2],Integer.parseInt(a[3]))) System.exit(2);}
                case "status" -> {if(!status(Path.of(a[1]),a[2],Integer.parseInt(a[3]),a[4],Boolean.parseBoolean(a[5]))) System.exit(2);}
                case "log" -> log(Path.of(a[1]));
                default -> throw new IllegalArgumentException("Unknown host-tools command");
            }
        } catch(Exception e) {System.err.println(e.getClass().getSimpleName()+": "+e.getMessage());System.exit(1);}
    }
}
