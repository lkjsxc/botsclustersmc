package org.botsclustersmc.tests;

import java.util.*;
import java.util.jar.JarFile;
import org.botsclustersmc.core.*;
import org.botsclustersmc.diagnostic.SlotExpansion;

/** Independent coefficient placement and expressiveness checks; not a learned-skill test. */
public final class SlotExpansionTest {
    private static int checks;
    private static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    private static void same(float a,float b,String why){check(Float.floatToRawIntBits(a)==Float.floatToRawIntBits(b),why);}
    private static void rejects(Runnable r){boolean failed=false;try{r.run();}catch(IllegalArgumentException e){failed=true;}check(failed,"malformed expansion rejected");}
    private static void mapping(){
        float[] old=new float[68842];for(int i=0;i<old.length;i++)old[i]=(i-34421)/65536f;
        old[2]=-0.0f;float[] saved=old.clone(),actual=SlotExpansion.expand(old);
        check(actual.length==81258,"explicit expanded layout");check(Arrays.equals(old,saved),"source not mutated");
        for(int i=0;i<58560;i++)same(actual[i],old[i],"trunk coefficient preserved");
        for(int row=0;row<41;row++) {
            for(int col=0;col<96;col++)same(actual[58560+row*96+col],old[58560+row*96+col],"parent projection");
            same(actual[81024+row],old[68736+row],"parent bias");
        }
        for(int click=0;click<3;click++)for(int slot=0;slot<64;slot++) {
            int row=41+click*64+slot;
            for(int col=0;col<96;col++)same(actual[58560+row*96+col],old[62496+slot*96+col],"copied click projection");
            same(actual[81024+row],old[68777+slot],"copied click bias");
        }
        for(int col=0;col<96;col++)same(actual[80928+col],old[68640+col],"critic moved, not duplicated slot");
        same(actual[81257],old[68841],"critic bias");
        actual[0]=9;same(old[0],saved[0],"no array alias");
        rejects(()->SlotExpansion.expand(new float[68841]));rejects(()->SlotExpansion.expand(new float[68843]));
        for(float invalid:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY}){
            float[] bad=saved.clone();bad[68777]=invalid;rejects(()->SlotExpansion.expand(bad));
        }
        rejects(()->SlotExpansion.sourceRow(-1));rejects(()->SlotExpansion.sourceRow(234));
    }
    private static void independentClicks(){
        boolean[] mask=Task.CRAFT_WORKBENCH.mask(64,true);Task.only(mask,6,1,2,3);Task.only(mask,7);
        for(int op=1;op<=3;op++){mask[Schema.slotOffset(op)]=true;mask[Schema.slotOffset(op)+1]=true;}
        float[] logits=new float[Schema.OUTPUTS];logits[41]=2;logits[106]=2;
        double[] p=new double[Schema.DISTRIBUTION];Distribution.probabilities(logits,mask,p);
        check(p[41]>p[42]&&p[105]<p[106]&&p[169]==p[170],"opposite click preferences on identical legal supports");
        for(int selected=1;selected<=3;selected++){
            int[] action=Schema.IDLE.clone();action[6]=selected;action[7]=0;float[] gradient=new float[Schema.OUTPUTS];
            Distribution.gradient(p,action,.7,0,gradient);
            for(int op=1;op<=3;op++)for(int slot=0;slot<64;slot++) {
                float g=gradient[Schema.slotOffset(op)+slot];
                if(op!=selected||slot>1)same(g,0,"sample advantage does not update another click slot output");
                else check(slot==0?g<0:g>0,"selected output receives a useful gradient");
            }
            same(gradient[Schema.LOGITS],0,"actor objective does not overwrite critic");
        }
    }
    private static void archives()throws Exception{
        for(String name:List.of("dist/botsclustersmc.jar","dist/training.jar"))try(JarFile jar=new JarFile(name)){
            check(jar.stream().noneMatch(e->e.getName().contains("SlotExpansion")),"offline mapper not shipped");
        }
    }
    public static void main(String[] args)throws Exception{
        mapping();independentClicks();archives();System.out.println("PASS click-conditioned expansion checks="+checks);
    }
}
