#!/usr/bin/env python3
"""Lexical delimiter regression ONLY. Not a Rust parser/type/borrow checker."""
from pathlib import Path
from pygments import lex
from pygments.lexers import RustLexer
from pygments.token import Comment, String, Error
root=Path(__file__).resolve().parents[1]
count=0
for folder in ['app','learning/src','launcher']:
    for path in sorted((root/folder).glob('*.rs')):
        stack=[]
        for token,value in lex(path.read_text(),RustLexer()):
            if token in Comment or token in String:
                continue
            if token in Error:
                raise AssertionError(f'{path}: lexer rejected {value!r}')
            for char in value:
                if char in '({[':
                    stack.append(char)
                elif char in ')}]':
                    assert stack and stack.pop()=={')':'(',']':'[','}':'{'}[char], str(path)
        assert not stack, str(path)
        count+=1
print(f'PASS: balanced lexical delimiters in {count} Rust files. NOT compilation or Rust grammar validation.')
