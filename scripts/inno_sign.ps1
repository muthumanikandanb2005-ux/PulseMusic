param(
    [string]$FileToSign = $args[0]
)

if (-not $FileToSign) {
    Write-Host "No file specified to sign."
    exit 1
}

$now = Get-Date
$cert = Get-ChildItem -Path Cert:\CurrentUser\My -CodeSigningCert |
    Where-Object { $_.Subject -like "*CN=Manikandan*" -and $_.HasPrivateKey -and $_.NotAfter -gt $now } |
    Sort-Object NotAfter -Descending |
    Select-Object -First 1

if (-not $cert) {
    Write-Host "Code signing certificate not found!"
    exit 1
}

Write-Host "Signing $FileToSign with cert $($cert.Thumbprint)..."
$timestamps = @("http://timestamp.digicert.com", "http://timestamp.sectigo.com")
$sig = $null
foreach ($ts in $timestamps) {
    $sig = Set-AuthenticodeSignature -Certificate $cert -FilePath $FileToSign -HashAlgorithm SHA256 -TimestampServer $ts -IncludeChain All
    if ($sig.SignerCertificate -ne $null -and $sig.Status -ne "HashMismatch") {
        break
    }
}
if ($sig -eq $null -or $sig.SignerCertificate -eq $null) {
    $sig = Set-AuthenticodeSignature -Certificate $cert -FilePath $FileToSign -HashAlgorithm SHA256 -IncludeChain All
}
Write-Host "Result: $($sig.Status)"
if ($sig.SignerCertificate -ne $null -and ($sig.Status -eq "Valid" -or $sig.Status -eq "UnknownError")) {
    exit 0
} else {
    exit 1
}
