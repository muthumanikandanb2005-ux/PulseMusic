@echo off
setlocal
echo ========================================================
echo   Pulse Music Studio - Publisher Certificate Trust Tool
echo ========================================================
echo.
echo Installing Pulse Music Studio certificate into Windows Trust Stores...
echo.

set "CER_PATH=%~dp0PulseMusic.cer"
if not exist "%CER_PATH%" (
    set "CER_PATH=%~dp0..\dist\PulseMusic.cer"
)
if not exist "%CER_PATH%" (
    set "CER_PATH=%~dp0dist\PulseMusic.cer"
)
if not exist "%CER_PATH%" (
    echo Error: PulseMusic.cer not found at %CER_PATH%
    pause
    exit /b 1
)

:: 1. Add to CurrentUser TrustedPublisher and TrustedPeople (silent, no admin required)
echo Configuring CurrentUser TrustedPublisher and TrustedPeople...
certutil.exe -user -addstore -f "TrustedPublisher" "%CER_PATH%" >nul 2>&1
certutil.exe -user -addstore -f "TrustedPeople" "%CER_PATH%" >nul 2>&1

:: 2. Add to LocalMachine Root (requires admin elevation via PowerShell Start-Process -Verb RunAs)
echo Requesting administrator privilege to trust root certificate...
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "Start-Process certutil.exe -ArgumentList '-addstore -f Root \"\"\"%CER_PATH%\"\"\"' -Verb RunAs -Wait"
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "Start-Process certutil.exe -ArgumentList '-addstore -f TrustedPublisher \"\"\"%CER_PATH%\"\"\"' -Verb RunAs -Wait"

echo.
echo Trust configuration completed!
echo Windows will now recognize Pulse Music Studio as a verified publisher.
echo.
pause
