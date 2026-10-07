package org.botsclustersmc.commons;

import java.nio.file.*;
import org.botsclustersmc.core.*;

/** An explicitly UNTRAINED policy for engineering smoke checks, not checkpoint recovery. */
public final class CommonsPolicy {
    public static void main(String[] args)throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("<fresh-seed> <new-policy-file>");
        Path target=Path.of(args[1]);if(Files.exists(target))throw new IllegalArgumentException("Policy output already exists");
        PolicyFile.write(target,Policy.initialize(Long.parseLong(args[0])));
        System.out.println("FRESH UNTRAINED POLICY; updates=0; trained_samples=0; seed="+args[0]);
    }
}
