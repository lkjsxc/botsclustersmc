package org.botsclustersmc.commons;

/** Owner-thread, test-only material ledger. Transfers have no effect; actual recipes consume inputs.
 * A lost item cannot reappear, even if the later total still fits the original wood-unit ceiling. */
public final class CommonsLedger {
    private final CommonsStock source;
    private CommonsStock lost=CommonsStock.EMPTY;
    private long sticks,picks;
    public CommonsLedger(CommonsStock source) { validSource(source);this.source=source; }
    private static void validSource(CommonsStock source) {
        if(source==null||source.planks()>3||source.sticks()>2||source.picks()!=0)
            throw new IllegalArgumentException("Expected admitted raw commons supplies");
    }
    /** Stick credit is the number of output items, as recorded by Pocket.crafted, not recipe clicks. */
    public static CommonsStock loss(CommonsStock source,CommonsStock total,long craftedSticks,long craftedPicks) {
        validSource(source);
        if(craftedSticks<0||craftedSticks>4||craftedSticks%4!=0||craftedPicks<0||craftedPicks>1)
            throw new IllegalArgumentException("Impossible commons recipe counters");
        int planks=source.planks()-2*(int)(craftedSticks/4)-3*(int)craftedPicks;
        int sticks=source.sticks()+(int)craftedSticks-2*(int)craftedPicks;
        if(planks<0||sticks<0)throw new IllegalArgumentException("Recipes exceed admitted ingredients");
        return new CommonsStock(planks,sticks,(int)craftedPicks).minus(total);
    }
    /** A rejected observation does not partially advance the ledger. */
    public CommonsStock sample(CommonsStock total,long craftedSticks,long craftedPicks) {
        CommonsStock next=loss(source,total,craftedSticks,craftedPicks);
        if(craftedSticks<sticks||craftedPicks<picks)throw new IllegalArgumentException("Recipe history went backwards");
        if(!next.contains(lost))throw new IllegalArgumentException("Lost commons resources reappeared");
        sticks=craftedSticks;picks=craftedPicks;lost=next;return next;
    }
}
