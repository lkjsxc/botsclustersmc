package org.botsclustersmc.tests;

import org.botsclustersmc.core.*;
import org.botsclustersmc.diagnostic.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;

/** Independent tensor-index oracle; no production classifier in expected values. */
public final class PolicyBlocksTest {
    private static int checks;
    private static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    private static boolean same(float a, float b) { return Float.floatToRawIntBits(a) == Float.floatToRawIntBits(b); }
    private static void equal(float[] a, float[] b, String message) {
        check(a.length == b.length, message);
        for (int i = 0; i < a.length; i++) check(same(a[i], b[i]), message);
    }
    private static void equal(double[] a, double[] b, String message) {
        check(a.length == b.length, message);
        for (int i = 0; i < a.length; i++)
            check(Double.doubleToRawLongBits(a[i]) == Double.doubleToRawLongBits(b[i]), message);
    }
    @FunctionalInterface private interface Operation { void run() throws Exception; }
    private static void reject(Operation operation, String message) throws Exception {
        try { operation.run(); } catch (IOException | IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError(message);
    }

    private static int[] oracle() {
        int[] group = new int[81258]; Arrays.fill(group, -1);
        int at = 0;
        for (int row = 0; row < 96; row++) for (int col = 0; col < 512; col++)
            group[at++] = col >= 16 && col <= 33 ? 0 : 1;
        for (int i = 0; i < 96 + 96 * 96 + 96; i++) group[at++] = 1;
        for (int row = 0; row < 234; row++) for (int col = 0; col < 96; col++)
            group[at++] = row == 233 ? 3 : 2;
        for (int row = 0; row < 234; row++) group[at++] = row == 233 ? 3 : 2;
        check(at == group.length && at == Policy.PARAMETERS, "independent layout consumes all parameters");
        return group;
    }

    private static void parameters() throws Exception {
        int[] group = oracle(), expected = {1728, 56832, 22601, 97}, counts = new int[4];
        for (int i = 0; i < group.length; i++) {
            check(PolicyBlocks.block(i).ordinal() == group[i], "block index oracle"); counts[group[i]]++;
        }
        check(Arrays.equals(counts, expected), "four disjoint exhaustive parameter blocks");
        float[] a = Policy.initialize(19).copyWeights(), b = Policy.initialize(47).copyWeights();
        // Different signed zeros must survive exact selection, including biases.
        a[Policy.B1] = -0.0f; b[Policy.B1] = 0.0f;
        a[Policy.B3] = 0.0f; b[Policy.B3] = -0.0f;
        Policy base = new Policy(a, 123, 4567), donor = new Policy(b, 456, 12345);
        byte[] originalA = PolicyFile.encode(base), originalB = PolicyFile.encode(donor);
        Policy[] mixtures = new Policy[16];
        for (int mask = 0; mask < 16; mask++) {
            Policy mixed = mixtures[mask] = PolicyBlocks.compose(base, donor, mask);
            float[] actual = mixed.copyWeights();
            for (int i = 0; i < actual.length; i++)
                check(same(actual[i], (mask & (1 << group[i])) == 0 ? a[i] : b[i]), "exact mask support");
            check(mixed.updates() == 123 && mixed.samples() == 4567, "explicit base counter scope");
            check(Arrays.equals(PolicyFile.encode(mixed), PolicyFile.encode(PolicyFile.decode(PolicyFile.encode(mixed)))), "encoded round trip");
            equal(actual, PolicyBlocks.compose(base, donor, mask).copyWeights(), "deterministic composition");
        }
        check(Arrays.equals(originalA, PolicyFile.encode(base)) && Arrays.equals(originalB, PolicyFile.encode(donor)), "immutable inputs");
        equal(a, mixtures[0].copyWeights(), "base endpoint"); equal(b, mixtures[15].copyWeights(), "donor endpoint weights");
        for (int task = 0; task < 18; task++) {
            float[][] inputs = new float[4][512]; Random random = new Random(1000 + task);
            for (float[] x : inputs) {
                for (int i = 0; i < x.length; i++) x[i] = random.nextFloat() * 2 - 1;
                Arrays.fill(x, 16, 34, 0); x[16 + task] = 1;
            }
            for (int mask = 0; mask < 16; mask++) {
                Policy mixed = mixtures[mask]; Policy.BatchWorkspace batch = new Policy.BatchWorkspace(4);
                mixed.forwardBatch(inputs, 4, batch);
                for (int lane = 0; lane < 4; lane++) {
                    boolean[] allowed = Task.at(task).mask(64, lane % 2 == 0);
                    Policy.Workspace w = new Policy.Workspace(); mixed.forward(inputs[lane], allowed, w);
                    float[] logits = new float[234]; batch.lane(lane, logits); equal(logits, w.logits, "scalar/batch");
                    if (mask < 8) {
                        Policy.Workspace critic = new Policy.Workspace(); mixtures[mask + 8].forward(inputs[lane], allowed, critic);
                        equal(w.h1, critic.h1, "critic cannot affect first layer"); equal(w.h2, critic.h2, "critic cannot affect second layer");
                        for (int output = 0; output < 233; output++) check(same(w.logits[output], critic.logits[output]), "critic cannot affect actor logits");
                        equal(w.probabilities, critic.probabilities, "critic cannot affect action probabilities");
                        check(!same(w.logits[233], critic.logits[233]), "critic control genuinely changes value");
                    }
                }
            }
        }
        reject(() -> PolicyBlocks.compose(base, donor, -1), "negative mask");
        reject(() -> PolicyBlocks.compose(base, donor, 16), "excess mask");
        reject(() -> PolicyBlocks.block(-1), "negative parameter");
        reject(() -> PolicyBlocks.block(Policy.PARAMETERS), "excess parameter");
        float[] zero = new float[Policy.PARAMETERS], two = new float[Policy.PARAMETERS]; Arrays.fill(two, 2);
        var differences = PolicyBlocks.differences(new Policy(zero, 0, 0), new Policy(two, 0, 0));
        for (int i = 0; i < 4; i++) {
            var d = differences[i]; check(d.parameters() == expected[i] && d.changed() == expected[i], "difference counts");
            check(d.maximum() == 2 && d.l2() == Math.sqrt(4.0 * expected[i]), "independent difference magnitude");
        }
        two = zero.clone(); two[0] = -0.0f;
        var signed = PolicyBlocks.differences(new Policy(zero, 0, 0), new Policy(two, 0, 0));
        check(signed[1].changed() == 1 && signed[1].l2() == 0 && signed[1].maximum() == 0, "signed zero count versus distance");
        zero[0] = -Float.MAX_VALUE; two[0] = Float.MAX_VALUE;
        var extreme = PolicyBlocks.differences(new Policy(zero, 0, 0), new Policy(two, 0, 0));
        check(Double.isFinite(extreme[1].l2()) && extreme[1].maximum() == 2.0 * Float.MAX_VALUE, "finite full float range statistics");
    }

    private static void files() throws Exception {
        Path root = Files.createTempDirectory("bcmc-policy-blocks-");
        try {
            Path first = Files.createDirectory(root.resolve("base")), second = Files.createDirectory(root.resolve("donor"));
            Path a = first.resolve("policy.bcmc"), b = second.resolve("policy.bcmc"), out = root.resolve("output");
            byte[] aa = PolicyFile.encode(new Policy(Policy.initialize(123).copyWeights(), 17, 999));
            byte[] bb = PolicyFile.encode(new Policy(Policy.initialize(456).copyWeights(), 27, 1999));
            Files.write(a, aa); Files.write(b, bb); PolicyBlocks.run(a, b, out);
            check(Arrays.equals(aa, Files.readAllBytes(a)) && Arrays.equals(bb, Files.readAllBytes(b)), "source files unchanged");
            check(Arrays.equals(aa, Files.readAllBytes(out.resolve("base-policy.bcmc"))), "exact base retained");
            check(Arrays.equals(bb, Files.readAllBytes(out.resolve("donor-policy.bcmc"))), "exact donor retained");
            String manifest = Files.readString(out.resolve("counterfactuals.json"));
            check(manifest.contains(GoalTransfer.digest(aa)) && manifest.contains(GoalTransfer.digest(bb)), "both source identities");
            check(manifest.contains("\"diagnostic_only\": true") && manifest.contains("\"new_training_samples\": 0") && manifest.contains("inherits base counters"), "diagnostic counter scope");
            for (int mask = 0; mask < 16; mask++) {
                byte[] actual = Files.readAllBytes(out.resolve("mask-" + mask + ".bcmc"));
                check(Arrays.equals(actual, PolicyFile.encode(PolicyBlocks.compose(PolicyFile.decode(aa), PolicyFile.decode(bb), mask))), "file composition");
                check(manifest.contains(GoalTransfer.digest(actual)) && manifest.contains("\"donor_mask\": " + mask + ","), "all sixteen manifest identities");
            }
            try (var children = Files.list(out)) { check(children.count() == 19, "exact complete output inventory"); }
            reject(() -> PolicyBlocks.run(a, b, out), "existing output");
            check(manifest.equals(Files.readString(out.resolve("counterfactuals.json"))), "retry preserves manifest");
            reject(() -> PolicyBlocks.run(a, b, first.resolve("nested")), "base descendant");
            reject(() -> PolicyBlocks.run(a, b, second.resolve("nested")), "donor descendant");
            reject(() -> PolicyBlocks.run(a, b, root), "source ancestor");
            Path malformed = first.resolve("malformed"); Files.write(malformed, Arrays.copyOf(aa, aa.length - 1));
            reject(() -> PolicyBlocks.run(malformed, b, root.resolve("bad-base")), "bad base");
            reject(() -> PolicyBlocks.run(a, malformed, root.resolve("bad-donor")), "bad donor");
            check(!Files.exists(root.resolve("bad-base")) && !Files.exists(root.resolve("bad-donor")), "validate both inputs before output");
            Files.write(malformed, Arrays.copyOf(bb, bb.length + 1));
            reject(() -> PolicyBlocks.run(a, malformed, root.resolve("trailing")), "trailing input");
            Locale originalLocale = Locale.getDefault(Locale.Category.FORMAT);
            try {
                Locale.setDefault(Locale.Category.FORMAT, Locale.forLanguageTag("ar-EG"));
                Path localized = root.resolve("localized"); PolicyBlocks.run(a, b, localized);
                String text = Files.readString(localized.resolve("counterfactuals.json"));
                check(text.contains("\"base_policy_updates\": 17"), "locale-independent base updates");
                check(text.contains("\"base_trained_samples\": 999"), "locale-independent base samples");
                check(text.contains("\"donor_policy_updates\": 27"), "locale-independent donor updates");
                check(text.contains("\"donor_trained_samples\": 1999"), "locale-independent donor samples");
            } finally { Locale.setDefault(Locale.Category.FORMAT, originalLocale); }
            Path link = root.resolve("link");
            try {
                Files.createSymbolicLink(link, second);
                reject(() -> PolicyBlocks.run(a, link.resolve("policy.bcmc"), root.resolve("linked-donor")), "linked donor");
                reject(() -> PolicyBlocks.run(link.resolve("policy.bcmc"), b, root.resolve("linked-base")), "linked base");
                reject(() -> PolicyBlocks.run(a, b, link.resolve("out")), "linked output");
            } catch (UnsupportedOperationException | FileSystemException unavailable) {
                System.out.println("SKIP policy-block symlink fixture: " + unavailable.getClass().getSimpleName());
            }
        } finally {
            try (var paths = Files.walk(root)) { for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(p); }
        }
        for (String name : List.of("dist/training.jar", "dist/botsclustersmc.jar")) try (JarFile jar = new JarFile(name)) {
            check(jar.stream().noneMatch(e -> e.getName().contains("PolicyBlocks") || e.getName().contains("GoalTransfer")), "offline tools absent from " + name);
        }
    }

    public static void main(String[] args) throws Exception {
        parameters(); files(); System.out.println("PASS policy block counterfactuals: " + checks + " checks");
    }
}
