package org.botsclustersmc.needs;

import java.util.*;
import org.botsclustersmc.needs.SharedNeed.*;

/** Single-owner, bounded reward history. Only immutable records leave this object.
 * Boundary t labels [t,t+1); this is a sampled-stock objective, not continuous-event reconstruction. */
public final class NeedWindow {
    private final Contract contract;
    private final double gamma;
    private final double[] utility;
    private final int[] stamps, cursor, claimed, discarded, sequences;
    private Frame current;
    private int knownTicks, unknownTicks;
    private double coverageTicks;
    private final double[] materialCoverage = new double[3];
    private final int[] covered = new int[3], shortage = new int[3], gap = new int[3], longestGap = new int[3];
    private int fullyCoveredTicks;

    public NeedWindow(Contract contract, int capacity, double discountPerTick) {
        this.contract = Objects.requireNonNull(contract);
        if (capacity < 1 || capacity > contract.horizon() || !Double.isFinite(discountPerTick)
                || discountPerTick < 0 || discountPerTick > 1)
            throw new IllegalArgumentException("reward window bounds");
        gamma = discountPerTick; utility = new double[capacity]; stamps = new int[capacity];
        Arrays.fill(stamps, -1);
        cursor = new int[contract.members()]; claimed = new int[cursor.length];
        discarded = new int[cursor.length]; sequences = new int[cursor.length];
    }
    public int latestTick() { return current == null ? 0 : current.tick(); }
    public int oldestTick() { return Math.max(0, latestTick() - utility.length); }
    public void append(Frame next) {
        Objects.requireNonNull(next);
        if (!next.contract().equals(contract) || next.tick() != (current == null ? 0 : current.tick() + 1))
            throw new IllegalArgumentException("noncontiguous need frame or changed contract");
        if (current != null) {
            int tick = current.tick(), slot = tick % utility.length;
            double value = current.known() ? current.coverage() : -1;
            utility[slot] = value; stamps[slot] = tick;
            if (value >= 0) {
                knownTicks++; coverageTicks += value; boolean all = true;
                for (int k = 0; k < 3; k++) if (contract.target().at(k) > 0) {
                    int need = contract.target().at(k), available = current.bank().at(k);
                    materialCoverage[k] += Math.min(available, need) / (double) need;
                    if (available >= need) { covered[k]++; gap[k] = 0; }
                    else { all = false; shortage[k]++; longestGap[k] = Math.max(longestGap[k], ++gap[k]); }
                }
                if (all) fullyCoveredTicks++;
            } else { unknownTicks++; Arrays.fill(gap, 0); }
        }
        current = next;
    }
    public record Claim(long episode, int member, int sequence, int fromTick, int toTick,
                        double teamReward, double memberReward, double bootstrapDiscount, boolean terminal) {}
    public record Discontinuity(long episode, int member, int sequence, int fromTick, int toTick) {}
    public record Progress(int member, int cursor, int claimedTicks, int discardedTicks, int sequence) {}
    public record MaterialAudit(int kind, int target, int coveredTicks, int shortageTicks,
                                int longestConfirmedShortage, double coverageTicks) {}
    public record Audit(int elapsedTicks, int knownTicks, int unknownTicks, double coverageTicks,
                        int fullyCoveredTicks, boolean outcomeComplete, OptionalDouble score,
                        List<MaterialAudit> materials, List<Progress> members) {
        public Audit { materials = List.copyOf(materials); members = List.copyOf(members); }
    }
    private int from(int member, int end) {
        if (member < 0 || member >= cursor.length) throw new IllegalArgumentException("member index");
        int start = cursor[member];
        if (current == null || end <= start || end > latestTick())
            throw new IllegalArgumentException("nonpositive or future reward interval");
        return start;
    }
    /** Each member consumes its own contiguous intervals exactly once.
     * teamReward is the common return; memberReward is its 1/N accounting share.
     * These are NOT interchangeable learner loss weights or a claim of gradient-scale invariance. */
    public Claim claim(int member, int end) {
        int start = from(member, end);
        if (start < oldestTick()) throw new IllegalStateException("reward history overrun; explicit discontinuity required");
        double reward = 0, factor = 1;
        for (int tick = start; tick < end; tick++) {
            int slot = tick % utility.length;
            if (stamps[slot] != tick || utility[slot] < 0)
                throw new IllegalStateException("unknown reward interval; explicit discontinuity required");
            reward += factor * utility[slot] / contract.horizon();
            factor *= gamma;
        }
        // Validation must complete before any member cursor or sequence changes.
        cursor[member] = end; claimed[member] += end - start;
        return new Claim(contract.episode(), member, sequences[member]++, start, end, reward, reward / contract.members(),
                end == contract.horizon() ? 0 : factor, end == contract.horizon());
    }
    /** Explicitly abandon samples. A caller must also end the actor's trajectory fragment; no zero reward is invented. */
    public Discontinuity discardThrough(int member, int end) {
        int start = from(member, end);
        cursor[member] = end; discarded[member] += end - start;
        return new Discontinuity(contract.episode(), member, sequences[member]++, start, end);
    }
    public Progress progress(int member) {
        if (member < 0 || member >= cursor.length) throw new IllegalArgumentException("member index");
        return new Progress(member, cursor[member], claimed[member], discarded[member], sequences[member]);
    }
    /** Team score is one clock, not a sum over NPC decisions. Unknown time makes final evidence incomplete. */
    public Audit audit() {
        boolean complete = current != null && current.tick() == contract.horizon() && unknownTicks == 0;
        List<Progress> members = new ArrayList<>();
        for (int member = 0; member < cursor.length; member++) members.add(progress(member));
        List<MaterialAudit> materials = new ArrayList<>();
        for (int k = 0; k < 3; k++) if (contract.target().at(k) > 0)
            materials.add(new MaterialAudit(k, contract.target().at(k), covered[k], shortage[k], longestGap[k], materialCoverage[k]));
        return new Audit(latestTick(), knownTicks, unknownTicks, coverageTicks, fullyCoveredTicks, complete,
                complete ? OptionalDouble.of(coverageTicks / contract.horizon()) : OptionalDouble.empty(), materials, members);
    }
}
