package org.botsclustersmc.core;

import java.util.*;

/** Deliberately bounded, disclosed vanilla-shaped crafting catalogue, not a recipe-action macro. */
public final class Recipes {
    private Recipes(){}
    public record Recipe(Stack output,int[] consume){}
    public static Recipe match(Stack[] grid,int width) {
        if(grid.length!=9||(width!=2&&width!=3))throw new IllegalArgumentException("crafting grid");
        List<Integer> used=new ArrayList<>();int minX=9,minY=9,maxX=-1,maxY=-1;
        for(int y=0;y<width;y++)for(int x=0;x<width;x++){int i=y*3+x;if(!grid[i].empty()){used.add(i);minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);}}
        if(used.isEmpty())return null;
        int w=maxX-minX+1,h=maxY-minY+1;Stack result=null;
        if(used.size()==1){String s=grid[used.get(0)].item();if(s.startsWith("STRIPPED_"))s=s.substring(9);if(s.endsWith("_LOG"))result=new Stack(s.substring(0,s.length()-4)+"_PLANKS",4);
            else if(s.equals("CRIMSON_STEM")||s.equals("WARPED_STEM"))result=new Stack(s.replace("_STEM","_PLANKS"),4);}
        if(w==1&&h==2&&used.size()==2&&used.stream().allMatch(i->Stack.kind(grid[i].item())==3))result=new Stack("STICK",4);
        if(w==2&&h==2&&used.size()==4&&used.stream().allMatch(i->Stack.kind(grid[i].item())==3))result=new Stack("CRAFTING_TABLE",1);
        if(w==3&&h==3&&used.size()==5){
            boolean wood=true,stone=true;
            for(int x=0;x<3;x++){wood&=Stack.kind(grid[x].item())==3;stone&=grid[x].item().equals("COBBLESTONE");}
            if(grid[3].empty()&&grid[5].empty()&&grid[6].empty()&&grid[8].empty()&&grid[4].item().equals("STICK")&&grid[7].item().equals("STICK")){
                if(wood)result=new Stack("WOODEN_PICKAXE",1);else if(stone)result=new Stack("STONE_PICKAXE",1);
            }
        }
        return result==null?null:new Recipe(result,used.stream().mapToInt(Integer::intValue).toArray());
    }
}
