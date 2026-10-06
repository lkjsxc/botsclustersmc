package org.botsclustersmc.training;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import org.botsclustersmc.core.PolicyFile;

/** Real-parent, offline scheduling opportunity check. No learner, resets or gameplay. */
public final class ReviewOpportunityTest {
    private static int checks;
    private static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    private static String sha(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    private record Duration(int decisions,boolean success){}
    @SuppressWarnings("unchecked")
    private static List<Duration>[][] fixture(Path path)throws Exception{
        List<Duration>[][] data=(List<Duration>[][])new List<?>[2][12];
        for(int seed=0;seed<2;seed++)for(int t=0;t<12;t++)data[seed][t]=new ArrayList<>();
        List<String> lines=Files.readAllLines(path);check(lines.size()==769,"two complete older frozen duration sets");
        check(lines.getFirst().equals("seed,task,actor,observations,elapsed_ticks,success"),"duration schema");
        Set<String> seen=new HashSet<>();
        for(String line:lines.subList(1,lines.size())){
            String[] x=line.split(",");check(x.length==6,"six duration fields");
            long seed=Long.parseLong(x[0]);int index=(int)(seed-2026100611L),task=Integer.parseInt(x[1]);
            int actor=Integer.parseInt(x[2]),decisions=Integer.parseInt(x[3]),ticks=Integer.parseInt(x[4]),success=Integer.parseInt(x[5]);
            check(index>=0&&index<2&&task>=0&&task<12&&actor==task*32+data[index][task].size(),"ordered unique historical cases");
            check(decisions>0&&decisions<=600&&ticks>=decisions*5&&ticks<=decisions*5+10&&(success==0||success==1),"bounded historical duration");
            check(seen.add(seed+":"+actor),"no duplicate physical duration case");
            data[index][task].add(new Duration(decisions,success==1));
        }
        for(var seed:data)for(var task:seed)check(task.size()==32,"complete task duration bank");
        return data;
    }
    private static boolean simulate(byte[] parent,List<Duration>[] durations,String arm,int durationSeed,int variant)throws Exception{
        Course c=Course.decode(parent,512);Course.Lesson[] active=new Course.Lesson[512];
        int[] left=new int[512],draws=new int[512];boolean[] succeeds=new boolean[512];
        long[] counts=new long[13],previous=new long[13],lateStart=null;
        long initialEpisodes=c.stageMetrics()[12].trainingEpisodes(),samples=0,observedTicks=0;int window=0;boolean delivered=true;
        // Offer one transition per ready actor per round, with explicit 5-tick intervals.
        // The skew variant skips half the actors every other round, retaining in-flight work.
        for(int round=0;samples<250000;round++)for(int a=0;a<512&&samples<250000;a++){
            int actor=variant==1?511-a:a;
            if(variant==2&&actor%2==0&&round%2==0)continue;
            if(active[actor]==null){
                check(!c.needsExam(actor),"no synthetic exam qualification in short opportunity window");
                active[actor]=c.issue(actor);int task=active[actor].task().ordinal();
                check(task<=12&&active[actor].kind()!=Course.Kind.EXAM,"training task scope");
                if(task==12){left[actor]=600;succeeds[actor]=false;}
                else if(variant==3){left[actor]=1;succeeds[actor]=true;}
                else{
                    Duration d=durations[task].get(Math.floorMod(actor*7+draws[actor]*13,32));
                    left[actor]=d.decisions();succeeds[actor]=d.success();
                }
                draws[actor]++;
            }
            int task=active[actor].task().ordinal();counts[task]++;samples++;observedTicks+=5;
            c.recordEffort(actor,active[actor].serial(),5);
            if(--left[actor]==0){c.finish(actor,active[actor].serial(),succeeds[actor]);active[actor]=null;}
            if(samples%50000==0){
                long[] delta=new long[13];long review=0;
                for(int t=0;t<13;t++){delta[t]=counts[t]-previous[t];if(t<12)review+=delta[t];}
                double share=review/50000.0;boolean pass=share>=.05&&share<=.40&&delta[12]>=25000;
                if(window==0)for(long n:counts)pass&=n>0;
                delivered&=pass;
                System.out.printf(Locale.ROOT,"{\"arm\":\"%s\",\"duration_seed\":%d,\"variant\":%d,\"modeled_samples\":%d,\"review_share\":%.8f,\"by_task\":%s,\"window_pass\":%s}%n",arm,2026100611L+durationSeed,variant,samples,share,Arrays.toString(delta),pass);
                if(samples==100000)lateStart=counts.clone();previous=counts.clone();window++;
            }
        }
        long[] late=new long[12];for(int t=0;t<12;t++){late[t]=counts[t]-lateStart[t];delivered&=late[t]>=100;}
        Course.Effort e=c.effort();check(e.frontierTicks()+e.reviewTicks()==observedTicks&&e.examTicks()==0,"all modeled ticks conserved separately from samples");
        long completed=c.stageMetrics()[12].trainingEpisodes()-initialEpisodes;
        // Fast actors in the skew case may finish a 600-decision frontier episode.
        if(variant!=2)check(completed==0,"opportunities while every long frontier episode remains unfinished");
        System.out.printf(Locale.ROOT,"{\"arm\":\"%s\",\"duration_seed\":%d,\"variant\":%d,\"completed_frontier\":%d,\"late_by_task\":%s,\"opportunity_screen\":%s,\"new_training_samples\":0}%n",arm,2026100611L+durationSeed,variant,completed,Arrays.toString(late),delivered);
        return delivered;
    }
    public static void main(String[] args)throws Exception{
        if(args.length!=4||!Set.of("control","candidate","cohort").contains(args[3]))throw new IllegalArgumentException("checkpoint policy duration-csv control|candidate|cohort");
        byte[] checkpoint=Files.readAllBytes(Path.of(args[0])),policy=Files.readAllBytes(Path.of(args[1]));
        check(sha(checkpoint).equals("a0ef5870276abc8f2cb6f7a30c95e3a57d20c0d9d3f8097a98132517b7e0e05e"),"exact common parent");
        check(sha(policy).equals("ca9215b398f5a7097135af84f03f6857e104fdaf0bfcf79c0529669debdac1bc"),"exact initial policy");
        TrainingState state=TrainingState.decode(checkpoint);
        check(Arrays.equals(checkpoint,state.encode())&&Arrays.equals(policy,PolicyFile.encode(state.policy())),"bit-identical model/Adam/export");
        check(sha(Files.readAllBytes(Path.of(args[2]))).equals("38268911c0a258c77fb5a17203b8434e3346923c5847202c3eed800ddcda8bcc"),"exact historical duration fixture");
        var durations=fixture(Path.of(args[2]));
        int delivered=0;
        for(int seed=0;seed<2;seed++)for(int variant=0;variant<4;variant++)if(simulate(state.course(),durations[seed],args[3],seed,variant))delivered++;
        check(Arrays.equals(checkpoint,Files.readAllBytes(Path.of(args[0])))&&Arrays.equals(checkpoint,state.encode()),"input state immutable");
        check(args[3].equals("candidate")?delivered==8:delivered==0,"required candidate feasibility / negative-control opportunity contrast");
        System.out.printf("{\"arm\":\"%s\",\"checks\":%d,\"passed_scenarios\":%d,\"scenarios\":8,\"new_training_samples\":0}%n",args[3],checks,delivered);
    }
}
