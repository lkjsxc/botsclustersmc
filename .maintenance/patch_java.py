from pathlib import Path
p=Path('bridge/src/org/botsclustersmc/lab/CampusPlan.java')
s=p.read_text()
s=s.replace(".append(' ').append(ox(id)+1).append(' ').append(oz(id)+1).append(' ').append(ox(id)+14).append(' ').append(oz(id)+14).append(\" 96 104\\n\")", ".append('\\n')")
p.write_text(s)
p=Path('bridge/src/org/botsclustersmc/lab/EnvironmentConfig.java')
s=p.read_text().replace('bots>32','bots>64').replace('1..32','1..64');p.write_text(s)
