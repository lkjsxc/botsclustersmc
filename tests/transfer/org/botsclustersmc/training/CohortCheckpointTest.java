package org.botsclustersmc.training;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import org.botsclustersmc.core.PolicyFile;

/** Read-only real-parent qualification plus synthetic Course calls; never starts gameplay. */
public final class CohortCheckpointTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static String sha(byte[] bytes)throws Exception{
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    public static void main(String[] args)throws Exception{
        if(args.length!=3||(!args[2].equals("control")&&!args[2].equals("candidate")))
            throw new IllegalArgumentException("canonical-checkpoint initial-policy control|candidate");
        boolean candidate=args[2].equals("candidate");
        byte[] original=Files.readAllBytes(Path.of(args[0])),policy=Files.readAllBytes(Path.of(args[1]));
        TrainingState state=TrainingState.decode(original);
        check(sha(original).equals("a0ef5870276abc8f2cb6f7a30c95e3a57d20c0d9d3f8097a98132517b7e0e05e"),"exact complete parent");
        check(sha(policy).equals("ca9215b398f5a7097135af84f03f6857e104fdaf0bfcf79c0529669debdac1bc"),"exact inference policy");
        check(Arrays.equals(original,state.encode()),"all weights, Adam, and canonical course preserved");
        check(Arrays.equals(policy,PolicyFile.encode(state.policy())),"bit-identical inference export");
        check(state.policy().updates()==1182364&&state.policy().samples()==344488915,"native saved counters");
        Course course=Course.decode(state.course(),512);String decoded=sha(course.encode());
        check(course.effort().equals(new Course.Effort(0,0,0,0)),"no invented observed work on restore");
        int[] population=new int[18];ByteArrayOutputStream firstBytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(firstBytes)){
            for(int actor=0;actor<512;actor++){
                check(course.stage(actor)==12&&!course.needsExam(actor),"real preserved stage and examination eligibility");
                long[] certificates=new long[18];for(int task=0;task<18;task++)certificates[task]=course.certifiedVersion(actor,task);
                Course.Lesson l=course.issue(actor);int task=l.task().ordinal();population[task]++;
                check(task<12&&l.kind()!=Course.Kind.EXAM,"both arms have the same resume-first condition");
                out.writeLong(l.serial());out.writeInt(task);out.writeDouble(l.difficulty());out.writeLong(l.seed());out.writeInt(l.kind().ordinal());
                for(int t=0;t<18;t++)check(course.certifiedVersion(actor,t)==certificates[t],"first assignment does not replace earned certificates");
                // Simulate real observed fragments before later actors issue their first lesson.
                course.recordEffort(actor,l.serial(),10);course.finish(actor,l.serial(),true);
            }
        }
        for(int task=0;task<12;task++)check(population[task]>0,"every reached earlier task initially represented");
        String postFirst=sha(course.encode());
        check(course.effort().reviewTicks()==5120&&course.effort().frontierTicks()==0,"only simulated actually recorded ticks");
        Course.Lesson[] donors=new Course.Lesson[8];
        for(int actor=0;actor<8;actor++){
            donors[actor]=course.issue(actor);check(donors[actor].task().ordinal()==12,"donors execute existing frontier lessons");
        }
        for(int actor=0;actor<8;actor++)course.recordEffort(actor,donors[actor].serial(),3000); // All donor lessons remain unfinished.
        Course.Lesson recipient=course.issue(8);
        check(candidate?recipient.task().ordinal()<12:recipient.task().ordinal()==12,"expected sharing difference after identical startup");
        check(course.effort().frontierTicks()==24000&&course.effort().reviewTicks()==5120,"no forecast counted as actual work");
        check(Arrays.equals(original,state.encode())&&Arrays.equals(original,Files.readAllBytes(Path.of(args[0]))),"qualified input file never modified");
        System.out.println("{\"arm\":\""+args[2]+"\",\"checks\":"+checks+",\"decoded_course_sha256\":\""+decoded
            +"\",\"first_lessons_sha256\":\""+sha(firstBytes.toByteArray())+"\",\"post_first_course_sha256\":\""+postFirst
            +"\",\"initial_population\":"+Arrays.toString(population)+",\"recipient_task\":"+recipient.task().ordinal()+",\"new_training_samples\":0}");
    }
}
