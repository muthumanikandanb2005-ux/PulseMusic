@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0inno_sign.ps1" "%~1"
exit /b %errorlevel%
