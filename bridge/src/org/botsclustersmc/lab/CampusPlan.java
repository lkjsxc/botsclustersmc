package org.botsclustersmc.lab;
import java.util.*;

/** Pure bounded geometry: one enclosed 16x16 cell per actor, eight rows of eight. */
public final class CampusPlan {
    public static final int COLUMNS=8,STRIDE=16,FLOOR=96,TOP=104;
    private static final String[] COLORS={"WHITE_CONCRETE","LIGHT_BLUE_CONCRETE","LIME_CONCRETE","YELLOW_CONCRETE","ORANGE_CONCRETE","PINK_CONCRETE","PURPLE_CONCRETE","CYAN_CONCRETE"};
    private CampusPlan(){}
    public record Pos(int x,int y,int z){}
    public record Edit(Pos pos,String material){}
    private static void check(int id){if(id<0||id>=64)throw new IllegalArgumentException("cell id");}
    public static int ox(int id){check(id);return id%8*16;}
    public static int oz(int id){check(id);return id/8*16;}
    public static String baseMaterial(int id,int x,int y,int z){
        check(id);int dx=x-ox(id),dz=z-oz(id);
        if(dx<0||dx>=16||dz<0||dz>=16||y<FLOOR||y>TOP)throw new IllegalArgumentException("position outside owned cell");
        if(y==FLOOR){if((dx==3||dx==12)&&(dz==3||dz==12))return "SEA_LANTERN";return dx<=1||dx>=14||dz<=1||dz>=14?COLORS[id/8]:"SMOOTH_STONE";}
        if(y==TOP||dx==1||dx==14||dz==1||dz==14)return "GLASS";
        return "AIR";
    }
    public static List<Edit> base(int id){
        var out=new ArrayList<Edit>(2304);
        for(int y=FLOOR;y<=TOP;y++)for(int z=oz(id);z<oz(id)+16;z++)for(int x=ox(id);x<ox(id)+16;x++)out.add(new Edit(new Pos(x,y,z),baseMaterial(id,x,y,z)));
        return out;
    }
    public static Map<Pos,String> overlay(Protocol.Request r){
        check(r.id());if(r.stage()<0||r.stage()>=18)throw new IllegalArgumentException("task id");
        var out=new LinkedHashMap<Pos,String>();int x=(int)Math.floor(r.gx()),y=(int)Math.floor(r.gy()),z=(int)Math.floor(r.gz());
        if(r.stage()==4)for(int dx=2;dx<=13;dx++)out.put(new Pos(ox(r.id())+dx,97,oz(r.id())+8),"SMOOTH_STONE");
        String material=switch(r.stage()){case 2->"GOLD_BLOCK";case 5,6,17->"OAK_LOG";case 12->"STONE";case 11,13->"CRAFTING_TABLE";case 14->"FURNACE";case 15->"CHEST";default->null;};
        if(material!=null)out.put(new Pos(x,y,z),material);
        if(r.stage()==7||r.stage()==16){int count=r.stage()==16?3:1;for(int i=0;i<count;i++)out.put(new Pos(x+i,FLOOR,z),"GOLD_BLOCK");}
        else if(r.stage()!=2&&r.stage()<5)out.put(new Pos(x,FLOOR,z),"GOLD_BLOCK");
        return out;
    }
    /** Full readback clears all player placements, not just the previous target. */
    public static List<Edit> reset(Protocol.Request previous,Protocol.Request next){
        if(previous!=null&&previous.id()!=next.id())throw new IllegalArgumentException("cross-cell reset");
        Map<Pos,String> overlay=overlay(next);var edits=new ArrayList<Edit>(2304);
        for(Edit b:base(next.id()))edits.add(new Edit(b.pos(),overlay.getOrDefault(b.pos(),b.material())));
        return edits;
    }
    public static boolean inside(int id,int x,int y,int z){return x>=ox(id)+2&&x<=ox(id)+13&&z>=oz(id)+2&&z<=oz(id)+13&&y>=97&&y<=103;}
    public static boolean placeable(int id,int x,int y,int z){return x>=ox(id)+3&&x<=ox(id)+12&&z>=oz(id)+3&&z<=oz(id)+12&&y>=97&&y<=100;}
    public static String manifest(String run,int bots){
        if(!run.matches("[a-zA-Z0-9-]{1,80}")||bots<1||bots>64)throw new IllegalArgumentException("manifest envelope");
        StringBuilder b=new StringBuilder("BCMCCAMPUS1 "+run+" "+bots+" 8 16 96\n");
        for(int id=0;id<bots;id++)b.append(id).append(' ').append(ox(id)).append(' ').append(oz(id)).append('\n');
        return b.toString();
    }
}
