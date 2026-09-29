package org.botsclustersmc.diagnostic;

import org.botsclustersmc.core.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Offline counterfactuals only. Never part of a training or inference artifact. */
public final class PolicyBlocks {
    private PolicyBlocks() {}
    public enum Block { GOAL, TRUNK, ACTOR, CRITIC }
    public record Difference(int parameters, int changed, double l2, double maximum) {}
    private static final int GOAL_START = 16, GOAL_END = 34;

    private static void layout() {
        if (!Schema.ID.equals("bcmc-click-conditioned-slots") || Schema.INPUTS != 512
                || Schema.HIDDEN != 96 || Schema.LOGITS != 233 || Task.values().length != 18)
            throw new IllegalArgumentException("Unsupported parameter/observation layout");
    }

    /** Every current parameter belongs to exactly one block. */
    public static Block block(int parameter) {
        layout();
        if (parameter < 0 || parameter >= Policy.PARAMETERS)
            throw new IllegalArgumentException("Parameter outside policy");
        return classify(parameter);
    }

    private static Block classify(int parameter) {
        if (parameter < Policy.B1) {
            int input = (parameter - Policy.W1) % Schema.INPUTS;
            return input >= GOAL_START && input < GOAL_END ? Block.GOAL : Block.TRUNK;
        }
        if (parameter < Policy.W3) return Block.TRUNK;
        if (parameter < Policy.B3)
            return (parameter - Policy.W3) / Schema.HIDDEN == Schema.LOGITS ? Block.CRITIC : Block.ACTOR;
        return parameter - Policy.B3 == Schema.LOGITS ? Block.CRITIC : Block.ACTOR;
    }

    /** A set bit takes that complete block from donor; unset bits retain base.
     * Counters are inherited from base and do not describe newly trained weights.
     */
    public static Policy compose(Policy base, Policy donor, int donorMask) {
        Objects.requireNonNull(base); Objects.requireNonNull(donor); layout();
        if(base.learningTask()!=-1 || donor.learningTask()!=-1)throw new IllegalArgumentException("Block composition requires unfocused policies");
        if (donorMask < 0 || donorMask >= 16) throw new IllegalArgumentException("Use a four-bit donor mask");
        float[] weights = base.copyWeights(), other = donor.copyWeights();
        for (int i = 0; i < weights.length; i++)
            if ((donorMask & (1 << classify(i).ordinal())) != 0) weights[i] = other[i];
        return new Policy(weights, base.updates(), base.samples());
    }

    public static Difference[] differences(Policy base, Policy donor) {
        layout();
        float[] a = base.copyWeights(), b = donor.copyWeights();
        int[] count = new int[4], changed = new int[4];
        double[] squares = new double[4], maximum = new double[4];
        for (int i = 0; i < a.length; i++) {
            int group = classify(i).ordinal(); count[group]++;
            if (Float.floatToRawIntBits(a[i]) != Float.floatToRawIntBits(b[i])) changed[group]++;
            double delta = (double)b[i] - a[i];
            squares[group] += delta * delta;
            maximum[group] = Math.max(maximum[group], Math.abs(delta));
        }
        Difference[] result = new Difference[4];
        for (int i = 0; i < result.length; i++)
            result[i] = new Difference(count[i], changed[i], Math.sqrt(squares[i]), maximum[i]);
        return result;
    }

    /** Reserves a fresh output once; manifests are written only after all 16 policies. */
    public static void run(Path basePath, Path donorPath, Path destination) throws IOException {
        Path first = PolicyFile.managedPath(basePath), second = PolicyFile.managedPath(donorPath);
        Path output = PolicyFile.managedPath(destination);
        for (Path input : List.of(first, second))
            if (output.startsWith(input.getParent()) || input.startsWith(output))
                throw new IOException("Keep output separate from both source policy directories");
        byte[] baseBytes = PolicyFile.readBounded(first, Schema.MAX_MODEL_BYTES);
        byte[] donorBytes = PolicyFile.readBounded(second, Schema.MAX_MODEL_BYTES);
        Policy base = PolicyFile.decode(baseBytes), donor = PolicyFile.decode(donorBytes);
        Difference[] stats = differences(base, donor);
        Files.createDirectory(output);
        writeNew(output.resolve("base-policy.bcmc"), baseBytes);
        writeNew(output.resolve("donor-policy.bcmc"), donorBytes);
        StringJoiner policies = new StringJoiner(",\n");
        for (int mask = 0; mask < 16; mask++) {
            byte[] bytes = PolicyFile.encode(compose(base, donor, mask));
            String name = "mask-" + mask + ".bcmc";
            writeNew(output.resolve(name), bytes);
            policies.add("    {\"donor_mask\": " + mask + ", \"file\": \"" + name
                    + "\", \"sha256\": \"" + GoalTransfer.digest(bytes) + "\"}");
        }
        StringJoiner groups = new StringJoiner(",\n");
        for (Block group : Block.values()) {
            Difference d = stats[group.ordinal()];
            groups.add("    {\"name\": \"" + group.name().toLowerCase(Locale.ROOT) + "\", \"bit\": "
                    + (1 << group.ordinal()) + ", \"parameters\": " + d.parameters()
                    + ", \"changed_parameters\": " + d.changed() + ", \"l2\": " + d.l2()
                    + ", \"maximum_absolute_difference\": " + d.maximum() + "}");
        }
        String manifest = String.format(Locale.ROOT, """
            {
              "kind": "parameter-block-counterfactuals",
              "diagnostic_only": true,
              "new_training_samples": 0,
              "schema": "%s",
              "base_policy_updates": %d,
              "base_trained_samples": %d,
              "donor_policy_updates": %d,
              "donor_trained_samples": %d,
              "base_policy_sha256": "%s",
              "donor_policy_sha256": "%s",
              "counter_scope": "Every mixture inherits base counters; none is a newly trained checkpoint.",
              "mask_scope": "Set bit selects donor; unset bit selects base. No interpolation or action override.",
              "claim": "Parameter sensitivity diagnostic, not proof of gradient cause, retained competence, or a deployment candidate.",
              "blocks": [
            %s
              ],
              "policies": [
            %s
              ]
            }
            """, Schema.ID, base.updates(), base.samples(), donor.updates(), donor.samples(),
                GoalTransfer.digest(baseBytes), GoalTransfer.digest(donorBytes), groups, policies);
        writeNew(output.resolve("counterfactuals.json"), manifest.getBytes(StandardCharsets.UTF_8));
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
        if (args.length != 3)
            throw new IllegalArgumentException("Usage: PolicyBlocks BASE_POLICY DONOR_POLICY NEW_DIRECTORY");
        run(Path.of(args[0]), Path.of(args[1]), Path.of(args[2]));
        System.out.println("Created all 16 diagnostic counterfactuals. No training or skill certification.");
    }
}
