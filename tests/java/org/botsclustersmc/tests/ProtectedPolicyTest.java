package org.botsclustersmc.tests;

import org.botsclustersmc.core.*;
import org.botsclustersmc.training.*;
import org.botsclustersmc.diagnostic.FocusExperiment;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarFile;

/** Independent routing, gradients, optimizer continuity and immutable sharing checks. */
public final class ProtectedPolicyTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void equal(float[] a,float[] b,String m){check(a.length==b.length,m);for(int i=0;i<a.length;i++)check(Float.floatToRawIntBits(a[i])==Float.floatToRawIntBits(b[i]),m);}
    private static void equal(double[] a,double[] b,String m){check(a.length==b.length,m);for(int i=0;i<a.length;i++)check(Double.doubleToRawLongBits(a[i])==Double.doubleToRawLongBits(b[i]),m);}
    @FunctionalInterface interface Call{void run()throws Exception;}
    private static void reject(Call f,String m)throws Exception{try{f.run();}catch(IllegalArgumentException|IOException expected){checks++;return;}throw new AssertionError(m);}
    private static float[] input(int task,int seed){
        Random r=new Random(seed);float[] x=new float[Schema.INPUTS];
        for(int i=0;i<x.length;i++)x[i]=(r.nextFloat()-.5f)*.4f;
        Arrays.fill(x,16,34,0);x[16+task]=1;x[0]=1;return x;
    }
    private static void function(Policy expected,Policy actual,float[] x,boolean[] mask){
        Policy.Workspace a=new Policy.Workspace(),b=new Policy.Workspace();expected.forward(x,mask,a);actual.forward(x,mask,b);
        equal(a.h1,b.h1,"first hidden");equal(a.h2,b.h2,"second hidden");equal(a.logits,b.logits,"logits and value");equal(a.probabilities,b.probabilities,"probabilities");
    }
    private static void batches(Policy source,Policy fork,Policy active){
        Policy.BatchWorkspace workspace=new Policy.BatchWorkspace(64);
        for(int n:new int[]{64,1,17,32,2,64}) {
            float[][] x=new float[n][];
            for(int lane=0;lane<n;lane++)x[lane]=input((lane*7+n)%18,1000+lane);
            fork.forwardBatch(x,n,workspace);
            for(int lane=0;lane<n;lane++){
                int task=(lane*7+n)%18;Policy oracle=task==11?active:source;
                Policy.Workspace s=new Policy.Workspace();oracle.forward(x[lane],Schema.unrestrictedMask(),s);
                float[] logits=new float[Schema.OUTPUTS];workspace.lane(lane,logits);equal(s.logits,logits,"packed mixed-task lane order");
                function(oracle,fork,x[lane],Task.at(task).mask(64,lane%2==0));
            }
        }
    }
    private static void preservation()throws Exception{
        Policy parent=new Policy(Policy.initialize(29).copyWeights(),100,20000);
        Policy candidate=Policy.focus(parent,11,true),control=Policy.focus(parent,11,false);
        check(candidate.anchor()==parent && control.anchor()==null,"initial sharing and control");
        byte[] parentBytes=PolicyFile.encode(parent);
        for(int task=0;task<18;task++)for(int sample=0;sample<8;sample++){
            float[] x=input(task,sample);boolean[] mask=Task.at(task).mask(64,sample%2==0);
            function(parent,candidate,x,mask);function(parent,control,x,mask);
        }
        float[] m=new float[Policy.PARAMETERS],v=new float[Policy.PARAMETERS];
        Arrays.fill(m,.001f);Arrays.fill(v,.002f);Adam optimizer=new Adam(m,v,100),reference=new Adam(m,v,100);
        Policy plain=parent;List<Policy> snapshots=new ArrayList<>();
        for(int step=0;step<48;step++){
            float[] x=input(11,step),dout=new float[Schema.OUTPUTS];
            dout[2]=.5f;dout[Schema.LOGITS]=-.4f;
            Policy.Workspace w=new Policy.Workspace();candidate.forward(x,Schema.unrestrictedMask(),w);
            float[] g=new float[Policy.PARAMETERS];candidate.backward(x,w,dout,g);
            Adam.Update next=optimizer.update(candidate,g,4,.001),same=reference.update(plain,g,4,.001);
            candidate=next.policy();optimizer=next.optimizer();plain=same.policy();reference=same.optimizer();
            equal(candidate.copyWeights(),plain.copyWeights(),"identical active Adam arithmetic");
            equal(optimizer.first(),reference.first(),"momentum continuity");equal(optimizer.second(),reference.second(),"variance continuity");
            check(candidate.anchor()==parent,"one shared immutable anchor through updates");snapshots.add(candidate);
            for(int task:new int[]{0,6,10,12,17})function(parent,candidate,input(task,step),Schema.unrestrictedMask());
        }
        check(!Arrays.equals(parent.copyWeights(),candidate.copyWeights()),"active copy actually learned/changed");
        check(Arrays.equals(parentBytes,PolicyFile.encode(parent)),"anchor bytes unchanged");
        check(snapshots.stream().map(Policy::anchor).distinct().count()==1,"all snapshots share one anchor not a bank copy");
        check((long)(snapshots.size()+1)*Policy.PARAMETERS*4==13493032L,"bounded weight payload for 48 snapshots plus shared anchor");
        batches(parent,candidate,plain);
        Policy restored=PolicyFile.decode(PolicyFile.encode(candidate));
        batches(parent,restored,plain);
        equal(parent.copyWeights(),restored.anchor().copyWeights(),"persistent anchor");
        check(restored.learningTask()==11 && restored.anchor().updates()==100,"persistent routing and source counter");
        reject(()->Policy.focus(parent,-1,true),"negative task");reject(()->Policy.focus(parent,18,true),"excess task");
        Policy finalCandidate=candidate;reject(()->Policy.focus(finalCandidate,12,true),"nested fork");
        float[] malformed=input(11,7);malformed[16]=1;
        reject(()->finalCandidate.forward(malformed,Schema.unrestrictedMask(),new Policy.Workspace()),"two goals");
        malformed[16]=0;malformed[27]=.5f;
        reject(()->finalCandidate.forwardBatch(new float[][]{malformed},1,new Policy.BatchWorkspace(1)),"fractional goal");
        malformed[27]=0;reject(()->Policy.goal(malformed),"no goal");
        float[] prior=input(10,1);Policy.Workspace state=new Policy.Workspace();finalCandidate.forward(prior,Schema.unrestrictedMask(),state);
        reject(()->finalCandidate.backward(prior,state,new float[Schema.OUTPUTS],new float[Policy.PARAMETERS]),"protected task gradient");
    }
    private static double objective(Policy p,float[] x,float[] d){Policy.Workspace w=new Policy.Workspace();p.forward(x,Schema.unrestrictedMask(),w);double s=0;for(int i=0;i<d.length;i++)s+=d[i]*w.logits[i];return s;}
    private static void gradients(){
        Policy p=Policy.focus(Policy.initialize(47),11,true);float[] x=input(11,19),d=new float[Schema.OUTPUTS];d[1]=.7f;d[Schema.LOGITS]=-.3f;
        Policy.Workspace s=new Policy.Workspace();p.forward(x,Schema.unrestrictedMask(),s);float[] gradient=new float[Policy.PARAMETERS];p.backward(x,s,d,gradient);
        int[] selected={Policy.W1+8,Policy.W1+27,Policy.B1,Policy.W2+97,Policy.B2+1,Policy.W3+Schema.HIDDEN+1,Policy.B3+1,Policy.W3+Schema.LOGITS*Schema.HIDDEN+2,Policy.B3+Schema.LOGITS};
        for(int i:selected){
            float[] plus=p.copyWeights(),minus=p.copyWeights();float e=.002f;plus[i]+=e;minus[i]-=e;
            double numeric=(objective(p.withWeights(plus,0,0),x,d)-objective(p.withWeights(minus,0,0),x,d))/(plus[i]-minus[i]);
            check(Math.abs(numeric-gradient[i])<.00015+.025*Math.abs(numeric),"meaningful active finite difference "+i);
        }
    }
    private static Trajectory trace(Policy p,int task,long actor,long sequence){
        float[] x=input(task,1);boolean[] mask=Schema.unrestrictedMask();Policy.Workspace w=new Policy.Workspace();p.forward(x,mask,w);
        int[] action=Schema.IDLE.clone();double log=Distribution.logProbability(w.probabilities,action);
        return new Trajectory(actor,7,sequence,List.of(new Transition(x,mask,action,log,p.updates(),1,4,x,mask,true)));
    }
    private static void scope()throws Exception{
        Policy p=Policy.focus(Policy.initialize(61),11,true);
        for(int task=0;task<18;task++)check(trace(p,task,task,0).learnable(p)==(task==11),"whole task selection");
        Trajectory old=trace(p,10,0,0);reject(()->Gradient.compute(p,List.of(old)),"direct gradient rejects excluded task");
        Transition t=old.steps().getFirst();
        Trajectory mixed=new Trajectory(7,7,0,List.of(new Transition(t.observation(),t.mask(),t.action(),t.behaviorLogProbability(),0,0,4,input(11,1),t.nextMask(),true)));
        reject(()->mixed.learnable(p),"trace changes goal even at terminal");
        AtomicReference<Throwable> failure=new AtomicReference<>();Learner learner=new Learner(p,new Adam(),1,8,1,64,unused->{},failure::set);
        try{
            check(!learner.offer(old),"old task not queued");
            check(learner.scopeSkipped.sum()==1 && learner.rejected.sum()==0,"separate skipped accounting");
            reject(()->learner.offer(old),"skipped sequence cannot replay");
            check(learner.offer(trace(p,11,1,0)),"frontier accepted");
        }finally{learner.close();check(learner.awaitTermination(10000),"learner termination");}
        check(failure.get()==null,"learner healthy");
        check(learner.policy().updates()==1 && learner.policy().samples()==1 && learner.taskSamples()[11]==1,"only true learned samples counted");
        check(learner.policy().anchor()==p.anchor(),"learner publishes shared protected anchor");
    }
    private static byte[] patched(byte[] bytes,int offset,int value)throws Exception{
        byte[] payload=PolicyFile.verify(bytes,Schema.MAX_MODEL_BYTES);ByteBuffer.wrap(payload).putInt(offset,value);return PolicyFile.checked(payload);
    }
    private static void data()throws Exception{
        Policy base=new Policy(Policy.initialize(3).copyWeights(),100,1000),p=Policy.focus(base,11,true);
        byte[] good=PolicyFile.encode(p);int offset;
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(PolicyFile.verify(good,Schema.MAX_MODEL_BYTES)))){
            in.readUTF();in.readUTF();in.readInt();in.readInt();int heads=in.readInt();for(int i=0;i<heads;i++)in.readInt();in.readLong();in.readLong();in.readInt();in.skipNBytes(4L*Policy.PARAMETERS);offset=good.length-8-in.available();
        }
        int taskOffset=offset;
        reject(()->PolicyFile.decode(patched(good,taskOffset,-1)),"anchor with no focus");
        reject(()->PolicyFile.decode(patched(good,taskOffset,18)),"invalid focus");
        byte[] invalidFlag=PolicyFile.verify(good,Schema.MAX_MODEL_BYTES);invalidFlag[taskOffset+4]=2;
        reject(()->PolicyFile.decode(PolicyFile.checked(invalidFlag)),"noncanonical flag");
        reject(()->PolicyFile.decode(patched(good,taskOffset+21,0x7fc00000)),"anchor nonfinite");
        byte[] future=PolicyFile.verify(good,Schema.MAX_MODEL_BYTES);ByteBuffer.wrap(future).putLong(taskOffset+5,101);
        reject(()->PolicyFile.decode(PolicyFile.checked(future)),"future anchor");
        byte[] missing=Arrays.copyOf(PolicyFile.verify(good,Schema.MAX_MODEL_BYTES),good.length-12);
        reject(()->PolicyFile.decode(PolicyFile.checked(missing)),"truncated anchor");
        byte[] trailing=Arrays.copyOf(PolicyFile.verify(good,Schema.MAX_MODEL_BYTES),good.length-7);
        reject(()->PolicyFile.decode(PolicyFile.checked(trailing)),"trailing data");
        TrainingState state=new TrainingState(p,new Adam(new float[Policy.PARAMETERS],new float[Policy.PARAMETERS],100),new Course(2,7).encode());
        TrainingState restored=TrainingState.decode(state.encode());
        check(Arrays.equals(state.encode(),restored.encode()),"complete training round trip");
        Path root=Files.createTempDirectory("bcmc-focus-");
        try{
            TrainingState source=new TrainingState(base,state.optimizer(),state.course());Path out=root.resolve("fork");FocusExperiment.create(source,out,11);
            for(String arm:List.of("protected","control")){
                TrainingState imported=TrainingState.read(out.resolve(arm+"-training.bcmc"));
                equal(imported.policy().copyWeights(),base.copyWeights(),"warm initial active weights");
                equal(imported.optimizer().first(),source.optimizer().first(),"original optimizer");
                check(Arrays.equals(imported.course(),source.course()),"exact course no invented certificates");
                check((imported.policy().anchor()!=null)==arm.equals("protected"),"only protected arm has anchor");
            }
            reject(()->FocusExperiment.create(source,out,11),"no overwrite");
        }finally{try(var paths=Files.walk(root)){for(Path f:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(f);}}
        for(String name:List.of("dist/botsclustersmc.jar","dist/training.jar"))try(JarFile jar=new JarFile(name)){
            check(jar.stream().noneMatch(e->e.getName().contains("FocusExperiment")),"offline construction absent from artifact");
        }
    }
    public static void main(String[] args)throws Exception{
        preservation();gradients();scope();data();System.out.println("PASS protected frontier: "+checks+" checks");
    }
}
