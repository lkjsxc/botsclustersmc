"""Final reviewed test extension and removal of one-shot migration machinery.
Executed only in the disposable integration checkout, never by normal startup.
"""
from pathlib import Path
import shutil

p=Path('tests/live-client.rs');s=p.read_text()
s=s.replace('view12:bool,view16:bool}', 'view12:bool,view16:bool,live_hud:bool,live_tab:bool}')
s=s.replace('view12:false,view16:false}}', 'view12:false,view16:false,live_hud:false,live_tab:false}}')
needle='840=>{if(p.x-8.).abs()>1.'
assert needle in s
s=s.replace(needle,'''840=>{
                if env::var("BCMC_OBSERVER_REQUIRE_STATUS").as_deref()==Ok("true") && (!self.live_hud||!self.live_tab){return Err(format!("observer did not receive live learner HUD/TAB: hud={} tab={}",self.live_hud,self.live_tab));}
                if self.live_hud&&self.live_tab{eprintln!("PASS: observer received actual learner task/PPO action bar and trained-sample TAB packets");}
                if(p.x-8.).abs()>1.''')
old='Event::Packet(packet)=>{if s.observer{if let azalea::protocol::packets::game::ClientboundGamePacket::SetChunkCacheRadius(p)=packet.as_ref(){eprintln!("OBSERVER cache radius={}",p.radius);s.view12|=p.radius==12;s.view16|=p.radius==16;}}},'
new='''Event::Packet(packet)=>{if s.observer{
            use azalea::protocol::packets::game::ClientboundGamePacket as Packet;
            match packet.as_ref(){
                Packet::SetChunkCacheRadius(p)=>{eprintln!("OBSERVER cache radius={}",p.radius);s.view12|=p.radius==12;s.view16|=p.radius==16;},
                Packet::SetActionBarText(p)=>{let text=p.text.to_string();s.live_hud|=text.contains("PPO ")&&text.contains("difficulty")&&text.contains("bcmc");},
                Packet::TabList(p)=>{s.live_tab|=p.header.to_string().contains("64 RL actors")&&p.footer.to_string().contains("trained samples");},
                _=>{}
            }
        }},'''
assert old in s;s=s.replace(old,new);p.write_text(s)

p=Path('tests/public_entry_checks.py');s=p.read_text().replace("RUN_SECONDS='240'", "RUN_SECONDS='600'")
needle="            for name,args in [('status',['./status.sh']),('console',['./console.sh','list'])]:"
insert='''            if round_number==1:
                # An actual 65th connection uses the production observer UI while
                # all 64 reward-trained actors continue. No test writes a lesson
                # request, world action, model parameter or reward in this run.
                binary=root/'.build/azalea/target/release/examples/bcmc_fixture_check'
                if not binary.is_file():raise RuntimeError('Build the isolated live diagnostic client first')
                initial=status['version']
                observer_env=dict(env,BCMC_ROOT=str(root/'academy-v2'),BCMC_RUN_ID=status['run_id'],
                    BOT_SERVER='127.0.0.1:25565',BCMC_TEST_MODE='observer',BCMC_OBSERVER_REQUIRE_STATUS='true')
                with (out/'public-observer.log').open('w') as observer_log:
                    result=subprocess.run([str(binary)],cwd=root,env=observer_env,stdout=observer_log,stderr=subprocess.STDOUT,timeout=110)
                if result.returncode:raise RuntimeError('simultaneous observer failed: '+(out/'public-observer.log').read_text()[-8000:])
                until=time.monotonic()+100
                while True:
                    if process.poll() is not None:raise RuntimeError('learner exited during observer acceptance')
                    current=json.loads((root/'academy-v2/state/status.json').read_text())
                    if current.get('error'):raise RuntimeError(current['error'])
                    if len(current['agents'])!=64 or any(a['disconnects'] for a in current['agents']):raise RuntimeError('observer disturbed the 64-actor population')
                    if current['version']>initial:break
                    if time.monotonic()>until:raise RuntimeError('no additional real PPO update during/after observer visit')
                    time.sleep(0.5)
                print('PASS: simultaneous observer plus 64 learning actors; real HUD/TAB, menu/tour/view packets; PPO advanced '+str(initial)+' -> '+str(current['version']),flush=True)
'''
assert needle in s;s=s.replace(needle,insert+needle);p.write_text(s)

p=Path('NOTICE');s=p.read_text();s+='''
The repository-owned pins/azalea-client.patch modifies the MIT-licensed Azalea
client at its declared immutable revision. The upstream notice is retained in
licenses/Azalea-MIT.txt. It corrects server inventory/cursor synchronization and
excludes a duplicate ECS representation of the local server identity from
crosshair picking. It adds no navigation, auto-aim or recipe controller.
''';p.write_text(s)
p=Path('docs/VALIDATION.md');s=p.read_text().replace('already-built\nnative binaries','already-built\nnative and isolated diagnostic-client binaries')
s=s.replace('Its test-only sentinel', 'The first run also connects a read-only observer alongside all 64 actors and\nchecks actual HUD/TAB packets, viewing operations and continued PPO updates.\nIts test-only sentinel')
p.write_text(s)
# These paths contain only one-shot development sources, not operator data.
shutil.rmtree('.maintenance')
Path('.github/workflows/integration-workbench.yml').unlink()
Path('.github/workflows/native-toolchain.yml').unlink(missing_ok=True)
print('Final plaintext source prepared; one-shot import/patch workflows removed.')
