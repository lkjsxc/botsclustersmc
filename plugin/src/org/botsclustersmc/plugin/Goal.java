package org.botsclustersmc.plugin;

import org.botsclustersmc.core.Task;

public record Goal(Task task,double x,double y,double z,long episode,double difficulty,int horizon) {
    public Goal {
        if(task==null||!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||Math.abs(x)>29_000_000||Math.abs(z)>29_000_000
            ||episode<0||!Double.isFinite(difficulty)||difficulty<0||difficulty>1||horizon<1||horizon>12000)throw new IllegalArgumentException("goal bounds");
    }
}
