param(
    [Parameter(Mandatory = $true)]
    [string]$WorkspaceRoot
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function New-Secret {
    $bytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $rng.GetBytes($bytes)
    } finally {
        $rng.Dispose()
    }
    return [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+', 'A').Replace('/', 'B')
}

$mysqlEnv = Join-Path $WorkspaceRoot 'mysql.env'
if (-not (Test-Path -LiteralPath $mysqlEnv)) {
    @(
        'MYSQL_DATABASE=global'
        'MYSQL_USER=scape_app'
        "MYSQL_PASSWORD=$(New-Secret)"
        "MYSQL_ROOT_PASSWORD=$(New-Secret)"
    ) | Set-Content -LiteralPath $mysqlEnv -Encoding ascii
    Write-Host 'Created local mysql.env with generated database credentials.'
} else {
    Write-Host 'Using existing local mysql.env.'
}

$configDirectory = Join-Path $WorkspaceRoot 'config'
$localConfig = Join-Path $configDirectory 'default.conf'
if (-not (Test-Path -LiteralPath $localConfig)) {
    New-Item -ItemType Directory -Force -Path $configDirectory | Out-Null
    Copy-Item -LiteralPath (Join-Path $WorkspaceRoot 'Server\worldprops\default.conf') -Destination $localConfig
    Write-Host 'Created local config/default.conf.'
} else {
    Write-Host 'Using existing local config/default.conf.'
}
