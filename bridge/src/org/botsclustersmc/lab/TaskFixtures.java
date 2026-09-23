package org.botsclustersmc.lab;
import java.util.*;

/** Raw reset resources, not a controller. No crafted output is ever furnished. */
public final class TaskFixtures {
    private TaskFixtures(){}
    public static final String[] MATERIALS={"OAK_LOG","OAK_PLANKS","STICK","CRAFTING_TABLE","WOODEN_PICKAXE","COBBLESTONE","STONE_PICKAXE","COAL","FURNACE","RAW_IRON","IRON_INGOT","DIRT","CHEST"};
    public static final String[] NAMES={"Forward and stop","Turn and stop","Aim and hold","Navigate and stop","One-block step","Break a log","Collect a log","Place a block","Craft planks","Craft sticks","Craft a workbench","Craft wooden pickaxe","Mine cobblestone","Craft stone pickaxe","Smelt iron","Supply a chest","Build a platform","Log to workbench"};
    public record Supply(String material,int amount){}
    public record Ingredient(int slot,String material){}
    public static List<Supply> supplies(int stage){return switch(stage){
        case 7->List.of(new Supply("OAK_PLANKS",4));case 8->List.of(new Supply("OAK_LOG",1));case 9->List.of(new Supply("OAK_PLANKS",2));case 10->List.of(new Supply("OAK_PLANKS",4));
        case 11->List.of(new Supply("OAK_PLANKS",3),new Supply("STICK",2));case 12->List.of(new Supply("WOODEN_PICKAXE",1));
        case 13->List.of(new Supply("COBBLESTONE",3),new Supply("STICK",2));case 14->List.of(new Supply("RAW_IRON",1),new Supply("COAL",1));
        case 15->List.of(new Supply("OAK_LOG",4));case 16->List.of(new Supply("OAK_PLANKS",12));
        case 0,1,2,3,4,5,6,17->List.of();default->throw new IllegalArgumentException("task id");};}
    public static String craftOutput(int stage){return switch(stage){case 8->"OAK_PLANKS";case 9->"STICK";case 10,17->"CRAFTING_TABLE";case 11->"WOODEN_PICKAXE";case 13->"STONE_PICKAXE";default->null;};}
    public static List<Ingredient> recipe(int stage){return switch(stage){
        case 8->List.of(new Ingredient(0,"OAK_LOG"));case 9->List.of(new Ingredient(0,"OAK_PLANKS"),new Ingredient(2,"OAK_PLANKS"));
        case 10->List.of(new Ingredient(0,"OAK_PLANKS"),new Ingredient(1,"OAK_PLANKS"),new Ingredient(2,"OAK_PLANKS"),new Ingredient(3,"OAK_PLANKS"));
        case 11,13->{String m=stage==11?"OAK_PLANKS":"COBBLESTONE";yield List.of(new Ingredient(0,m),new Ingredient(1,m),new Ingredient(2,m),new Ingredient(4,"STICK"),new Ingredient(7,"STICK"));}
        default->List.of();};}
    public static int assistance(Protocol.Request r){
        if(r.full()||r.difficulty()>=0.85)return 0;
        return Math.min(recipe(r.stage()).size(),Math.max(0,(int)Math.floor((0.85-r.difficulty())/0.6*recipe(r.stage()).size()+1e-8)));
    }
    public static boolean mayOpenAtReset(Protocol.Request r){return !r.full()&&r.difficulty()<0.85;}
    public static int itemIndex(String material){for(int i=0;i<MATERIALS.length;i++)if(MATERIALS[i].equals(material))return i;return -1;}
}
