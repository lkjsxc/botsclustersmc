package org.botsclustersmc.training;

import org.botsclustersmc.core.*;

/** Reset-only raw stock and a menu-correct training potential. Never chooses gameplay. */
public final class InitialCrafting {
    private InitialCrafting() {}
    // Entries are source storage slot, physical grid x, physical grid y.
    private static int[][] layout(int task) { return switch(task) {
        case 8 -> new int[][]{{0,0,0}};
        case 9 -> new int[][]{{0,0,0},{0,0,1}};
        case 10 -> new int[][]{{0,0,0},{0,1,0},{0,0,1},{0,1,1}};
        case 11,13 -> new int[][]{{0,0,0},{0,1,0},{0,2,0},{1,1,1},{1,1,2}};
        default -> new int[0][];
    }; }
    private static int width(Pocket pocket) { return switch(pocket.menu()) {
        case INVENTORY -> 2;case WORKBENCH -> 3;default -> 0;
    }; }
    private static int span(int[][] cells,int axis) {
        int size=0;for(int[] cell:cells)size=Math.max(size,cell[axis]+1);return size;
    }
    private static boolean fits(int[][] cells,int width) {
        return cells.length>0&&span(cells,1)<=width&&span(cells,2)<=width;
    }
    private static int slot(int width,int x,int y) { return 36+y*width+x; }
    private static boolean matches(String item,int task,int source) {
        if(source==1)return item.equals("STICK");
        return task==13?item.equals("COBBLESTONE"):
            org.botsclustersmc.core.Stack.kind(item)==(task==8?2:3);
    }
    public static void prepare(Pocket pocket,int task,double difficulty,RandomSource rng) {
        if(!Double.isFinite(difficulty)||difficulty<0||difficulty>1)throw new IllegalArgumentException("difficulty");
        int[][] cells=layout(task);int width=width(pocket);
        if(difficulty>=1||!fits(cells,width))return;
        int missing=(int)Math.ceil(difficulty*cells.length);
        for(int i=0;i<cells.length;i++) {
            int[] cell=cells[i];
            boolean furnish=task==11||task==13?i>=missing:rng.unit()>difficulty;
            if(furnish&&!pocket.storage(cell[0]).empty()) {
                pocket.click(1,cell[0],Pocket.NONE);
                pocket.click(2,slot(width,cell[1],cell[2]),Pocket.NONE);
                pocket.click(1,cell[0],Pocket.NONE);
            }
        }
        // No output item is supplied; all cursor/grid units come from raw reset stock.
        if(rng.unit()>difficulty)for(int source=0;source<2;source++)if(!pocket.storage(source).empty()) {
            pocket.click(1,source,Pocket.NONE);break;
        }
    }
    public static double progress(Pocket pocket,int task) {
        int[][] cells=layout(task);int width=width(pocket);
        if(!fits(cells,width))return 0;
        double best=0;
        // Respect the same translations as the actual crafting matcher.
        for(int oy=0;oy<=width-span(cells,2);oy++)for(int ox=0;ox<=width-span(cells,1);ox++) {
            int matches=0,wrong=0;
            for(int y=0;y<width;y++)for(int x=0;x<width;x++) {
                var item=pocket.get(slot(width,x,y),Pocket.NONE);int source=-1;
                for(int[] cell:cells)if(cell[1]+ox==x&&cell[2]+oy==y){source=cell[0];break;}
                if(source>=0&&matches(item.item(),task,source))matches++;
                else if(!item.empty())wrong++;
            }
            best=Math.max(best,(matches-wrong)/(double)cells.length);
        }
        return best;
    }
}
