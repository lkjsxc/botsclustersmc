package org.botsclustersmc.tests;

import java.util.Arrays;
import java.util.Map;
import org.botsclustersmc.training.ActivationHealth;

/** Synthetic diagnostic arithmetic only; these tests do not establish learned competence. */
public final class ActivationHealthTest {
    private static int checks;
    private static void check(boolean ok){checks++;if(!ok)throw new AssertionError("activation health check "+checks);}
    private static void near(double a,double b){check(Math.abs(a-b)<1e-12);}
    private static void fails(Runnable action){boolean failed=false;try{action.run();}catch(IllegalArgumentException expected){failed=true;}check(failed);}
    public static void main(String[] args) {
        var health=new ActivationHealth.Accumulator(71,4,19);
        var empty=health.snapshot();check(empty.totalSamples()==0);
        for(double n:empty.saturation(0))near(n,-1);
        float threshold=ActivationHealth.SATURATION_THRESHOLD;
        float[] first={0,1,-1,threshold},second={.5f,-.5f,0,-threshold};
        float[] preservedFirst=first.clone(),preservedSecond=second.clone();
        health.observe(10,first,second);
        var one=health.snapshot();
        check(Arrays.equals(first,preservedFirst)&&Arrays.equals(second,preservedSecond));
        check(one.totalSamples()==1&&one.samples()[10]==1&&one.policyUpdates()==71&&one.hiddenUnits()==4);
        near(one.saturation(0)[10],.5);near(one.saturation(1)[10],0);
        near(one.meanSlope(0)[10],(2-(double)threshold*threshold)/4);
        near(one.meanSlope(1)[10],(3.5-(double)threshold*threshold)/4);
        Arrays.fill(first,0);Arrays.fill(second,1);health.observe(10,first,second);
        near(one.saturation(0)[10],.5);check(one.totalSamples()==1); // Snapshot owns its values.
        one.samples()[10]=200;one.saturation(0)[10]=0;one.meanSlope(0)[10]=0;
        near(one.saturation(0)[10],.5);check(one.samples()[10]==1);
        var two=health.snapshot();near(two.saturation(0)[10],.25);near(two.saturation(1)[10],.5);
        for(int task=0;task<19;task++)if(task!=10){check(two.samples()[task]==0);near(two.meanSlope(0)[task],-1);}
        for(float value:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,1.001f,-1.001f}) {
            float[] invalid={0,0,0,value};fails(()->health.observe(10,invalid,second));
            fails(()->health.observe(10,first,invalid));check(health.snapshot().totalSamples()==2);
        }
        fails(()->health.observe(-1,first,second));fails(()->health.observe(19,first,second));
        fails(()->health.observe(0,new float[3],second));fails(()->health.observe(0,first,new float[3]));
        fails(()->new ActivationHealth.Accumulator(-1,4,19));fails(()->new ActivationHealth.Accumulator(0,0,19));
        fails(()->new ActivationHealth.Accumulator(0,4097,19));fails(()->new ActivationHealth.Accumulator(0,4,0));
        fails(()->new ActivationHealth.Accumulator(0,4,65));
        fails(()->health.merge(new ActivationHealth.Accumulator(72,4,19).snapshot()));
        fails(()->health.merge(new ActivationHealth.Accumulator(71,3,19).snapshot()));
        fails(()->health.merge(new ActivationHealth.Accumulator(71,4,18).snapshot()));
        check(health.snapshot().totalSamples()==2);
        fails(()->two.saturation(2));fails(()->two.meanSlope(-1));
        fails(()->new ActivationHealth.Measurement(empty,1,true));fails(()->new ActivationHealth.Measurement(two,0,true));
        var total=new ActivationHealth.Accumulator(71,4,19);
        var partitions=new ActivationHealth.Accumulator[7];
        for(int i=0;i<partitions.length;i++)partitions[i]=new ActivationHealth.Accumulator(71,4,19);
        for(int i=0;i<2048;i++) {
            int task=i%19;
            float[] a={(float)Math.tanh(i%17-8),.25f,-.99f,0};
            float[] b={(float)Math.tanh(i%13-6),-.75f,.999f,1};
            total.observe(task,a,b);partitions[i%7].observe(task,a,b);
        }
        var combined=new ActivationHealth.Accumulator(71,4,19);
        for(var partition:partitions)combined.merge(partition.snapshot());
        var expected=total.snapshot();var actual=combined.snapshot();
        check(actual.totalSamples()==2048&&Arrays.equals(expected.samples(),actual.samples()));
        for(int layer=0;layer<2;layer++)for(int task=0;task<19;task++){
            near(expected.saturation(layer)[task],actual.saturation(layer)[task]);
            near(expected.meanSlope(layer)[task],actual.meanSlope(layer)[task]);
        }
        for(boolean accepted:new boolean[]{true,false}) {
            var measurement=new ActivationHealth.Measurement(actual,123456789,accepted);
            Map<String,Object> status=measurement.status();
            check(status.get("activation_health_samples").equals(2048L));
            check(status.get("activation_health_policy_updates").equals(71L));
            check(status.get("activation_health_update_accepted").equals(accepted));
            check(status.get("activation_health_scope").equals("last-learner-batch-unweighted"));
        }
        System.out.println("PASS activation health arithmetic, immutable snapshots, absence and partition checks="+checks);
    }
}
