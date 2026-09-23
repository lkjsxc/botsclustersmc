@echo off
cd /d "%~dp0"
if not defined JAVA_BIN set "JAVA_BIN=java"
"%JAVA_BIN%" -Xmx256m host/Host.java evaluate %*
