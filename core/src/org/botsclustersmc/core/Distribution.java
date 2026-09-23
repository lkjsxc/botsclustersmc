package org.botsclustersmc.core;

import java.util.Arrays;

/** Masked categorical heads; GUI slot is conditional on a click operation. */
public final class Distribution {
    private Distribution() {}
    public record Choice(int[] actions, double logProbability, double entropy) {}
    public static void probabilities(float[] logits, boolean[] mask, double[] probabilities) {
        if(mask.length!=Schema.LOGITS || logits.length<Schema.LOGITS || probabilities.length!=Schema.LOGITS)
            throw new IllegalArgumentException("distribution dimensions");
        int off=0;
        for(int size:Schema.HEADS) {
            double max=Double.NEGATIVE_INFINITY;
            for(int j=0;j<size;j++) {
                if(!Float.isFinite(logits[off+j])) throw new IllegalArgumentException("non-finite logits");
                if(mask[off+j]) max=Math.max(max, logits[off+j]);
            }
            if(!Double.isFinite(max)) throw new IllegalArgumentException("empty action head");
            double total=0;
            for(int j=0;j<size;j++) { double p=mask[off+j]?Math.exp(logits[off+j]-max):0; probabilities[off+j]=p; total+=p; }
            for(int j=0;j<size;j++) probabilities[off+j]/=total;
            off+=size;
        }
    }
    public static Choice choose(double[] p, RandomSource rng, boolean greedy) {
        int[] a=new int[Schema.HEADS.length]; int off=0;
        for(int h=0;h<a.length;h++) {
            int size=Schema.HEADS[h];
            if(h==7 && !Schema.slotActive(a[6])) a[h]=0;
            else if(greedy) {
                int best=0; for(int j=1;j<size;j++) if(p[off+j]>p[off+best]) best=j; a[h]=best;
            } else {
                double u=rng.unit(), sum=0; int last=-1;
                for(int j=0;j<size;j++) { if(p[off+j]>0) last=j; sum+=p[off+j]; if(u<sum) {a[h]=j; last=-1; break;} }
                if(last>=0) a[h]=last;
            }
            off+=size;
        }
        return new Choice(a, logProbability(p,a), entropy(p));
    }
    public static double logProbability(double[] p, int[] a) {
        Schema.checkAction(a); int off=0; double result=0;
        for(int h=0;h<a.length;h++) {
            if(h!=7 || Schema.slotActive(a[6])) {
                double q=p[off+a[h]];
                if(!(q>0) || !Double.isFinite(q)) throw new IllegalArgumentException("impossible action");
                result+=Math.log(q);
            }
            off+=Schema.HEADS[h];
        }
        return result;
    }
    private static int slotOffset() { return Schema.LOGITS-Schema.HEADS[7]; }
    private static int operationOffset() { return slotOffset()-Schema.HEADS[6]; }
    private static double headEntropy(double[] p,int off,int n) {
        double e=0; for(int j=0;j<n;j++) if(p[off+j]>0) e-=p[off+j]*Math.log(p[off+j]); return e;
    }
    public static double entropy(double[] p) {
        int off=0; double e=0;
        for(int h=0;h<7;h++) { e+=headEntropy(p,off,Schema.HEADS[h]); off+=Schema.HEADS[h]; }
        int parent=operationOffset(); double active=p[parent+1]+p[parent+2]+p[parent+3];
        return e+active*headEntropy(p,slotOffset(),Schema.HEADS[7]);
    }
    /** Adds d(-advantage*log pi - entropyCoefficient*H)/d logits. */
    public static void gradient(double[] p,int[] a,double advantage,double entropyCoefficient,float[] out) {
        Schema.checkAction(a); Arrays.fill(out,0); int off=0;
        int parent=operationOffset(), child=slotOffset();
        double active=p[parent+1]+p[parent+2]+p[parent+3];
        double childEntropy=headEntropy(p,child,Schema.HEADS[7]);
        for(int h=0;h<Schema.HEADS.length;h++) {
            int n=Schema.HEADS[h]; double entropy=headEntropy(p,off,n);
            boolean contributes=h!=7 || Schema.slotActive(a[6]);
            double entropyWeight=h==7?active:1;
            for(int j=0;j<n;j++) {
                double q=p[off+j]; if(q==0) continue;
                double g=contributes?advantage*(q-(j==a[h]?1:0)):0;
                g+=entropyCoefficient*entropyWeight*q*(Math.log(q)+entropy);
                if(h==6) g-=entropyCoefficient*childEntropy*q*((Schema.slotActive(j)?1:0)-active);
                out[off+j]=(float)g;
            }
            off+=n;
        }
    }
}
