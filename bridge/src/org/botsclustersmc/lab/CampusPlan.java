package org.botsclustersmc.lab;
import java.util.*;

/** Pure bounded geometry used by both tests and the actual Folia block writer. */
public final class CampusPlan {
    public static final int COLUMNS=8,STRIDE=16,FLOOR=96,TOP=104;
    private static final String[] COLORS={"LIGHT_BLUE_CONCRETE","LIME_CONCRETE","YELLOW_CONCRETE","PURPLE_CONCRETE","ORANGE_CONCRETE","PINK_CONCRETE","CYAN_CONCRETE","RED_CONCRETE"};
    public record Pos(int x,int y,int z){}
    public record Edit(Pos pos,String material){}
    private CampusPlan(){}
    public static int ox(int id){check(id);return id%COLUMNS*STRIDE;}
    public static int oz(int id){check(id);return id/COLUMNS*STRIDE;}
    private static void check(int id){if(id<0||id>=64)throw new IllegalArgumentException("cell id");}
    public static boolean interior(int id,Pos p){return p.x>=ox(id)+2&&p.x<=ox(id)+13&&p.z>=oz(id)+2&&p.z<=oz(id)+13&&p.y>FLOOR&&p.y<TOP;}
    public static String baseAt(int id,int x,int y,int z){
        int lx=x-ox(id),lz=z-oz(id);
        if(lx<0||lx>15||lz<0||lz>15||y<FLOOR||y>TOP)throw new IllegalArgumentException("outside owned chunk");
        if(y==FLOOR){if((lx==2||lx==13)&&(lz==2||lz==13))return "SEA_LANTERN";if(lx==0||lx==15||lz==0||lz==15)return COLORS[id/8];return "SMOOTH_STONE";}
        if(lx>=1&&lx<=14&&lz>=1&&lz<=14&&(y==TOP||lx==1||lx==14||lz==1||lz==14))return "GLASS";
        return "AIR";
    }
    public static List<Edit> base(int id){
        List<Edit> out=new ArrayList<>(16*16*9);
        for(int y=FLOOR;y<=TOP;y++)for(int x=ox(id);x<ox(id)+16;x++)for(int z=oz(id);z<oz(id)+16;z++)out.add(new Edit(new Pos(x,y,z),baseAt(id,x,y,z)));
        return out;
    }
    public static Pos goal(Protocol.Request r){return new Pos((int)Math.floor(r.gx()),(int)Math.floor(r.gy()),(int)Math.floor(r.gz()));}
    public static List<Pos> targets(Protocol.Request r){Pos p=goal(r);return r.stage()==16?List.of(p,new Pos(p.x+1,p.y,p.z),new Pos(p.x+2,p.y,p.z)):List.of(p);}
    public static Map<Pos,String> overlay(Protocol.Request r){
        Map<Pos,String> out=new LinkedHashMap<>();if(r==null)return out;
        if(r.stage()==4)for(int x=2;x<=13;x++)out.put(new Pos(ox(r.id())+x,97,oz(r.id())+8),"SMOOTH_STONE");
        Pos p=goal(r);
        String material=switch(r.stage()){case 5,6,17->"OAK_LOG";case 11,13->"CRAFTING_TABLE";case 12->"STONE";case 14->"FURNACE";case 15->"CHEST";default->null;};
        if(material!=null)out.put(p,material);
        else if(r.stage()==2)out.put(p,"GOLD_BLOCK");
        else for(Pos target:targets(r))out.put(new Pos(target.x,FLOOR,target.z),"GOLD_BLOCK");
        return out;
    }
    public static List<Edit> reset(Protocol.Request previous,Protocol.Request next){
        if(previous!=null&&previous.id()!=next.id())throw new IllegalArgumentException("cross-cell reset");
        Map<Pos,String> before=overlay(previous),after=overlay(next);
        Set<Pos> changed=new LinkedHashSet<>(before.keySet());changed.addAll(after.keySet());
        // Advanced exercises may place mistakes anywhere inside their room.
        // Restore every legal editable position, not only the goal cells.
        if(next.stage()>=6||(previous!=null&&previous.stage()>=6)){
            for(int y=97;y<TOP;y++)for(int x=2;x<=13;x++)for(int z=2;z<=13;z++)changed.add(new Pos(ox(next.id())+x,y,oz(next.id())+z));
        }
        List<Edit> out=new ArrayList<>();
        for(Pos p:changed)out.add(new Edit(p,after.getOrDefault(p,baseAt(next.id(),p.x,p.y,p.z))));
        if(out.size()>1060)throw new IllegalStateException("unbounded reset");return out;
    }
    public static String manifest(String run,int bots){
        if(!run.matches("[a-zA-Z0-9-]{1,80}")||bots<1||bots>64)throw new IllegalArgumentException("manifest envelope");
        StringBuilder out=new StringBuilder("BCMCCAMPUS1 "+run+" "+bots+" 8 16 96\n");
        for(int i=0;i<bots;i++)out.append(i).append(' ').append(ox(i)).append(' ').append(oz(i)).append('\n');return out.toString();
    }
}
