package org.botsclustersmc.tests;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.botsclustersmc.core.*;
import org.botsclustersmc.core.Stack;
import org.botsclustersmc.training.Transition;

/** Real batched inference workers and trajectory inputs, without Bukkit or an artificial learner. */
public final class ConditionalInferenceTest {
    private static int checks;
    private static void check(boolean ok,String message) {
        checks++;if(!ok)throw new AssertionError(message);
    }
    private static void fails(Runnable call,String message) {
        boolean failed=false;try {call.run();}catch(IllegalArgumentException expected){failed=true;}
        check(failed,message);
    }
    public static void main(String[] args)throws Exception {
        int n=256;Policy policy=Policy.initialize(951);float[] weights=policy.copyWeights();
        Pocket[] pockets=new Pocket[n];float[][] observations=new float[n][Schema.INPUTS];
        boolean[][] masks=new boolean[n][];InferencePool.Result[] results=new InferencePool.Result[n];
        CountDownLatch done=new CountDownLatch(n);AtomicReference<Throwable> failure=new AtomicReference<>();
        InferencePool pool=new InferencePool(2,n);
        try {
            fails(()->pool.offer(new InferencePool.Request(0,policy,observations[0],new boolean[Schema.LOGITS],
                0,false,r->{},e->{},System.nanoTime())),"inference rejects obsolete transient masks");
            for(int i=0;i<n;i++) {
                Pocket pocket=new Pocket();pockets[i]=pocket;
                if(i%4!=0) {
                    pocket.open(i%4==3?Pocket.Menu.WORKBENCH:Pocket.Menu.INVENTORY);
                    pocket.setStorage(i%9,new Stack("OAK_PLANKS",4));
                    if(i%4>=2) {
                        pocket.click(1,i%9,Pocket.NONE);pocket.click(2,36,Pocket.NONE);
                    }
                }
                observations[i][0]=1;observations[i][1]=(i%11-5)/5f;
                observations[i][16+Task.CRAFT_WORKBENCH.ordinal()]=1;
                masks[i]=Task.CRAFT_WORKBENCH.mask(pocket.slots(),pocket.menu()!=Pocket.Menu.CLOSED);
                MenuInputs.restrict(pocket,Pocket.NONE,masks[i]);
                final int index=i;
                check(pool.offer(new InferencePool.Request(i,policy,observations[i],masks[i],i+719,i%2==0,
                    result->{results[index]=result;done.countDown();},
                    error->{failure.compareAndSet(null,error);done.countDown();},System.nanoTime())),"admitted");
            }
            check(done.await(20,TimeUnit.SECONDS),"all conditional callbacks completed");
        } finally {
            pool.close();check(pool.awaitTermination(5000),"workers terminated");
        }
        check(failure.get()==null,"conditional inference failure: "+failure.get());
        check(pool.completed.sum()==n&&pool.failed.sum()==0,"exact joined callback accounting");
        Policy.Workspace scalar=new Policy.Workspace();
        for(int i=0;i<n;i++) {
            InferencePool.Result result=results[i];check(result!=null,"result present");
            policy.forward(observations[i],masks[i],scalar);
            Distribution.Choice expected=Distribution.choose(scalar.probabilities,new RandomSource(i+719),i%2==0);
            check(Arrays.equals(result.actions(),expected.actions()),"scalar/batched conditional action identity");
            check(Math.abs(result.logProbability()-expected.logProbability())<1e-5,"behavior likelihood matches scalar policy");
            check(Math.abs(result.value()-scalar.logits[Schema.LOGITS])<1e-5,"value output is not a conditional probability slot");
            check(result.policyVersion()==policy.updates(),"policy identity unchanged");
            int op=result.actions()[6],slot=result.actions()[7];
            check(!Schema.slotActive(op)||pockets[i].wouldChange(op,slot,Pocket.NONE),"batched click has a mechanical effect");
            Transition step=new Transition(observations[i],masks[i],result.actions(),result.logProbability(),
                result.policyVersion(),0,4,observations[i],masks[i],true);
            check(step.mask().length==Schema.DISTRIBUTION&&step.nextMask().length==Schema.DISTRIBUTION,"trajectory retains all conditional branches");
        }
        int[] idle=Schema.IDLE.clone();float[] x=observations[0];boolean[] m=masks[0];
        fails(()->new Transition(x,new boolean[Schema.LOGITS],idle,0,0,0,4,x,m,true),"old current mask rejected");
        fails(()->new Transition(x,m,idle,0,0,0,4,x,new boolean[Schema.LOGITS],true),"old next mask rejected");
        check(Arrays.equals(weights,policy.copyWeights()),"inference never changes learned weights");
        System.out.println("PASS conditional batched inference/transition checks="+checks);
    }
}
