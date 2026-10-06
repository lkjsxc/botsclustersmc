package org.botsclustersmc.training;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import org.botsclustersmc.core.PolicyFile;

/** Finite long-run scheduling replay; no neural updates, Minecraft or skill claims. */
public final class ReviewAdmissionReplayTest {
    private static final int ACTORS=512,TARGET=3_000_000,BURN_IN=1_000_000;
    private static int checks;
    private static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    private static String sha(byte[] value)throws Exception{
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    }
    private record Duration(int decisions,boolean success){}
    private static Duration[][][] durations(byte[] csv)throws Exception{
        check(sha(csv).equals("38268911c0a258c77fb5a17203b8434e3346923c5847202c3eed800ddcda8bcc"),"exact historical duration fixture");
        Duration[][][] bank=new Duration[2][12][32];String[] lines=new String(csv,java.nio.charset.StandardCharsets.UTF_8).split("\\R");
        check(lines.length==769&&lines[0].equals("seed,task,actor,observations,elapsed_ticks,success"),"complete known fixture");
        for(int line=1;line<lines.length;line++){
            String[] fields=lines[line].split(",");check(fields.length==6,"duration fields");
            int seed=(int)(Long.parseLong(fields[0])-2026100611L),task=Integer.parseInt(fields[1]),actor=Integer.parseInt(fields[2]);
            int decisions=Integer.parseInt(fields[3]),ticks=Integer.parseInt(fields[4]),success=Integer.parseInt(fields[5]);
            int slot=actor-task*32;
            check(seed>=0&&seed<2&&task>=0&&task<12&&slot>=0&&slot<32,"historical trial identity");
            check(bank[seed][task][slot]==null&&decisions>0&&decisions<=600&&ticks>=decisions*5&&ticks<=decisions*5+10
                &&(success==0||success==1),"unique bounded duration");
            bank[seed][task][slot]=new Duration(decisions,success==1);
        }
        for(var seed:bank)for(var task:seed)for(var duration:task)check(duration!=null,"no missing historical case");
        return bank;
    }
    private static boolean replay(byte[] encoded,Duration[][] bank,String arm,int seed,int variant)throws Exception{
        Course course=Course.decode(encoded,ACTORS);Course.Lesson[] current=new Course.Lesson[ACTORS];
        int[] left=new int[ACTORS],draws=new int[ACTORS],completedFrontier=new int[ACTORS];boolean[] succeeds=new boolean[ACTORS];
        long[][] exposure=new long[ACTORS][13];long[] counts=new long[13],atBurn=null,old=new long[13];
        long decisions=0,minFrontier=Long.MAX_VALUE,minReview=Long.MAX_VALUE,minPerTask=Long.MAX_VALUE;
        int changedStage=0;double minimumWindow=1,maximumWindow=0;
        for(int round=0;decisions<TARGET;round++)for(int order=0;order<ACTORS&&decisions<TARGET;order++){
            int actor=variant==1?ACTORS-1-order:order;
            if(variant==2&&actor%2==0&&round%2==0)continue;
            if(current[actor]==null){
                // Do not invent successful exams, force a frontier or reset earned progress.
                // A stage change makes this fixed-parent scheduling screen inapplicable.
                if(course.stage(actor)!=12||course.needsExam(actor)){
                    changedStage++;
                    System.out.printf(Locale.ROOT,"{\"arm\":\"%s\",\"duration_seed\":%d,\"variant\":%d,\"modeled_decisions\":%d,\"stage_change_or_exam\":true,\"screen_pass\":false,\"new_training_samples\":0}%n",
                        arm,2026100611L+seed,variant,decisions);
                    return false;
                }
                current[actor]=course.issue(actor);int task=current[actor].task().ordinal();
                check(task<=12&&current[actor].kind()!=Course.Kind.EXAM,"training-only replay scope");
                if(task==12){left[actor]=600;succeeds[actor]=false;}
                else if(variant==3){left[actor]=1;succeeds[actor]=true;}
                else{
                    Duration d=bank[task][Math.floorMod(actor*7+draws[actor]*13,32)];
                    left[actor]=d.decisions();succeeds[actor]=d.success();
                }
                draws[actor]++;
            }
            Course.Lesson lesson=current[actor];int task=lesson.task().ordinal();
            decisions++;counts[task]++;exposure[actor][task]++;course.recordEffort(actor,lesson.serial(),5);
            if(--left[actor]==0){
                course.finish(actor,lesson.serial(),succeeds[actor]);current[actor]=null;
                if(task==12)completedFrontier[actor]++;
            }
            if(decisions==BURN_IN)atBurn=counts.clone();
            if(decisions%50000==0){
                long review=0;for(int t=0;t<12;t++)review+=counts[t]-old[t];
                double share=review/50000.0;minimumWindow=Math.min(minimumWindow,share);maximumWindow=Math.max(maximumWindow,share);old=counts.clone();
            }
        }
        long lateReview=0;for(int t=0;t<12;t++)lateReview+=counts[t]-atBurn[t];
        double share=lateReview/(double)(TARGET-BURN_IN);
        for(int actor=0;actor<ACTORS;actor++){
            minFrontier=Math.min(minFrontier,completedFrontier[actor]);long review=0;
            for(int task=0;task<12;task++){review+=exposure[actor][task];minPerTask=Math.min(minPerTask,exposure[actor][task]);}
            minReview=Math.min(minReview,review);
        }
        Course.Effort effort=course.effort();
        check(effort.frontierTicks()+effort.reviewTicks()==5*decisions&&effort.examTicks()==0&&effort.foundationTicks()==0,"all modeled actor-ticks conserved");
        boolean pass=changedStage==0&&share>=.10&&share<=.40&&minFrontier>=1&&minPerTask>=1;
        System.out.printf(Locale.ROOT,"{\"arm\":\"%s\",\"duration_seed\":%d,\"variant\":%d,\"modeled_decisions\":%d,\"late_review_share\":%.8f,\"minimum_frontier_completions_per_actor\":%d,\"minimum_review_decisions_per_actor\":%d,\"minimum_decisions_per_actor_earlier_task\":%d,\"minimum_50k_window_review_share\":%.8f,\"maximum_50k_window_review_share\":%.8f,\"by_task\":%s,\"screen_pass\":%s,\"new_training_samples\":0}%n",
            arm,2026100611L+seed,variant,decisions,share,minFrontier,minReview,minPerTask,minimumWindow,maximumWindow,Arrays.toString(counts),pass);
        return pass;
    }
    public static void main(String[] args)throws Exception{
        if(args.length!=4||!Set.of("candidate","control").contains(args[3]))throw new IllegalArgumentException("checkpoint policy duration-csv candidate|control");
        byte[] checkpoint=Files.readAllBytes(Path.of(args[0])),policy=Files.readAllBytes(Path.of(args[1]));
        check(sha(checkpoint).equals("a0ef5870276abc8f2cb6f7a30c95e3a57d20c0d9d3f8097a98132517b7e0e05e"),"exact common parent");
        check(sha(policy).equals("ca9215b398f5a7097135af84f03f6857e104fdaf0bfcf79c0529669debdac1bc"),"exact initial policy");
        TrainingState state=TrainingState.decode(checkpoint);
        check(Arrays.equals(checkpoint,state.encode())&&Arrays.equals(policy,PolicyFile.encode(state.policy())),"unchanged model, Adam, course and policy");
        Duration[][][] bank=durations(Files.readAllBytes(Path.of(args[2])));int passed=0;
        for(int seed=0;seed<2;seed++)for(int variant=0;variant<4;variant++)if(replay(state.course(),bank[seed],args[3],seed,variant))passed++;
        check(Arrays.equals(checkpoint,state.encode())&&Arrays.equals(checkpoint,Files.readAllBytes(Path.of(args[0]))),"complete saved parent unchanged");
        System.out.printf("{\"arm\":\"%s\",\"checks\":%d,\"scenarios\":8,\"passed_scenarios\":%d,\"new_training_samples\":0}%n",args[3],checks,passed);
        check(passed==8,"all predeclared long-run qualification cases must pass");
    }
}
