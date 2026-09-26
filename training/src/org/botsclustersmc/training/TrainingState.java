package org.botsclustersmc.training;

import org.botsclustersmc.core.*;
import java.io.*;
import java.nio.file.*;

/** A current-schema training transaction; deployable policy never contains Adam/course. */
public record TrainingState(Policy policy,Adam optimizer,byte[] course) {
    private static final String MAGIC="BCMC-TRAINING";
    public TrainingState {
        if(policy.updates()!=optimizer.step() || course.length>16_000_000)throw new IllegalArgumentException("training identity");
        course=course.clone();
    }
    public byte[] encode() throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)) {
            out.writeUTF(MAGIC);out.writeUTF(Schema.ID);
            byte[] p=PolicyFile.encode(policy);out.writeInt(p.length);out.write(p);out.writeLong(optimizer.step());
            out.writeInt(Policy.EXPERTS);for(long clock:optimizer.expertSteps())out.writeLong(clock);
            for(float v:optimizer.first())out.writeFloat(v);for(float v:optimizer.second())out.writeFloat(v);
            out.writeInt(course.length);out.write(course);
        }
        return PolicyFile.checked(bytes.toByteArray());
    }
    public static TrainingState decode(byte[] bytes) throws IOException {
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(PolicyFile.verify(bytes,32*1024*1024)))) {
            if(!in.readUTF().equals(MAGIC)||!in.readUTF().equals(Schema.ID))throw new IOException("training schema differs; no migration");
            int n=in.readInt();if(n<0||n>Schema.MAX_MODEL_BYTES)throw new IOException("policy length");
            Policy policy=PolicyFile.decode(in.readNBytes(n));long step=in.readLong();
            if(in.readInt()!=Policy.EXPERTS)throw new IOException("optimizer expert count");
            long[] clocks=new long[Policy.EXPERTS];for(int i=0;i<clocks.length;i++)clocks[i]=in.readLong();
            float[] first=new float[Policy.PARAMETERS],second=new float[Policy.PARAMETERS];
            for(int i=0;i<first.length;i++)first[i]=in.readFloat();for(int i=0;i<second.length;i++)second[i]=in.readFloat();
            int size=in.readInt();if(size<0||size>16_000_000)throw new IOException("course length");
            byte[] course=in.readNBytes(size);if(course.length!=size||in.read()!=-1)throw new IOException("training trailing/truncated bytes");
            try{return new TrainingState(policy,new Adam(first,second,step,clocks),course);}catch(IllegalArgumentException e){throw new IOException("invalid training state",e);}
        }
    }
    public void write(Path file)throws IOException{PolicyFile.atomicWrite(file,encode());}
    public static TrainingState read(Path file)throws IOException{return decode(PolicyFile.readBounded(file,32*1024*1024));}
}
