package org.botsclustersmc.core;

/** Explicit, persistable SplitMix64 state. No shared global random generator. */
public final class RandomSource {
    private long state;
    public RandomSource(long seed) { state=seed; }
    public long state() { return state; }
    public void restore(long value) { state=value; }
    public long nextLong() {
        long z=(state+=0x9e3779b97f4a7c15L);
        z=(z^(z>>>30))*0xbf58476d1ce4e5b9L;
        z=(z^(z>>>27))*0x94d049bb133111ebL;
        return z^(z>>>31);
    }
    public double unit() { return (nextLong()>>>11)*0x1.0p-53; }
    public int nextInt(int bound) {
        if(bound<=0) throw new IllegalArgumentException("bound");
        return (int)Math.floor(unit()*bound);
    }
    public float symmetric(double radius) { return (float)((2*unit()-1)*radius); }
}
