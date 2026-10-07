package org.botsclustersmc.commons;

/** Full carried/banked/dropped resource counts, never recipe previews or selected-input counts. */
public record CommonsStock(int planks,int sticks,int picks) {
    public static final CommonsStock EMPTY=new CommonsStock(0,0,0);
    public static final CommonsStock INITIAL=new CommonsStock(3,2,0);
    public CommonsStock {
        if(planks<0||sticks<0||picks<0||planks>4096||sticks>4096||picks>4096)
            throw new IllegalArgumentException("bounded nonnegative stock required");
    }
    public CommonsStock plus(CommonsStock other) {
        return new CommonsStock(Math.addExact(planks,other.planks),Math.addExact(sticks,other.sticks),Math.addExact(picks,other.picks));
    }
    public CommonsStock minus(CommonsStock other) {
        return new CommonsStock(planks-other.planks,sticks-other.sticks,picks-other.picks);
    }
    public boolean contains(CommonsStock other) {
        return planks>=other.planks&&sticks>=other.sticks&&picks>=other.picks;
    }
    public static CommonsStock carried(org.botsclustersmc.core.Pocket pocket) {
        for(int kind=0;kind<20;kind++)if(kind!=3&&kind!=4&&kind!=6&&pocket.countKind(kind)!=0)
            throw new IllegalArgumentException("Unexpected carried resource in closed commons");
        if(pocket.countKind(3)!=pocket.count("OAK_PLANKS"))throw new IllegalArgumentException("Unexpected plank material");
        return new CommonsStock(pocket.count("OAK_PLANKS"),pocket.count("STICK"),pocket.count("WOODEN_PICKAXE"));
    }
    public int woodUnits() { return 2*planks+sticks+8*picks; }
    public String json() { return "["+planks+","+sticks+","+picks+"]"; }
    public static CommonsStock item(String item,int amount) {
        if(amount<0||amount>64)throw new IllegalArgumentException("item count");
        return switch(item) {
            case "AIR" -> {if(amount!=0)throw new IllegalArgumentException("nonempty air");yield EMPTY;}
            case "OAK_PLANKS" -> new CommonsStock(amount,0,0);
            case "STICK" -> new CommonsStock(0,amount,0);
            case "WOODEN_PICKAXE" -> new CommonsStock(0,0,amount);
            default -> throw new IllegalArgumentException("Unexpected resource in closed commons: "+item);
        };
    }
    /** Start with exactly three planks/two sticks and immutable stations. Transfers are zero-sum;
     * two planks -> four sticks and three planks + two sticks -> one pick conserve these units. */
    public static boolean delivered(CommonsStock total,CommonsStock banks,long craftedSticks,long craftedPicks) {
        CommonsLedger.loss(INITIAL,total,craftedSticks,craftedPicks);
        if(!total.contains(banks))
            throw new IllegalArgumentException("Impossible commons resource evidence");
        return craftedPicks==1&&banks.picks()==1&&total.equals(new CommonsStock(0,0,1));
    }
}
