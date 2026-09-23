package org.botsclustersmc.core;

/** Exact material names are retained; observation buckets do not rewrite actual items. */
public record Stack(String item,int count) {
    public static final Stack EMPTY=new Stack("AIR",0);
    public Stack {
        if(item==null||!item.matches("[A-Z][A-Z0-9_]{0,63}")||count<0||count>64||((count==0)!=item.equals("AIR")))
            throw new IllegalArgumentException("invalid item stack");
    }
    public boolean empty(){return count==0;}
    public int maximum(){return item.endsWith("_PICKAXE")?1:64;}
    public Stack withCount(int n){return n==0?EMPTY:new Stack(item,n);}
    public static int kind(String name) {
        if(name.equals("AIR")||name.endsWith("_AIR"))return 0;
        if(name.endsWith("_LOG")||name.endsWith("_STEM"))return 2;
        if(name.endsWith("_PLANKS"))return 3;
        return switch(name){case "STICK"->4;case "CRAFTING_TABLE"->5;case "WOODEN_PICKAXE"->6;case "STONE_PICKAXE"->7;
            case "COBBLESTONE"->8;case "STONE"->9;case "RAW_IRON"->10;case "IRON_ORE","DEEPSLATE_IRON_ORE"->11;
            case "COAL","CHARCOAL"->12;case "IRON_INGOT"->13;case "CHEST"->14;case "FURNACE"->15;
            case "GLASS"->16;case "BEDROCK"->17;case "DIRT"->18;case "GRASS_BLOCK"->19;default->1;};
    }
}
