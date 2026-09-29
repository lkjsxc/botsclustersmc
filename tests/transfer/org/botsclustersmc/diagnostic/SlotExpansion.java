package org.botsclustersmc.diagnostic;

import org.botsclustersmc.core.*;
import org.botsclustersmc.training.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Offline, operator-selected trusted-runtime experiment. Never shipped in a runtime JAR. */
public final class SlotExpansion {
    private SlotExpansion() {}
    public static final int OLD_LOGITS=105, OLD_OUTPUTS=106, OLD_B3=68736, OLD_PARAMETERS=68842;
    private static long checks;
    private static void require(boolean condition,String message) {
        checks++;if(!condition)throw new IllegalArgumentException(message);
    }
    public static int sourceRow(int row) {
        if(row<0||row>=Schema.OUTPUTS)throw new IllegalArgumentException("output row");
        return row==Schema.LOGITS?OLD_LOGITS:row<41?row:41+(row-41)%64;
    }
    /** Identical mapping for weights and each Adam moment, with no new random values. */
    public static float[] expand(float[] old) {
        require(Policy.W3==58560&&Schema.LOGITS==233&&Schema.OUTPUTS==234,"unexpected target contract");
        require(old.length==OLD_PARAMETERS,"source parameter dimensions");
        for(float v:old)require(Float.isFinite(v),"non-finite source coefficient");
        float[] result=new float[Policy.PARAMETERS];
        System.arraycopy(old,0,result,0,Policy.W3);
        for(int row=0;row<Schema.OUTPUTS;row++) {
            int from=sourceRow(row);
            System.arraycopy(old,58560+from*96,result,Policy.W3+row*96,96);
            result[Policy.B3+row]=old[OLD_B3+from];
        }
        return result;
    }
    private static Object call(Object object,String method)throws Exception {
        return object.getClass().getMethod(method).invoke(object);
    }
    private static long counter(Object object,String method)throws Exception {return (long)call(object,method);}
    private static float[] weights(Object p)throws Exception {return (float[])call(p,"copyWeights");}
    private static void equal(float[] a,float[] b,String message) {require(Arrays.equals(a,b),message);}
    private static void logits(float[] actual,float[] old) {
        require(old.length==OLD_OUTPUTS&&actual.length==Schema.OUTPUTS,"logit dimensions");
        for(int row=0;row<actual.length;row++)
            require(Float.floatToIntBits(actual[row])==Float.floatToIntBits(old[sourceRow(row)]),"changed initial/anchor logit "+row);
    }
    private static TrainingState imported(Object original,ClassLoader loader)throws Exception {
        Class<?> schema=loader.loadClass("org.botsclustersmc.core.Schema");
        require(schema.getField("ID").get(null).equals("bcmc-citizen-egocentric-context"),"unexpected source schema");
        require(schema.getField("INPUTS").getInt(null)==512&&schema.getField("HIDDEN").getInt(null)==96
                &&schema.getField("LOGITS").getInt(null)==OLD_LOGITS
                &&schema.getField("DISTRIBUTION").getInt(null)==Schema.DISTRIBUTION
                &&Arrays.equals((int[])schema.getField("HEADS").get(null),Schema.HEADS),"changed primitive contract");
        Object old=call(original,"policy"),anchor=call(old,"anchor"),adam=call(original,"optimizer");
        int task=(int)call(old,"learningTask");Task.at(task);
        require(anchor!=null&&(int)call(anchor,"learningTask")==-1&&call(anchor,"anchor")==null,"unprotected/nested source");
        equal(weights(old),weights(anchor),"source must be the original, untrained warm fork");
        require(counter(old,"updates")==counter(anchor,"updates")&&counter(old,"samples")==counter(anchor,"samples"),"source counters differ");
        Policy base=new Policy(expand(weights(anchor)),counter(anchor,"updates"),counter(anchor,"samples"));
        Policy p=Policy.focus(base,task,true);
        Adam a=new Adam(expand((float[])call(adam,"first")),expand((float[])call(adam,"second")),counter(adam,"step"));
        return new TrainingState(p,a,(byte[])call(original,"course"));
    }
    private static void functions(Object original,Policy target,ClassLoader loader,boolean initial)throws Exception {
        Object old=call(original,"policy");Class<?> wc=loader.loadClass("org.botsclustersmc.core.Policy$Workspace");
        Class<?> rc=loader.loadClass("org.botsclustersmc.core.RandomSource"),dc=loader.loadClass("org.botsclustersmc.core.Distribution");
        Class<?> bc=loader.loadClass("org.botsclustersmc.core.Policy$BatchWorkspace");
        Random random=new Random(2026092991L);float[][] batch=new float[18][];
        for(int task=0;task<18;task++)for(int sample=0;sample<16;sample++) {
            float[] x=new float[512];for(int i=0;i<x.length;i++)x[i]=random.nextFloat()*2-1;
            Arrays.fill(x,16,34,0);x[16+task]=1;if(sample==0)batch[task]=x;
            boolean[] mask=Task.at(task).mask(64,sample%2==0);
            if(sample>=8) {
                mask=Schema.unrestrictedMask();Task.only(mask,6,0,1,2,3,5);
                for(int op=1;op<=3;op++)for(int slot=0;slot<64;slot++)
                    mask[Schema.slotOffset(op)+slot]=(slot+sample+op)%4!=0;
            }
            Policy.Workspace actual=new Policy.Workspace();target.forward(x,mask,actual);
            if(!initial&&task==target.learningTask())continue;
            Object expected=wc.getConstructor().newInstance();
            old.getClass().getMethod("forward",float[].class,boolean[].class,wc).invoke(old,x,mask,expected);
            logits(actual.logits,(float[])wc.getField("logits").get(expected));
            equal(actual.h1,(float[])wc.getField("h1").get(expected),"first hidden function changed");
            equal(actual.h2,(float[])wc.getField("h2").get(expected),"second hidden function changed");
            double[] prior=(double[])wc.getField("probabilities").get(expected);
            require(Arrays.equals(actual.probabilities,prior),"joint actor probabilities changed");
            long seed=task*31L+sample;Object oldRandom=rc.getConstructor(long.class).newInstance(seed);
            RandomSource newRandom=new RandomSource(seed);
            for(int choice=0;choice<9;choice++) {
                boolean greedy=choice==8;
                Object before=dc.getMethod("choose",double[].class,rc,boolean.class).invoke(null,prior,oldRandom,greedy);
                Distribution.Choice after=Distribution.choose(actual.probabilities,newRandom,greedy);
                require(Arrays.equals(after.actions(),(int[])call(before,"actions")),"physical primitive/RNG sequence changed");
                require(Double.doubleToLongBits(after.logProbability())==Double.doubleToLongBits((double)call(before,"logProbability")),"likelihood changed");
                require(Double.doubleToLongBits(after.entropy())==Double.doubleToLongBits((double)call(before,"entropy")),"joint entropy changed");
            }
        }
        Object oldBatch=bc.getConstructor(int.class).newInstance(18);
        old.getClass().getMethod("forwardBatch",float[][].class,int.class,bc).invoke(old,(Object)batch,18,oldBatch);
        Policy.BatchWorkspace actual=new Policy.BatchWorkspace(18);target.forwardBatch(batch,18,actual);
        for(int lane=0;lane<18;lane++) {
            float[] before=new float[OLD_OUTPUTS],after=new float[Schema.OUTPUTS];
            bc.getMethod("lane",int.class,float[].class).invoke(oldBatch,lane,before);actual.lane(lane,after);
            Policy.Workspace scalar=new Policy.Workspace();target.forward(batch[lane],Schema.unrestrictedMask(),scalar);
            equal(after,scalar.logits,"batch/scalar mismatch");
            if(initial||lane!=target.learningTask())logits(after,before);
        }
    }
    private static void state(TrainingState expected,TrainingState actual,boolean initial) {
        Policy p=actual.policy(),base=expected.policy();
        require(p.learningTask()==base.learningTask()&&p.anchor()!=null,"learning scope/protection changed");
        equal(p.anchor().copyWeights(),base.anchor().copyWeights(),"anchor weights changed");
        require(p.anchor().updates()==base.anchor().updates()&&p.anchor().samples()==base.anchor().samples(),"anchor counters changed");
        if(initial) {
            equal(p.copyWeights(),base.copyWeights(),"initial weights changed");
            equal(actual.optimizer().first(),expected.optimizer().first(),"first moments not copied");
            equal(actual.optimizer().second(),expected.optimizer().second(),"second moments not copied");
            require(p.updates()==base.updates()&&p.samples()==base.samples()&&actual.optimizer().step()==expected.optimizer().step(),"initial counters changed");
            require(Arrays.equals(actual.course(),expected.course()),"initial course/RNG changed");
        } else require(p.samples()>base.samples()&&p.updates()>base.updates()
                &&!Arrays.equals(p.copyWeights(),base.copyWeights()),"active policy did not learn");
    }
    private static String sha(byte[] b)throws Exception {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
    public static void main(String[] args)throws Exception {
        require(args.length==4&&Set.of("create","initial","continued").contains(args[0]),
                "SlotExpansion create|initial|continued TRUSTED_RUNTIME ORIGINAL_PROTECTED_CHECKPOINT DESTINATION");
        boolean create=args[0].equals("create"),initial=!args[0].equals("continued");
        Path jar=PolicyFile.managedPath(Path.of(args[1])),input=PolicyFile.managedPath(Path.of(args[2]));
        Path destination=PolicyFile.managedPath(Path.of(args[3]));
        byte[] raw=PolicyFile.readBounded(input,32*1024*1024),runtime=PolicyFile.readBounded(jar,16*1024*1024);
        require(!destination.startsWith(input.getParent())&&!input.startsWith(destination),"overlapping output/source");
        try(URLClassLoader loader=new URLClassLoader(new URL[]{jar.toUri().toURL()},ClassLoader.getPlatformClassLoader())) {
            Class<?> sc=loader.loadClass("org.botsclustersmc.training.TrainingState");
            Object old=sc.getMethod("decode",byte[].class).invoke(null,(Object)raw);
            TrainingState expected=imported(old,loader),actual=create?expected:TrainingState.read(destination);
            Course.decode(actual.course(),512);
            state(expected,actual,initial);functions(old,actual.policy(),loader,initial);
            TrainingState roundtrip=TrainingState.decode(actual.encode());state(actual,roundtrip,true);
            equal(roundtrip.optimizer().first(),actual.optimizer().first(),"saved first moment");
            equal(roundtrip.optimizer().second(),actual.optimizer().second(),"saved second moment");
            require(Arrays.equals(roundtrip.course(),actual.course()),"saved course");
            boolean rejects=false;try{TrainingState.decode(raw);}catch(IOException e){rejects=true;}
            require(rejects,"runtime must reject old schema");
            boolean reverseRejects=false;try{sc.getMethod("decode",byte[].class).invoke(null,(Object)actual.encode());}
            catch(java.lang.reflect.InvocationTargetException e){reverseRejects=e.getCause() instanceof IOException;}
            require(reverseRejects,"old runtime must reject expanded schema");
            String manifest=String.format(Locale.ROOT,
                "{\"kind\":\"click-conditioned-slot-expansion\",\"mode\":\"%s\",\"checks\":%d,\"source_parameters\":%d,\"target_parameters\":%d,\"updates\":%d,\"samples\":%d,\"source_checkpoint_sha256\":\"%s\",\"trusted_runtime_sha256\":\"%s\",\"deployment\":false}%n",
                args[0],checks,OLD_PARAMETERS,Policy.PARAMETERS,actual.policy().updates(),actual.policy().samples(),sha(raw),sha(runtime));
            if(create) {
                Files.createDirectory(destination);
                Files.write(destination.resolve("protected-training.bcmc"),actual.encode(),StandardOpenOption.CREATE_NEW);
                Files.write(destination.resolve("protected-policy.bcmc"),PolicyFile.encode(actual.policy()),StandardOpenOption.CREATE_NEW);
                Files.write(destination.resolve("base-policy.bcmc"),PolicyFile.encode(actual.policy().anchor()),StandardOpenOption.CREATE_NEW);
                Files.writeString(destination.resolve("expansion.json"),manifest,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
            }
            System.out.print(manifest);
        }
    }
}
