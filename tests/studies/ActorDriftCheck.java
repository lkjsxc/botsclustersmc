import org.botsclustersmc.core.*;
import java.nio.file.*;
import java.util.*;

/** Independent range-copy oracle on actual archived models; no learning or gameplay. */
public final class ActorDriftCheck {
    private static long checks;
    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("Counterfactual identity check " + checks);
    }
    private static Policy load(Path path) throws Exception {
        return PolicyFile.decode(PolicyFile.readBounded(PolicyFile.managedPath(path), Schema.MAX_MODEL_BYTES));
    }
    private static void copy(float[] source, float[] target, int start, int end) {
        System.arraycopy(source, start, target, start, end - start);
    }
    private static void equal(float a, float b) {
        check(Float.floatToRawIntBits(a) == Float.floatToRawIntBits(b));
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("MIXTURE_DIRECTORY NEW_REPORT");
        check(Schema.INPUTS == 512 && Schema.HIDDEN == 96 && Schema.LOGITS == 105);
        Path directory = Path.of(args[0]);
        Policy base = load(directory.resolve("base-policy.bcmc"));
        Policy donor = load(directory.resolve("donor-policy.bcmc"));
        float[] a = base.copyWeights(), b = donor.copyWeights();
        Policy[] mixtures = new Policy[16];
        for (int mask = 0; mask < 16; mask++) {
            Policy mixed = mixtures[mask] = load(directory.resolve("mask-" + mask + ".bcmc"));
            float[] expected = a.clone(), actual = mixed.copyWeights();
            for (int row = 0; row < 96; row++) {
                int start = row * 512;
                if ((mask & 1) != 0) copy(b, expected, start + 16, start + 34);
                if ((mask & 2) != 0) {
                    copy(b, expected, start, start + 16);
                    copy(b, expected, start + 34, start + 512);
                }
            }
            if ((mask & 2) != 0) copy(b, expected, Policy.B1, Policy.W3);
            if ((mask & 4) != 0) {
                copy(b, expected, Policy.W3, Policy.W3 + 105 * 96);
                copy(b, expected, Policy.B3, Policy.B3 + 105);
            }
            if ((mask & 8) != 0) {
                copy(b, expected, Policy.W3 + 105 * 96, Policy.B3);
                expected[Policy.B3 + 105] = b[Policy.B3 + 105];
            }
            for (int i = 0; i < expected.length; i++) equal(expected[i], actual[i]);
            check(mixed.updates() == base.updates() && mixed.samples() == base.samples());
        }
        check(Arrays.equals(PolicyFile.encode(base), PolicyFile.encode(mixtures[0])));
        Random random = new Random(2026100200L);
        float[][] inputs = new float[256][512];
        for (float[] input : inputs) for (int i = 0; i < input.length; i++) input[i] = random.nextFloat() * 2 - 1;
        Policy.BatchWorkspace left = new Policy.BatchWorkspace(256), right = new Policy.BatchWorkspace(256);
        float[] x = new float[Schema.OUTPUTS], y = new float[Schema.OUTPUTS];
        donor.forwardBatch(inputs, 256, left);
        for (int mask : new int[]{7, 15}) {
            mixtures[mask].forwardBatch(inputs, 256, right);
            for (int row = 0; row < 256; row++) {
                left.lane(row, x); right.lane(row, y);
                for (int i = 0; i < (mask == 7 ? Schema.LOGITS : Schema.OUTPUTS); i++) equal(x[i], y[i]);
            }
        }
        check(Arrays.equals(a, base.copyWeights()) && Arrays.equals(b, donor.copyWeights()));
        String result = "{\"checks\":" + checks + ",\"masks\":16,\"synthetic_observations\":256,"
            + "\"parent_updates\":" + base.updates() + ",\"parent_samples\":" + base.samples()
            + ",\"candidate_updates\":" + donor.updates() + ",\"candidate_samples\":" + donor.samples()
            + ",\"new_training_samples\":0,\"actor_logits_identical\":true,\"deployment\":false}\n";
        Files.writeString(PolicyFile.managedPath(Path.of(args[1])), result,
            StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        System.out.print(result);
    }
}
