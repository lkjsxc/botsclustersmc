package org.botsclustersmc.tests;

import java.util.*;
import java.io.*;
import java.nio.ByteBuffer;
import org.botsclustersmc.core.*;
import org.botsclustersmc.training.*;

/** Routing, derivative support and inactive-state isolation are separate assertions. */
public final class ExpertIsolationTest {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static void same(float[] a,float[] b,String why){check(Arrays.equals(a,b),why);}
    private static void near(double a,double b,double epsilon,String why){check(Math.abs(a-b)<=epsilon,why+": "+a+" / "+b);}
    private interface Attempt {void run()throws Exception;}
    private static void rejects(Attempt action,String why)throws Exception{
        try{action.run();}catch(IllegalArgumentException|ArithmeticException|IOException expected){checks++;return;}
        throw new AssertionError("Accepted "+why);
    }
    private static int[] counts(int task,int count){int[] n=new int[Policy.EXPERTS];n[task]=count;return n;}
    private static float[] observation(int task,RandomSource random){
        float[] x=new float[Schema.INPUTS];
        for(int i=0;i<x.length;i++)x[i]=random.symmetric(.2);
        Arrays.fill(x,16,16+Policy.EXPERTS,0);x[16+task]=1;x[1]=.3f;return x;
    }
    private static float[] slice(float[] a,int task){
        int begin=task*Policy.NETWORK_PARAMETERS;
        return Arrays.copyOfRange(a,begin,begin+Policy.NETWORK_PARAMETERS);
    }
    private static double critic(Policy p,float[] x){
        Policy.Workspace w=new Policy.Workspace();p.forward(x,Schema.unrestrictedMask(),w);return w.logits[Schema.LOGITS];
    }
    private static float[] oracle(float[] weights,int task,float[] x){
        int base=task*Policy.NETWORK_PARAMETERS;
        float[] a=layer(weights,base+Policy.W1,base+Policy.B1,x,Schema.HIDDEN,true);
        float[] b=layer(weights,base+Policy.W2,base+Policy.B2,a,Schema.HIDDEN,true);
        return layer(weights,base+Policy.W3,base+Policy.B3,b,Schema.OUTPUTS,false);
    }
    private static float[] layer(float[] weights,int matrix,int bias,float[] input,int count,boolean nonlinear){
        float[] result=new float[count];
        for(int row=0;row<count;row++){
            float sum=weights[bias+row];
            for(int col=0;col<input.length;col++)sum+=input[col]*weights[matrix+row*input.length+col];
            result[row]=nonlinear?(float)Math.tanh(sum):sum;
        }
        return result;
    }
    public static void main(String[] args)throws Exception{
        routing();derivatives();optimizer();persistence();
        System.out.println("PASS isolated experts checks="+checks+" experts="+Policy.EXPERTS);
    }
    private static void routing()throws Exception{
        check(Policy.EXPERTS==Task.values().length,"task catalogue and expert count");
        Policy seed=Policy.initialize(192);float[] weights=seed.copyWeights();RandomSource r=new RandomSource(887);
        for(int task=0;task<Policy.EXPERTS;task++){
            same(slice(weights,0),slice(weights,task),"identical cold initialization");
        }
        for(int task=0;task<Policy.EXPERTS;task++){
            int b=task*Policy.NETWORK_PARAMETERS;
            weights[b+Policy.B1+7]+=task*.017f;
            weights[b+Policy.B2+3]-=task*.011f;
            weights[b+Policy.B3+Schema.LOGITS]+=task*.07f;
        }
        Policy p=new Policy(weights,0,0);Policy.Workspace w=new Policy.Workspace();
        float[][] observations=new float[256][];
        for(int i=0;i<observations.length;i++){
            int task=i*7%Policy.EXPERTS;observations[i]=observation(task,r);
            check(Policy.expert(observations[i])==task,"one-hot route");
            p.forward(observations[i],Schema.unrestrictedMask(),w);
            same(w.logits,oracle(weights,task,observations[i]),"scalar independent oracle");
        }
        Policy.BatchWorkspace b=new Policy.BatchWorkspace(256);float[] logits=new float[Schema.OUTPUTS];
        for(int n:new int[]{256,1,37,18,129,2,255}){
            p.forwardBatch(observations,n,b);
            for(int i=0;i<n;i++){
                b.lane(i,logits);p.forward(observations[i],Schema.unrestrictedMask(),w);
                same(logits,w.logits,"grouped lane restored to original order");
            }
            rejects(()->b.lane(n,logits),"stale lane beyond current count");
            Collections.rotate(Arrays.asList(observations),19);
        }
        rejects(()->p.forwardBatch(observations,0,b),"empty batch");
        rejects(()->new Policy(new float[Policy.NETWORK_PARAMETERS],0,0),"single network in bank schema");
        float[] original=p.copyWeights();weights[0]=12345;same(original,p.copyWeights(),"constructor isolation");
        float[] copy=p.copyWeights();copy[0]=12345;same(original,p.copyWeights(),"accessor isolation");
    }
    private static void derivatives(){
        Policy p=Policy.initialize(541);RandomSource r=new RandomSource(776);
        int[] locals={7*Schema.INPUTS+1,Policy.B1+7,Policy.W2+11*Schema.HIDDEN+7,
            Policy.B2+11,Policy.W3+Schema.LOGITS*Schema.HIDDEN+11,Policy.B3+Schema.LOGITS};
        for(int task=0;task<Policy.EXPERTS;task++){
            float[] x=observation(task,r),gradient=new float[Policy.PARAMETERS],d=new float[Schema.OUTPUTS];
            d[Schema.LOGITS]=1;Policy.Workspace w=new Policy.Workspace();p.forward(x,Schema.unrestrictedMask(),w);
            p.backward(x,w,d,gradient);int nonzero=0;
            for(int local:locals){
                int k=task*Policy.NETWORK_PARAMETERS+local;float[] weights=p.copyWeights();float h=.002f;
                weights[k]+=h;double plus=critic(new Policy(weights,0,0),x);
                weights[k]-=2*h;double minus=critic(new Policy(weights,0,0),x);
                near(gradient[k],(plus-minus)/(2*h),.00025,"nonzero routed finite difference "+task+"/"+local);
                if(Math.abs(gradient[k])>1e-7)nonzero++;
            }
            check(nonzero>=5,"derivative check must not pass on inactive zeros");
            for(int other=0;other<Policy.EXPERTS;other++)if(other!=task){
                for(float g:slice(gradient,other))if(g!=0)throw new AssertionError("gradient leaked to "+other);
                checks++;
            }
        }
    }
    private static void optimizer()throws Exception{
        Policy initial=Policy.initialize(84);float[] weights=initial.copyWeights();
        float[] first=new float[Policy.PARAMETERS],second=new float[Policy.PARAMETERS];
        Arrays.fill(first,.03f);Arrays.fill(second,.02f);long[] clocks=new long[Policy.EXPERTS];
        for(int i=0;i<clocks.length;i++)clocks[i]=i;
        Policy p=new Policy(weights,30,123);Adam a=new Adam(first,second,30,clocks);
        float[] g=new float[Policy.PARAMETERS];g[12*Policy.NETWORK_PARAMETERS+Policy.B3]=.5f;
        int[] n=counts(12,8);Adam.Update u=a.update(p,g,n,.0003);
        check(u.policy().updates()==31&&u.policy().samples()==131,"exact global counters");
        float[] after=u.policy().copyWeights(),m=u.optimizer().first(),v=u.optimizer().second();
        for(int task=0;task<Policy.EXPERTS;task++){
            check(u.optimizer().expertSteps()[task]==clocks[task]+(task==12?1:0),"local clock");
            if(task==12){check(!Arrays.equals(slice(weights,task),slice(after,task)),"active expert updates");continue;}
            same(slice(weights,task),slice(after,task),"absent weights include residual momentum");
            same(slice(first,task),slice(m,task),"absent first moment");
            same(slice(second,task),slice(v,task),"absent second moment");
        }
        same(weights,p.copyWeights(),"original policy unchanged");same(first,a.first(),"original optimizer unchanged");
        check(Arrays.equals(clocks,a.expertSteps()),"original clocks unchanged");
        // Other experts' sample count, gradient norm and global clock cannot dilute this update.
        float[] mixed=g.clone();mixed[Policy.B3]=100000f;int[] many=n.clone();many[0]=512;
        Adam.Update mixedUpdate=a.update(p,mixed,many,.0003);
        same(slice(after,12),slice(mixedUpdate.policy().copyWeights(),12),"other-task gradient cannot rescale expert");
        same(slice(m,12),slice(mixedUpdate.optimizer().first(),12),"independent moment normalization");
        Adam late=new Adam(first,second,3000,clocks);
        Adam.Update lateUpdate=late.update(new Policy(weights,3000,123),g,n,.0003);
        same(after,lateUpdate.policy().copyWeights(),"bias correction uses local rather than global clock");
        // An active all-zero gradient still consumes one local time step; absent experts do not.
        Adam.Update zero=a.update(p,new float[Policy.PARAMETERS],n,.0003);
        check(zero.optimizer().expertSteps()[12]==13,"zero active gradient advances local clock");
        check(!Arrays.equals(slice(weights,12),slice(zero.policy().copyWeights(),12)),"active momentum still applied");
        rejects(()->a.update(p,g,counts(11,8),.0003),"nonzero inactive gradient");
        rejects(()->a.update(p,g,new int[Policy.EXPERTS],.0003),"empty update");
        rejects(()->a.update(p,g,counts(12,-1),.0003),"negative count");
        rejects(()->a.update(p,g,n,0),"zero learning rate");
        rejects(()->a.update(p,g,n,Double.NaN),"nonfinite learning rate");
        float[] invalid=g.clone();invalid[0]=Float.NaN;
        rejects(()->a.update(p,invalid,n,.0003),"nonfinite inactive gradient");
        rejects(()->a.update(initial,g,n,.0003),"optimizer/model version mismatch");
        long[] bad=clocks.clone();bad[0]=31;
        rejects(()->new Adam(first,second,30,bad),"future expert clock");
        long[] copy=a.expertSteps();copy[0]=999;check(a.expertSteps()[0]==0,"clock accessor does not alias");
        // Repeated updates to one expert preserve every other expert, not just one step.
        Policy current=p;Adam optimizer=a;
        for(int i=0;i<25;i++){Adam.Update next=optimizer.update(current,g,n,.0003);current=next.policy();optimizer=next.optimizer();}
        for(int task=0;task<Policy.EXPERTS;task++)if(task!=12){
            same(slice(weights,task),slice(current.copyWeights(),task),"long absent weight invariance");
            same(slice(first,task),slice(optimizer.first(),task),"long absent moment invariance");
        }
    }
    private static void persistence()throws Exception{
        Policy initial=Policy.initialize(998);float[] g=new float[Policy.PARAMETERS];
        g[17*Policy.NETWORK_PARAMETERS+Policy.B3]=1;
        Adam.Update update=new Adam().update(initial,g,counts(17,4),.001);
        TrainingState state=new TrainingState(update.policy(),update.optimizer(),new byte[]{7,8,9});
        byte[] encoded=state.encode();TrainingState restored=TrainingState.decode(encoded);
        same(state.policy().copyWeights(),restored.policy().copyWeights(),"bank roundtrip");
        same(state.optimizer().first(),restored.optimizer().first(),"moments roundtrip");
        check(Arrays.equals(state.optimizer().expertSteps(),restored.optimizer().expertSteps()),"local clocks roundtrip");
        check(encoded.length<32*1024*1024,"bounded training payload");
        byte[] policy=PolicyFile.encode(state.policy());
        check(policy.length<Schema.MAX_MODEL_BYTES,"bounded policy payload");
        // Change a clock, then recompute the CRC: reject semantics, not merely corruption.
        byte[] payload=PolicyFile.verify(encoded,32*1024*1024);
        ByteArrayInputStream bytes=new ByteArrayInputStream(payload);DataInputStream in=new DataInputStream(bytes);
        in.readUTF();in.readUTF();int length=in.readInt();in.skipNBytes(length);in.readLong();in.readInt();
        int position=payload.length-bytes.available();ByteBuffer.wrap(payload).putLong(position,2L);
        rejects(()->TrainingState.decode(PolicyFile.checked(payload)),"CRC-valid impossible local clock");
        byte[] body=PolicyFile.verify(policy,Schema.MAX_MODEL_BYTES);
        ByteArrayInputStream policyBytes=new ByteArrayInputStream(body);DataInputStream header=new DataInputStream(policyBytes);
        header.readUTF();header.readUTF();header.readInt();header.readInt();int heads=header.readInt();
        for(int i=0;i<heads;i++)header.readInt();
        ByteBuffer.wrap(body).putInt(body.length-policyBytes.available(),Policy.EXPERTS-1);
        rejects(()->PolicyFile.decode(PolicyFile.checked(body)),"CRC-valid wrong expert count");
        byte[] before=state.encode();
        rejects(()->TrainingState.decode(Arrays.copyOf(encoded,encoded.length-1)),"truncated expert state");
        check(Arrays.equals(before,state.encode()),"rejections never mutate state");
    }
}
