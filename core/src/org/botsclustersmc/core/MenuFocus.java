package org.botsclustersmc.core;

/** A menu owns the input focus; clicks cannot also mine/place/drop in the world. */
public final class MenuFocus {
    private MenuFocus() {}
    public static boolean active(boolean menuOpen,int operation) {
        return menuOpen||operation==4;
    }
    public static void restrict(boolean[] mask) {
        for(int head=0;head<6;head++)Task.only(mask,head,Schema.IDLE[head]);
    }
}
