package org.botsclustersmc.lab;

/** Bounded local-file reset requests. No field specifies a gameplay action. */
public final class Protocol {
    private Protocol() {}
    public record Request(String run,String token,int id,int stage,double x,double y,double z,
                          float yaw,float pitch,double gx,double gy,double gz,long seed,double difficulty,boolean full) {
        public Request(String run,String token,int id,int stage,double x,double y,double z,float yaw,float pitch,double gx,double gy,double gz) {
            this(run,token,id,stage,x,y,z,yaw,pitch,gx,gy,gz,0,1,true);
        }
    }
    public static Request parse(String text,String run,int bots) {
        if(text.length()>1024 || bots<1 || bots>64) throw new IllegalArgumentException("request size/population");
        String[] f=text.trim().split("\\s+");
        if(f.length!=16 || !f[0].equals("BCMCLAB3") || !f[1].equals(run) || !f[2].matches("[a-zA-Z0-9-]{1,80}"))
            throw new IllegalArgumentException("request envelope");
        int id=Integer.parseInt(f[3]),stage=Integer.parseInt(f[4]);
        if(id<0||id>=bots||stage<0||stage>=18) throw new IllegalArgumentException("task range");
        double x=value(f[5]),y=value(f[6]),z=value(f[7]),yaw=value(f[8]),pitch=value(f[9]);
        double gx=value(f[10]),gy=value(f[11]),gz=value(f[12]),difficulty=value(f[14]);
        long seed=Long.parseUnsignedLong(f[13]);
        if(!f[15].equals("true")&&!f[15].equals("false")) throw new IllegalArgumentException("full difficulty flag");
        boolean full=Boolean.parseBoolean(f[15]);double ox=(id%8)*16,oz=(id/8)*16;
        if(!(x>ox+2&&x<ox+14&&z>oz+2&&z<oz+14&&y==97&&gx>ox+2&&gx<ox+14&&gz>oz+2&&gz<oz+14&&gy>=97&&gy<=102
                &&Math.abs(yaw)<=180&&Math.abs(pitch)<=89&&difficulty>=0.2&&difficulty<=1&&(!full||difficulty==1)))
            throw new IllegalArgumentException("cell/difficulty boundary");
        return new Request(run,f[2],id,stage,x,y,z,(float)yaw,(float)pitch,gx,gy,gz,seed,difficulty,full);
    }
    private static double value(String s){double d=Double.parseDouble(s);if(!Double.isFinite(d))throw new IllegalArgumentException("nonfinite coordinate");return d;}
}
