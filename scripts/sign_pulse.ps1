# Select active Code Signing certificate for Manikandan
$ErrorActionPreference = "Stop"
$subject = "CN=Manikandan, O=Manikandan"
$now = Get-Date
$cert = Get-ChildItem -Path Cert:\CurrentUser\My -CodeSigningCert |
    Where-Object { $_.Subject -like "*CN=Manikandan*" -and $_.HasPrivateKey -and $_.NotAfter -gt $now } |
    Sort-Object NotAfter -Descending |
    Select-Object -First 1

if (-not $cert) {
    Write-Host "Creating new Code Signing certificate..."
    $cert = New-SelfSignedCertificate -Type CodeSigningCert -Subject $subject -CertStoreLocation "Cert:\CurrentUser\My" -NotAfter $now.AddYears(5)
}

Write-Host "Certificate Subject: $($cert.Subject)"
Write-Host "Thumbprint: $($cert.Thumbprint)"

# Export public certificate for installer and in-app trust verification
$crtPath = "dist\PulseMusic.cer"
if (-not (Test-Path "dist")) {
    New-Item -ItemType Directory -Path "dist" -Force | Out-Null
}
Export-Certificate -Cert $cert -FilePath $crtPath -Force | Out-Null
Write-Host "Exported certificate to $crtPath"

# Stage certificate inside packaged desktop app directories so in-app inspector can verify it directly
$appDir = "desktopApp\build\compose\binaries\main\app\Pulse"
if (Test-Path $appDir) {
    Copy-Item -Path $crtPath -Destination (Join-Path $appDir "PulseMusic.cer") -Force
    $resDir = Join-Path $appDir "app\resources"
    if (Test-Path $resDir) {
        Copy-Item -Path $crtPath -Destination (Join-Path $resDir "PulseMusic.cer") -Force
    }
}

# Register in build user TrustedPublisher/TrustedPeople stores (does not modify machine Root silently)
certutil -user -addstore -f "TrustedPublisher" $crtPath | Out-Null
certutil -user -addstore -f "TrustedPeople" $crtPath | Out-Null

function Sign-PulseBinary {
    param([string]$TargetPath)
    if (-not (Test-Path $TargetPath)) { return }
    Set-ItemProperty -Path $TargetPath -Name IsReadOnly -Value $false -ErrorAction SilentlyContinue
    $timestamps = @("http://timestamp.digicert.com", "http://timestamp.sectigo.com")
    $lastSig = $null
    foreach ($ts in $timestamps) {
        $lastSig = Set-AuthenticodeSignature -Certificate $cert -FilePath $TargetPath -HashAlgorithm SHA256 -TimestampServer $ts -IncludeChain All
        if ($lastSig.Status -ne "HashMismatch" -and $lastSig.SignerCertificate -ne $null) {
            return $lastSig
        }
    }
    return Set-AuthenticodeSignature -Certificate $cert -FilePath $TargetPath -HashAlgorithm SHA256 -IncludeChain All
}

# Sign Pulse.exe
$exePath = Join-Path $appDir "Pulse.exe"
if (Test-Path $exePath) {
    Write-Host "Signing $exePath ..."
    $sig = Sign-PulseBinary -TargetPath $exePath
    Write-Host "Pulse.exe signature status: $($sig.Status)"
} else {
    Write-Host "Target exe not found: $exePath"
}

# Sign bundled native DLLs (e.g., libmpv-2.dll) before Inno Setup packages them
$nativeMpv = "mpv-natives\windows-x64\libmpv-2.dll"
if (Test-Path $nativeMpv) {
    $mpvSig = Get-AuthenticodeSignature -LiteralPath $nativeMpv
    if ($mpvSig.Status -eq "NotSigned" -or -not ($mpvSig.SignerCertificate.Subject -like "*CN=Manikandan*")) {
        Write-Host "Signing $nativeMpv ..."
        $res = Sign-PulseBinary -TargetPath $nativeMpv
        Write-Host "libmpv-2.dll signature status: $($res.Status)"
    }
}

# Sign any remaining unsigned app DLLs/EXEs inside Pulse bundle (preserve already-Valid third-party signed runtime DLLs)
if (Test-Path $appDir) {
    $binaries = Get-ChildItem -Path $appDir -Recurse -File -Include *.exe, *.dll | Where-Object {
        $_.FullName -ne (Resolve-Path $exePath -ErrorAction SilentlyContinue).Path
    }
    $signedCount = 0
    foreach ($bin in $binaries) {
        $existing = Get-AuthenticodeSignature -LiteralPath $bin.FullName
        if ($existing.Status -eq "NotSigned") {
            Sign-PulseBinary -TargetPath $bin.FullName | Out-Null
            $signedCount++
        }
    }
    Write-Host "Signed $signedCount previously unsigned bundled binaries."
}
