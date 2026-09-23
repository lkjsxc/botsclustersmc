import org.botsclustersmc.lab.ObserverState;
public final class ObserverStateTest{
    private static void check(boolean v){if(!v)throw new AssertionError();}
    private static void bad(String s){try{ObserverState.parse(s,"run",64);throw new AssertionError();}catch(IllegalArgumentException expected){}}
    public static void main(String[] args){
        StringBuilder b=new StringBuilder("BCMCPROGRESS2 run 100 17 training 8 1000 300 63 64\n");for(int i=0;i<64;i++)b.append(i+" 17 practice 40 0.8 1.0 16 14\n");String s=b.toString();
        var snapshot=ObserverState.parse(s,"run",64);check(snapshot.rows().size()==64&&snapshot.rows().get(63).id()==63);check(snapshot.fresh(110)&&!snapshot.fresh(116)&&!snapshot.fresh(90));
        check(ObserverState.pages(64)==2&&ObserverState.pages(32)==1&&ObserverState.pages(45)==1&&ObserverState.pages(46)==2);
        bad(s.replace("run","old"));bad(s.replace(" 0.8 "," NaN "));bad(s.replace("16 14","16 17"));bad(s.replace("63 17 practice","62 17 practice"));bad(s+"0 0\n");bad(s.repeat(20));
        try{snapshot.rows().clear();throw new AssertionError();}catch(UnsupportedOperationException expected){}
        System.out.println("Observer: 64 immutable rows, two pages, stale and malformed state rejected");
    }
}
