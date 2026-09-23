"""Explicitly version runtime ownership and population; never read operator data."""
from pathlib import Path
files=list(Path('.').glob('*.sh'))+list(Path('scripts').glob('*.sh'))+list(Path('scripts').glob('*.java'))+list(Path('launcher').glob('*.rs'))+list(Path('tests').glob('*.py'))
for p in files:
    s=p.read_text()
    s=s.replace('botsclustersmc-academy-v1','botsclustersmc-academy-v2').replace('bcmc_academy_v1','bcmc_academy_v2').replace('BCMCLAB2','BCMCLAB3')
    s=s.replace('academy/', 'academy-v2/').replace('/academy"','/academy-v2"').replace("/academy'","/academy-v2'").replace("'academy'","'academy-v2'").replace('"academy"','"academy-v2"')
    s=s.replace('1..=32','1..=64').replace('1..32','1..64').replace('BOTS <= 32','BOTS <= 64').replace('10#$BOTS <= 32','10#$BOTS <= 64')
    s=s.replace('bots>32','bots>64').replace('bots > 32','bots > 64').replace('id in 0..32','id in 0..64')
    p.write_text(s)
p=Path('academy.sh');s=p.read_text()
s=s.replace('STARTUP_TIMEOUT_SECONDS; do','STARTUP_TIMEOUT_SECONDS BOT_VIEW_DISTANCE SPECTATOR_VIEW_DISTANCE BOT_JOIN_DELAY_MS COHORT_TIMEOUT_SECONDS; do')
s=s.replace("'ROLLOUT_STEPS=64'", "\"ROLLOUT_STEPS=$ROLLOUT_STEPS\"")
s=s.replace('ROLLOUT_STEPS=64 BCMC_ACADEMY_LOCK=1', 'ROLLOUT_STEPS="$ROLLOUT_STEPS" BCMC_ACADEMY_LOCK=1')
p.write_text(s)
p=Path('scripts/runtime-config.sh');s=p.read_text().replace('bcmc31','bcmc63')
s=s.replace('RUN_SECONDS STARTUP_TIMEOUT_SECONDS; do','RUN_SECONDS STARTUP_TIMEOUT_SECONDS BOT_VIEW_DISTANCE SPECTATOR_VIEW_DISTANCE BOT_JOIN_DELAY_MS COHORT_TIMEOUT_SECONDS; do')
if 'SPECTATOR_VIEW_DISTANCE >= 3' not in s:
    at=s.index('  [[ ${BIND_ADDRESS:')
    s=s[:at]+'''  (( BOT_VIEW_DISTANCE >= 3 && BOT_VIEW_DISTANCE <= 6 )) || { echo 'BOT_VIEW_DISTANCE must be 3..6.' >&2; return 1; }
  (( SPECTATOR_VIEW_DISTANCE >= 3 && SPECTATOR_VIEW_DISTANCE <= 16 )) || { echo 'SPECTATOR_VIEW_DISTANCE must be 3..16.' >&2; return 1; }
  (( BOT_JOIN_DELAY_MS >= 100 && BOT_JOIN_DELAY_MS <= 3000 )) || { echo 'BOT_JOIN_DELAY_MS must be 100..3000.' >&2; return 1; }
  (( COHORT_TIMEOUT_SECONDS >= 180 && COHORT_TIMEOUT_SECONDS <= 3600 )) || { echo 'COHORT_TIMEOUT_SECONDS must be 180..3600.' >&2; return 1; }
''' +s[at:]
p.write_text(s)
p=Path('launcher/config.rs');s=p.read_text()
s=s.replace('bots:number("BOTS",32,1,32)?','bots:number("BOTS",64,1,64)?')
s=s.replace('max-players=48\\n','max-players={}\\n').replace('self.port,self.bind,self.view,self.sim','self.port,self.bind,self.bots+16,self.view,self.sim')
s=s.replace('entity-broadcast-range-percentage=75','entity-broadcast-range-percentage=100')
if 'entity-tracking-range:' not in s:
    s=s.replace('        Ok(())','        install::atomic(&server.join("spigot.yml"),b"# Managed observer visibility; does not change actor simulation distance.\\nworld-settings:\\n  default:\\n    entity-tracking-range:\\n      players: 128\\n      animals: 48\\n      monsters: 48\\n      misc: 32\\n      display: 128\\n      other: 64\\n")?;\n        Ok(())')
p.write_text(s)
p=Path('launcher/audit.rs');s=p.read_text().replace('learning::checkpoint::Checkpoint','learning::bundle::Bundle')
s=s.replace('let cp=Checkpoint::load(&root.join("state/policy.bcmc")).map_err(|e|format!("checkpoint: {e}"))?;', 'let cp=Bundle::load(&root.join("state/training.bcmc"),integer(&run,"bots")? as usize,1).map_err(|e|format!("checkpoint: {e}"))?.checkpoint;')
p.write_text(s)
p=Path('launcher/academy_audit.rs');s=p.read_text().replace('checkpoint::Checkpoint,curriculum::Curriculum','bundle::Bundle')
s=s.replace('stage>=6','stage>=18')
start=s.index('    let cp=Checkpoint::load(') if '    let cp=Checkpoint::load(' in s else -1
if start>=0:
    end=s.index('    let mut text=',start)
    s=s[:start]+'''    let bundle=Bundle::load(&root.join("state/training.bcmc"),bots,1).map_err(|e|e.to_string())?;
    if state.get("schema")?.integer()?!=3||bundle.curriculum.frontier() as u64!=state.get("stage")?.integer()?||bundle.checkpoint.model.version!=state.get("policy_version")?.integer()?{return Err("atomic checkpoint/academy status mismatch".into());}
''' +s[end:]
s=s.replace('paired curriculum checkpoint','atomic policy/curriculum/RNG checkpoint');p.write_text(s)
p=Path('tests/package_checks.py');s=p.read_text().replace('bots=32','bots=64').replace('test_default_32','test_default_64').replace("'32 0.0.0.0 true'","'64 0.0.0.0 true'").replace('/academy true 32','/academy-v2 true 64').replace('/academy-v2 true 32','/academy-v2 true 64');p.write_text(s)
p=Path('bridge/EnvironmentConfigTest.java');s=p.read_text().replace(',33)',',65)').replace(', 33)',', 65)');p.write_text(s)
p=Path('bridge/src/org/botsclustersmc/lab/EnvironmentConfig.java');s=p.read_text().replace('bots>32','bots>64').replace('bots > 32','bots > 64');p.write_text(s)
p=Path('bridge/src/org/botsclustersmc/lab/BotsClustersMCLab.java');s=p.read_text().replace('setting("BOT_VIEW_DISTANCE",3,2,6)','setting("BOT_VIEW_DISTANCE",3,3,6)');p.write_text(s)
p=Path('.gitignore');s=p.read_text()
if '/academy-v2/' not in s:s+='\n/academy-v2/\n/.botsclustersmc-academy-v2\n'
p.write_text(s)
print('Source ownership, audit and configuration now use Academy v2 and 64 actors.')
