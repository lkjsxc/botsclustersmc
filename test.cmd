@echo off
cd /d "%~dp0"
if defined JAVA_BIN ("%JAVA_BIN%" host/Host.java test %*) else (java host/Host.java test %*)
