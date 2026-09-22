package org.botsclustersmc.lab;

/** Same identity contract as the pinned Rust client; checked before Java starts too. */
public final class EnvironmentConfig {
    private EnvironmentConfig() {}
    public static void validate(String run,String prefix,int bots) {
        if(run==null||!run.matches("[a-zA-Z0-9-]{1,80}"))
            throw new IllegalArgumentException("BCMC_RUN_ID must be 1..80 ASCII letters, digits or hyphens");
        if(prefix==null||!prefix.matches("[a-zA-Z0-9_]{1,13}"))
            throw new IllegalArgumentException("BOT_PREFIX must be 1..13 ASCII letters, digits or underscores; use bcmc, not botsclustersmc");
        if(bots<1||bots>32) throw new IllegalArgumentException("BOTS must be 1..32");
    }
}
