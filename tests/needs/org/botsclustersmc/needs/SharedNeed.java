package org.botsclustersmc.needs;

/** Source-only public-demand contract. Not an installed sensor, policy schema or material ledger. */
public final class SharedNeed {
    private SharedNeed() {}
    public static final String SIGNAL = "commons-demand-sidecar-v1";
    public static final int WIDTH = 13, MAX_STOCK = 4096, MAX_TICKS = 12000, MAX_MEMBERS = 512;

    /** Exact counts of oak planks, sticks and wooden pickaxes; no recipe previews. */
    public record Stock(int planks, int sticks, int picks) {
        public static final Stock EMPTY = new Stock(0, 0, 0);
        public Stock {
            if (planks < 0 || sticks < 0 || picks < 0 || planks > MAX_STOCK || sticks > MAX_STOCK || picks > MAX_STOCK)
                throw new IllegalArgumentException("bounded stock required");
        }
        public int at(int kind) {
            return switch (kind) { case 0 -> planks; case 1 -> sticks; case 2 -> picks;
                default -> throw new IllegalArgumentException("material index"); };
        }
    }
    /** Membership and demand are fixed within an episode, not silently renormalized after departures. */
    public record Contract(long episode, int horizon, int members, Stock target) {
        public Contract {
            if (episode < 0 || horizon < 1 || horizon > MAX_TICKS || members < 1 || members > MAX_MEMBERS
                    || target == null || target.equals(Stock.EMPTY))
                throw new IllegalArgumentException("shared-need contract");
        }
    }
    /** An owner-sampled boundary. Unknown stock contains no cached values and is NOT an empty bank. */
    public record Frame(Contract contract, int tick, boolean known, Stock bank) {
        public Frame {
            if (contract == null || tick < 0 || tick > contract.horizon() || bank == null
                    || !known && !bank.equals(Stock.EMPTY))
                throw new IllegalArgumentException("shared-need observation");
        }
        public double coverage() {
            if (!known) throw new IllegalStateException("unknown stock has no coverage score");
            int kinds = 0; double sum = 0;
            for (int k = 0; k < 3; k++) if (contract.target().at(k) > 0) {
                kinds++; sum += Math.min(bank.at(k), contract.target().at(k)) / (double) contract.target().at(k);
            }
            return sum / kinds;
        }
        /** Separate sidecar only. Do not splice into the current 512-float model under its old schema. */
        public float[] inputs() {
            float[] result = new float[WIDTH];
            result[0] = 1; result[1] = known ? 1 : 0;
            result[2] = (contract.horizon() - tick) / (float) contract.horizon();
            for (int k = 0; k < 3; k++) {
                int requested = contract.target().at(k);
                result[3 + k] = requested / (float) MAX_STOCK;
                if (known) {
                    result[6 + k] = bank.at(k) / (float) MAX_STOCK;
                    result[9 + k] = requested == 0 ? 0 : Math.max(0, requested - bank.at(k)) / (float) requested;
                }
            }
            if (known) result[12] = (float) coverage();
            return result;
        }
    }
}
