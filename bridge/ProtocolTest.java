import org.botsclustersmc.lab.Protocol;
public final class ProtocolTest{
    private static void check(boolean v){if(!v)throw new AssertionError();}
    private static void bad(String s){try{Protocol.parse(s,"run",64);throw new AssertionError(s);}catch(IllegalArgumentException expected){}}
    public static void main(String[] args){
        String s="BCMCLAB3 run 1-63-4 63 17 120 97 120 0 0 120.5 98.5 123.5 18446744073709551615 1 true";
        var r=Protocol.parse(s,"run",64);check(r.id()==63&&r.stage()==17&&r.seed()==-1L&&r.full());
        for(int stage=0;stage<18;stage++)check(Protocol.parse(s.replace("63 17 120","63 "+stage+" 120"),"run",64).stage()==stage);
        bad(s.replace("BCMCLAB3","BCMCLAB2"));bad(s.replace(" run "," old "));bad(s.replace("63 17 120","64 17 120"));bad(s.replace("63 17 120","63 18 120"));
        bad(s.replace("120 97 120","NaN 97 120"));bad(s.replace("120 97 120","100 97 120"));bad(s.replace("1 true","0.5 true"));bad(s.replace("1 true","0.1 false"));bad(s.replace("1 true","1 maybe"));bad(s+" extra");bad(s.repeat(20));
        var easy=Protocol.parse(s.replace("1 true","0.25 false"),"run",64);check(!easy.full()&&easy.difficulty()==0.25);
        System.out.println("Protocol: all 18 tasks, actor 63, unsigned seeds and 11 rejection cases passed");
    }
}
