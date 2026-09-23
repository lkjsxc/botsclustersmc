package org.botsclustersmc.training;

import org.botsclustersmc.core.*;
import java.io.*;
import java.util.*;

/** Per-actor curriculum: no population-wide training or evaluation barrier. */
public final class Course {
    public enum Kind {PRACTICE,PROBE,EXAM}
    public record Lesson(long serial,Task task,double difficulty,long seed,Kind kind){}
    private static final int TASKS=18;
    private static final class Agent {
        final RandomSource rng;final int[] episodes=new int[TASKS],probes=new int[TASKS],examSuccess=new int[TASKS];
        final double[] ema=new double[TASKS],probeEma=new double[TASKS];final long[] certified=new long[TASKS];
        int stage,draws,sinceExam,probesSinceExam,examIndex;boolean complete,exam;long examVersion=-1;Lesson current;
        Agent(long seed){rng=new RandomSource(seed);Arrays.fill(ema,.2);Arrays.fill(certified,-1);}
    }
    private final Agent[] agents;private long serial,episodes,successes,exams,passed,abandoned,regressions;
    public Course(int actors,long seed){if(actors<1||actors>10000)throw new IllegalArgumentException("actor count");agents=new Agent[actors];for(int i=0;i<actors;i++)agents[i]=new Agent(seed+i*7919L);}
    private Agent agent(long id){if(id<0||id>=agents.length)throw new IllegalArgumentException("actor identity");return agents[(int)id];}
    private boolean eligible(Agent a){return !a.exam&&a.episodes[a.stage]>=40&&a.probes[a.stage]>=8&&a.probeEma[a.stage]>=.7&&a.sinceExam>=(a.complete?256:20)&&a.probesSinceExam>=4;}
    public synchronized boolean needsExam(long actor){Agent a=agent(actor);return a.current==null&&eligible(a);}
    public synchronized void beginExam(long actor,long policyVersion){Agent a=agent(actor);if(a.current!=null||!eligible(a)||policyVersion<0)throw new IllegalStateException("actor not ready for a frozen exam");a.exam=true;a.examVersion=policyVersion;a.examIndex=0;Arrays.fill(a.examSuccess,0);exams++;}
    public synchronized Lesson issue(long actor){
        Agent a=agent(actor);if(a.current!=null)throw new IllegalStateException("actor already has a lesson");if(eligible(a))return null;
        int selected=a.stage;Kind kind;double difficulty;
        if(a.exam){kind=Kind.EXAM;selected=a.examIndex<16?a.stage:(a.examIndex-16)/4;difficulty=1;}
        else{a.draws++;if(a.stage>0&&a.rng.nextInt(5)==0)selected=a.rng.nextInt(a.stage);kind=a.draws%5==1?Kind.PROBE:Kind.PRACTICE;difficulty=kind==Kind.PROBE?1:Math.max(.1,Math.min(1,.15+.85*a.ema[selected]));}
        a.current=new Lesson(++serial,Task.at(selected),difficulty,a.rng.nextLong(),kind);return a.current;
    }
    public synchronized void finish(long actor,long lessonSerial,boolean success){
        Agent a=agent(actor);Lesson lesson=a.current;if(lesson==null||lesson.serial()!=lessonSerial)throw new IllegalStateException("lesson identity mismatch");a.current=null;episodes++;if(success)successes++;
        int task=lesson.task().ordinal();
        if(lesson.kind()==Kind.EXAM){
            if(success)a.examSuccess[task]++;a.examIndex++;
            if(a.examIndex==16+a.stage*4){
                boolean pass=a.examSuccess[a.stage]>=14;for(int i=0;i<a.stage;i++)pass&=a.examSuccess[i]>=3;
                if(pass){for(int i=0;i<=a.stage;i++)a.certified[i]=a.examVersion;passed++;if(a.stage==TASKS-1)a.complete=true;else a.stage++;}
                a.exam=false;a.examVersion=-1;a.sinceExam=0;a.probesSinceExam=0;
            }
        }else{
            a.episodes[task]++;a.sinceExam++;a.ema[task]=.95*a.ema[task]+.05*(success?1:0);
            if(lesson.kind()==Kind.PROBE){a.probes[task]++;a.probeEma[task]=.9*a.probeEma[task]+.1*(success?1:0);if(task==a.stage)a.probesSinceExam++;
                // A real regression returns THIS actor to the forgotten skill, not every actor.
                if(task<a.stage&&a.probes[task]>=8&&a.probeEma[task]<.45){a.stage=task;a.complete=false;a.sinceExam=0;a.probesSinceExam=0;regressions++;}
            }
        }
    }
    public synchronized void abandon(long actor){Agent a=agent(actor);if(a.current!=null){a.current=null;abandoned++;}if(a.exam){a.exam=false;a.examVersion=-1;a.examIndex=0;a.sinceExam=0;a.probesSinceExam=0;Arrays.fill(a.examSuccess,0);}}
    public synchronized long examVersion(long actor){return agent(actor).examVersion;}
    public record Progress(int stage,int practiceEpisodes,int probes,double practiceSuccess,double probeSuccess,boolean exam,long examPolicy,int examCases,boolean completed) {}
    public synchronized Progress progress(long actor){
        Agent a=agent(actor);
        return new Progress(a.stage,a.episodes[a.stage],a.probes[a.stage],a.ema[a.stage],a.probeEma[a.stage],a.exam,a.examVersion,a.examIndex,a.complete);
    }
    public record Metrics(double practiceMean,double probeMean,double bestProbe,int ready) {}
    public synchronized Metrics metrics(){
        double practice=0,probe=0,best=0;int ready=0;
        for(Agent a:agents){practice+=a.ema[a.stage];probe+=a.probeEma[a.stage];best=Math.max(best,a.probeEma[a.stage]);if(eligible(a))ready++;}
        return new Metrics(practice/agents.length,probe/agents.length,best,ready);
    }
    public synchronized int stage(long actor){return agent(actor).stage;}
    public synchronized boolean completed(long actor){return agent(actor).complete;}
    public synchronized long certifiedVersion(long actor,int task){return agent(actor).certified[task];}
    public synchronized int task(){int min=17;for(Agent a:agents)min=Math.min(min,a.stage);return min;}
    public synchronized int maximumTask(){int max=0;for(Agent a:agents)max=Math.max(max,a.stage);return max;}
    public synchronized int[] population(){int[] result=new int[TASKS];for(Agent a:agents)result[a.stage]++;return result;}
    public synchronized int running(){int n=0;for(Agent a:agents)if(a.current!=null)n++;return n;}
    public synchronized int examAgents(){int n=0;for(Agent a:agents)if(a.exam)n++;return n;}
    public synchronized int completedAgents(){int n=0;for(Agent a:agents)if(a.complete)n++;return n;}
    public synchronized boolean completed(){return completedAgents()==agents.length;}
    public synchronized long episodes(){return episodes;}public synchronized long successes(){return successes;}public synchronized long exams(){return exams;}public synchronized long passedExams(){return passed;}public synchronized long abandoned(){return abandoned;}public synchronized long regressions(){return regressions;}
    public synchronized byte[] encode()throws IOException{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(bytes)){
            out.writeUTF("BCMC-INDEPENDENT-COURSE");out.writeInt(agents.length);for(long x:new long[]{serial,episodes,successes,exams,passed,abandoned,regressions})out.writeLong(x);
            for(Agent a:agents){out.writeLong(a.rng.state());out.writeInt(a.stage);out.writeInt(a.draws);out.writeInt(a.sinceExam);out.writeInt(a.probesSinceExam);out.writeBoolean(a.complete);out.writeBoolean(a.current!=null);out.writeBoolean(a.exam);
                for(int i=0;i<TASKS;i++){out.writeInt(a.episodes[i]);out.writeInt(a.probes[i]);out.writeDouble(a.ema[i]);out.writeDouble(a.probeEma[i]);out.writeLong(a.certified[i]);}}
        }return bytes.toByteArray();
    }
    public static Course decode(byte[] bytes,int actors)throws IOException{
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))){
            if(!in.readUTF().equals("BCMC-INDEPENDENT-COURSE")||in.readInt()!=actors)throw new IOException("course schema/actor count differs; choose a fresh Academy");Course c=new Course(actors,0);
            c.serial=in.readLong();c.episodes=in.readLong();c.successes=in.readLong();c.exams=in.readLong();c.passed=in.readLong();c.abandoned=in.readLong();c.regressions=in.readLong();
            if(c.serial<0||c.episodes<0||c.successes<0||c.successes>c.episodes||c.exams<0||c.passed<0||c.passed>c.exams||c.abandoned<0||c.regressions<0)throw new IOException("course counters");
            for(int n=0;n<actors;n++){Agent a=new Agent(in.readLong());c.agents[n]=a;a.stage=in.readInt();a.draws=in.readInt();a.sinceExam=in.readInt();a.probesSinceExam=in.readInt();a.complete=in.readBoolean();boolean active=in.readBoolean(),exam=in.readBoolean();
                if(a.stage<0||a.stage>=TASKS||a.draws<0||a.sinceExam<0||a.probesSinceExam<0||(a.complete&&a.stage!=17))throw new IOException("course actor state");
                if(active)c.abandoned++;if(exam){a.sinceExam=0;a.probesSinceExam=0;} // Whole unfinished exam is discarded, never cherry-picked.
                for(int i=0;i<TASKS;i++){a.episodes[i]=in.readInt();a.probes[i]=in.readInt();a.ema[i]=in.readDouble();a.probeEma[i]=in.readDouble();a.certified[i]=in.readLong();
                    if(a.episodes[i]<0||a.probes[i]<0||a.probes[i]>a.episodes[i]||!Double.isFinite(a.ema[i])||a.ema[i]<0||a.ema[i]>1||!Double.isFinite(a.probeEma[i])||a.probeEma[i]<0||a.probeEma[i]>1||a.certified[i]<-1)throw new IOException("course statistics");}}
            if(in.available()!=0)throw new IOException("trailing course bytes");return c;
        }catch(IllegalArgumentException e){throw new IOException("invalid course",e);}
    }
}
