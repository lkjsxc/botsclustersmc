package org.botsclustersmc.tests;

import org.botsclustersmc.core.*;
import org.botsclustersmc.training.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public final class CoreTest {
    static int checks;
    interface Throwing {void run()throws Exception;}
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static void near(double a,double b,double e,String message){check(Math.abs(a-b)<=e,message+" "+a+" != "+b);}
    static void fails(Throwing r,String message)throws Exception{boolean fail=false;try{r.run();}catch(Exception expected){fail=true;}check(fail,message);}
    public static void main(String[] args)throws Exception {
        Policy p=Policy.initialize(12);boolean[] mask=Schema.unrestrictedMask();RandomSource rng=new RandomSource(42);
        float[][] obs=new float[32][Schema.INPUTS];for(float[] a:obs)for(int i=0;i<a.length;i++)a[i]=rng.symmetric(1);
        Policy.BatchWorkspace batch=new Policy.BatchWorkspace(32);Policy.Workspace ws=new Policy.Workspace();float[] out=new float[Schema.OUTPUTS];
        for(int n:new int[]{1,3,16,32}){p.forwardBatch(obs,n,batch);for(int i=0;i<n;i++){p.forward(obs[i],mask,ws);batch.lane(i,out);for(int j=0;j<out.length;j++)near(out[j],ws.logits[j],3e-6,"scalar/batch");}}
        float[] logits=new float[Schema.OUTPUTS];for(int i=0;i<logits.length;i++)logits[i]=rng.symmetric(2);
        double[] probs=new double[Schema.LOGITS];Distribution.probabilities(logits,mask,probs);
        for(int gui:new int[]{0,1,2,3,4,5}){
            int[] act=Schema.IDLE.clone();act[6]=gui;act[7]=Schema.slotActive(gui)?13:0;
            float[] grad=new float[Schema.OUTPUTS];Distribution.gradient(probs,act,.7,.031,grad);
            for(int j=0;j<Schema.LOGITS;j++){
                float old=logits[j],h=.001f;logits[j]=old+h;double plus=loss(logits,mask,act);logits[j]=old-h;double minus=loss(logits,mask,act);logits[j]=old;
                near(grad[j],(plus-minus)/(2*h),1e-4,"conditional distribution gradient "+gui+"/"+j);
            }
        }
        int[] bad=Schema.IDLE.clone();bad[7]=1;fails(()->Distribution.logProbability(probs,bad),"inactive slot must be zero");
        boolean[] empty=new boolean[Schema.LOGITS];fails(()->Distribution.probabilities(logits,empty,probs),"empty mask fails");
        Arrays.fill(mask,false);int off=0;for(int i=0;i<Schema.HEADS.length;i++){mask[off+Schema.IDLE[i]]=true;off+=Schema.HEADS[i];}
        p.forward(obs[0],mask,ws);Distribution.Choice choice=Distribution.choose(ws.probabilities,rng,false);check(Arrays.equals(choice.actions(),Schema.IDLE),"mechanical mask");near(choice.logProbability(),0,1e-8,"deterministic probability");
        mask=Schema.unrestrictedMask();p.forward(obs[0],mask,ws);float[] gradient=new float[Policy.PARAMETERS];float[] d=new float[Schema.OUTPUTS];for(int j=0;j<d.length;j++)d[j]=rng.symmetric(.1);p.backward(obs[0],ws,d,gradient);
        int[] indices={0,1234,Policy.B1,Policy.W2+201,Policy.B2,Policy.W3+34,Policy.B3+6};
        for(int k:indices){float[] weights=p.copyWeights();float h=.002f;weights[k]+=h;double plus=linear(new Policy(weights,0,0),obs[0],mask,d);weights[k]-=2*h;double minus=linear(new Policy(weights,0,0),obs[0],mask,d);near(gradient[k],(plus-minus)/(2*h),2e-4,"network gradient "+k);}
        byte[] bytes=PolicyFile.encode(p);Policy decoded=PolicyFile.decode(bytes);check(Arrays.equals(p.copyWeights(),decoded.copyWeights()),"policy exact roundtrip");
        byte[] corrupt=bytes.clone();corrupt[20]^=1;fails(()->PolicyFile.decode(corrupt),"checksum corruption");fails(()->PolicyFile.decode(Arrays.copyOf(bytes,30)),"truncated policy");
        float[] invalid=p.copyWeights();invalid[0]=Float.NaN;fails(()->new Policy(invalid,0,0),"NaN policy");
        Path root=Files.createTempDirectory("bcmc-core-test");PolicyFile.write(root.resolve("policy.bcmc"),p);check(PolicyFile.read(root.resolve("policy.bcmc")).updates()==0,"atomic policy read");
        Path linked=root.resolve("linked");boolean hasLink=true;
        try{Files.createSymbolicLink(linked,root.resolve("policy.bcmc"));}
        catch(FileSystemException | UnsupportedOperationException e){if(!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win"))throw e;hasLink=false;System.out.println("SKIP Windows symlink fixture: OS does not permit link creation: "+e.getMessage());}
        if(hasLink){fails(()->PolicyFile.read(linked),"symlink read");fails(()->PolicyFile.write(linked,p),"symlink replace");}
        VTrace.Returns vt=VTrace.compute(new double[]{1,2},new double[]{.9,0},new double[]{.5,.3},new double[]{.3,0},new double[]{0,0},new boolean[]{true,false});near(vt.values()[0],2.8,1e-12,"on-policy return");near(vt.values()[1],2,1e-12,"terminal return");near(vt.advantages()[0],2.3,1e-12,"actor advantage");
        vt=VTrace.compute(new double[]{1},new double[]{.9},new double[]{.5},new double[]{.7},new double[]{Math.log(.5)},new boolean[]{false});near(vt.values()[0],1.065,1e-12,"rho half bootstrapped fragment");near(VTrace.discount(8,false),Math.pow(.997,2),1e-12,"actual ticks");
        Adam adam=new Adam();Adam.Update updated=adam.update(p,gradient,1,.0003);check(updated.policy().updates()==1&&updated.optimizer().step()==1,"Adam identity");check(adam.step()==0&&p.updates()==0,"immutable transaction");
        gradient[0]=Float.NaN;fails(()->adam.update(p,gradient,1,.0003),"invalid optimizer gradient");check(adam.step()==0,"failed optimizer unchanged");
        TrainingState state=new TrainingState(updated.policy(),updated.optimizer(),new byte[]{1,2,3});TrainingState restored=TrainingState.decode(state.encode());check(Arrays.equals(state.policy().copyWeights(),restored.policy().copyWeights()),"training weights roundtrip");check(Arrays.equals(state.optimizer().second(),restored.optimizer().second()),"Adam exact resume");
        pool(p,obs[0]);learner(p,obs[0]);
        System.out.println("PASS core checks="+checks+" parameters="+Policy.PARAMETERS);
    }
    static double loss(float[] logits,boolean[] mask,int[] action){double[] p=new double[Schema.LOGITS];Distribution.probabilities(logits,mask,p);return -.7*Distribution.logProbability(p,action)-.031*Distribution.entropy(p);}
    static double linear(Policy p,float[] x,boolean[] mask,float[] d){Policy.Workspace w=new Policy.Workspace();p.forward(x,mask,w);double sum=0;for(int i=0;i<d.length;i++)sum+=d[i]*w.logits[i];return sum;}
    static void pool(Policy p,float[] obs)throws Exception {
        int n=1000;CountDownLatch done=new CountDownLatch(n);AtomicInteger failures=new AtomicInteger();InferencePool pool=new InferencePool(3,1000);
        for(int i=0;i<n;i++)check(pool.offer(new InferencePool.Request(i,p,obs,Schema.unrestrictedMask(),i,false,r->{Schema.checkAction(r.actions());done.countDown();},e->{failures.incrementAndGet();done.countDown();},System.nanoTime())),"admission");
        check(done.await(20,TimeUnit.SECONDS),"1000 inferences completed");check(failures.get()==0,"no inference failure");check(pool.completed.sum()==n,"exact inference accounting");pool.close();check(pool.awaitTermination(2000),"pool shutdown");
        check(!pool.offer(new InferencePool.Request(0,p,obs,Schema.unrestrictedMask(),0,false,r->{},e->{},0)),"closed rejects");
    }
    static void learner(Policy p,float[] obs)throws Exception{
        AtomicReference<Throwable> fatal=new AtomicReference<>();Learner l=new Learner(p,new Adam(),2,64,64,32,x->{},fatal::set);
        Policy.Workspace w=new Policy.Workspace();boolean[] mask=Schema.unrestrictedMask();p.forward(obs,mask,w);
        int[] action=Distribution.choose(w.probabilities,new RandomSource(22),false).actions();
        Transition t=new Transition(obs,mask,action,Distribution.logProbability(w.probabilities,action),0,1,4,obs,mask,true);
        for(int i=0;i<32;i++)check(l.offer(new Trajectory(i,0,0,List.of(t))),"learner admission");
        l.close();check(l.awaitTermination(20000),"learner drains");check(fatal.get()==null,"learner healthy "+fatal.get());check(l.policy().samples()==32,"trained samples not invented");check(l.policy().updates()>0,"actual optimizer update");
        fails(()->new Trajectory(0,0,0,List.of(t,t)),"trace cannot cross terminal");
    }
}
