param(
    [string]$TargetFile = "dist\Pulse-test.exe"
)

$subject = "CN=Pulse Music Studio, O=Pulse Music Studio"
$cert = Get-ChildItem -Path Cert:\CurrentUser\My -CodeSigningCert | Where-Object { $_.Subject -like "*Pulse Music Studio*" } | Select-Object -First 1

if (-not $cert) {
    Write-Host "Creating new Code Signing certificate..."
    $cert = New-SelfSignedCertificate -Type CodeSigningCert -Subject $subject -CertStoreLocation "Cert:\CurrentUser\My" -NotAfter (Get-Date).AddYears(5)
}

Write-Host "Using Certificate: $($cert.Thumbprint)"
Write-Host "Target: $TargetFile"

$sig = Set-AuthenticodeSignature -Certificate $cert -FilePath $TargetFile -HashAlgorithm SHA256 -TimestampServer "http://timestamp.digicert.com"
Write-Host "Signature result: $($sig.Status)"
Get-AuthenticodeSignature $TargetFile
