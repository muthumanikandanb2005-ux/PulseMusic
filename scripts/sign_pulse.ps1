# Create a trusted Code Signing certificate
$subject = "CN=Pulse Music Studio, O=Pulse Music, OU=Pulse Team, C=US"
$cert = Get-ChildItem -Path Cert:\CurrentUser\My -CodeSigningCert | Where-Object { $_.Subject -like "*Pulse Music Studio*" } | Select-Object -First 1

if (-not $cert) {
    Write-Host "Creating new Code Signing certificate..."
    $cert = New-SelfSignedCertificate -Type CodeSigningCert -Subject $subject -CertStoreLocation "Cert:\CurrentUser\My" -NotAfter (Get-Date).AddYears(5)
}

Write-Host "Certificate Subject: $($cert.Subject)"
Write-Host "Thumbprint: $($cert.Thumbprint)"

# Export public certificate
$crtPath = "dist\PulseMusic.cer"
Export-Certificate -Cert $cert -FilePath $crtPath -Force
Write-Host "Exported certificate to $crtPath"

# Sign Pulse.exe
$exePath = "desktopApp\build\compose\binaries\main-release\app\Pulse\Pulse.exe"
if (Test-Path $exePath) {
    Write-Host "Signing $exePath ..."
    $sig = Set-AuthenticodeSignature -Certificate $cert -FilePath $exePath -HashAlgorithm SHA256 -TimestampServer "http://timestamp.digicert.com"
    Write-Host "Signature status: $($sig.Status)"
} else {
    Write-Host "Target exe not found: $exePath"
}
