package org.botsclustersmc.commonslearning;

import org.botsclustersmc.core.Schema;

/** Full Cartesian feature products, not equality labels, preferred slots or an action controller. */
public final class TensorInputs {
    public static final String ID="synthetic-communal-tensor-binding-memory-only";
    public static final int LOCAL=50,LOCAL_WIDTH=108,PUBLIC=206,PUBLIC_WIDTH=2,OUTPUT=220;
    private TensorInputs(){}
    public static float[] bind(float[] observation){
        Schema.checkObservation(observation);
        for(int i=OUTPUT;i<OUTPUT+LOCAL_WIDTH*PUBLIC_WIDTH;i++)
            if(observation[i]!=0)throw new IllegalArgumentException("synthetic product columns already occupied");
        float[] result=observation.clone();
        for(int i=0;i<LOCAL_WIDTH;i++)for(int j=0;j<PUBLIC_WIDTH;j++){
            float value=observation[LOCAL+i]*observation[PUBLIC+j];
            if(!Float.isFinite(value))throw new IllegalArgumentException("non-finite feature product");
            result[OUTPUT+i*PUBLIC_WIDTH+j]=value;
        }
        return result;
    }
}
