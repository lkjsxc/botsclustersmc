"""Reviewed integration corrections; executed after prepare.py in the workbench."""
from pathlib import Path
import re
p=Path('learning/src/ppo.rs');s=p.read_text();s=re.sub(r'(->\s*Transition)\{ticks:4,',r'\1{',s);p.write_text(s)
p=Path('learning/src/next/math/trust.rs');s=p.read_text();a=s.index('#[cfg(test)]');s=s[:a]+s[a:].replace('use super::super::Rng;','use super::super::super::Rng;');p.write_text(s)
p=Path('learning/src/curriculum.rs');s=p.read_text();s=re.sub(r'id\s*<\s*32\s*&&\s*stage','id < 64 && stage',s);p.write_text(s)
for name in ['app/settings.rs','launcher/config.rs']:
 p=Path(name);s=p.read_text();s=re.sub(r'(number\("BOTS",\s*)32(\s*,\s*1,\s*)32\)',r'\g<1>64\g<2>64)',s);p.write_text(s)
for name in ['academy.sh','scripts/runtime-config.sh']:
 p=Path(name);s=p.read_text();s=re.sub(r'BOTS\s*<=\s*32','BOTS<=64',s);p.write_text(s)
p=Path('bridge/src/org/botsclustersmc/lab/EnvironmentConfig.java');s=p.read_text();s=re.sub(r'bots\s*>\s*32','bots>64',s);p.write_text(s)
print('Corrected nested test imports and preserved ordinary function bodies.')
