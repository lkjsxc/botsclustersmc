package org.botsclustersmc.tests;

import org.botsclustersmc.core.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** Deterministic late-reply ordering, not a timing-dependent sleep test. */
public final class ConcurrencyTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static InferencePool.Result result(long request){return new InferencePool.Result(request,Schema.IDLE.clone(),0,0,0,0,0);}
    public static void main(String[] args)throws Exception {
        InferenceTicket previous=new InferenceTicket(1),current=new InferenceTicket(2);
        check(previous.poll()==null&&current.poll()==null,"unready poll returns without waiting");
        current.deliver(result(2));previous.deliver(result(1));
        check(current.poll().request()==2,"late obsolete reply cannot overwrite current reply");
        previous.fail(new IllegalStateException("old request failure"));
        check(current.poll().request()==2,"obsolete failure cannot poison current request");
        InferenceTicket discarded=new InferenceTicket(3);discarded.cancel();discarded.deliver(result(3));
        current.deliver(result(2));check(current.poll().request()==2,"discarded and duplicate callbacks isolated");
        InferenceTicket wrong=new InferenceTicket(4);wrong.deliver(result(5));
        boolean mismatch=false;try{wrong.poll();}catch(CompletionException expected){mismatch=true;}
        check(mismatch,"wrong reply identity rejected");
        inversion();System.out.println("PASS concurrency checks="+checks);
    }
    private static void inversion()throws Exception {
        Policy policy=Policy.initialize(7);InferenceTicket old=new InferenceTicket(11),fresh=new InferenceTicket(12);
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),ready=new CountDownLatch(1),oldDone=new CountDownLatch(1);
        AtomicReference<Throwable> error=new AtomicReference<>();InferencePool pool=new InferencePool(2,8);
        try {
            check(pool.offer(new InferencePool.Request(11,policy,new float[Schema.INPUTS],Schema.unrestrictedMask(),1,false,r->{
                entered.countDown();try{if(!release.await(10,TimeUnit.SECONDS))throw new IllegalStateException("test gate timeout");old.deliver(r);oldDone.countDown();}catch(InterruptedException e){Thread.currentThread().interrupt();error.set(e);}
            },error::set,System.nanoTime())),"old request admitted");
            check(entered.await(10,TimeUnit.SECONDS),"old callback held");old.cancel();
            check(pool.offer(new InferencePool.Request(12,policy,new float[Schema.INPUTS],Schema.unrestrictedMask(),2,false,r->{fresh.deliver(r);ready.countDown();},error::set,System.nanoTime())),"new request admitted");
            check(ready.await(10,TimeUnit.SECONDS),"new callback completed before old");
            check(fresh.poll().request()==12,"new request received exact reply");release.countDown();check(oldDone.await(10,TimeUnit.SECONDS),"old callback returned");
        } finally {release.countDown();pool.close();check(pool.awaitTermination(5000),"workers terminate");}
        check(error.get()==null,"no callback failure: "+error.get());
        check(fresh.poll().request()==12,"current reply survives old completion and pool shutdown");
    }
}
