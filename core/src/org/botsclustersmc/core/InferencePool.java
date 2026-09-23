package org.botsclustersmc.core;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;

/** One bounded shared queue, no per-agent thread, no Bukkit objects, no tick-thread waits. */
public final class InferencePool implements AutoCloseable {
    public record Result(long request,int[] actions,double logProbability,float value,long policyVersion,long computeNanos,long queueNanos) {}
    public record Request(long request,Policy policy,float[] observation,boolean[] mask,long seed,boolean greedy,Consumer<Result> deliver,Consumer<Throwable> fail,long enqueued) {}
    private final ArrayBlockingQueue<Request> queue;
    private final List<Thread> workers=new ArrayList<>();
    private final AtomicBoolean closed=new AtomicBoolean();
    public final LongAdder completed=new LongAdder(),failed=new LongAdder(),computeNanos=new LongAdder(),queueNanos=new LongAdder();
    public InferencePool(int threads,int capacity) {
        if(threads<1 || threads>256 || capacity<1 || capacity>100_000) throw new IllegalArgumentException("inference bounds");
        queue=new ArrayBlockingQueue<>(capacity);
        for(int i=0;i<threads;i++) {
            Thread t=new Thread(this::run,"bcmc-inference-"+i); t.setDaemon(true); workers.add(t); t.start();
        }
    }
    public boolean offer(Request request) {
        Objects.requireNonNull(request); Objects.requireNonNull(request.policy()); Objects.requireNonNull(request.deliver()); Objects.requireNonNull(request.fail());
        Schema.checkObservation(request.observation());
        if(request.mask().length!=Schema.LOGITS)throw new IllegalArgumentException("mask shape");
        synchronized(closed){return !closed.get() && queue.offer(request);}
    }
    private void fail(Request request,Throwable cause){failed.increment();try{request.fail().accept(cause);}catch(Throwable ignored){}}

    public int queued(){return queue.size();}
    private void run() {
        Policy.BatchWorkspace batch=new Policy.BatchWorkspace(32); Policy.Workspace scalar=new Policy.Workspace();
        List<Request> jobs=new ArrayList<>(32); float[][] observations=new float[32][];
        while(!closed.get()) {
            try {
                jobs.clear(); jobs.add(queue.take()); queue.drainTo(jobs,31);
                // Published identity is immutable; consecutive same-policy jobs can share a dense batch.
                for(int begin=0;begin<jobs.size();) {
                    int end=begin+1; Policy policy=jobs.get(begin).policy();
                    while(end<jobs.size() && jobs.get(end).policy()==policy) end++;
                    int n=end-begin; long started=System.nanoTime();
                    try {
                        for(int k=0;k<n;k++) observations[k]=jobs.get(begin+k).observation();
                        policy.forwardBatch(observations,n,batch);
                        long elapsed=System.nanoTime()-started;
                        for(int k=0;k<n;k++) {
                            Request r=jobs.get(begin+k);
                            try {
                            if(closed.get())throw new CancellationException("inference stopped");
                            batch.lane(k,scalar.logits);
                            Distribution.probabilities(scalar.logits,r.mask(),scalar.probabilities);
                            Distribution.Choice choice=Distribution.choose(scalar.probabilities,new RandomSource(r.seed()),r.greedy());
                            long waiting=Math.max(0,started-r.enqueued());
                            r.deliver().accept(new Result(r.request(),choice.actions(),choice.logProbability(),scalar.logits[Schema.LOGITS],policy.updates(),elapsed/n,waiting));
                            completed.increment(); computeNanos.add(elapsed/n); queueNanos.add(waiting);
                            }catch(Throwable failure){fail(r,failure);}
                        }
                    } catch(Throwable failure) {
                        for(int k=begin;k<end;k++) fail(jobs.get(k),failure);
                    }
                    begin=end;
                }
            } catch(InterruptedException stopped) { Thread.currentThread().interrupt(); break; }
        }
    }
    @Override public void close() {
        synchronized(closed) {
            if(closed.compareAndSet(false,true)) { for(Thread t:workers) t.interrupt(); Request r; while((r=queue.poll())!=null) fail(r,new CancellationException("inference stopped")); }
        }
    }
    public boolean awaitTermination(long milliseconds) throws InterruptedException {
        long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(milliseconds);
        for(Thread t:workers) {long left=deadline-System.nanoTime(); if(left>0)t.join(Math.max(1,TimeUnit.NANOSECONDS.toMillis(left)));}
        return workers.stream().noneMatch(Thread::isAlive);
    }
}
