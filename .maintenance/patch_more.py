from pathlib import Path
p=Path('learning/src/next/cohort.rs')
s=p.read_text().replace('[0,33]', '[0,65]').replace('[0, 33]', '[0, 65]')
p.write_text(s)
