package org.botsclustersmc.tests;

import org.botsclustersmc.core.*;
import org.botsclustersmc.diagnostic.GoalTransfer;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Independent parameter-support and forward-function oracles, plus CLI data boundaries. */
public final class GoalTransferTest {
    private static int checks;
    private static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    private static void equal(float[] a, float[] b, String message) {
        check(a.length == b.length, message + " length");
        for (int i = 0; i < a.length; i++)
            check(Float.floatToRawIntBits(a[i]) == Float.floatToRawIntBits(b[i]), message + " at " + i);
    }
    private static void equal(double[] a, double[] b, String message) {
        check(a.length == b.length, message + " length");
        for (int i = 0; i < a.length; i++)
            check(Double.doubleToRawLongBits(a[i]) == Double.doubleToRawLongBits(b[i]), message + " at " + i);
    }
    @FunctionalInterface private interface Operation { void run() throws Exception; }
    private static void reject(Operation operation, String message) throws Exception {
        try { operation.run(); } catch (IOException | IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError(message);
    }

    private static void functions() throws Exception {
        Policy initialized = Policy.initialize(987163);
        Policy source = new Policy(initialized.copyWeights(), 1234, 987654);
        byte[] sourceBytes = PolicyFile.encode(source);
        int donor = 6, recipient = 12;
        Policy candidate = GoalTransfer.transfer(source, donor, recipient);
        float[] a = source.copyWeights(), b = candidate.copyWeights();
        int changed = 0;
        for (int i = 0; i < a.length; i++) {
            boolean target = i >= Policy.W1 && i < Policy.B1 && (i - Policy.W1) % Schema.INPUTS == 16 + recipient;
            int expected = target ? i - recipient + donor : i;
            check(Float.floatToRawIntBits(b[i]) == Float.floatToRawIntBits(a[expected]), "weight support " + i);
            if (Float.floatToRawIntBits(a[i]) != Float.floatToRawIntBits(b[i])) changed++;
        }
        check(changed == Schema.HIDDEN, "exactly one nontrivial goal column");
        check(candidate.updates() == source.updates() && candidate.samples() == source.samples(), "inherited counters");
        Random random = new Random(31097);
        float[][] observations = new float[32][Schema.INPUTS];
        for (int task = 0; task < Task.values().length; task++) for (int sample = 0; sample < 32; sample++) {
            float[] input = new float[Schema.INPUTS];
            for (int i = 0; i < input.length; i++) input[i] = random.nextFloat() * 2 - 1;
            Arrays.fill(input, 16, 34, 0); input[0] = 1; input[16 + task] = 1;
            float[] oracle = input.clone();
            if (task == recipient) { oracle[16 + recipient] = 0; oracle[16 + donor] = 1; }
            boolean[] mask = Task.at(task).mask(64, sample % 2 == 0);
            Policy.Workspace reference = new Policy.Workspace(), actual = new Policy.Workspace();
            // The oracle substitutes only the one-hot input. It RETAINS the recipient's mask.
            source.forward(oracle, mask, reference); candidate.forward(input, mask, actual);
            equal(reference.h1, actual.h1, "first layer"); equal(reference.h2, actual.h2, "second layer");
            equal(reference.logits, actual.logits, "logits/value"); equal(reference.probabilities, actual.probabilities, "distribution");
            if (task == recipient) observations[sample] = input;
        }
        Policy.BatchWorkspace batch = new Policy.BatchWorkspace(observations.length);
        candidate.forwardBatch(observations, observations.length, batch);
        for (int lane = 0; lane < observations.length; lane++) {
            Policy.Workspace scalar = new Policy.Workspace();
            candidate.forward(observations[lane], Schema.unrestrictedMask(), scalar);
            float[] logits = new float[Schema.OUTPUTS]; batch.lane(lane, logits);
            equal(scalar.logits, logits, "scalar/batch");
        }
        check(Arrays.equals(sourceBytes, PolicyFile.encode(source)), "source immutable");
        equal(candidate.copyWeights(), PolicyFile.decode(PolicyFile.encode(candidate)).copyWeights(), "strict roundtrip");
        reject(() -> GoalTransfer.transfer(source, -1, 12), "negative donor");
        reject(() -> GoalTransfer.transfer(source, 6, 18), "recipient out of range");
        reject(() -> GoalTransfer.transfer(source, 6, 6), "same task");
    }

    private static void files() throws Exception {
        Path root = Files.createTempDirectory("bcmc-goal-transfer-");
        try {
            Path sourceDir = Files.createDirectory(root.resolve("source")), input = sourceDir.resolve("policy.bcmc");
            byte[] original = PolicyFile.encode(Policy.initialize(717)); Files.write(input, original);
            Path output = root.resolve("candidate"); GoalTransfer.run(input, output, 6, 12);
            check(Arrays.equals(original, Files.readAllBytes(input)), "input remains unchanged");
            check(Arrays.equals(original, Files.readAllBytes(output.resolve("source-policy.bcmc"))), "exact parent bytes retained");
            byte[] candidate = Files.readAllBytes(output.resolve("policy.bcmc")); PolicyFile.decode(candidate);
            String manifest = Files.readString(output.resolve("transfer.json"));
            check(manifest.contains(GoalTransfer.digest(original)) && manifest.contains(GoalTransfer.digest(candidate)), "provenance identities");
            check(manifest.contains("\"new_training_samples\": 0") && manifest.contains("inherited source counters"), "no learning claim");
            check(manifest.contains("\"donor_task\": 6") && manifest.contains("\"recipient_task\": 12"), "manifest task identity");
            reject(() -> GoalTransfer.run(input, output, 6, 12), "never overwrite a previous experiment");
            check(Arrays.equals(candidate, Files.readAllBytes(output.resolve("policy.bcmc"))), "retry preserved candidate");
            reject(() -> GoalTransfer.run(input, sourceDir.resolve("nested"), 6, 12), "source directory excluded");
            check(!Files.exists(sourceDir.resolve("nested")), "no nested output");
            Path invalid = sourceDir.resolve("invalid.bcmc"); Files.write(invalid, new byte[32]);
            reject(() -> GoalTransfer.run(invalid, root.resolve("bad"), 6, 12), "malformed policy");
            check(!Files.exists(root.resolve("bad")), "decode before destination creation");
            reject(() -> GoalTransfer.run(input, root.resolve("invalid-task"), 18, 12), "invalid task before I/O");
            check(!Files.exists(root.resolve("invalid-task")), "no invalid-task output");
            Path symlink = root.resolve("source-link");
            try {
                Files.createSymbolicLink(symlink, sourceDir);
                reject(() -> GoalTransfer.run(symlink.resolve("policy.bcmc"), root.resolve("symlink-input"), 6, 12), "symlink parent");
                reject(() -> GoalTransfer.run(input, symlink.resolve("symlink-output"), 6, 12), "symlink output parent");
            } catch (UnsupportedOperationException | FileSystemException unavailable) {
                System.out.println("SKIP symlink creation unavailable: " + unavailable.getClass().getSimpleName());
            }
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
    public static void main(String[] args) throws Exception {
        functions(); files(); System.out.println("PASS goal transfer: " + checks + " checks");
    }
}
