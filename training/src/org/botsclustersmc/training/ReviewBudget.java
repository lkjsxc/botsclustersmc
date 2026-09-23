package org.botsclustersmc.training;

/** Process-local actor-time allocation; it does not select a gameplay action. */
public final class ReviewBudget {
    public static final double FRACTION=.20;
    private double debt;
    private long currentTicks,olderTicks;
    public boolean shouldReview(){return debt>0;}
    public double debt(){return debt;}
    public long currentTicks(){return currentTicks;}
    public long olderTicks(){return olderTicks;}
    public void record(boolean current,int ticks) {
        if(ticks<1)throw new IllegalArgumentException("Training work must contain real ticks");
        if(current){currentTicks=Math.addExact(currentTicks,ticks);debt+=ticks*(FRACTION/(1-FRACTION));}
        else{olderTicks=Math.addExact(olderTicks,ticks);debt-=ticks;}
    }
}
