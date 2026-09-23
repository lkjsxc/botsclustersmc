package org.botsclustersmc.training;

import org.botsclustersmc.core.Policy;
import org.botsclustersmc.core.PolicyFile;
import org.botsclustersmc.core.Schema;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

/** Offline export from the single authoritative checkpoint. Never shipped in inference. */
public final class CheckpointTool {
    private CheckpointTool() {}

    public static Policy exportPolicy(Path checkpoint,Path destination) throws IOException {
        Path source=PolicyFile.managedPath(checkpoint),target=PolicyFile.managedPath(destination);
        if(source.equals(target)) throw new IOException("Export must not replace the training checkpoint");
        Policy policy=TrainingState.read(source).policy();
        PolicyFile.write(target,policy);
        return policy;
    }

    public static void verifyExport(Path checkpoint,Path exported) throws IOException {
        byte[] expected=PolicyFile.encode(TrainingState.read(checkpoint).policy());
        byte[] actual=PolicyFile.readBounded(exported,Schema.MAX_MODEL_BYTES);
        if(!Arrays.equals(expected,actual)) throw new IOException("Export differs from the canonical training checkpoint");
    }

    public static void main(String[] args) throws Exception {
        if(args.length!=3) throw new IllegalArgumentException("export|verify-export <training.bcmc> <policy.bcmc>");
        Path checkpoint=Path.of(args[1]),exported=Path.of(args[2]);
        switch(args[0]) {
            case "export" -> {
                Policy policy=exportPolicy(checkpoint,exported);
                System.out.println("Exported canonical checkpoint: updates="+policy.updates()+", trained_samples="+policy.samples());
            }
            case "verify-export" -> { verifyExport(checkpoint,exported);System.out.println("PASS export exactly matches the canonical checkpoint"); }
            default -> throw new IllegalArgumentException("Unknown checkpoint operation: "+args[0]);
        }
    }
}
