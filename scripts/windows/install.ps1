<#
.SYNOPSIS
    Pulse Music Studio - Windows 10/11 Installer
    Publisher: Pulse Music Studio
    Hardware: Infinix Y3 Max / Intel / AMD / Qualcomm
#>

# Requires Run as Administrator
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Host "[Pulse Music] Elevating to Administrator..." -ForegroundColor Cyan
    Start-Process powershell -Verb RunAs -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`""
    exit
}

$ScriptDir = Split-Path -Parent $PSCommandPath
Set-Location $ScriptDir

Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host "                   PULSE MUSIC - OFFICIAL INSTALLER" -ForegroundColor White
Write-Host "                      Publisher: Pulse Music Studio" -ForegroundColor Green
Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Enable Sideloading
Write-Host "[1/4] Enabling Windows App Sideloading permissions..." -ForegroundColor Yellow
Set-ItemProperty -Path "HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\AppModelUnlock" -Name "AllowAllTrustedApps" -Value 1 -Type DWord -Force -ErrorAction SilentlyContinue
Set-ItemProperty -Path "HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\AppModelUnlock" -Name "AllowDevelopmentWithoutDevLicense" -Value 1 -Type DWord -Force -ErrorAction SilentlyContinue

# 2. Import Certificate for Pulse Music Studio
Write-Host "[2/4] Trusting publisher: Pulse Music Studio..." -ForegroundColor Yellow
$CertFiles = Get-ChildItem -Path $ScriptDir -Include "*.crt","*.cer" -Recurse -File -ErrorAction SilentlyContinue
if ($CertFiles.Count -gt 0) {
    $Cert = $CertFiles[0]
    Write-Host "      Importing certificate: $($Cert.FullName)" -ForegroundColor Gray
    try {
        Import-Certificate -FilePath $Cert.FullName -CertStoreLocation Cert:\LocalMachine\TrustedPeople | Out-Null
        Import-Certificate -FilePath $Cert.FullName -CertStoreLocation Cert:\LocalMachine\Root | Out-Null
        Write-Host "      Publisher certificate trusted successfully!" -ForegroundColor Green
    } catch {
        Write-Warning "      Failed to import certificate automatically: $_"
    }
}

# 3. Locate MSIX Package
Write-Host "`n[3/4] Installing Pulse Music application..." -ForegroundColor Yellow
$MsixFiles = Get-ChildItem -Path $ScriptDir -Filter "*.msix" -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending
if ($MsixFiles.Count -eq 0) {
    Write-Host "[ERROR] No .msix installation package found in $ScriptDir" -ForegroundColor Red
    pause
    exit 1
}

$TargetMsix = $MsixFiles[0].FullName
Write-Host "      Installing: $($MsixFiles[0].Name)" -ForegroundColor Gray

try {
    Add-AppxPackage -Path $TargetMsix -ForceApplicationShutdown -ForceUpdateFromAnyVersion -ErrorAction Stop
    Write-Host "      Pulse Music installed successfully!" -ForegroundColor Green
} catch {
    Write-Host "      Retrying with package refresh..." -ForegroundColor Yellow
    Get-AppxPackage -Name "*Pulse*" | Remove-AppxPackage -ErrorAction SilentlyContinue
    Get-AppxPackage -Name "*Simpmusic*" | Remove-AppxPackage -ErrorAction SilentlyContinue
    try {
        Add-AppxPackage -Path $TargetMsix -ForceApplicationShutdown -ErrorAction Stop
        Write-Host "      Pulse Music installed successfully!" -ForegroundColor Green
    } catch {
        Write-Host "[ERROR] Add-AppxPackage failed: $($_.Exception.Message)" -ForegroundColor Red
        pause
        exit 1
    }
}

# 4. Launch App
Write-Host "`n[4/4] Launching Pulse Music..." -ForegroundColor Yellow
$InstalledPkg = Get-AppxPackage -Name "*Pulse*" | Select-Object -First 1
if ($InstalledPkg) {
    $Manifest = Get-AppxPackageManifest $InstalledPkg
    $AppId = $Manifest.Package.Applications.Application.Id
    Start-Process "shell:appsFolder\$($InstalledPkg.PackageFamilyName)!$AppId"
} else {
    Start-Process "shell:appsFolder\Simpmusic_ejp2bhxmz1qq6!Simpmusic"
}

Write-Host "`n============================================================================" -ForegroundColor Cyan
Write-Host "   Pulse Music installed and launched successfully! Enjoy your music!" -ForegroundColor Green
Write-Host "============================================================================" -ForegroundColor Cyan
Start-Sleep -Seconds 3
