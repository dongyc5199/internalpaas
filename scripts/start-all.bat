@echo off
REM Windows ?????????? PowerShell ???
REM ??????????
REM   start-all.bat -Pull
REM   start-all.bat -Recreate

setlocal
set SCRIPT_DIR=%~dp0
set PS_SCRIPT=%SCRIPT_DIR%start-all.ps1

if not exist "%PS_SCRIPT%" (
  echo [ERROR] ??? %PS_SCRIPT%
  exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%PS_SCRIPT%" %*