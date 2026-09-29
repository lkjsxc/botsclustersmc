package org.botsclustersmc.diagnostic;

import org.botsclustersmc.core.Policy;
import org.botsclustersmc.core.PolicyFile;
import org.botsclustersmc.core.Schema;
import org.botsclustersmc.core.Task;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Offline initialization experiment, never included in either runtime JAR. */
public final class GoalTransfer {
    private GoalTransfer() {}
    public static final int GOAL_OFFSET = 16;

    /** Copy one first-layer goal column; retain every other weight and source counter. */
    public static Policy transfer(Policy source, int donor, int recipient) {
        validate(donor, recipient);
        if(source.learningTask()!=-1)throw new IllegalArgumentException("Goal transfer requires an unfocused policy");
        float[] weights = source.copyWeights();
        for (int row = 0; row < Schema.HIDDEN; row++) {
            int start = Policy.W1 + row * Schema.INPUTS + GOAL_OFFSET;
            weights[start + recipient] = weights[start + donor];
        }
        return new Policy(weights, source.updates(), source.samples());
    }

    private static void validate(int donor, int recipient) {
        Task.at(donor); Task.at(recipient);
        if (donor == recipient) throw new IllegalArgumentException("Donor and recipient must differ");
        // This transform depends on the current sensor layout, not just tensor dimensions.
        if (!Schema.ID.equals("bcmc-click-conditioned-slots") || Task.values().length != 18
                || Schema.INPUTS != 512 || GOAL_OFFSET + Task.values().length != 34)
            throw new IllegalArgumentException("Unsupported goal observation layout");
    }

    public static String digest(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    /** A fresh directory is reserved once. Failed/partial attempts are retained, never overwritten. */
    public static void run(Path input, Path destination, int donor, int recipient) throws IOException {
        validate(donor, recipient);
        Path source = PolicyFile.managedPath(input), output = PolicyFile.managedPath(destination);
        if (output.startsWith(source.getParent()) || source.startsWith(output))
            throw new IOException("Keep experiment output separate from the source policy directory");
        byte[] original = PolicyFile.readBounded(source, Schema.MAX_MODEL_BYTES);
        Policy parent = PolicyFile.decode(original), candidate = transfer(parent, donor, recipient);
        byte[] transformed = PolicyFile.encode(candidate);
        float[] before = parent.copyWeights(), after = candidate.copyWeights();
        int changed = 0;
        for (int i = 0; i < before.length; i++)
            if (Float.floatToRawIntBits(before[i]) != Float.floatToRawIntBits(after[i])) changed++;
        String manifest = """
            {
              "kind": "goal-column-transfer",
              "experimental_initialization": true,
              "schema": "%s",
              "donor_task": %d,
              "recipient_task": %d,
              "eligible_parameters": %d,
              "changed_parameters": %d,
              "source_policy_updates": %d,
              "source_trained_samples": %d,
              "new_training_samples": 0,
              "counter_scope": "inherited source counters; no additional neural training",
              "source_policy_sha256": "%s",
              "candidate_policy_sha256": "%s",
              "claim": "Unmeasured initialization, not a skill certificate. No action mask, observation encoder, reset, reward, optimizer or course change."
            }
            """.formatted(Schema.ID, donor, recipient, Schema.HIDDEN, changed, parent.updates(), parent.samples(),
                digest(original), digest(transformed));
        Files.createDirectory(output);
        writeNew(output.resolve("source-policy.bcmc"), original);
        writeNew(output.resolve("policy.bcmc"), transformed);
        // Written last: only a successful complete transform has a manifest.
        writeNew(output.resolve("transfer.json"), manifest.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeNew(Path path, byte[] data) throws IOException {
        PolicyFile.managedPath(path);
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
            ByteBuffer bytes = ByteBuffer.wrap(data);
            while (bytes.hasRemaining()) channel.write(bytes);
            channel.force(true);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4)
            throw new IllegalArgumentException("Usage: GoalTransfer.java SOURCE_POLICY NEW_DIRECTORY DONOR_TASK RECIPIENT_TASK");
        run(Path.of(args[0]), Path.of(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
        System.out.println("Created experimental goal-column initialization. No neural training or skill certification performed.");
    }
}
