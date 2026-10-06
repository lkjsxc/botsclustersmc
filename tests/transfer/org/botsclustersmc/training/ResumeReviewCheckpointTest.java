package org.botsclustersmc.training;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import org.botsclustersmc.core.PolicyFile;

/** Read-only actual-parent qualification. This class is absent from both runtime JARs. */
public final class ResumeReviewCheckpointTest {
    private static int checks;
    private static void check(boolean value,String message) {
        checks++;if(!value)throw new AssertionError(message);
    }
    private static String sha(byte[] bytes)throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=3||(!args[2].equals("control")&&!args[2].equals("candidate")))
            throw new IllegalArgumentException("canonical-checkpoint initial-policy control|candidate");
        byte[] original=Files.readAllBytes(Path.of(args[0])),policy=Files.readAllBytes(Path.of(args[1]));
        TrainingState state=TrainingState.decode(original);
        check(sha(original).equals("a0ef5870276abc8f2cb6f7a30c95e3a57d20c0d9d3f8097a98132517b7e0e05e"),"exact complete parent");
        check(sha(policy).equals("ca9215b398f5a7097135af84f03f6857e104fdaf0bfcf79c0529669debdac1bc"),"exact policy");
        check(Arrays.equals(original,state.encode()),"all weights, Adam moments and course roundtrip exactly");
        check(Arrays.equals(policy,PolicyFile.encode(state.policy())),"canonical export bit-identical");
        check(state.policy().updates()==1182364&&state.policy().samples()==344488915,"exact inherited counters");
        Course course=Course.decode(state.course(),512);
        byte[] decoded=course.encode();long episodes=course.episodes(),successes=course.successes();
        check(course.effort().equals(new Course.Effort(0,0,0,0)),"zero invented observed effort");
        int[] population=new int[18];
        for(int actor=0;actor<512;actor++) {
            check(course.stage(actor)==12&&!course.needsExam(actor),"preserved task12 nonexam parent");
            long[] certificates=new long[18];for(int task=0;task<18;task++)certificates[task]=course.certifiedVersion(actor,task);
            Course.Lesson lesson=course.issue(actor);int task=lesson.task().ordinal();population[task]++;
            check(args[2].equals("candidate")?task<12:task==12,"only initial review allocation differs");
            check(lesson.kind()!=Course.Kind.EXAM,"training lesson, not an earned exam");
            for(int t=0;t<18;t++)check(course.certifiedVersion(actor,t)==certificates[t],"certificates unchanged");
        }
        check(course.episodes()==episodes&&course.successes()==successes,"issued lessons are not completed experience");
        check(course.effort().equals(new Course.Effort(0,0,0,0)),"assignments are not observed exposure");
        check(Arrays.equals(original,state.encode())&&Arrays.equals(original,Files.readAllBytes(Path.of(args[0]))),"source state stays immutable");
        if(args[2].equals("candidate"))for(int task=0;task<12;task++)check(population[task]>0,"actual parent assigns every earlier task");
        System.out.println("{\"arm\":\""+args[2]+"\",\"checks\":"+checks+",\"decoded_course_sha256\":\""+sha(decoded)
            +"\",\"canonical_course_sha256\":\""+sha(state.course())+"\",\"initial_population\":"+Arrays.toString(population)+",\"new_training_samples\":0}");
    }
}
