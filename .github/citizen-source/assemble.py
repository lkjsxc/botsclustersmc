"""One-shot source delivery: verify every edit and the complete resulting Git tree."""
from pathlib import Path
import hashlib
import json
import os
import subprocess

EXPECTED_TREE = 'c942bcd29de2d72af1710dc935567be20ee37f9d'
BRANCH = 'refs/heads/work/citizen-observation-20260923'
if os.environ.get('GITHUB_REF') != BRANCH:
    raise SystemExit('This delivery is restricted to its named engineering branch.')
root = Path.cwd().resolve()
transport = root / '.github/citizen-source'

def blob_id(data):
    return hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest()

for part in range(4):
    changes = json.loads((transport / f'changes-{part}.json').read_text(encoding='utf-8'))
    for entry in changes:
        path = root / entry['path']
        if not path.is_relative_to(root) or '..' in path.parts or '.git' in path.parts:
            raise ValueError('Invalid source path')
        if any(p.is_symlink() for p in (path, *path.parents)):
            raise ValueError('Source paths must not use symbolic links')
        data = path.read_bytes()
        if blob_id(data) != entry['before']:
            raise ValueError('Source base differs: ' + entry['path'])
        text = data.decode('utf-8')
        prior = 0
        for start, end, replacement in entry['changes']:
            if not 0 <= prior <= start <= end <= len(text):
                raise ValueError('Invalid source edit range')
            prior = end
        for start, end, replacement in reversed(entry['changes']):
            text = text[:start] + replacement + text[end:]
        result = text.encode('utf-8')
        if blob_id(result) != entry['after']:
            raise ValueError('Reconstructed source differs: ' + entry['path'])
        path.write_bytes(result)
        print('Verified source:', entry['path'])

# Only these temporary delivery files are removed, never operator or runtime data.
for part in range(4):
    (transport / f'changes-{part}.json').unlink()
(transport / 'assemble.py').unlink()
transport.rmdir()
(root / '.github/workflows/citizen-assemble.yml').unlink()
subprocess.run(['git', 'add', '-A'], check=True)
tree = subprocess.check_output(['git', 'write-tree'], text=True).strip()
if tree != EXPECTED_TREE:
    raise ValueError('Final source tree mismatch: ' + tree)
print('PASS exact locally tested source tree:', tree)
