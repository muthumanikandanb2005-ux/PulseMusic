# Create a trusted Code Signing certificate
$subject = "CN=Pulse Music Studio, O=Pulse Music Studio"
$cert = Get-ChildItem -Path Cert:\CurrentUser\My -CodeSigningCert | Where-Object { $_.Subject -like "*CN=Pulse Music Studio*" } | Select-Object -First 1

if (-not $cert) {
    Write-Host "Creating new Code Signing certificate..."
    $cert = New-SelfSignedCertificate -Type CodeSigningCert -Subject $subject -CertStoreLocation "Cert:\CurrentUser\My" -NotAfter (Get-Date).AddYears(5)
}

Write-Host "Certificate Subject: $($cert.Subject)"
Write-Host "Thumbprint: $($cert.Thumbprint)"

# Ensure certificate is in CurrentUser TrustedPublisher and TrustedPeople stores
$crtPath = "dist\PulseMusic.cer"
if (-not (Test-Path "dist")) {
    New-Item -ItemType Directory -Path "dist" -Force | Out-Null
}
Export-Certificate -Cert $cert -FilePath $crtPath -Force | Out-Null
Write-Host "Exported certificate to $crtPath"

# Add to user TrustedPublisher and TrustedPeople (silent, no UAC needed)
certutil -user -addstore -f "TrustedPublisher" $crtPath | Out-Null
certutil -user -addstore -f "TrustedPeople" $crtPath | Out-Null
Write-Host "Added certificate to CurrentUser TrustedPublisher and TrustedPeople"

# Sign Pulse.exe
$exePath = "desktopApp\build\compose\binaries\main\app\Pulse\Pulse.exe"
if (Test-Path $exePath) {
    Write-Host "Signing $exePath ..."
    Set-ItemProperty -Path $exePath -Name IsReadOnly -Value $false -ErrorAction SilentlyContinue
    $sig = Set-AuthenticodeSignature -Certificate $cert -FilePath $exePath -HashAlgorithm SHA256 -TimestampServer "http://timestamp.digicert.com" -IncludeChain All
    Write-Host "Signature status: $($sig.Status)"
} else {
    Write-Host "Target exe not found: $exePath"
}
