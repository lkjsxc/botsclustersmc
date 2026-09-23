package org.botsclustersmc.lab;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Human-only presentation. It cannot change a lesson, an action, or a reward. */
public final class ObserverSupport {
    private final JavaPlugin plugin;
    private final World world;
    private final int bots,viewDistance,tourDefault;
    private final String run,prefix;
    private final Path telemetry;
    private final Map<UUID,View> views=new ConcurrentHashMap<>();
    private volatile Snapshot snapshot;
    private long nextRead;
    private static final class View {int watched,interval;boolean overview=true,hud=true;volatile boolean teleporting;long nextTour;int refresh;}
    private record BotInfo(int stage,double difficulty,double success,long attempts,int passed,int trials,boolean waiting,String name){}
    private record Snapshot(double time,long policy,long samples,int frontier,String phase,int sealed,Map<Integer,BotInfo> actors){}
    public ObserverSupport(JavaPlugin plugin,World world,int bots,String run,String prefix,Path directory){
        this.plugin=plugin;this.world=world;this.bots=bots;this.run=run;this.prefix=prefix;telemetry=directory.resolve("observer.txt");
        viewDistance=setting("OBSERVER_VIEW_DISTANCE",10,3,16);tourDefault=setting("OBSERVER_TOUR_SECONDS",15,5,120);
    }
    static int setting(String name,int fallback,int min,int max){int n=Integer.parseInt(System.getenv().getOrDefault(name,""+fallback));if(n<min||n>max)throw new IllegalArgumentException(name+" outside "+min+".."+max);return n;}
    public void join(Player p){
        p.setGameMode(GameMode.SPECTATOR);p.setInvulnerable(true);p.setViewDistance(viewDistance);p.setSendViewDistance(viewDistance);p.setSimulationDistance(3);
        View v=new View();views.put(p.getUniqueId(),v);overview(p,v);
        p.sendMessage(Component.text("RL Academy: /academy watch 0.."+(bots-1)+" | next | prev | overview | tour [5..120] | tour off | hud"));
        p.sendMessage(Component.text("Set your client render distance to at least "+viewDistance+" chunks. Server rendering does not override your video settings."));
        p.getScheduler().runAtFixedRate(plugin,task->{
            View current=views.get(p.getUniqueId());if(current!=v){task.cancel();return;}
            long now=System.nanoTime();if(v.interval>0&&now>=v.nextTour&&!v.teleporting){v.watched=(v.watched+1)%bots;watch(p,v,v.watched);v.nextTour=now+v.interval*1_000_000_000L;}
            Snapshot s=snapshot;boolean fresh=s!=null&&Math.abs(System.currentTimeMillis()/1000.0-s.time)<10;
            if(v.hud){
                String text;
                if(!fresh)text="RL Academy | waiting for current learner telemetry";
                else if(v.overview)text=String.format(Locale.ROOT,"%d learners | Policy %d | %,d samples | %s | cohort %d/%d",bots,s.policy,s.samples,s.phase,s.sealed,bots);
                else{BotInfo b=s.actors.get(v.watched);String outcome=b.attempts==0?"untried":String.format(Locale.ROOT,"practice EMA %.0f%% (%d trials)",b.success*100,b.attempts);
                    text=String.format(Locale.ROOT,"%s%02d | %s | difficulty %.0f%% | %s | exam %d/%d | P%d%s",prefix,v.watched,b.name,b.difficulty*100,outcome,b.passed,b.trials,s.policy,b.waiting?" | cohort wait":"");}
                p.sendActionBar(Component.text(text));
            }
            if(++v.refresh%5==0){
                p.sendPlayerListHeaderAndFooter(Component.text("botsclustersmc • "+bots+" RL learners\n"+(fresh?"Policy "+s.policy+" • "+s.phase:"Waiting for telemetry")),
                    Component.text("/academy next  /academy prev  /academy overview\n/academy tour "+tourDefault+"  /academy tour off"));
            }
        },()->views.remove(p.getUniqueId(),v),20,20);
    }
    public void quit(Player p){views.remove(p.getUniqueId());}
    private void teleport(Player p,View v,Location target){
        if(v.teleporting)return;v.teleporting=true;
        p.teleportAsync(target).whenComplete((ok,error)->{v.teleporting=false;if(error!=null)plugin.getLogger().warning("Observer teleport failed: "+error.getClass().getSimpleName());});
    }
    private void overview(Player p,View v){
        v.overview=true;v.interval=0;double rows=Math.ceil(bots/8.0);
        teleport(p,v,new Location(world,64,145,rows*8,0,90));
    }
    private void watch(Player p,View v,int id){
        if(id<0||id>=bots)throw new IllegalArgumentException("observer target");v.watched=id;v.overview=false;
        teleport(p,v,new Location(world,CampusPlan.ox(id)+8,109,CampusPlan.oz(id)+8,0,85));
    }
    public boolean command(Player p,String[] args){
        View v=views.get(p.getUniqueId());if(v==null){p.sendMessage(Component.text("Join as a human observer first."));return true;}
        try{
            String cmd=args.length==0?"help":args[0].toLowerCase(Locale.ROOT);
            switch(cmd){
                case "watch"->{if(args.length!=2)throw new IllegalArgumentException();String n=args[1];if(n.startsWith(prefix))n=n.substring(prefix.length());watch(p,v,Integer.parseInt(n));}
                case "next"->watch(p,v,(v.watched+1)%bots);
                case "prev"->watch(p,v,(v.watched+bots-1)%bots);
                case "overview"->overview(p,v);
                case "tour"->{if(args.length==2&&args[1].equalsIgnoreCase("off")){v.interval=0;p.sendMessage(Component.text("Tour stopped."));}
                    else{int seconds=args.length==1?tourDefault:Integer.parseInt(args[1]);if(args.length>2||seconds<5||seconds>120)throw new IllegalArgumentException();
                        v.interval=seconds;v.nextTour=System.nanoTime()+seconds*1_000_000_000L;watch(p,v,v.watched);p.sendMessage(Component.text("Tour: one learner every "+seconds+" seconds."));}}
                case "hud"->{v.hud=!v.hud;if(!v.hud)p.sendActionBar(Component.empty());p.sendMessage(Component.text("HUD "+(v.hud?"on":"off")));}
                default->p.sendMessage(Component.text("/academy watch 0.."+(bots-1)+" | next | prev | overview | tour [seconds|off] | hud"));
            }
        }catch(IllegalArgumentException e){p.sendMessage(Component.text("Use /academy watch 0.."+(bots-1)+" or /academy tour 5..120 (or off)."));}
        return true;
    }
    public List<String> complete(String[] args){
        List<String> candidates=new ArrayList<>();if(args.length==1)candidates.addAll(List.of("watch","next","prev","overview","tour","hud"));
        else if(args.length==2&&args[0].equals("watch"))for(int i=0;i<bots;i++)candidates.add(""+i);
        else if(args.length==2&&args[0].equals("tour"))candidates.addAll(List.of("10","15","30","off"));
        String start=args.length==0?"":args[args.length-1].toLowerCase(Locale.ROOT);return candidates.stream().filter(s->s.startsWith(start)).toList();
    }
    /** Called only by the existing IO thread. No disk access on player/region ticks. */
    public void poll(){
        if(System.nanoTime()<nextRead)return;nextRead=System.nanoTime()+500_000_000L;
        try{
            if(!Files.isRegularFile(telemetry)||Files.size(telemetry)>32768)return;
            String[] lines=Files.readString(telemetry).strip().split("\\R");if(lines.length!=bots+1)return;
            String[] h=lines[0].split(" ");if(h.length!=9||!h[0].equals("BCMCOBS1")||!h[1].equals(run)||Integer.parseInt(h[8])!=bots)return;
            double time=Double.parseDouble(h[2]);long policy=Long.parseLong(h[3]),samples=Long.parseLong(h[4]);int frontier=Integer.parseInt(h[5]),sealed=Integer.parseInt(h[7]);
            if(!Double.isFinite(time)||policy<0||samples<0||frontier<0||frontier>=18||sealed<0||sealed>bots||!(h[6].equals("training")||h[6].equals("frozen-evaluation")))return;
            Map<Integer,BotInfo> actors=new HashMap<>();
            for(int i=1;i<lines.length;i++){
                String[] f=lines[i].split(" ");if(f.length!=9)return;int id=Integer.parseInt(f[0]),stage=Integer.parseInt(f[1]);double difficulty=Double.parseDouble(f[2]),rate=Double.parseDouble(f[3]);
                long attempts=Long.parseLong(f[4]);int passed=Integer.parseInt(f[5]),trials=Integer.parseInt(f[6]);
                if(id<0||id>=bots||stage<0||stage>=18||!Double.isFinite(difficulty)||difficulty<0.2||difficulty>1||!Double.isFinite(rate)||rate<0||rate>1||attempts<0||passed<0||passed>trials||trials>16||
                    !(f[7].equals("true")||f[7].equals("false"))||!f[8].matches("[a-z][a-z0-9-]{1,48}"))return;
                if(actors.put(id,new BotInfo(stage,difficulty,rate,attempts,passed,trials,Boolean.parseBoolean(f[7]),f[8]))!=null)return;
            }
            snapshot=new Snapshot(time,policy,samples,frontier,h[6],sealed,Map.copyOf(actors));
        }catch(Exception ignored){/* Presentation failure never alters training. HUD expires after ten seconds. */}
    }
}
