package org.botsclustersmc.core;

import java.util.Arrays;

/** One current semantic contract, independent of server protocol and actor count. */
public final class Schema {
    private Schema() {}
    public static final String ID = "bcmc-citizen-egocentric-context";
    public static final int INPUTS = 512;
    public static final int HIDDEN = 96;
    public static final int[] HEADS = {9, 5, 5, 3, 4, 9, 6, 64};
    public static final int LOGITS = Arrays.stream(HEADS).sum();
    public static final int OUTPUTS = LOGITS + 1;
    public static final int[] IDLE = {0, 2, 2, 0, 0, 0, 0, 0};
    public static final int DECISION_TICKS = 4;
    public static final int MAX_MODEL_BYTES = 8 * 1024 * 1024;
    public static boolean slotActive(int operation) { return operation >= 1 && operation <= 3; }
    public static boolean[] unrestrictedMask() { boolean[] m=new boolean[LOGITS]; Arrays.fill(m,true); return m; }
    public static void checkObservation(float[] x) {
        if(x.length != INPUTS) throw new IllegalArgumentException("observation dimension");
        for(float f:x) if(!Float.isFinite(f)) throw new IllegalArgumentException("non-finite observation");
    }
    public static void checkAction(int[] a) {
        if(a.length != HEADS.length) throw new IllegalArgumentException("action dimension");
        for(int h=0;h<a.length;h++) if(a[h]<0 || a[h]>=HEADS[h]) throw new IllegalArgumentException("action range");
        if(!slotActive(a[6]) && a[7]!=0) throw new IllegalArgumentException("inactive slot must be zero");
    }
}
