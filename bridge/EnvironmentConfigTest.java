import org.botsclustersmc.lab.EnvironmentConfig;
public class EnvironmentConfigTest {
    private static void rejects(String run,String prefix,int bots,String field) {
        try { EnvironmentConfig.validate(run,prefix,bots); }
        catch(IllegalArgumentException e) {
            if(!e.getMessage().contains(field)) throw new AssertionError(e);
            return;
        }
        throw new AssertionError("accepted invalid "+field);
    }
    public static void main(String[] args) {
        EnvironmentConfig.validate("1790030000-42","bcmc",32);
        EnvironmentConfig.validate("test","abcdefghijklm",1);
        rejects("run","botsclustersmc",32,"BOT_PREFIX");
        rejects("run","bad-name",32,"BOT_PREFIX");
        rejects("run","",32,"BOT_PREFIX");
        rejects("bad run","bcmc",32,"BCMC_RUN_ID");
        rejects("run","bcmc",0,"BOTS");
        rejects("run","bcmc",33,"BOTS");
        System.out.println("PASS: default identities, boundaries and six configuration rejections");
    }
}
