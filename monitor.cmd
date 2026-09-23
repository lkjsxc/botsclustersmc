@echo off
cd /d "%~dp0"
if not defined JAVA_BIN set "JAVA_BIN=java"
"%JAVA_BIN%" host/Host.java monitor %*
