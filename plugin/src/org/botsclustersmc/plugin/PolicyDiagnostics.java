package org.botsclustersmc.plugin;

import java.util.Locale;
import org.botsclustersmc.core.Policy;
import org.botsclustersmc.core.Schema;

/** Pure, read-only inspection of a cached observation. Never resamples the world. */
public final class PolicyDiagnostics {
    private PolicyDiagnostics() {}
    private static double saturation(float[] hidden) {
        int saturated=0;for(float value:hidden)if(Math.abs(value)>.99f)saturated++;
        return 100.0*saturated/hidden.length;
    }
    public static String describe(Policy live,Frame frame) {
        Policy.Workspace workspace=new Policy.Workspace();
        live.forward(frame.observation(),frame.mask(),workspace);
        float[] x=frame.observation();double[] p=workspace.probabilities;
        return String.format(Locale.ROOT,"Live policy %d on cached state (not the frozen exam policy): stop=%.3f forward=%.3f backward=%.3f; ego_forward=%.3f ego_right=%.3f still=%.2f; value=%.3f; hidden_saturation=%.0f%%/%.0f%%",live.updates(),p[0],p[1],p[2],x[335],x[334],x[343],workspace.logits[Schema.LOGITS],saturation(workspace.h1),saturation(workspace.h2));
    }
}
