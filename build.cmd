@echo off
cd /d "%~dp0"
if defined JAVA_BIN ("%JAVA_BIN%" host/Host.java build %*) else (java host/Host.java build %*)
