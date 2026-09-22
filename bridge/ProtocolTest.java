import org.botsclustersmc.lab.Protocol;
public class ProtocolTest {
    private static final String VALID="BCMCLAB2 test-run 0-0 0 0 8 97 5 0 0 8 97 9";
    private static void rejects(String text) {
        try { Protocol.parse(text,"test-run",8); throw new AssertionError("accepted "+text); }
        catch(IllegalArgumentException expected) { }
    }
    public static void main(String[] args) {
        var r=Protocol.parse(VALID,"test-run",8);
        if(r.id()!=0||r.stage()!=0||r.y()!=97) throw new AssertionError();
        rejects(VALID.replace("test-run","old-run"));
        rejects(VALID.replace("8 97 5","NaN 97 5"));
        rejects(VALID.replace("8 97 5","200 97 5"));
        rejects(VALID.replace("0-0 0 0","0-0 32 0"));
        rejects(VALID.replace("0-0 0 0","0-0 0 6"));
        rejects(VALID+" EXTRA");
        rejects("x".repeat(2048));
        System.out.println("PASS: 1 valid + 7 rejected protocol fixtures; not a Folia integration test");
    }
}
