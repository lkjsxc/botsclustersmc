package org.botsclustersmc.tests;

import org.botsclustersmc.core.*;
import org.botsclustersmc.training.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Actual filesystem regressions, with no Minecraft process or EULA acceptance. */
public final class PersistenceTest {
    private static int checks;
    interface Operation {void run()throws Exception;}
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void rejects(Operation operation,String message)throws Exception {
        boolean rejected=false;try{operation.run();}catch(IOException|IllegalArgumentException expected){rejected=true;}
        check(rejected,message);
    }
    public static void main(String[] args)throws Exception {
        Path root=Files.createTempDirectory("bcmc-persistence-").toRealPath();
        try {
            Policy initial=Policy.initialize(7);float[] gradient=new float[Policy.PARAMETERS];gradient[0]=1;
            Adam.Update update=new Adam().update(initial,gradient,32,.0003);
            TrainingState state=new TrainingState(update.policy(),update.optimizer(),new Course(2,7).encode());
            Path checkpoint=root.resolve("training.bcmc"),cached=root.resolve("policy.bcmc"),exported=root.resolve("deploy/policy.bcmc");
            state.write(checkpoint);PolicyFile.write(cached,initial);
            rejects(()->CheckpointTool.verifyExport(checkpoint,cached),"stale but valid policy is not the checkpoint");
            Policy result=CheckpointTool.exportPolicy(checkpoint,exported);
            check(result.updates()==1&&result.samples()==32,"canonical optimizer counters exported");
            CheckpointTool.verifyExport(checkpoint,exported);checks++;
            check(Arrays.equals(result.copyWeights(),state.policy().copyWeights()),"exact checkpoint weights exported");
            check(PolicyFile.read(cached).updates()==0,"unused cached policy not migrated or modified");
            byte[] checkpointBytes=Files.readAllBytes(checkpoint),exportBytes=Files.readAllBytes(exported);
            rejects(()->CheckpointTool.exportPolicy(checkpoint,checkpoint),"export cannot overwrite its source");
            check(Arrays.equals(checkpointBytes,Files.readAllBytes(checkpoint)),"source preserved on invalid destination");
            byte[] corrupt=checkpointBytes.clone();corrupt[20]^=1;Files.write(checkpoint,corrupt);
            rejects(()->CheckpointTool.exportPolicy(checkpoint,exported),"corrupt checkpoint cannot fall back to cached policy");
            check(Arrays.equals(exportBytes,Files.readAllBytes(exported)),"failed export preserves previous completed export");
            check(Arrays.equals(corrupt,Files.readAllBytes(checkpoint)),"damaged checkpoint not repaired by initialization");
            Files.write(checkpoint,checkpointBytes);
            Path absent=root.resolve("absent.bcmc"),noOutput=root.resolve("must-not-exist/policy.bcmc");
            rejects(()->CheckpointTool.exportPolicy(absent,noOutput),"missing checkpoint rejected");
            check(!Files.exists(noOutput.getParent()),"missing input produces no output directories");
            for(int bound:new int[]{-1,0,Integer.MAX_VALUE})rejects(()->PolicyFile.readBounded(checkpoint,bound),"invalid read bound rejected");
            rejects(()->PolicyFile.readBounded(checkpoint,16),"oversized input rejected");
            symlinks(root,checkpoint,cached,exported);
            System.out.println("PASS persistence checks="+checks);
        }finally{try(var paths=Files.walk(root)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}}
    }
    private static void symlinks(Path root,Path checkpoint,Path cached,Path exported)throws Exception {
        Path link=root.resolve("alias");
        try{Files.createSymbolicLink(link,root);}
        catch(FileSystemException|UnsupportedOperationException error){
            if(!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win"))throw error;
            System.out.println("SKIP symlink fixtures: this Windows account cannot create links");return;
        }
        rejects(()->PolicyFile.read(link.resolve("policy.bcmc")),"parent symlink policy read rejected");
        rejects(()->TrainingState.read(link.resolve("training.bcmc")),"parent symlink checkpoint read rejected");
        rejects(()->CheckpointTool.exportPolicy(link.resolve("training.bcmc"),exported),"export cannot read through parent symlink");
        rejects(()->PolicyFile.write(link.resolve("new-directory/policy.bcmc"),Policy.initialize(7)),"symlink checked before creating directories");
        check(!Files.exists(root.resolve("new-directory")),"no write side effects beyond parent symlink");
        Path leaf=root.resolve("leaf.bcmc");Files.createSymbolicLink(leaf,cached);
        byte[] original=Files.readAllBytes(cached);
        rejects(()->PolicyFile.read(leaf),"leaf symlink read rejected");
        rejects(()->CheckpointTool.exportPolicy(checkpoint,leaf),"leaf symlink export rejected");
        check(Arrays.equals(original,Files.readAllBytes(cached)),"link target preserved");
    }
}
