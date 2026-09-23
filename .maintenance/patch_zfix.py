"""Reviewed compatibility corrections; never executed by normal entrypoints."""
from pathlib import Path

def replace(path, old, new):
    p=Path(path);text=p.read_text()
    if new in text:
        return
    assert old in text, (path, old)
    p.write_text(text.replace(old,new))

replace('bridge/src/org/botsclustersmc/lab/BotsClustersMCLab.java','Statistic.PICKUP_ITEM','Statistic.PICKUP')
# A strict token replacement avoids PICKUP being a prefix of PICKUP_ITEM.
p=Path('bridge/src/org/botsclustersmc/lab/BotsClustersMCLab.java')
p.write_text(p.read_text().replace('Statistic.PICKUP_ITEM','Statistic.PICKUP'))
replace('bridge/EnvironmentConfigTest.java','rejects("run","bcmc",33,"BOTS");','EnvironmentConfig.validate("run","bcmc",64);\n        rejects("run","bcmc",65,"BOTS");')
replace('scripts/control-root.sh','cd -- academy','cd -- academy-v2')
replace('tests/recovery_checks.py',"'bcmc 32 25565 0.0.0.0'","'bcmc 64 25565 0.0.0.0'")
replace('smoke.sh','[[ $bots =~ ^([1-9]|[12][0-9]|3[0-2])$ ]]','[[ $bots =~ ^([1-9]|[1-5][0-9]|6[0-4])$ ]]')
replace('learning/src/curriculum.rs','ProgressGate,Task,ITEM_COUNT','ProgressGate,ITEM_COUNT')
print('Corrected pinned API spelling, 64-actor limits and Academy-v2 control routing.')
