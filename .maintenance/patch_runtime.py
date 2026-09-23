"""Idempotent correction after the already-committed GUI/identity integration.
Never used by normal runtime entrypoints. This file is removed before delivery.
"""
from pathlib import Path
p=Path('learning/src/control.rs')
s=p.read_text().replace('pub policy:Arc<Model>,identity:PolicyId,pub course:Curriculum,','pub policy:Arc<Model>,pub course:Curriculum,')
assert s.count('identity:PolicyId') == 1, 'Expected one cached policy identity'
p.write_text(s)
print('One immutable coordinator policy identity; other verified changes preserved.')
