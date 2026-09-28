import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Exact byte/line bounds without timing, Minecraft, or operator files. */
public final class MonitorReadTest {
    static int checks;
    static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    interface Attempt {void run()throws Exception;}
    static void rejected(Attempt attempt,String message)throws Exception{
        boolean failed=false;try{attempt.run();}catch(IOException expected){failed=true;}check(failed,message);
    }
    static String text(byte[] bytes){return bytes==null?null:new String(bytes,StandardCharsets.UTF_8);}
    static String row(int n){return "{\"n\":"+n+"}";}
    public static void main(String[] args)throws Exception{
        for(int limit:new int[]{0,1,7,8192}){
            try(var input=new Monitor.BoundedInput(new ByteArrayInputStream(new byte[limit]),limit)){
                check(input.read(new byte[0])==0,"zero-length read");
                check(input.readAllBytes().length==limit,"exact byte boundary "+limit);
                check(input.read()==-1,"EOF at boundary "+limit);
            }
            try(var input=new Monitor.BoundedInput(new ByteArrayInputStream(new byte[limit+1]),limit)){
                rejected(input::readAllBytes,"extra byte rejected "+limit);
                rejected(()->input.read(new byte[1]),"failed stream stays failed "+limit);
            }
        }
        try(var input=new Monitor.BoundedInput(new ByteArrayInputStream(new byte[4]),3)){
            check(input.read()==0,"single read");
            check(input.read(new byte[2])==2,"mixed reads use one bound");
            rejected(input::read,"mixed reads reject extra byte");
        }
        Path folder=Files.createTempDirectory("bcmc-monitor-read-").toAbsolutePath();
        Path status=folder.resolve("status.json"),history=folder.resolve("history.jsonl");
        Path previous=folder.resolve("history.previous.jsonl"),report=folder.resolve("evaluation.json");
        Path heartbeat=folder.resolve("evaluation-status.json");
        boolean passed=false;
        try{
            var empty=Monitor.load(folder);
            check(empty.status()==null,"missing current status");
            check("[]".equals(text(empty.history())),"missing history is empty");
            check("{\"result\":null,\"monitor\":null}".equals(text(empty.evaluation())),"missing evaluation");
            String exact="{\"x\":\""+"a".repeat(Monitor.OBJECT_BYTES-8)+"\"}";
            Files.writeString(status,exact);
            check(Arrays.equals(exact.getBytes(StandardCharsets.UTF_8),Monitor.load(folder).status()),"exact 128-KiB object");
            Files.writeString(status,exact+" ");
            check(Monitor.load(folder).status()==null,"one byte over object limit");
            for(String invalid:List.of("","[]","not-json","{\"x\":1","\"x\":1}")){
                Files.writeString(status,invalid);
                check(Monitor.load(folder).status()==null,"invalid object envelope");
            }
            Files.write(status,new byte[]{'{','"',(byte)0xc3,'(','"',':','0','}'});
            check(Monitor.load(folder).status()==null,"malformed UTF-8 object");
            Files.writeString(status," \n{\"label\":\"共有\"}\n");
            check(text(Monitor.load(folder).status()).equals(" \n{\"label\":\"共有\"}\n"),"valid UTF-8 is preserved");
            Files.delete(status);Files.createDirectory(status);
            check(Monitor.load(folder).status()==null,"directory is not a status file");
            Files.delete(status);Files.writeString(status,"{}");

            String line="{\"x\":\""+"a".repeat(Monitor.LINE_CHARS-8)+"\"}";
            Files.writeString(history,line+"\r\n"+line+" \n"+row(9));
            check(text(Monitor.load(folder).history()).equals("["+line+","+row(9)+"]"),"exact CRLF line, oversized line, final no-newline");
            Files.writeString(previous,String.join("\n",java.util.stream.IntStream.range(0,400).mapToObj(MonitorReadTest::row).toList())+"\n");
            Files.writeString(history,String.join("\n",java.util.stream.IntStream.range(400,800).mapToObj(MonitorReadTest::row).toList())+"\n");
            String retained="["+String.join(",",java.util.stream.IntStream.range(80,800).mapToObj(MonitorReadTest::row).toList())+"]";
            check(text(Monitor.load(folder).history()).equals(retained),"last 720 rows ordered across rotation");
            Files.delete(previous);
            try(var file=new RandomAccessFile(history.toFile(),"rw")){file.setLength(Monitor.HISTORY_BYTES);}
            check(Monitor.load(folder).history()!=null,"exact history byte bound");
            try(var file=new RandomAccessFile(history.toFile(),"rw")){file.setLength(Monitor.HISTORY_BYTES+1L);}
            check(Monitor.load(folder).history()==null,"oversized history unavailable, not empty");
            check("{}".equals(text(Monitor.load(folder).status())),"oversized history leaves status available");
            Files.write(history,new byte[]{(byte)0xc3,'('});
            check(Monitor.load(folder).history()==null,"malformed UTF-8 history");
            Files.delete(history);Files.createDirectory(history);
            check(Monitor.load(folder).history()==null,"existing directory is not absent history");
            Files.delete(history);

            Files.writeString(report,"{}");Files.writeString(heartbeat,"[]");
            check(text(Monitor.load(folder).evaluation()).equals("{\"result\":{},\"monitor\":null}"),"invalid heartbeat preserves report");
            Files.writeString(report,"[]");Files.writeString(heartbeat,"{}");
            check(text(Monitor.load(folder).evaluation()).equals("{\"result\":null,\"monitor\":{}}"),"invalid report preserves heartbeat");

            // A real file grows after opening; the streaming guard still enforces its original cap.
            Path growing=folder.resolve("growing");
            Files.write(growing,new byte[4]);
            try(var input=new Monitor.BoundedInput(Files.newInputStream(growing),4)){
                Files.write(growing,new byte[4],StandardOpenOption.APPEND);
                rejected(input::readAllBytes,"file growth after open cannot bypass the read bound");
            }
            // Windows may lack symlink privilege. Linux must exercise no-follow handling.
            if(!System.getProperty("os.name").startsWith("Windows")){
                Path target=folder.resolve("target");
                Files.writeString(target,"{}");Files.delete(status);Files.createSymbolicLink(status,target);
                check(Monitor.load(folder).status()==null,"status symlink rejected");
                Files.delete(status);Files.writeString(status,"{}");
                Files.createSymbolicLink(history,target);
                check(Monitor.load(folder).history()==null,"history symlink is invalid, not absent");
                Files.delete(history);Files.delete(report);Files.createSymbolicLink(report,target);
                check(text(Monitor.load(folder).evaluation()).equals("{\"result\":null,\"monitor\":{}}"),"report symlink preserves heartbeat");
                Path alias=folder.resolve("alias");Files.createSymbolicLink(alias,folder);
                check(Monitor.load(alias).status()==null,"symlink ancestor rejected");
            }else System.out.println("SKIP symlink cases on Windows: privilege not assumed");
            passed=true;
        }finally{
            if(passed){try(var paths=Files.walk(folder)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
            else System.err.println("Retained monitor read failure evidence: "+folder);
        }
        System.out.println("PASS monitor exact byte/line/rotation/read-growth bounds; "+checks+" checks");
    }
}
