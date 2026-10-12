param(
    [string]$FileToSign = $args[0]
)

if (-not $FileToSign) {
    Write-Host "No file specified to sign."
    exit 1
}

$cert = Get-ChildItem -Path Cert:\CurrentUser\My -CodeSigningCert | Where-Object { $_.Subject -like "*Pulse Music Studio*" } | Select-Object -First 1

if (-not $cert) {
    Write-Host "Code signing certificate not found!"
    exit 1
}

Write-Host "Signing $FileToSign with cert $($cert.Thumbprint)..."
$sig = Set-AuthenticodeSignature -Certificate $cert -FilePath $FileToSign -HashAlgorithm SHA256 -TimestampServer "http://timestamp.digicert.com"
Write-Host "Result: $($sig.Status)"
if ($sig.Status -eq "Valid" -or $sig.Status -eq "UnknownError") {
    exit 0
} else {
    exit 1
}
