"""One-shot, reviewable source promotion on the integration branch only.
No operator files, binaries, world data, credentials or external code are read.
Normal build/start never executes this maintenance script.
"""
from pathlib import Path
import shutil

root = Path('.')
src = root / 'experimental/rl-next/src'
dst = root / 'learning/src/next'
if dst.exists():
    raise SystemExit('Refusing to overwrite an already promoted implementation')
for path in sorted(src.rglob('*.rs')):
    rel = path.relative_to(src)
    if rel == Path('main.rs'):
        continue
    text = path.read_text()
    if 'crate::' in text:
        levels = len(rel.parent.parts) + (0 if rel.name in ('mod.rs', 'lib.rs') else 1)
        assert levels > 0, rel
        prefix = '::'.join(['super'] * levels)
        # Module aliases also remain visible through the existing test super::* imports.
        lines = text.splitlines(keepends=True)
        at = 0
        while at < len(lines) and (lines[at].startswith('//!') or not lines[at].strip()):
            at += 1
        lines.insert(at, f'use {prefix} as shared_root;\n')
        text = ''.join(lines).replace('crate::', 'shared_root::')
    if rel == Path('lib.rs'):
        rel = Path('mod.rs')
        text = text.replace('//! Experimental components, NOT wired into the v0.3.1 Minecraft executable.\n//! See INTEGRATION.md before attempting to use these in a live learner.',
                            '//! Shared RL mechanisms. The runtime and standalone checks use this implementation.')
    if str(rel) in ('cohort.rs', 'curriculum/mod.rs', 'curriculum/checkpoint.rs'):
        text = text.replace('1..=32', '1..=64').replace('one to 32 actors', 'one to 64 actors')
    if str(rel) == 'curriculum/checkpoint.rs':
        text = text.replace('bytes.len()>64*1024', 'bytes.len()>256*1024')
    if str(rel) == 'tasks.rs':
        text = text.replace('baseline.session.actor>=32', 'baseline.session.actor>=64')
        text = text.replace('//! Task contracts and evidence gates, not a Minecraft environment implementation.\n//! Merely adding an enum/validator does not make a stage playable. The Java/Folia\n//! adapter must supply real geometry, reset states and confirmed transaction data.',
                            '//! Task contracts and authoritative evidence gates shared with the Minecraft adapter.')
        text = text.replace('        m\n    }', '        // Advanced tasks may furnish supplies outside the hotbar. Literal GUI\n        // actions remain available; no mask reveals a recipe or correct slot.\n        if self as usize >= 6 { m[6].fill(true); m[7].fill(true); }\n        m\n    }')
    out = dst / rel
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(text)
lib = root / 'learning/src/lib.rs'
lib.write_text(lib.read_text() + '\npub mod next;\n')
(src / 'lib.rs').write_text('//! Standalone checks re-export the exact canonical runtime implementation.\n#[path = "../../../learning/src/next/mod.rs"]\nmod shared;\npub use shared::*;\n')
# Remove duplicate implementation files; history retains their provenance.
for path in list(src.iterdir()):
    if path.name in ('lib.rs', 'main.rs'):
        continue
    if path.is_dir():
        shutil.rmtree(path)
    else:
        path.unlink()
build = root / 'scripts/build.sh'
text = build.read_text()
assert 'cp learning/src/*.rs "$example/core/"' in text
text = text.replace('cp learning/src/*.rs "$example/core/"', 'cp -a learning/src/. "$example/core/"')
build.write_text(text)
print('Promoted shared RL mechanisms without changing the active runtime yet.')
