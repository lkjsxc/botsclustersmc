package org.botsclustersmc.lab;
import java.util.*;

/** Immutable snapshots from Rust. Parsing never reads or mutates a game object. */
public final class ObserverState {
    private ObserverState(){}
    public record Row(int id,int task,String state,long attempts,double success,double difficulty,int trials,int successes){}
    public record Snapshot(long updated,int frontier,String phase,long policy,long samples,int buffered,int sealed,List<Row> rows){
        public Snapshot{rows=List.copyOf(rows);}
        public boolean fresh(long now){return updated<=now+2&&updated>=now-15;}
    }
    public static int pages(int bots){if(bots<1||bots>64)throw new IllegalArgumentException("population");return (bots+44)/45;}
    public static Snapshot parse(String text,String run,int bots){
        if(text.length()>16384||bots<1||bots>64)throw new IllegalArgumentException("observer payload bound");
        String[] lines=text.strip().split("\\R");if(lines.length!=bots+1)throw new IllegalArgumentException("observer row count");
        String[] h=lines[0].split(" ");
        if(h.length!=10||!h[0].equals("BCMCPROGRESS2")||!h[1].equals(run)||Integer.parseInt(h[9])!=bots)throw new IllegalArgumentException("observer run envelope");
        long updated=number(h[2]),policy=number(h[5]),samples=number(h[6]);int frontier=range(h[3],0,17),buffered=range(h[7],0,6400000),sealed=range(h[8],0,bots);
        if(!Set.of("training","updating","exam").contains(h[4]))throw new IllegalArgumentException("observer phase");
        List<Row> rows=new ArrayList<>(bots);
        for(int id=0;id<bots;id++){
            String[] f=lines[id+1].split(" ");if(f.length!=8||range(f[0],0,bots-1)!=id||!Set.of("practice","probe","review","exam","waiting").contains(f[2]))throw new IllegalArgumentException("observer row");
            int trials=range(f[6],0,16),successes=range(f[7],0,trials);
            rows.add(new Row(id,range(f[1],0,17),f[2],number(f[3]),fraction(f[4],0,1),fraction(f[5],0.2,1),trials,successes));
        }
        return new Snapshot(updated,frontier,h[4],policy,samples,buffered,sealed,rows);
    }
    private static long number(String text){long n=Long.parseLong(text);if(n<0)throw new IllegalArgumentException("negative observer count");return n;}
    private static int range(String text,int lo,int hi){long n=number(text);if(n<lo||n>hi)throw new IllegalArgumentException("observer range");return (int)n;}
    private static double fraction(String text,double lo,double hi){double n=Double.parseDouble(text);if(!Double.isFinite(n)||n<lo||n>hi)throw new IllegalArgumentException("observer fraction");return n;}
}
