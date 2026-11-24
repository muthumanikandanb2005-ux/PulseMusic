$raw = "ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyUAfhmijvFBT8pPh5PqKgzsRHfUZzMKSjj3NUx57Nm2u/4CmI0GLa9hw7tS9a8Gtq"
$bytes = [System.Convert]::FromBase64String($raw)
$des = [System.Security.Cryptography.DES]::Create()
$des.Mode = [System.Security.Cryptography.CipherMode]::ECB
$des.Padding = [System.Security.Cryptography.PaddingMode]::PKCS7
$des.Key = [System.Text.Encoding]::UTF8.GetBytes("38346591")
$dec = $des.CreateDecryptor().TransformFinalBlock($bytes, 0, $bytes.Length)
$url = [System.Text.Encoding]::UTF8.GetString($dec)
Write-Host "Decrypted URL: $url"
