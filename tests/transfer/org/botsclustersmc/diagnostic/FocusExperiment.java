package org.botsclustersmc.diagnostic;

import org.botsclustersmc.core.*;
import org.botsclustersmc.training.*;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Explicit offline research construction. No legacy decoder is shipped in either runtime. */
public final class FocusExperiment {
    private FocusExperiment() {}
    public static void create(TrainingState source,Path destination,int task)throws IOException {
        Task.at(task);
        if(source.policy().learningTask()!=-1 || source.policy().anchor()!=null)
            throw new IllegalArgumentException("source must be an unfocused shared policy");
        PolicyFile.managedPath(destination);Files.createDirectory(destination);
        for(boolean protectedArm:new boolean[]{false,true}) {
            String name=protectedArm?"protected":"control";
            Policy policy=Policy.focus(source.policy(),task,protectedArm);
            TrainingState state=new TrainingState(policy,source.optimizer(),source.course());
            Files.write(destination.resolve(name+"-training.bcmc"),state.encode(),StandardOpenOption.CREATE_NEW);
            Files.write(destination.resolve(name+"-policy.bcmc"),PolicyFile.encode(policy),StandardOpenOption.CREATE_NEW);
        }
        Files.write(destination.resolve("base-policy.bcmc"),PolicyFile.encode(source.policy()),StandardOpenOption.CREATE_NEW);
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=4)throw new IllegalArgumentException("FocusExperiment TRUSTED_SOURCE_RUNTIME SOURCE_CHECKPOINT NEW_DIRECTORY TASK");
        Path jar=PolicyFile.managedPath(Path.of(args[0])),input=PolicyFile.managedPath(Path.of(args[1]));
        Path out=PolicyFile.managedPath(Path.of(args[2]));int task=Integer.parseInt(args[3]);Task.at(task);
        if(out.startsWith(input.getParent()) || input.startsWith(out))throw new IOException("output must be separate from input");
        byte[] original=PolicyFile.readBounded(input,32*1024*1024);
        TrainingState imported;
        // Operator-selected trusted source implementation reads its own checkpoint format.
        // Isolated classloader prevents accidentally decoding with the new candidate classes.
        try(URLClassLoader loader=new URLClassLoader(new java.net.URL[]{jar.toUri().toURL()},ClassLoader.getPlatformClassLoader())) {
            Class<?> stateClass=loader.loadClass("org.botsclustersmc.training.TrainingState");
            Object state=stateClass.getMethod("decode",byte[].class).invoke(null,(Object)original);
            Object policy=stateClass.getMethod("policy").invoke(state),adam=stateClass.getMethod("optimizer").invoke(state);
            Class<?> p=policy.getClass(),a=adam.getClass();
            Policy current=new Policy((float[])p.getMethod("copyWeights").invoke(policy),
                    (long)p.getMethod("updates").invoke(policy),(long)p.getMethod("samples").invoke(policy));
            Adam optimizer=new Adam((float[])a.getMethod("first").invoke(adam),
                    (float[])a.getMethod("second").invoke(adam),(long)a.getMethod("step").invoke(adam));
            imported=new TrainingState(current,optimizer,(byte[])stateClass.getMethod("course").invoke(state));
        }
        create(imported,out,task);
        Files.write(out.resolve("source-training.bcmc"),original,StandardOpenOption.CREATE_NEW);
        String manifest=String.format(Locale.ROOT,"""
            {"kind":"one-frontier-warm-fork","experimental":true,"task":%d,
             "source_updates":%d,"source_samples":%d,"new_training_samples":0,
             "source_checkpoint_sha256":"%s","trusted_source_runtime_sha256":"%s",
             "control":"frontier-only updates; shared active function for every goal",
             "protected":"same frontier updates; immutable source for every other goal",
             "claim":"No skills earned by construction. Original Adam and course bytes retained. No live deployment."}
            """,task,imported.policy().updates(),imported.policy().samples(),GoalTransfer.digest(original),
                GoalTransfer.digest(PolicyFile.readBounded(jar,16*1024*1024)));
        Files.writeString(out.resolve("experiment.json"),manifest,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
        System.out.println(manifest);
    }
}
