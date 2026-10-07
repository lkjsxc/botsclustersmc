package org.botsclustersmc.commons;

import java.util.Locale;
import org.botsclustersmc.core.RandomSource;

/** Prospective paired cases. Conditions and resource owners are not policy inputs. */
public record CommonsCase(int index, Condition condition, long seed) {
    public enum Condition { SPLIT_SHARED, POOLED_SHARED, SPLIT_ISOLATED;
        public String label() { return name().toLowerCase(Locale.ROOT).replace('_','-'); }
    }
    public CommonsCase {
        if(index<0||index>=32||condition==null)throw new IllegalArgumentException("commons case");
    }
    public static CommonsCase of(int index,Condition condition,long bankSeed) {
        return new CommonsCase(index,condition,Math.addExact(bankSeed,Math.multiplyExact(index,104729L)));
    }
    public int rooms() { return condition==Condition.SPLIT_ISOLATED?2:1; }
    public int owner() { return index%2; }
    public boolean mirror() { return (index/2)%2!=0; }
    public int planks(int member) { checkMember(member);return member==owner()?3:0; }
    public int sticks(int member) {
        checkMember(member);
        return member==(condition==Condition.POOLED_SHARED?owner():1-owner())?2:0;
    }
    public long actorSeed(int member) { checkMember(member);return seed^(member==0?0x632be59bd9b4e019L:0x9e3779b97f4a7c15L); }
    public float yaw(int member) { return (float)(new RandomSource(actorSeed(member)^0x34a9L).unit()*360-180); }
    public static void checkMember(int member) { if(member<0||member>1)throw new IllegalArgumentException("member must be 0 or 1"); }
}
