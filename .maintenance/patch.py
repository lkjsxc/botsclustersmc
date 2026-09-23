"""Finite, idempotent import-path correction for promoted native modules."""
from pathlib import Path
for path in Path('learning/src/next').rglob('*.rs'):
    text = path.read_text()
    text = text.replace('use super as shared_root;', 'use super::{self as shared_root};')
    text = text.replace('use super::super as shared_root;', 'use super::super::{self as shared_root};')
    path.write_text(text)
