package org.botsclustersmc.core;

import java.util.Arrays;

/** P(menu) P(world controls | not opening) P(slot | click), with shared neural logits. */
public final class Distribution {
    private Distribution() {}
    public record Choice(int[] actions,double logProbability,double entropy) {}
    private static void shape(double[] p) {
        if(p.length!=Schema.DISTRIBUTION)throw new IllegalArgumentException("distribution dimensions");
    }
    public static void probabilities(float[] logits,boolean[] mask,double[] p) {
        shape(p);
        if(mask.length!=Schema.DISTRIBUTION||logits.length<Schema.LOGITS)
            throw new IllegalArgumentException("distribution dimensions");
        for(int i=0;i<Schema.LOGITS;i++)
            if(!Float.isFinite(logits[i]))throw new IllegalArgumentException("non-finite logits");
        Arrays.fill(p,0);
        int parent=Task.offset(6),child=Task.offset(7);
        softmax(logits,parent,mask,parent,Schema.HEADS[6],p);
        boolean world=false;
        for(int op=0;op<Schema.HEADS[6];op++)world|=mask[parent+op]&&MenuFocus.worldBranch(op);
        if(world)for(int head=0;head<6;head++) {
            int off=Task.offset(head);softmax(logits,off,mask,off,Schema.HEADS[head],p);
        }
        for(int op=1;op<=3;op++)if(mask[parent+op])
            softmax(logits,child,mask,Schema.slotOffset(op),Schema.HEADS[7],p);
    }
    private static void softmax(float[] logits,int logitOffset,boolean[] mask,int offset,int size,double[] p) {
        double max=Double.NEGATIVE_INFINITY;
        for(int j=0;j<size;j++)if(mask[offset+j])max=Math.max(max,logits[logitOffset+j]);
        if(!Double.isFinite(max))throw new IllegalArgumentException("empty active action head");
        double total=0;
        for(int j=0;j<size;j++) {
            double q=mask[offset+j]?Math.exp(logits[logitOffset+j]-max):0;
            p[offset+j]=q;total+=q;
        }
        for(int j=0;j<size;j++)p[offset+j]/=total;
    }
    private static int pick(double[] p,int off,int size,RandomSource rng,boolean greedy) {
        if(greedy) {
            int best=0;for(int j=1;j<size;j++)if(p[off+j]>p[off+best])best=j;
            if(!(p[off+best]>0))throw new IllegalArgumentException("empty sampled head");
            return best;
        }
        double u=rng.unit(),sum=0;int last=-1;
        for(int j=0;j<size;j++) {
            if(p[off+j]>0)last=j;
            sum+=p[off+j];if(u<sum)return j;
        }
        if(last<0)throw new IllegalArgumentException("empty sampled head");
        return last; // Rounding at the upper endpoint, never an extra random draw.
    }
    public static Choice choose(double[] p,RandomSource rng,boolean greedy) {
        shape(p);int[] a=Schema.IDLE.clone();
        a[6]=pick(p,Task.offset(6),Schema.HEADS[6],rng,greedy);
        if(MenuFocus.worldBranch(a[6]))for(int head=0;head<6;head++)
            a[head]=pick(p,Task.offset(head),Schema.HEADS[head],rng,greedy);
        if(Schema.slotActive(a[6]))a[7]=pick(p,Schema.slotOffset(a[6]),Schema.HEADS[7],rng,greedy);
        return new Choice(a,logProbability(p,a),entropy(p));
    }
    private static double log(double q) {
        if(!(q>0)||!Double.isFinite(q))throw new IllegalArgumentException("impossible action");
        return Math.log(q);
    }
    public static double logProbability(double[] p,int[] a) {
        shape(p);Schema.checkAction(a);double result=log(p[Task.offset(6)+a[6]]);
        if(MenuFocus.worldBranch(a[6]))for(int head=0;head<6;head++)
            result+=log(p[Task.offset(head)+a[head]]);
        if(Schema.slotActive(a[6]))result+=log(p[Schema.slotOffset(a[6])+a[7]]);
        return result;
    }
    private static double headEntropy(double[] p,int off,int size) {
        double e=0;for(int j=0;j<size;j++)if(p[off+j]>0)e-=p[off+j]*Math.log(p[off+j]);return e;
    }
    private static double worldProbability(double[] p) {
        double result=0;int parent=Task.offset(6);
        for(int op=0;op<Schema.HEADS[6];op++)if(MenuFocus.worldBranch(op))result+=p[parent+op];
        return result;
    }
    private static double worldEntropy(double[] p) {
        double result=0;
        for(int head=0;head<6;head++)result+=headEntropy(p,Task.offset(head),Schema.HEADS[head]);
        return result;
    }
    public static double entropy(double[] p) {
        shape(p);int parent=Task.offset(6);
        double e=headEntropy(p,parent,Schema.HEADS[6])+worldProbability(p)*worldEntropy(p);
        for(int op=1;op<=3;op++)e+=p[parent+op]*headEntropy(p,Schema.slotOffset(op),Schema.HEADS[7]);
        return e;
    }
    /** d(-advantage*log pi - entropyCoefficient*H)/d the SHARED neural logits. */
    public static void gradient(double[] p,int[] a,double advantage,double entropyCoefficient,float[] out) {
        shape(p);Schema.checkAction(a);
        if(out.length!=Schema.OUTPUTS||!Double.isFinite(advantage)||!Double.isFinite(entropyCoefficient))
            throw new IllegalArgumentException("gradient dimensions/value");
        Arrays.fill(out,0);
        int parent=Task.offset(6),child=Task.offset(7),off=0;
        double worldWeight=worldProbability(p),worldEntropy=worldEntropy(p);
        double[] childEntropy=new double[4];double expectedChildEntropy=worldWeight*worldEntropy;
        for(int op=1;op<=3;op++) {
            childEntropy[op]=headEntropy(p,Schema.slotOffset(op),Schema.HEADS[7]);
            expectedChildEntropy+=p[parent+op]*childEntropy[op];
        }
        for(int head=0;head<7;head++) {
            int size=Schema.HEADS[head];double h=headEntropy(p,off,size);
            for(int j=0;j<size;j++) {
                double q=p[off+j];if(q==0)continue;
                double scoreWeight=head==6||MenuFocus.worldBranch(a[6])?advantage:0;
                double entropyWeight=head==6?1:worldWeight;
                double g=scoreWeight*(q-(j==a[head]?1:0))+entropyCoefficient*entropyWeight*q*(Math.log(q)+h);
                if(head==6) {
                    double conditional=(MenuFocus.worldBranch(j)?worldEntropy:0)+(Schema.slotActive(j)?childEntropy[j]:0);
                    g-=entropyCoefficient*q*(conditional-expectedChildEntropy);
                }
                out[off+j]=(float)g;
            }
            off+=size;
        }
        for(int j=0;j<Schema.HEADS[7];j++) {
            double g=0;
            for(int op=1;op<=3;op++) {
                double q=p[Schema.slotOffset(op)+j];if(q==0)continue;
                if(a[6]==op)g+=advantage*(q-(j==a[7]?1:0));
                g+=entropyCoefficient*p[parent+op]*q*(Math.log(q)+childEntropy[op]);
            }
            out[child+j]=(float)g;
        }
    }
    /** Exact joint KL(before || after), including each conditional child only when reached. */
    public static double divergence(double[] before,double[] after) {
        shape(before);shape(after);int parent=Task.offset(6);
        double result=headDivergence(before,after,parent,Schema.HEADS[6]),worldWeight=worldProbability(before);
        if(worldWeight>0)for(int head=0;head<6;head++)
            result+=worldWeight*headDivergence(before,after,Task.offset(head),Schema.HEADS[head]);
        for(int op=1;op<=3;op++)if(before[parent+op]>0)
            result+=before[parent+op]*headDivergence(before,after,Schema.slotOffset(op),Schema.HEADS[7]);
        return Math.max(0,result);
    }
    private static double headDivergence(double[] p,double[] q,int off,int size) {
        double result=0;
        for(int j=0;j<size;j++)if(p[off+j]>0) {
            if(!(q[off+j]>0))return Double.POSITIVE_INFINITY;
            result+=p[off+j]*Math.log(p[off+j]/q[off+j]);
        }
        return result;
    }
}
