import java.nio.file.*;
import java.net.*;
import java.util.*;
import org.botsclustersmc.core.*;
import org.botsclustersmc.training.*;

public final class FocusAudit {
    static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
    static void equal(float[] a,float[] b){require(Arrays.equals(a,b),"different parameter/moment arrays");}
    public static void main(String[] args)throws Exception {
        Path root=Path.of(args[0]),candidate=Path.of(args[1]);boolean initial=args[2].equals("initial");
        Policy base=PolicyFile.read(root.resolve("base-policy.bcmc"));TrainingState target=TrainingState.read(candidate);
        Policy p=target.policy();require(p.learningTask()==11,"wrong learning scope");
        require(args.length==4&&(args[3].equals("protected")||args[3].equals("control")),"explicit arm required");
        boolean protectedArm=args[3].equals("protected");
        require((p.anchor()!=null)==protectedArm,"missing or unexpected anchor");
        if(p.anchor()!=null){equal(p.anchor().copyWeights(),base.copyWeights());require(p.anchor().updates()==base.updates()&&p.anchor().samples()==base.samples(),"anchor counters changed");}
        if(initial){
            equal(p.copyWeights(),base.copyWeights());require(p.updates()==base.updates()&&p.samples()==base.samples(),"initial counters");
            Path old=root.resolve("source-runtime.jar");
            try(URLClassLoader loader=new URLClassLoader(new URL[]{old.toUri().toURL()},ClassLoader.getPlatformClassLoader())){
                Class<?> clazz=loader.loadClass("org.botsclustersmc.training.TrainingState");
                Object source=clazz.getMethod("decode",byte[].class).invoke(null,(Object)Files.readAllBytes(root.resolve("source-training.bcmc")));
                Object oldPolicy=clazz.getMethod("policy").invoke(source),adam=clazz.getMethod("optimizer").invoke(source);
                equal(p.copyWeights(),(float[])oldPolicy.getClass().getMethod("copyWeights").invoke(oldPolicy));
                equal(target.optimizer().first(),(float[])adam.getClass().getMethod("first").invoke(adam));
                equal(target.optimizer().second(),(float[])adam.getClass().getMethod("second").invoke(adam));
                require(target.optimizer().step()==(long)adam.getClass().getMethod("step").invoke(adam),"Adam clock changed");
                require(Arrays.equals(target.course(),(byte[])clazz.getMethod("course").invoke(source)),"course changed");
                Random random=new Random(71);int count=0;
                for(int task=0;task<18;task++)for(int sample=0;sample<16;sample++){
                    float[] x=new float[512];for(int i=0;i<x.length;i++)x[i]=(random.nextFloat()-.5f)*2;
                    Arrays.fill(x,16,34,0);x[16+task]=1;boolean[] mask=Task.at(task).mask(64,sample%2==0);
                    Policy.Workspace actual=new Policy.Workspace();p.forward(x,mask,actual);
                    Class<?> wc=loader.loadClass("org.botsclustersmc.core.Policy$Workspace");Object expected=wc.getConstructor().newInstance();
                    oldPolicy.getClass().getMethod("forward",float[].class,boolean[].class,wc).invoke(oldPolicy,x,mask,expected);
                    equal(actual.logits,(float[])wc.getField("logits").get(expected));
                    require(Arrays.equals(actual.probabilities,(double[])wc.getField("probabilities").get(expected)),"old/new actor function");count++;
                }
                System.out.println("Verified "+count+" original-runtime/new-runtime functions and exact Adam/course initialization.");
            }
        }else require(!Arrays.equals(p.copyWeights(),base.copyWeights())&&p.samples()>base.samples(),"active weights did not change");
        Course.decode(target.course(),512);
        System.out.printf(Locale.ROOT,"AUDIT updates=%d samples=%d protected=%s anchor_unchanged=%s active_bytes=%d policy_bytes=%d%n",p.updates(),p.samples(),p.anchor()!=null,p.anchor()!=null,4*Policy.PARAMETERS,PolicyFile.encode(p).length);
    }
}
