package org.botsclustersmc.training;

/** Dense islands limit chunk overhead; separated islands expose independent Folia regions. */
public record ArenaLayout(int actor,int chunkX,int chunkZ) {
    public int x(){return chunkX*16;}public int z(){return chunkZ*16;}
    public static int islandSize(int total,int regionThreads){
        if(total<1||regionThreads<1)throw new IllegalArgumentException("arena budget");
        int per=(int)Math.ceil(total/(double)Math.max(1,regionThreads*2));return per<=16?16:per<=64?64:256;
    }
    public static ArenaLayout forActor(int actor,int total){return forActor(actor,total,16);}
    public static ArenaLayout forActor(int actor,int total,int islandSize){
        if(actor<0||actor>=total||total<1||total>10000||(islandSize!=16&&islandSize!=64&&islandSize!=256))throw new IllegalArgumentException("arena identity");
        int island=actor/islandSize,local=actor%islandSize,side=(int)Math.sqrt(islandSize),width=(int)Math.ceil(Math.sqrt((total+islandSize-1)/(double)islandSize));
        return new ArenaLayout(actor,(island%width)*64+local%side,(island/width)*64+local/side);
    }
    public boolean contains(double x,double y,double z){return x>=x()+1&&x<=x()+15&&z>=z()+1&&z<=z()+15&&y>=64&&y<71;}
}
