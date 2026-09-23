"""One-shot integration, not part of normal startup."""
from pathlib import Path
import re

def change(s,a,b):
 assert a in s,a
 return s.replace(a,b)
p=Path('bridge/src/org/botsclustersmc/lab/BotsClustersMCLab.java');s=p.read_text()
a=s.index('    private static final class Session {');b=s.index('    @Override public void onEnable()',a)
s=s[:a]+s[b:]
s=re.sub(r'\bSession\b','TaskEnvironment.Session',s)
s=change(s,'    private World world;','    private World world;\n    private TaskEnvironment tasks;\n    private ObserverSupport observers;')
s=s.replace('getOrDefault("BOTS","32")','getOrDefault("BOTS","64")')
s=change(s,'            getServer().getPluginManager().registerEvents(this,this);','''            tasks=new TaskEnvironment(this,world,p->sessions.get(id(p)));
            observers=new ObserverSupport(this,world,bots,run,prefix,directory);
            getServer().getPluginManager().registerEvents(tasks,this);
            getServer().getPluginManager().registerEvents(this,this);''')
a=s.index('        if(id<0) {\n            p.setGameMode');b=s.index('        // This is environment placement',a)
s=s[:a]+'''        if(id<0){observers.join(p);return;}
        int view=ObserverSupport.setting("BOT_VIEW_DISTANCE",3,2,8);
        p.setViewDistance(view);p.setSendViewDistance(view);p.setSimulationDistance(3);
        p.setInvulnerable(true);p.setGameMode(GameMode.ADVENTURE);
'''+s[b:]
a=s.index('                Location l=p.getLocation();long tick=');b=s.index('            } catch(Throwable t) {fail(id,t);}',a)
s=s[:a]+'''                outgoing.put("frame-"+id+".txt",tasks.frame(p,s,run));
'''+s[b:]
a=s.index('    private void watch(');b=s.index('    @EventHandler public void quit',a)
s=s[:a]+'''    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(!(sender instanceof Player p)||id(p)>=0){sender.sendMessage("Use this command as a human observer.");return true;}
        return observers.command(p,args);
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String label,String[] args){
        return observers==null?List.of():observers.complete(args);
    }
'''+s[b:]
s=change(s,'Player p=e.getPlayer();int id=id(p);if(id<0) return;','Player p=e.getPlayer();int id=id(p);if(id<0){if(observers!=null)observers.quit(p);return;}')
s=change(s,'            if(campusReady&&!failed.get()) for(int i=0;i<bots;i++) {','            if(observers!=null)observers.poll();\n            if(campusReady&&!failed.get()) for(int i=0;i<bots;i++) {')
a=s.index('    private void reset(Protocol.Request r) {');b=s.index('    private boolean stopping()',a)
s=s[:a]+'''    private void reset(Protocol.Request r) {
        Player p=players.get(name(r.id()));if(p==null)return;
        TaskEnvironment.Session s=new TaskEnvironment.Session(r);sessions.put(r.id(),s);
        int cx=CampusPlan.ox(r.id())>>4,cz=CampusPlan.oz(r.id())>>4;
        // Close old GUI state on its owning entity before touching containers.
        p.getScheduler().run(this,begin->{
            try{
                if(sessions.get(r.id())!=s)return;
                p.closeInventory();p.setItemOnCursor(null);p.getInventory().clear();
                List<CampusPlan.Edit> edits=CampusPlan.reset(overlays.get(r.id()),r);int[] cursor={0};
                getServer().getRegionScheduler().runAtFixedRate(this,world,cx,cz,task->{
                    try{
                        if(sessions.get(r.id())!=s||failed.get()||stopping()){task.cancel();return;}
                        if(cursor[0]==0)tasks.clearRegion(s);
                        int end=Math.min(cursor[0]+256,edits.size());while(cursor[0]<end)putAndVerify(edits.get(cursor[0]++));
                        if(cursor[0]!=edits.size())return;
                        task.cancel();overlays.put(r.id(),r);
                        p.getScheduler().run(this,ready->{
                            try{
                                if(sessions.get(r.id())!=s)return;
                                p.closeInventory();p.setItemOnCursor(null);p.getInventory().clear();p.getInventory().setHeldItemSlot(0);
                                p.setGameMode(r.stage()>=5?GameMode.SURVIVAL:GameMode.ADVENTURE);
                                p.setInvulnerable(true);p.setAllowFlight(false);p.setFoodLevel(20);p.setSaturation(20);p.setHealth(20);p.setFireTicks(0);
                                tasks.furnish(p,s);
                                p.teleportAsync(new Location(world,r.x(),r.y(),r.z(),r.yaw(),r.pitch())).thenAccept(ok->{
                                    if(!ok){if(!stopping()&&sessions.get(r.id())==s&&players.get(name(r.id()))==p)fail(r.id(),new IllegalStateException("reset teleport refused"));return;}
                                    p.getScheduler().runDelayed(this,ignored->{
                                        try{if(sessions.get(r.id())==s){p.setVelocity(new Vector(0,0,0));tasks.start(p,s);s.ready=true;}}
                                        catch(Throwable error){fail(r.id(),error);}
                                    },()->retired(p,s),8);
                                }).exceptionally(error->{if(!stopping())fail(r.id(),error);return null;});
                            }catch(Throwable error){fail(r.id(),error);}
                        },()->retired(p,s));
                    }catch(Throwable error){task.cancel();fail(r.id(),error);}
                },1,1);
            }catch(Throwable error){fail(r.id(),error);}
        },()->retired(p,s));
    }
'''+s[b:]
a=s.index('    private boolean target(');b=s.index('    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void mobs',a)
s=s[:a]+s[b:];p.write_text(s)
# Correct tests for the wire version and explicitly test all 18 stage IDs.
p=Path('bridge/ProtocolTest.java')
if p.exists():
 s=p.read_text();s=re.sub(r'(BCMCLAB3 [^"\n]+)(")',lambda m:m[1]+' 1.0 17'+m[2],s)
 s=s.replace(' 6 ',' 18 ');p.write_text(s)
# Most build-copy lists were originally shallow. Include the canonical next/ tree.
p=Path('scripts/build.sh');s=p.read_text();s=s.replace('cp "$ROOT"/learning/src/*.rs "$EXAMPLE/core/"','cp -a "$ROOT/learning/src/." "$EXAMPLE/core/"');p.write_text(s)
# Source-generation gate protects the old v1 Academy rather than replacing it.
p=Path('.github/workflows/source-runtime.yml');p.write_text(p.read_text().replace('botsclustersmc-academy-v1','botsclustersmc-academy-v2').replace('bots=32','bots=64'))
print('Connected real task fixtures, reset transactions, telemetry and human-only observer presentation.')
