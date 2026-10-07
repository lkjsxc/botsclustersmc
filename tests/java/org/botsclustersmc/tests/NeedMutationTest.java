package org.botsclustersmc.tests;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.tools.ToolProvider;

/** Compiled semantic counterexamples, not compilation-failure-as-success or Minecraft execution. */
public final class NeedMutationTest {
    record Mutation(String name,String from,String to,String test,String assertion) {}
    static final List<Mutation> CASES=List.of(
        new Mutation("population-multiplier", "reward / contract.members(),", "reward / 1,", "scale", "team return separate from accounting share"),
        new Mutation("unknown-interval", "stamps[slot] != tick || utility[slot] < 0", "stamps[slot] != tick", "unknown", "unknown reward claim rejected"),
        new Mutation("decision-frequency", "reward / contract.members(),", "reward / contract.members() / (end - start),", "timing", "partition-invariant return"),
        new Mutation("replayed-cursor", "cursor[member] = end; claimed[member]", "cursor[member] = start; claimed[member]", "cursor", "duplicate reward claim rejected"),
        new Mutation("premature-score", "current != null && current.tick() == contract.horizon() && unknownTicks == 0", "current != null && unknownTicks == 0", "incomplete", "unfinished episode has no score"),
        new Mutation("terminal-bootstrap", "end == contract.horizon() ? 0 : factor", "factor", "terminal", "terminal bootstrap is zero")
    );
    private static String run(Path compiled,Path log,String test)throws Exception {
        String javaBin=Path.of(System.getProperty("java.home"),"bin","java").toString();
        String cp=compiled+java.io.File.pathSeparator+System.getProperty("java.class.path");
        Process child=new ProcessBuilder(javaBin,"-cp",cp,"org.botsclustersmc.tests.NeedWindowTest",test)
            .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!child.waitFor(30,TimeUnit.SECONDS)){child.destroyForcibly();child.waitFor();throw new AssertionError("unit-test child timeout, not mutation detection");}
        if(Files.size(log)>65536)throw new AssertionError("unexpected unit-test output volume");
        String output=Files.readString(log,StandardCharsets.UTF_8);
        return child.exitValue()+"\n"+output;
    }
    public static void main(String[] args)throws Exception {
        Path root=Path.of("").toAbsolutePath(), source=root.resolve("tests/needs/org/botsclustersmc/needs/NeedWindow.java");
        String original=Files.readString(source);Path temporary=Files.createTempDirectory(root.resolve(".build"),"need-mutations-");
        // A successful unmodified process under the same classpath is required before corrupting code.
        String positive=run(temporary,temporary.resolve("baseline.log"),"scale");
        if(!positive.startsWith("0\nPASS need window"))throw new AssertionError("positive child failed: "+positive);
        int detected=0;
        for(Mutation mutation:CASES){
            if(original.indexOf(mutation.from())<0||original.indexOf(mutation.from())!=original.lastIndexOf(mutation.from()))
                throw new AssertionError("mutation does not have exactly one source target: "+mutation.name());
            Path dir=Files.createDirectory(temporary.resolve(mutation.name()));
            Path copy=dir.resolve("NeedWindow.java");Files.writeString(copy,original.replace(mutation.from(),mutation.to()));
            int exit=ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,"--release","21","-proc:none",
                "-cp",System.getProperty("java.class.path"),"-d",dir.toString(),copy.toString());
            if(exit!=0)throw new AssertionError("compilation failed, not mutation detection: "+mutation.name());
            String evidence=run(dir,dir.resolve("test.log"),mutation.test());
            if(!evidence.startsWith("1\n")||!evidence.contains("java.lang.AssertionError: "+mutation.assertion()))
                throw new AssertionError("intended assertion did not detect "+mutation.name()+": "+evidence);
            System.out.println("PASS compiled need mutation "+mutation.name()+" -> "+mutation.assertion());detected++;
        }
        System.out.println("PASS need semantic mutations "+detected+"/"+CASES.size()+"; retained unit-test logs "+temporary);
    }
}
