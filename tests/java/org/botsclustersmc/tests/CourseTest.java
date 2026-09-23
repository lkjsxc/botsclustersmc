package org.botsclustersmc.tests;
import org.botsclustersmc.core.*;
import org.botsclustersmc.training.*;
import java.util.*;

public final class CourseTest {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    interface Throwing {void run()throws Exception;}
    static void fails(Throwing fn)throws Exception{boolean rejected=false;try{fn.run();}catch(Exception expected){rejected=true;}check(rejected,"invalid state rejected");}
    static void ready(Course course,long actor){
        for(int n=0;n<4000&&!course.needsExam(actor);n++){Course.Lesson l=course.issue(actor);check(l!=null&&l.kind()!=Course.Kind.EXAM,"practice before exam");if(l.kind()==Course.Kind.PROBE)check(l.difficulty()==1,"probe full difficulty");course.finish(actor,l.serial(),true);}
        check(course.needsExam(actor),"actor became ready");check(course.issue(actor)==null,"snapshot is mandatory before exam");
    }
    static void exam(Course c,long actor,long version,int failingTask,int misses){
        c.beginExam(actor,version);int stage=c.stage(actor),count=0;
        while(c.examVersion(actor)>=0){Course.Lesson l=c.issue(actor);check(l.kind()==Course.Kind.EXAM&&l.difficulty()==1,"full frozen exam");check(c.examVersion(actor)==version,"same snapshot throughout actor exam");boolean success=true;if(l.task().ordinal()==failingTask&&misses-->0)success=false;c.finish(actor,l.serial(),success);count++;}
        check(count==16+stage*4,"complete fixed exam denominator");
    }
    public static void main(String[] args)throws Exception{
        Course c=new Course(2,7);ready(c,0);check(c.stage(1)==0,"another actor has not been promoted");
        c.beginExam(0,7);Course.Lesson exam=c.issue(0);Course.Lesson other=c.issue(1);check(other.kind()!=Course.Kind.EXAM,"other actor still trains during exam");c.finish(1,other.serial(),false);c.finish(0,exam.serial(),true);
        while(c.examVersion(0)>=0){Course.Lesson l=c.issue(0);c.finish(0,l.serial(),true);}
        check(c.stage(0)==1&&c.stage(1)==0,"independent promotion, weak actor cannot hide in average");check(c.certifiedVersion(0,0)==7,"certificate records frozen version");
        ready(c,0);exam(c,0,12,1,3);check(c.stage(0)==1,"13/16 cannot pass current task");
        ready(c,0);exam(c,0,13,0,2);check(c.stage(0)==1,"2/4 cannot pass a retained skill");
        ready(c,0);c.beginExam(0,14);Course.Lesson partial=c.issue(0);c.finish(0,partial.serial(),true);c.issue(0);
        Course restored=Course.decode(c.encode(),2);check(restored.examVersion(0)==-1,"unfinished frozen exam is discarded");check(restored.stage(0)==1&&restored.certifiedVersion(0,0)==7,"previous earned certificate survives restart");check(restored.abandoned()==c.abandoned()+1,"unfinished episode is accounted");
        Course deterministic=Course.decode(restored.encode(),2);Course.Lesson one=restored.issue(1),two=deterministic.issue(1);check(one.equals(two),"seed/draw/serial resume exactly");restored.abandon(1);check(restored.abandoned()>0,"pause/reset abandons explicitly");
        fails(()->Course.decode(c.encode(),3));fails(()->new Course(0,1));fails(()->Course.decode(new byte[]{1,2},2));
        Course all=new Course(1,999);for(int task=0;task<18;task++){ready(all,0);exam(all,0,100+task,-1,0);check(all.stage(0)==Math.min(17,task+1),"all curriculum transitions reachable");}check(all.completed()&&all.completedAgents()==1,"all 18 independently certified");
        Course big=new Course(2048,3);for(int i=0;i<2048;i++){Course.Lesson l=big.issue(i);big.finish(i,l.serial(),i%2==0);}check(big.running()==0&&big.episodes()==2048,"population accounting");
        for(int population:new int[]{1,16,32,64,256,1024,2048,10000})for(int threads:new int[]{1,2,8,32}){
            int size=ArenaLayout.islandSize(population,threads);Set<String> positions=new HashSet<>();for(int i=0;i<population;i++){ArenaLayout a=ArenaLayout.forActor(i,population,size);check(positions.add(a.chunkX()+":"+a.chunkZ()),"unique physical cell");check(a.contains(a.x()+8,65,a.z()+8),"arena inside");check(!a.contains(a.x(),65,a.z()),"arena wall outside");}}
        System.out.println("PASS independent course checks="+checks);
    }
}
