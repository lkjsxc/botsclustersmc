package org.botsclustersmc.core;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.zip.CRC32;

/** Strict data format, not Java object deserialization. CRC detects accidental damage. */
public final class PolicyFile {
    private PolicyFile() {}
    private static final String MAGIC="BCMC-POLICY";
    public static byte[] encode(Policy policy) throws IOException {
        ByteArrayOutputStream buffer=new ByteArrayOutputStream();
        try(DataOutputStream d=new DataOutputStream(buffer)) {
            d.writeUTF(MAGIC); d.writeUTF(Schema.ID); d.writeInt(Schema.INPUTS); d.writeInt(Schema.HIDDEN);
            d.writeInt(Schema.HEADS.length); for(int n:Schema.HEADS) d.writeInt(n);
            d.writeInt(Policy.EXPERTS);d.writeInt(Policy.NETWORK_PARAMETERS);
            d.writeLong(policy.updates()); d.writeLong(policy.samples());
            float[] weights=policy.copyWeights(); d.writeInt(weights.length); for(float w:weights) d.writeFloat(w);
        }
        return checked(buffer.toByteArray());
    }
    public static Policy decode(byte[] bytes) throws IOException {
        byte[] payload=verify(bytes,Schema.MAX_MODEL_BYTES);
        try(DataInputStream d=new DataInputStream(new ByteArrayInputStream(payload))) {
            if(!MAGIC.equals(d.readUTF()) || !Schema.ID.equals(d.readUTF())) throw new IOException("incompatible policy schema; retrain with this source");
            if(d.readInt()!=Schema.INPUTS || d.readInt()!=Schema.HIDDEN || d.readInt()!=Schema.HEADS.length) throw new IOException("policy dimensions");
            for(int n:Schema.HEADS) if(d.readInt()!=n) throw new IOException("action meaning/dimensions");
            if(d.readInt()!=Policy.EXPERTS||d.readInt()!=Policy.NETWORK_PARAMETERS)throw new IOException("expert dimensions");
            long updates=d.readLong(),samples=d.readLong();
            if(d.readInt()!=Policy.PARAMETERS) throw new IOException("parameter length");
            float[] weights=new float[Policy.PARAMETERS]; for(int i=0;i<weights.length;i++) weights[i]=d.readFloat();
            if(d.available()!=0) throw new IOException("trailing policy data");
            try{return new Policy(weights,updates,samples);}catch(IllegalArgumentException e){throw new IOException(e.getMessage(),e);}
        }
    }
    public static Policy read(Path path) throws IOException { return decode(readBounded(path,Schema.MAX_MODEL_BYTES)); }
    public static void write(Path path,Policy policy) throws IOException { atomicWrite(path,encode(policy)); }
    public static byte[] readBounded(Path path,int maximum) throws IOException {
        if(maximum<1 || maximum==Integer.MAX_VALUE) throw new IllegalArgumentException("invalid data bound");
        path=managedPath(path);
        if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)) throw new IOException("not a regular owned data file: "+path);
        try(InputStream in=Files.newInputStream(path,LinkOption.NOFOLLOW_LINKS)) {
            byte[] data=in.readNBytes(maximum+1);
            if(data.length>maximum) throw new IOException("data file exceeds bound"); return data;
        }
    }
    public static byte[] checked(byte[] payload) throws IOException {
        CRC32 crc=new CRC32(); crc.update(payload);
        ByteArrayOutputStream b=new ByteArrayOutputStream(payload.length+8); b.write(payload);
        try(DataOutputStream d=new DataOutputStream(b)){d.writeLong(crc.getValue());} return b.toByteArray();
    }
    public static byte[] verify(byte[] bytes,int maximum) throws IOException {
        if(bytes.length<8 || bytes.length>maximum) throw new IOException("data size");
        int n=bytes.length-8; CRC32 crc=new CRC32(); crc.update(bytes,0,n);
        if(crc.getValue()!=ByteBuffer.wrap(bytes,n,8).getLong()) throw new IOException("data checksum mismatch");
        return java.util.Arrays.copyOf(bytes,n);
    }
    /** Check the whole lexical path, including nonexistent descendants, before any I/O. */
    public static Path managedPath(Path destination) throws IOException {
        Path path=destination.toAbsolutePath().normalize();
        for(Path p=path;p!=null;p=p.getParent()) if(Files.isSymbolicLink(p)) throw new IOException("symlinked managed path: "+p);
        return path;
    }
    public static void atomicWrite(Path destination,byte[] bytes) throws IOException {
        Path path=managedPath(destination),parent=path.getParent();
        Files.createDirectories(parent);
        managedPath(path);
        Path temporary=Files.createTempFile(parent,".write-",".tmp");
        try {
            try(FileChannel ch=FileChannel.open(temporary,StandardOpenOption.WRITE)) {
                ByteBuffer b=ByteBuffer.wrap(bytes); while(b.hasRemaining()) ch.write(b); ch.force(true);
            }
            // No non-atomic replacement fallback: a filesystem without atomic moves must fail.
            Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            try(FileChannel directory=FileChannel.open(parent,StandardOpenOption.READ)){directory.force(true);}
            catch(AccessDeniedException | UnsupportedOperationException ignored){/* Directory fsync is not available on every OS. */}
        } finally { Files.deleteIfExists(temporary); }
    }
}
