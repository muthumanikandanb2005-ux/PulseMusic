@echo off
setlocal
echo ========================================================
echo   Manikandan - Publisher Certificate Trust Tool
echo ========================================================
echo.
echo Select trust scope:
echo   - Adds PulseMusic.cer to Windows Certificate Store for verified publisher identity.
echo   - You can review certificate details at any time inside Pulse Music ^> Settings ^> Windows Trust Seal.
echo.

set "CER_PATH=%~dp0PulseMusic.cer"
if not exist "%CER_PATH%" (
    set "CER_PATH=%~dp0app\resources\PulseMusic.cer"
)
if not exist "%CER_PATH%" (
    set "CER_PATH=%~dp0..\dist\PulseMusic.cer"
)
if not exist "%CER_PATH%" (
    set "CER_PATH=%~dp0dist\PulseMusic.cer"
)
if not exist "%CER_PATH%" (
    echo Error: PulseMusic.cer not found next to this script.
    pause
    exit /b 1
)

echo Certificate file: %CER_PATH%
certutil.exe -dump "%CER_PATH%" | findstr /I "Subject: Thumbprint Sha1 NotAfter"
echo.

echo [1/2] Registering certificate in CurrentUser TrustedPublisher and TrustedPeople...
certutil.exe -user -addstore -f "TrustedPublisher" "%CER_PATH%"
certutil.exe -user -addstore -f "TrustedPeople" "%CER_PATH%"

echo.
echo [2/2] Registering certificate in CurrentUser Trusted Root Certification Authorities (Windows will prompt to confirm)...
certutil.exe -user -addstore -f "Root" "%CER_PATH%"

echo.
echo Trust setup finished. Re-open Pulse Music to verify the seal status.
pause
