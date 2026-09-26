import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Exercise the launcher boundary without starting a server or reading real Academy data. */
public final class AcademyBoundaryTest {
    private static int checks;
    private interface Attempt {void run()throws Exception;}
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void rejects(Attempt action,String why)throws Exception{
        try{action.run();}catch(IOException expected){checks++;return;}
        throw new AssertionError("Accepted: "+why);
    }
    private static Host configured(Path path)throws Exception{
        Host host=new Host();host.config.put("ACADEMY",path.toString());return host;
    }
    private static void owned(Path path)throws Exception{
        Files.createDirectories(path);Files.writeString(path.resolve(".botsclustersmc-academy"),"botsclustersmc-owned-training\n");
    }
    private static byte[] git(Path root,String... args)throws Exception{
        List<String> command=new ArrayList<>(List.of("git","-C",root.toString()));command.addAll(List.of(args));
        ProcessBuilder builder=new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.INHERIT);
        for(String key:List.of("GIT_DIR","GIT_WORK_TREE","GIT_INDEX_FILE","GIT_COMMON_DIR"))builder.environment().remove(key);
        Process process=builder.start();byte[] output=process.getInputStream().readAllBytes();
        if(process.waitFor()!=0)throw new IOException("Git fixture command failed: "+args[0]);
        return output;
    }
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("bcmc-academy-boundary-").toRealPath();
        try{
            Path fresh=root.resolve("arbitrary-session");Host first=configured(fresh);first.ownAcademy();
            check(Files.readString(fresh.resolve(".gitignore")).equals("*\n"),"custom-name Academy ignores all children");
            first.ownAcademy();try(var children=Files.list(fresh)){check(children.count()==2,"repeated ownership check is idempotent");}
            Path previous=root.resolve("previously-owned");owned(previous);
            Files.writeString(previous.resolve("world.dat"),"synthetic-world");configured(previous).ownAcademy();
            check(Files.readString(previous.resolve("world.dat")).equals("synthetic-world"),"old owned data is not rewritten");
            check(Files.readString(previous.resolve(".gitignore")).equals("*\n"),"old owned Academy gains protection");
            Path unowned=root.resolve("unowned");Files.createDirectories(unowned);Files.writeString(unowned.resolve("keep"),"original");
            rejects(()->configured(unowned).ownAcademy(),"nonempty unowned directory");
            check(!Files.exists(unowned.resolve(".gitignore"))&&!Files.exists(unowned.resolve(".botsclustersmc-academy")),"unowned directory not modified");
            Path wrong=root.resolve("wrong-marker");owned(wrong);Files.writeString(wrong.resolve(".botsclustersmc-academy"),"not-owned\n");
            rejects(()->configured(wrong).ownAcademy(),"wrong marker");check(!Files.exists(wrong.resolve(".gitignore")),"wrong marker does not acquire ignore");
            rejects(()->configured(Host.ROOT).ownAcademy(),"checkout cannot become Academy");
            for(String rules:List.of("","!policy.bcmc\n","*\n!server/\n","*\r\n","*\n# custom\n")){
                Path custom=root.resolve("custom-"+checks);owned(custom);Files.writeString(custom.resolve(".gitignore"),rules);
                rejects(()->configured(custom).ownAcademy(),"noncanonical private-data boundary");
                check(Files.readString(custom.resolve(".gitignore")).equals(rules),"custom rules not silently overwritten");
            }
            Path directory=root.resolve("ignore-is-directory");owned(directory);Files.createDirectory(directory.resolve(".gitignore"));
            rejects(()->configured(directory).ownAcademy(),"ignore directory");
            Path target=root.resolve("link-target");Files.writeString(target,"unchanged");
            Path linked=root.resolve("linked-ignore");owned(linked);
            try{
                Files.createSymbolicLink(linked.resolve(".gitignore"),target);
                rejects(()->configured(linked).ownAcademy(),"symlinked ignore");
                check(Files.readString(target).equals("unchanged"),"symlink target untouched");
                Files.delete(linked.resolve(".gitignore"));
                Files.createSymbolicLink(linked.resolve(".gitignore"),root.resolve("absent-target"));
                rejects(()->configured(linked).ownAcademy(),"dangling ignore symlink");
                check(!Files.exists(root.resolve("absent-target")),"dangling target not created");
                Files.delete(linked.resolve(".gitignore"));
            }catch(FileSystemException|UnsupportedOperationException error){
                if(!Host.isWindows())throw error;
                System.out.println("SKIP Windows symlink fixture: "+error.getMessage());
            }
            // Real Git semantics, including nested !negations. This test requires Git, not the launcher.
            Path repository=root.resolve("git-fixture");Files.createDirectory(repository);git(repository,"init","--quiet");
            Files.writeString(repository.resolve("README.md"),"synthetic source\n");
            Path custom=repository.resolve("session-with-any-name");configured(custom).ownAcademy();
            Path data=custom.resolve("server/plugins/BotsClustersMC");Files.createDirectories(data);
            Files.writeString(custom.resolve("server/.gitignore"),"!plugins/\n");
            Files.writeString(data.resolve("policy.bcmc"),"not a real model");
            Files.writeString(custom.resolve("run.lock"),"synthetic");
            Files.writeString(custom.resolve("operator-key.txt"),"not a real credential");
            String status=new String(git(repository,"status","--porcelain","--untracked-files=all"),StandardCharsets.UTF_8);
            check(status.contains("README.md"),"Git fixture detects source");
            check(!status.contains("session-with-any-name"),"Git status omits the entire custom Academy");
            String ignored=new String(git(repository,"check-ignore","--",data.resolve("policy.bcmc").toString(),custom.resolve("operator-key.txt").toString()),StandardCharsets.UTF_8);
            check(ignored.contains("policy.bcmc")&&ignored.contains("operator-key.txt"),"Git confirms policy and credentials ignored");
            git(repository,"add","--all");
            String staged=new String(git(repository,"diff","--cached","--name-only"),StandardCharsets.UTF_8);
            check(staged.strip().equals("README.md"),"broad git add cannot stage ordinary Academy data");
        }finally{Host.deleteTree(root);}
        System.out.println("PASS owned-Academy Git boundary checks="+checks);
    }
}
