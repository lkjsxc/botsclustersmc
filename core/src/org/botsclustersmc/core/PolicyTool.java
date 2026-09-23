package org.botsclustersmc.core;

import java.nio.file.Path;

public final class PolicyTool {
    private PolicyTool(){}
    public static void main(String[] args)throws Exception{
        if(args.length!=2||!args[0].equals("verify"))throw new IllegalArgumentException("verify <policy.bcmc>");
        Policy p=PolicyFile.read(Path.of(args[1]));System.out.println("Valid current policy: schema="+Schema.ID+", updates="+p.updates()+", trained_samples="+p.samples());
    }
}
