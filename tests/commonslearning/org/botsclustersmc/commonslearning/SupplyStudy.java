package org.botsclustersmc.commonslearning;

import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import org.botsclustersmc.core.*;
import org.botsclustersmc.training.*;

/** Opt-in bounded experiment. Fresh in-memory synthetic policies; no server, checkpoint or export. */
public final class SupplyStudy {
    private static final long[] SEEDS={2026100801L,2026100802L,2026100803L},EVAL={2026100881L,2026100882L};
    private static final Set<Integer> BOUNDARIES=Set.of(0,100,300,600);
    private static String ints(int[] values){StringJoiner join=new StringJoiner(",");for(int value:values)join.add(Integer.toString(value));return join.toString();}
    private static String digest(Policy p)throws NoSuchAlgorithmException{
        ByteBuffer buffer=ByteBuffer.allocate(16+Policy.PARAMETERS*4).order(ByteOrder.BIG_ENDIAN);
        buffer.putLong(p.updates()).putLong(p.samples());for(float value:p.copyWeights())buffer.putFloat(value);
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(buffer.array()));
    }
    private static void row(BufferedWriter writer,Object... fields)throws IOException{
        for(int i=0;i<fields.length;i++){if(i>0)writer.write('\t');writer.write(fields[i].toString());}writer.newLine();
    }
    private static void evaluate(BufferedWriter writer,Policy p,long seed,boolean visible,boolean tensor,String arm,long[] evals,int boundary)throws Exception{
        String hash=digest(p);
        for(long eval:evals)for(int n:new int[]{2,8}){
            int fast=0,full=0,first=0,wrong=0;
            for(int episode=0;episode<256;episode++){
                var result=SupplyEpisode.play(p,visible,eval,episode,n,false,tensor);var o=result.outcome();int[] steps=o.serviceSteps();
                if(steps[0]>0&&steps[0]<=2&&steps[1]>0&&steps[1]<=2)fast++;
                if(steps[0]>0&&steps[1]>0)full++;
                if(steps[o.first()]==1)first++;wrong+=o.wrongDeposits();
                row(writer,arm,seed,boundary,eval,n,episode,p.updates(),p.samples(),hash,o.first(),ints(o.initial()),
                    ints(result.actions()),ints(result.orders()),ints(steps),ints(o.consumed()),ints(o.bank()),ints(o.remaining()),ints(o.deposited()),ints(o.withdrawn()),o.wrongDeposits());
            }
            writer.flush();System.out.printf(Locale.ROOT,"EVAL arm=%s seed=%d boundary=%d eval=%d members=%d fast=%d/256 full=%d/256 first=%d/256 wrong=%d updates=%d samples=%d%n",arm,seed,boundary,eval,n,fast,full,first,wrong,p.updates(),p.samples());
        }
    }
    public static void main(String[] args)throws Exception{run(args,false);}
    static void run(String[] args,boolean binding)throws Exception{
        long[] seeds=binding?new long[]{2026100811L,2026100812L,2026100813L}:SEEDS;
        long[] evals=binding?new long[]{2026100891L,2026100892L}:EVAL;
        if(args.length!=2||!args[0].equals("--new-output"))throw new IllegalArgumentException("SupplyStudy --new-output <new directory>; fresh synthetic study only");
        Path dir=Path.of(args[1]).toAbsolutePath().normalize();
        for(Path part=dir;part!=null;part=part.getParent())if(Files.isSymbolicLink(part))throw new IOException("symlinked study output");
        Files.createDirectory(dir);
        Files.writeString(dir.resolve("identity.txt"),(binding?TensorInputs.ID:SupplyRoom.ID)+"\n600 attempted updates, 256 offered transitions/update/arm; 3 seeds; fixed 0/100/300/600 evaluation.\nNo policy/checkpoint export, no Minecraft, no retained-skill claim.\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
        try(BufferedWriter trials=Files.newBufferedWriter(dir.resolve("trials.tsv"),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
            BufferedWriter updates=Files.newBufferedWriter(dir.resolve("updates.tsv"),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW)){
            row(trials,"arm","learning_seed","boundary","eval_seed","population","case","updates","samples","policy_sha256","first","initial","actions","orders","service_steps","consumed","bank","remaining","deposited","withdrawn","wrong");
            row(updates,"arm","learning_seed","attempt","offered","accepted","updates","samples","backtracks","learning_rate","mean_kl","max_kl");
            for(long seed:seeds)for(int armIndex=0;armIndex<2;armIndex++){
                boolean visible=binding||armIndex==0,tensor=binding&&armIndex==0;
                String arm=binding?(tensor?"tensor":"plain"):(visible?"visible":"hidden");
                Policy policy=Policy.initialize(seed);Adam adam=new Adam();evaluate(trials,policy,seed,visible,tensor,arm,evals,0);
                for(int update=1;update<=600;update++){
                    List<Trajectory> batch=new ArrayList<>();
                    for(int episode=0;episode<32;episode++)batch.addAll(SupplyEpisode.play(policy,visible,seed,(update-1)*32+episode,2,true,tensor).trajectories());
                    Gradient.Result gradient=Gradient.compute(policy,batch,TaskBalance.forBatch(batch));
                    if(gradient.samples()!=256)throw new IllegalStateException("declared sample count changed");
                    var checked=UpdateGuard.update(policy,adam,gradient.weights(),gradient.samples(),batch);int accepted=0;
                    if(checked.update()!=null){policy=checked.update().policy();adam=checked.update().optimizer();accepted=256;}
                    row(updates,arm,seed,update,256,accepted,policy.updates(),policy.samples(),checked.backtracks(),checked.learningRate(),checked.change().mean(),checked.change().maximum());
                    if(BOUNDARIES.contains(update)){updates.flush();evaluate(trials,policy,seed,visible,tensor,arm,evals,update);}
                }
            }
        }
        Files.writeString(dir.resolve("completed.txt"),"All declared trajectories and evaluation cases completed. Learning gates require independent validation.\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
        System.out.println("COMPLETED fixed synthetic supply study; validate evidence before claiming a learning gate");
    }
}
