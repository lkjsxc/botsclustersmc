package org.botsclustersmc.tests;

import java.util.*;
import org.botsclustersmc.needs.SharedNeed;
import org.botsclustersmc.needs.SharedNeed.*;

public final class SharedNeedTest {
    private static int checks;
    private static void check(boolean ok, String text) { checks++; if (!ok) throw new AssertionError(text); }
    private static void near(double a, double b, String text) { check(Math.abs(a-b)<1e-7, text+": "+a+" != "+b); }
    private static void reject(Runnable action, String text) {
        try { action.run(); } catch (IllegalArgumentException | IllegalStateException | NullPointerException expected) { checks++; return; }
        throw new AssertionError(text+" rejected");
    }
    public static void main(String[] args) {
        Contract contract = new Contract(7, 100, 64, new Stock(3, 2, 1));
        Frame half = new Frame(contract, 25, true, new Stock(3, 1, 0));
        near(half.coverage(), .5, "equal-kind public demand coverage");
        near(new Frame(contract, 0, true, new Stock(4096,4096,4096)).coverage(), 1, "surplus saturation");
        float[] signal = half.inputs();
        check(signal.length==SharedNeed.WIDTH && signal[0]==1 && signal[1]==1, "sidecar presence and knowledge");
        near(signal[2], .75, "remaining horizon");
        near(signal[3], 3/4096.0, "requested planks"); near(signal[7], 1/4096.0, "observed sticks");
        near(signal[9], 0, "plank shortfall"); near(signal[10], .5, "stick shortfall"); near(signal[11], 1, "pick shortfall");
        near(signal[12], .5, "observable objective");
        for (float value : signal) check(Float.isFinite(value) && value>=0 && value<=1, "bounded signal");
        signal[0]=99; check(half.inputs()[0]==1, "fresh immutable signal");
        Frame anonymous = new Frame(new Contract(999,100,2,contract.target()),25,true,half.bank());
        check(Arrays.equals(half.inputs(),anonymous.inputs()), "no episode, role or population identity in signal");
        Frame unknown = new Frame(contract,25,false,Stock.EMPTY);
        float[] missing=unknown.inputs(); check(missing[0]==1 && missing[1]==0, "unknown not absent demand");
        for (int i=6;i<missing.length;i++) check(missing[i]==0, "unknown clears stock and derived values");
        check(!Arrays.equals(missing,new Frame(contract,25,true,Stock.EMPTY).inputs()), "unknown not empty stock");
        reject(unknown::coverage,"unknown coverage");
        reject(()->new Frame(contract,1,false,new Stock(0,0,1)),"stale stock in unknown frame");
        reject(()->new Stock(-1,0,0),"negative stock"); reject(()->new Stock(0,4097,0),"oversized stock");
        reject(()->new Stock(0,0,4097),"oversized tools"); reject(()->Stock.EMPTY.at(3),"unknown material");
        reject(()->new Contract(-1,1,1,new Stock(1,0,0)),"negative episode");
        reject(()->new Contract(1,0,1,new Stock(1,0,0)),"zero horizon");
        reject(()->new Contract(1,12001,1,new Stock(1,0,0)),"oversized horizon");
        reject(()->new Contract(1,1,0,new Stock(1,0,0)),"empty cohort");
        reject(()->new Contract(1,1,513,new Stock(1,0,0)),"oversized cohort");
        reject(()->new Contract(1,1,1,Stock.EMPTY),"vacuous demand");
        reject(()->new Contract(1,1,1,null),"missing target");
        reject(()->new Frame(contract,-1,true,Stock.EMPTY),"negative frame tick");
        reject(()->new Frame(contract,101,true,Stock.EMPTY),"post-horizon frame");
        reject(()->new Frame(contract,0,true,null),"missing stock");
        for (int mask=1;mask<8;mask++) {
            Stock target=new Stock((mask&1)==0?0:3,(mask&2)==0?0:2,(mask&4)==0?0:1);
            Contract c=new Contract(mask,1,1,target);
            near(new Frame(c,0,true,target).coverage(),1,"every requested subset can be covered");
            near(new Frame(c,0,true,Stock.EMPTY).coverage(),0,"every positive demand can be unmet");
            float[] fields=new Frame(c,0,true,Stock.EMPTY).inputs();
            for(int k=0;k<3;k++) near(fields[9+k],target.at(k)==0?0:1,"unrequested materials have no deficit");
        }
        System.out.println("PASS shared need sidecar "+checks+" checks; no policy integration or learned behavior");
    }
}
