@echo off
set "ISCC=%LOCALAPPDATA%\Programs\Inno Setup 6\ISCC.exe"
if not exist "%ISCC%" (
    echo ISCC not found at "%ISCC%"
    exit /b 1
)
echo Signing Pulse binary before packaging...
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0sign_pulse.ps1"

echo Compiling installer with Inno Setup...
"%ISCC%" "/Ssigntool=cmd.exe /c %~dp0sign_cmd.bat $f" "%~dp0installer.iss"
exit /b %errorlevel%
