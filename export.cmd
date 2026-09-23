@echo off
cd /d "%~dp0"
if defined JAVA_BIN ("%JAVA_BIN%" host/Host.java export %*) else (java host/Host.java export %*)
