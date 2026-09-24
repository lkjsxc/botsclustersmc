package org.botsclustersmc.training;

import org.botsclustersmc.core.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/** Asynchronous bounded learner; no Minecraft API and no all-actor terminal barrier. */
public final class Learner implements AutoCloseable {
    private final ArrayBlockingQueue<Trajectory> queue;
    private final ExecutorService kernels;
    private final Thread thread;
    private final int parallelism,batchSamples,maxLag;
    private final AtomicBoolean closing=new AtomicBoolean(),paused=new AtomicBoolean();
    private final Map<Long,Long> sequences=new HashMap<>();
    private final Consumer<Policy> publish;
    private final Consumer<Throwable> fatal;
    private volatile Policy policy;
    private volatile Adam optimizer;
    private volatile String state="collecting";
    private volatile boolean updating;
    public final LongAdder offered=new LongAdder(),rejected=new LongAdder(),stale=new LongAdder(),computeNanos=new LongAdder(),updates=new LongAdder();
    public volatile double gradientNorm,valueLoss,entropy,importance,meanPolicyKl,maxPolicyKl,learningRate;
    private final long[] learnedByTask=new long[TaskBalance.TASKS+1];
    private volatile long[] taskSampleSnapshot=learnedByTask.clone();
    private volatile TaskBalance lastBalance;
    private record MeasuredGradient(Gradient.Result gradient,ActivationHealth.Snapshot health) {}
    private volatile ActivationHealth.Measurement activationHealth;
    public ActivationHealth.Measurement activationHealth(){return activationHealth;}
    public long[] taskSamples(){return taskSampleSnapshot.clone();}
    public TaskBalance updateBalance(){return lastBalance;}
    public volatile int updateSamples;
    public final LongAdder guardBacktracks=new LongAdder(),guardRejectedSamples=new LongAdder(),batchWaitNanos=new LongAdder();
    public Learner(Policy policy,Adam optimizer,int threads,int capacity,int batchSamples,int maxLag,Consumer<Policy> publish,Consumer<Throwable> fatal) {
        if(threads<1||threads>128||capacity<1||capacity>16384||batchSamples<1||batchSamples>65536||maxLag<1||maxLag>10000)
            throw new IllegalArgumentException("learner bounds");
        if(policy.updates()!=optimizer.step())throw new IllegalArgumentException("optimizer identity");
        this.policy=policy;this.optimizer=optimizer;parallelism=threads;this.batchSamples=batchSamples;this.maxLag=maxLag;this.publish=publish;this.fatal=fatal;
        queue=new ArrayBlockingQueue<>(capacity);
        kernels=Executors.newFixedThreadPool(threads,r->{Thread t=new Thread(r,"bcmc-gradient");t.setDaemon(true);return t;});
        thread=new Thread(this::run,"bcmc-learner");thread.setDaemon(true);thread.start();
    }
    public boolean offer(Trajectory t) {
        offered.add(t.steps().size());
        synchronized(closing){
            if(t.actor()>100_000||t.sequence()<=sequences.getOrDefault(t.actor(),-1L))throw new IllegalArgumentException("duplicate/reversed trajectory");
            sequences.put(t.actor(),t.sequence());
            if(closing.get()||paused.get()||!queue.offer(t)){rejected.add(t.steps().size());return false;}
        }
        return true;
    }
    public int queued(){return queue.size();} public String state(){return state;}
    public Policy policy(){return policy;} public Adam optimizer(){return optimizer;}
    /** A coherent snapshot, also used while training continues. */
    public synchronized TrainingState snapshot(byte[] course){return new TrainingState(policy,optimizer,course);}
    public void pause(boolean pause){paused.set(pause);}
    public boolean quiescent(){synchronized(closing){return paused.get() && !updating && queue.isEmpty();}}
    private void run() {
        try {
            while(!closing.get()||!queue.isEmpty()) {
                Trajectory first;
                synchronized(closing){first=queue.poll();if(first!=null)updating=true;}
                if(first==null){state=paused.get()?"paused":"collecting";Thread.sleep(20);continue;}
                state="collecting-batch";long waiting=System.nanoTime(),deadline=waiting+100_000_000L;Policy target=policy;
                List<Trajectory> batch=new ArrayList<>();int count=0;
                Trajectory t=first;
                do {
                    boolean valid=true;
                    for(Transition s:t.steps()) if(s.behaviorVersion()>target.updates()||target.updates()-s.behaviorVersion()>maxLag){valid=false;break;}
                    if(valid){batch.add(t);count+=t.steps().size();}else stale.add(t.steps().size());
                    if(count>=batchSamples)break;
                    long remaining=deadline-System.nanoTime();
                    t=remaining>0&&!closing.get()?queue.poll(remaining,TimeUnit.NANOSECONDS):queue.poll();
                }while(t!=null);
                batchWaitNanos.add(System.nanoTime()-waiting);state="updating";long started=System.nanoTime();
                if(!batch.isEmpty()) {
                    TaskBalance balance=TaskBalance.forBatch(batch);lastBalance=balance;
                    int workers=Math.min(parallelism,batch.size());List<List<Trajectory>> groups=new ArrayList<>();
                    for(int i=0;i<workers;i++)groups.add(new ArrayList<>());
                    for(int i=0;i<batch.size();i++)groups.get(i%workers).add(batch.get(i));
                    List<Future<MeasuredGradient>> futures=new ArrayList<>();
                    for(List<Trajectory> group:groups)futures.add(kernels.submit(()->{
                        var health=new ActivationHealth.Accumulator(target.updates(),Schema.HIDDEN,TaskBalance.TASKS+1);
                        Gradient.Result gradient=Gradient.compute(target,group,balance,health);
                        return new MeasuredGradient(gradient,health.snapshot());
                    }));
                    var health=new ActivationHealth.Accumulator(target.updates(),Schema.HIDDEN,TaskBalance.TASKS+1);
                    float[] gradient=new float[Policy.PARAMETERS];int actual=0;double loss=0,ent=0,imp=0;
                    for(Future<MeasuredGradient> f:futures){MeasuredGradient measured=f.get();health.merge(measured.health());Gradient.Result g=measured.gradient();actual+=g.samples();loss+=g.valueLoss();ent+=g.entropy();imp+=g.importance();for(int i=0;i<gradient.length;i++)gradient[i]+=g.weights()[i];}
                    ActivationHealth.Snapshot measured=health.snapshot();
                    if(measured.totalSamples()!=actual)throw new IllegalStateException("activation measurement/sample accounting differs");
                    UpdateGuard.Result checked=UpdateGuard.update(target,optimizer,gradient,actual,batch);
                    guardBacktracks.add(checked.backtracks());updateSamples=actual;
                    if(checked.update()!=null){
                        Adam.Update update=checked.update();
                        synchronized(this){optimizer=update.optimizer();policy=update.policy();}
                        gradientNorm=update.gradientNorm();meanPolicyKl=checked.change().mean();maxPolicyKl=checked.change().maximum();learningRate=checked.learningRate();
                        int[] taskCounts=balance.counts();
                        for(int i=0;i<taskCounts.length;i++)learnedByTask[i]+=taskCounts[i];
                        taskSampleSnapshot=learnedByTask.clone();
                        publish.accept(policy);updates.increment();
                    }else{guardRejectedSamples.add(actual);learningRate=0;}
                    valueLoss=loss/actual;entropy=ent/actual;importance=imp/actual;
                    activationHealth=new ActivationHealth.Measurement(measured,System.currentTimeMillis(),checked.update()!=null);
                }
                computeNanos.add(System.nanoTime()-started);updating=false;
            }
            state="stopped";
        }catch(Throwable failure){state="failed";fatal.accept(failure);}
        finally{updating=false;kernels.shutdownNow();}
    }
    @Override public void close(){synchronized(closing){closing.set(true);}}
    public boolean awaitTermination(long milliseconds)throws InterruptedException{thread.join(milliseconds);return !thread.isAlive();}
}
