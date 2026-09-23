package org.botsclustersmc.training;

/** IMPALA-style clipped importance correction, with an actual-tick discount. */
public final class VTrace {
    private VTrace() {}
    public record Returns(double[] values,double[] advantages) {}
    public static Returns compute(double[] reward,double[] discount,double[] value,double[] nextValue,
                                  double[] logRatio,boolean[] carry) {
        int n=reward.length;
        if(n==0 || discount.length!=n || value.length!=n || nextValue.length!=n || logRatio.length!=n || carry.length!=n)
            throw new IllegalArgumentException("trace dimensions");
        double[] target=new double[n],advantage=new double[n];
        for(int i=n-1;i>=0;i--) {
            if(!Double.isFinite(reward[i]) || !Double.isFinite(discount[i]) || discount[i]<0 || discount[i]>1
                || !Double.isFinite(value[i]) || !Double.isFinite(nextValue[i]) || !Double.isFinite(logRatio[i]))
                throw new IllegalArgumentException("non-finite trace");
            double rho=Math.exp(Math.min(0,logRatio[i])); // rho_bar = c_bar = 1
            double next=carry[i] && i+1<n ? target[i+1] : nextValue[i];
            target[i]=value[i]+rho*(reward[i]+discount[i]*nextValue[i]-value[i])
                +discount[i]*rho*(next-nextValue[i]);
            advantage[i]=rho*(reward[i]+discount[i]*next-value[i]);
            if(!Double.isFinite(target[i]) || !Double.isFinite(advantage[i])) throw new IllegalArgumentException("trace overflow");
        }
        return new Returns(target,advantage);
    }
    public static double discount(int ticks,boolean terminal) {
        if(ticks<1) throw new IllegalArgumentException("ticks");
        return terminal?0:Math.pow(0.997,ticks/4.0);
    }
}
