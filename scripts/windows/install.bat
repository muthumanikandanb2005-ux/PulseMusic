@echo off
REM ============================================================================
REM Pulse Music Studio - Windows Official Installer
REM Supported OS: Windows 10 / Windows 11 (x64 / ARM64 via emulation)
REM Hardware: Infinix Y3 Max / Intel Core / AMD Ryzen / Snapdragon
REM Publisher: Pulse Music Studio
REM ============================================================================

setlocal EnableExtensions
cd /d "%~dp0"

REM --- 1. Elevate to Administrator --------------------------------------------
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo [Pulse Music] Requesting administrative privileges...
    powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /b
)

title Pulse Music Studio - Installer
cls
echo ============================================================================
echo                    PULSE MUSIC - OFFICIAL INSTALLER
echo                       Publisher: Pulse Music Studio
echo ============================================================================
echo.

REM --- 2. Enable Sideloading / Developer Mode in Windows ----------------------
echo [1/4] Configuring Windows App Sideloading permissions...
reg add "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\AppModelUnlock" /t REG_DWORD /f /v "AllowAllTrustedApps" /d 1 >nul 2>&1
reg add "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\AppModelUnlock" /t REG_DWORD /f /v "AllowDevelopmentWithoutDevLicense" /d 1 >nul 2>&1

REM --- 3. Find and Install Certificate for Pulse Music Studio -----------------
echo [2/4] Trusting publisher: Pulse Music Studio...
set "CERT="
for %%c in ("%~dp0pulse.crt" "%~dp0pulsemusic.crt" "%~dp0pulse.cer" "%~dp0simpmusic.crt" "%~dp0*.crt" "%~dp0*.cer") do (
    if exist "%%~fhc" (
        set "CERT=%%~fhc"
        goto :cert_found
    )
)

:cert_found
if defined CERT (
    echo       Importing certificate: %CERT%
    certutil -addstore -f "TrustedPeople" "%CERT%" >nul 2>&1
    certutil -addstore -f "Root" "%CERT%" >nul 2>&1
) else (
    echo       [NOTE] No offline certificate found; proceeding with package install.
)

REM --- 4. Locate MSIX Package -------------------------------------------------
echo.
echo [3/4] Locating Pulse Music package...
set "MSIX="
for %%f in ("%~dp0pulse-*.msix" "%~dp0Pulse-*.msix" "%~dp0pulse*.msix" "%~dp0simpmusic-*.msix" "%~dp0*.msix") do (
    if exist "%%~fhf" (
        set "MSIX=%%~fhf"
        goto :msix_found
    )
)

:msix_found
if not defined MSIX (
    echo [ERROR] No .msix installation package found in this directory!
    echo Please make sure the pulse-*.msix file is in the same folder as install.bat.
    echo.
    pause
    exit /b 1
)

echo       Installing package: %MSIX%
powershell -NoProfile -ExecutionPolicy Bypass -Command "try { Add-AppxPackage -Path '%MSIX%' -ForceApplicationShutdown -ForceUpdateFromAnyVersion -ErrorAction Stop; Write-Host 'Installation successful!' -ForegroundColor Green; exit 0 } catch { Write-Host 'Standard installation encountered error, trying repair mode...' -ForegroundColor Yellow; Get-AppxPackage -Name '*Pulse*' | Remove-AppxPackage -ErrorAction SilentlyContinue; Get-AppxPackage -Name '*Simpmusic*' | Remove-AppxPackage -ErrorAction SilentlyContinue; try { Add-AppxPackage -Path '%MSIX%' -ForceApplicationShutdown -ErrorAction Stop; Write-Host 'Installed successfully after repair!' -ForegroundColor Green; exit 0 } catch { Write-Host $_.Exception.Message -ForegroundColor Red; exit 1 } }"

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Installation failed.
    echo Troubleshooting:
    echo  1. Open Windows Settings -^> System -^> For developers -^> Enable Developer Mode
    echo  2. Restart your PC and run install.bat again as Administrator
    echo.
    pause
    exit /b 1
)

REM --- 5. Launch Pulse Music --------------------------------------------------
echo.
echo [4/4] Launching Pulse Music...
powershell -NoProfile -Command "$pkg = Get-AppxPackage -Name '*Pulse*' | Select-Object -First 1; if ($pkg) { Start-Process shell:appsFolder\$($pkg.PackageFamilyName)!$((Get-AppxPackageManifest $pkg).Package.Applications.Application.Id) } else { Start-Process shell:appsFolder\Simpmusic_ejp2bhxmz1qq6!Simpmusic }"

echo.
echo ============================================================================
echo    Pulse Music installed and launched successfully!
echo    Publisher: Pulse Music Studio
echo ============================================================================
echo.
pause
endlocal
