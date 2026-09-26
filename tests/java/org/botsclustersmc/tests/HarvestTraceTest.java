package org.botsclustersmc.tests;

import org.botsclustersmc.holdout.HarvestTrace;

/** Constructed counter tests, not learned Minecraft performance. */
public final class HarvestTraceTest {
    private static int checks;
    private static void check(boolean value){checks++;if(!value)throw new AssertionError("check "+checks);}
    private static void has(String json,String key,int n){check(json.contains("\""+key+"\":"+n+","));}
    private static void invalid(Runnable call) {
        try{call.run();throw new AssertionError("Expected invalid sample");}
        catch(IllegalArgumentException expected){checks++;}
    }
    public static void main(String[] args) {
        for(int task=-1;task<=18;task++)
            check(HarvestTrace.applies(task)==(task==5||task==6||task==12));
        HarvestTrace trace=new HarvestTrace();
        try{trace.json();throw new AssertionError("Empty trace accepted");}
        catch(IllegalStateException expected){checks++;}
        trace.observe(true,1,6,false,0); // A dig selection blocked by screen focus.
        trace.observe(false,1,6,true,4);
        trace.observe(false,1,7,true,39);
        trace.observe(false,1,0,true,5);
        trace.observe(false,0,0,false,0);
        trace.observe(false,2,6,false,0);
        trace.observe(false,3,0,true,0); // Just-broken samples must not count as positive contact.
        String json=trace.json();
        check(json.contains("decision-boundary-not-every-tick"));
        has(json,"observations",7);has(json,"menu_focused_selections",1);
        has(json,"world_dig_selections",3);has(json,"held_pick_observations",4);
        has(json,"target_pick_contact_observations",2);has(json,"target_other_contact_observations",1);
        has(json,"max_target_pick_ticks",39);has(json,"max_target_other_ticks",5);
        check(json.contains("\"interaction_selections\":[1, 4, 1, 1]"));
        check(json.equals(trace.json())); // Reading diagnostics cannot mutate counters.
        invalid(()->trace.observe(false,-1,6,true,4));
        invalid(()->trace.observe(false,4,6,true,4));
        invalid(()->trace.observe(false,1,-1,true,4));
        invalid(()->trace.observe(false,1,6,true,-1));
        invalid(()->trace.observe(false,1,6,false,4));
        check(json.equals(trace.json())); // Failed input is not partially recorded.
        System.out.println("PASS harvest boundary diagnostics="+checks);
    }
}
