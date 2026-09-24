package org.botsclustersmc.training;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Read-only hidden-activation measurements. Never changes an observation, reward or gradient. */
public final class ActivationHealth {
    private ActivationHealth() {}
    public static final float SATURATION_THRESHOLD=.99f;

    /** Worker-local bounded counters; no observations or network arrays are retained. */
    public static final class Accumulator {
        private final long policyUpdates;
        private final int hiddenUnits;
        private final long[] samples,firstSaturated,secondSaturated;
        private final double[] firstSlope,secondSlope;
        public Accumulator(long policyUpdates,int hiddenUnits,int tasks) {
            if(policyUpdates<0||hiddenUnits<1||hiddenUnits>4096||tasks<1||tasks>64)
                throw new IllegalArgumentException("activation health dimensions/identity");
            this.policyUpdates=policyUpdates;this.hiddenUnits=hiddenUnits;
            samples=new long[tasks];firstSaturated=new long[tasks];secondSaturated=new long[tasks];
            firstSlope=new double[tasks];secondSlope=new double[tasks];
        }
        public void require(long policy,int units,int tasks) {
            if(policyUpdates!=policy||hiddenUnits!=units||samples.length!=tasks)
                throw new IllegalArgumentException("activation health measurement identity differs");
        }
        public void observe(int task,float[] first,float[] second) {
            if(task<0||task>=samples.length||first.length!=hiddenUnits||second.length!=hiddenUnits)
                throw new IllegalArgumentException("activation health observation dimensions");
            long a=0,b=0;double da=0,db=0;
            // Validate the complete observation before updating any counters.
            for(int i=0;i<hiddenUnits;i++) {
                double x=first[i],y=second[i];
                if(!Double.isFinite(x)||!Double.isFinite(y)||Math.abs(x)>1||Math.abs(y)>1)
                    throw new IllegalArgumentException("hidden activation outside finite tanh range");
                if(Math.abs(x)>SATURATION_THRESHOLD)a++;
                if(Math.abs(y)>SATURATION_THRESHOLD)b++;
                da+=1-x*x;db+=1-y*y;
            }
            samples[task]=Math.addExact(samples[task],1);
            firstSaturated[task]=Math.addExact(firstSaturated[task],a);
            secondSaturated[task]=Math.addExact(secondSaturated[task],b);
            firstSlope[task]+=da;secondSlope[task]+=db;
        }
        public void merge(Snapshot other) {
            require(other.policyUpdates,other.hiddenUnits,other.samples.length);
            for(int task=0;task<samples.length;task++) {
                samples[task]=Math.addExact(samples[task],other.samples[task]);
                firstSaturated[task]=Math.addExact(firstSaturated[task],other.firstSaturated[task]);
                secondSaturated[task]=Math.addExact(secondSaturated[task],other.secondSaturated[task]);
                firstSlope[task]+=other.firstSlope[task];secondSlope[task]+=other.secondSlope[task];
            }
        }
        public Snapshot snapshot() {
            return new Snapshot(policyUpdates,hiddenUnits,samples,firstSaturated,secondSaturated,firstSlope,secondSlope);
        }
    }

    /** Immutable raw-sample statistics. A task absent from this batch has fraction -1, not zero. */
    public static final class Snapshot {
        private final long policyUpdates;
        private final int hiddenUnits;
        private final long[] samples,firstSaturated,secondSaturated;
        private final double[] firstSlope,secondSlope;
        private Snapshot(long policyUpdates,int hiddenUnits,long[] samples,long[] firstSaturated,long[] secondSaturated,
                         double[] firstSlope,double[] secondSlope) {
            this.policyUpdates=policyUpdates;this.hiddenUnits=hiddenUnits;
            this.samples=samples.clone();this.firstSaturated=firstSaturated.clone();this.secondSaturated=secondSaturated.clone();
            this.firstSlope=firstSlope.clone();this.secondSlope=secondSlope.clone();
        }
        public long policyUpdates(){return policyUpdates;}
        public int hiddenUnits(){return hiddenUnits;}
        public long[] samples(){return samples.clone();}
        public long totalSamples(){long n=0;for(long count:samples)n=Math.addExact(n,count);return n;}
        public double[] saturation(int layer) {
            long[] counts=switch(layer){case 0->firstSaturated;case 1->secondSaturated;default->throw new IllegalArgumentException("layer");};
            double[] result=new double[samples.length];
            for(int i=0;i<result.length;i++)result[i]=samples[i]==0?-1:counts[i]/((double)samples[i]*hiddenUnits);
            return result;
        }
        public double[] meanSlope(int layer) {
            double[] sums=switch(layer){case 0->firstSlope;case 1->secondSlope;default->throw new IllegalArgumentException("layer");};
            double[] result=new double[samples.length];
            for(int i=0;i<result.length;i++)result[i]=samples[i]==0?-1:sums[i]/((double)samples[i]*hiddenUnits);
            return result;
        }
    }

    /** One publication ties the measured PRE-update policy to its actual batch and disposition. */
    public record Measurement(Snapshot snapshot,long epochMillis,boolean updateAccepted) {
        public Measurement {
            if(snapshot==null||snapshot.totalSamples()<1||epochMillis<=0)throw new IllegalArgumentException("empty/timeless activation measurement");
        }
        public Map<String,Object> status() {
            Map<String,Object> values=new LinkedHashMap<>();
            values.put("activation_health_scope","last-learner-batch-unweighted");
            values.put("activation_health_policy_updates",snapshot.policyUpdates());
            values.put("activation_health_epoch_millis",epochMillis);
            values.put("activation_health_update_accepted",updateAccepted);
            values.put("activation_health_samples",snapshot.totalSamples());
            values.put("activation_health_hidden_units",snapshot.hiddenUnits());
            values.put("activation_health_saturation_threshold",SATURATION_THRESHOLD);
            values.put("activation_health_task_samples",Arrays.toString(snapshot.samples()));
            values.put("activation_health_first_saturation",Arrays.toString(snapshot.saturation(0)));
            values.put("activation_health_second_saturation",Arrays.toString(snapshot.saturation(1)));
            values.put("activation_health_first_mean_slope",Arrays.toString(snapshot.meanSlope(0)));
            values.put("activation_health_second_mean_slope",Arrays.toString(snapshot.meanSlope(1)));
            return Map.copyOf(values);
        }
    }
}
