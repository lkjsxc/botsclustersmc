package org.botsclustersmc.lab;

import java.util.*;

/** Pure finite geometry; also used by the actual Folia block writer. */
public final class CampusPlan {
    public static final int COLUMNS=8, STRIDE=16, FLOOR=96, TOP=104;
    private static final String[] COLORS={"LIGHT_BLUE_CONCRETE","LIME_CONCRETE","YELLOW_CONCRETE","PURPLE_CONCRETE"};
    public record Pos(int x,int y,int z) {}
    public record Edit(Pos pos,String material) {}
    private CampusPlan() {}
    public static int ox(int id) { check(id); return id%COLUMNS*STRIDE; }
    public static int oz(int id) { check(id); return id/COLUMNS*STRIDE; }
    private static void check(int id) { if(id<0||id>=32) throw new IllegalArgumentException("cell id"); }
    public static String baseAt(int id,int x,int y,int z) {
        int lx=x-ox(id),lz=z-oz(id);
        if(lx<0||lx>15||lz<0||lz>15||y<FLOOR||y>TOP) throw new IllegalArgumentException("outside owned chunk");
        if(y==FLOOR) {
            if((lx==2||lx==13)&&(lz==2||lz==13)) return "SEA_LANTERN";
            if(lx==0||lx==15||lz==0||lz==15) return COLORS[id/8];
            return "SMOOTH_STONE";
        }
        if(lx>=1&&lx<=14&&lz>=1&&lz<=14) {
            if(y==TOP) return "LIGHT_BLUE_STAINED_GLASS";
            if(lx==1||lx==14||lz==1||lz==14) return "GLASS";
        }
        return "AIR";
    }
    public static List<Edit> base(int id) {
        List<Edit> out=new ArrayList<>(16*16*9);
        for(int y=FLOOR;y<=TOP;y++) for(int x=ox(id);x<ox(id)+16;x++) for(int z=oz(id);z<oz(id)+16;z++)
            out.add(new Edit(new Pos(x,y,z),baseAt(id,x,y,z)));
        return out;
    }
    public static Map<Pos,String> overlay(Protocol.Request r) {
        Map<Pos,String> out=new LinkedHashMap<>();
        if(r==null) return out;
        if(r.stage()==4) for(int x=2;x<=13;x++) out.put(new Pos(ox(r.id())+x,97,oz(r.id())+8),"SMOOTH_STONE");
        int y=(r.stage()==2||r.stage()==5)?(int)Math.floor(r.gy()):FLOOR;
        out.put(new Pos((int)Math.floor(r.gx()),y,(int)Math.floor(r.gz())),r.stage()==5?"OAK_LOG":"GOLD_BLOCK");
        return out;
    }
    public static List<Edit> reset(Protocol.Request previous,Protocol.Request next) {
        if(previous!=null&&previous.id()!=next.id()) throw new IllegalArgumentException("cross-cell reset");
        Map<Pos,String> before=overlay(previous),after=overlay(next);
        Set<Pos> changed=new LinkedHashSet<>(before.keySet()); changed.addAll(after.keySet());
        List<Edit> out=new ArrayList<>();
        for(Pos p:changed) out.add(new Edit(p,after.getOrDefault(p,baseAt(next.id(),p.x,p.y,p.z))));
        if(out.size()>26) throw new IllegalStateException("unbounded reset");
        return out;
    }
    public static String manifest(String run,int bots) {
        if(!run.matches("[a-zA-Z0-9-]{1,80}")||bots<1||bots>32) throw new IllegalArgumentException("manifest envelope");
        StringBuilder out=new StringBuilder("BCMCCAMPUS1 "+run+" "+bots+" 8 16 96\n");
        for(int i=0;i<bots;i++) out.append(i).append(' ').append(ox(i)).append(' ').append(oz(i)).append('\n');
        return out.toString();
    }
}
