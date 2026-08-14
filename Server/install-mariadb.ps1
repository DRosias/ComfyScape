param(
    [string]$WorkspaceRoot = $PSScriptRoot,
    [switch]$DatabaseOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$serviceName = 'ComfyScapeMariaDB'
$mariaDbVersion = '11.4.12'
$msiFileName = "mariadb-$mariaDbVersion-winx64.msi"
$msiUri = "https://downloads.mariadb.org/rest-api/mariadb/$mariaDbVersion/$msiFileName"
$msiSha256 = '4d92fb5f16c0ec8d5a9fc1efdb33a377eaa712d6bce97451e151465c3041ccac'

function Test-Administrator {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($identity)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Quote-ProcessArgument([string]$Value) {
    return '"' + $Value.Replace('"', '\"') + '"'
}

function Invoke-ElevatedSelf {
    $arguments = @(
        '-NoProfile'
        '-ExecutionPolicy Bypass'
        '-File ' + (Quote-ProcessArgument $PSCommandPath)
        '-WorkspaceRoot ' + (Quote-ProcessArgument $WorkspaceRoot)
    ) -join ' '
    Write-Host 'Requesting administrator approval for the local MariaDB service...'
    $process = Start-Process -FilePath 'powershell.exe' -Verb RunAs -ArgumentList $arguments -Wait -PassThru
    exit $process.ExitCode
}

function Read-EnvironmentFile([string]$Path) {
    $values = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($line -match '^\s*#' -or [string]::IsNullOrWhiteSpace($line)) {
            continue
        }
        $separator = $line.IndexOf('=')
        if ($separator -lt 1) {
            throw "Invalid line in $Path. Expected NAME=value."
        }
        $name = $line.Substring(0, $separator).Trim()
        $values[$name] = $line.Substring($separator + 1)
    }
    return $values
}

function Get-RequiredValue([hashtable]$Values, [string]$Name) {
    if (-not $Values.ContainsKey($Name) -or [string]::IsNullOrWhiteSpace([string]$Values[$Name])) {
        throw "mysql.env is missing $Name."
    }
    return [string]$Values[$Name]
}

function Get-ServiceRecord {
    return Get-CimInstance Win32_Service -Filter "Name='$serviceName'" -ErrorAction SilentlyContinue
}

function Get-MariaDbClient {
    $record = Get-ServiceRecord
    if ($null -ne $record -and $record.PathName -match '^\s*"?([^" ]+.*?\\bin\\(?:mariadbd|mysqld)\.exe)"?(?:\s|$)') {
        $binDirectory = Split-Path -Parent $matches[1].Trim('"')
        foreach ($name in @('mariadb.exe', 'mysql.exe')) {
            $candidate = Join-Path $binDirectory $name
            if (Test-Path -LiteralPath $candidate) {
                return $candidate
            }
        }
    }

    $uninstallRoots = @(
        'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\*'
        'HKLM:\SOFTWARE\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall\*'
    )
    $installations = Get-ItemProperty -Path $uninstallRoots -ErrorAction SilentlyContinue |
        Where-Object { $_.DisplayName -like 'MariaDB*' -and $_.InstallLocation } |
        Sort-Object DisplayVersion -Descending
    foreach ($installation in $installations) {
        foreach ($name in @('mariadb.exe', 'mysql.exe')) {
            $candidate = Join-Path ([string]$installation.InstallLocation) "bin\$name"
            if (Test-Path -LiteralPath $candidate) {
                return $candidate
            }
        }
    }
    throw 'MariaDB was installed, but its command-line client could not be located.'
}

function Get-MariaDbOptionFile([string]$ClientPath) {
    $record = Get-ServiceRecord
    if ($null -ne $record -and $record.PathName -match '--defaults-file(?:=|\s+)"?([^" ]+\.ini)"?') {
        $candidate = $matches[1]
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }
    $installRoot = Split-Path -Parent (Split-Path -Parent $ClientPath)
    foreach ($candidate in @((Join-Path $installRoot 'data\my.ini'), (Join-Path $installRoot 'my.ini'))) {
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }
    throw 'MariaDB my.ini could not be located.'
}

function Set-LocalOnlyBinding([string]$OptionFile) {
    $lines = [Collections.Generic.List[string]]::new()
    foreach ($line in Get-Content -LiteralPath $OptionFile) {
        $lines.Add($line)
    }
    $sectionStart = -1
    $sectionEnd = $lines.Count
    for ($index = 0; $index -lt $lines.Count; $index++) {
        if ($lines[$index] -match '^\s*\[mysqld\]\s*$') {
            $sectionStart = $index
            for ($next = $index + 1; $next -lt $lines.Count; $next++) {
                if ($lines[$next] -match '^\s*\[') {
                    $sectionEnd = $next
                    break
                }
            }
            break
        }
    }
    if ($sectionStart -lt 0) {
        $lines.Add('')
        $lines.Add('[mysqld]')
        $lines.Add('bind-address=127.0.0.1')
    } else {
        $settingIndex = -1
        for ($index = $sectionStart + 1; $index -lt $sectionEnd; $index++) {
            if ($lines[$index] -match '^\s*bind-address\s*=') {
                $settingIndex = $index
                break
            }
        }
        if ($settingIndex -ge 0) {
            $lines[$settingIndex] = 'bind-address=127.0.0.1'
        } else {
            $lines.Insert($sectionStart + 1, 'bind-address=127.0.0.1')
        }
    }
    Set-Content -LiteralPath $OptionFile -Value $lines -Encoding ascii
}

function ConvertTo-OptionFileValue([string]$Value) {
    return $Value.Replace('\', '\\').Replace('"', '\"')
}

function ConvertTo-SqlString([string]$Value) {
    if ($Value -match '[\r\n\x00]') {
        throw 'Database credentials may not contain line breaks or null bytes.'
    }
    return $Value.Replace('\', '\\').Replace("'", "\'")
}

function Invoke-MariaDb([string]$ClientPath, [string]$DefaultsFile, [string]$Sql) {
    $output = & $ClientPath "--defaults-extra-file=$DefaultsFile" '--protocol=TCP' '--host=127.0.0.1' '--port=3306' '--batch' '--skip-column-names' "--execute=$Sql" 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "MariaDB command failed: $($output -join [Environment]::NewLine)"
    }
    return @($output)
}

function Wait-MariaDb([string]$ClientPath, [string]$DefaultsFile) {
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        try {
            Invoke-MariaDb $ClientPath $DefaultsFile 'SELECT 1;' | Out-Null
            return
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    throw 'MariaDB did not accept local authenticated connections within two minutes.'
}

$WorkspaceRoot = (Resolve-Path -LiteralPath $WorkspaceRoot).Path
$setupScript = Join-Path $WorkspaceRoot 'setup-local.ps1'
& $setupScript -WorkspaceRoot $WorkspaceRoot

if (-not (Test-Administrator) -and -not $DatabaseOnly) {
    Invoke-ElevatedSelf
}

$environmentPath = Join-Path $WorkspaceRoot 'mysql.env'
$environment = Read-EnvironmentFile $environmentPath
$databaseName = Get-RequiredValue $environment 'MYSQL_DATABASE'
$applicationUser = Get-RequiredValue $environment 'MYSQL_USER'
$applicationPassword = Get-RequiredValue $environment 'MYSQL_PASSWORD'
$rootPassword = Get-RequiredValue $environment 'MYSQL_ROOT_PASSWORD'

if ($databaseName -ne 'global') {
    throw 'MYSQL_DATABASE must remain global because the bundled schema targets that database.'
}
if ($applicationUser -notmatch '^[A-Za-z0-9_]+$') {
    throw 'MYSQL_USER may contain only letters, digits, and underscores.'
}
if ($applicationPassword -eq $rootPassword) {
    throw 'MYSQL_PASSWORD and MYSQL_ROOT_PASSWORD must be different.'
}

$toolDirectory = Join-Path $WorkspaceRoot '.local-tools'
New-Item -ItemType Directory -Force -Path $toolDirectory | Out-Null

if ($DatabaseOnly -and $null -eq (Get-ServiceRecord)) {
    throw 'Database-only repair requires the ComfyScapeMariaDB service to be installed.'
}

if ($null -eq (Get-ServiceRecord)) {
    $msiPath = Join-Path $toolDirectory $msiFileName
    if (-not (Test-Path -LiteralPath $msiPath) -or (Get-FileHash -LiteralPath $msiPath -Algorithm SHA256).Hash -ne $msiSha256) {
        Write-Host "Downloading MariaDB $mariaDbVersion from mariadb.org..."
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -Uri $msiUri -OutFile $msiPath -UseBasicParsing
    }
    $actualHash = (Get-FileHash -LiteralPath $msiPath -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actualHash -ne $msiSha256) {
        throw 'The MariaDB installer checksum does not match the pinned official checksum.'
    }
    $signature = Get-AuthenticodeSignature -LiteralPath $msiPath
    if ($signature.Status -ne [Management.Automation.SignatureStatus]::Valid -or $null -eq $signature.SignerCertificate -or $signature.SignerCertificate.Subject -notmatch 'MariaDB') {
        throw "The MariaDB installer does not have a valid MariaDB Authenticode signature (status: $($signature.Status))."
    }

    Write-Host "Installing MariaDB $mariaDbVersion as Windows service $serviceName..."
    $arguments = @(
        '/i'
        (Quote-ProcessArgument $msiPath)
        '/qn'
        '/norestart'
        "SERVICENAME=$serviceName"
        'PORT=3306'
        ('PASSWORD=' + (Quote-ProcessArgument $rootPassword))
        'ALLOWREMOTEROOTACCESS='
        'DEFAULTUSER='
        'STDCONFIG=1'
        'ADDLOCAL=DBInstance,Client,MYSQLSERVER,SharedLibraries'
        'REMOVE=HeidiSQL,DEVEL'
    ) -join ' '
    $installer = Start-Process -FilePath 'msiexec.exe' -ArgumentList $arguments -Wait -PassThru
    if ($installer.ExitCode -notin @(0, 3010)) {
        throw "MariaDB installation failed with Windows Installer exit code $($installer.ExitCode)."
    }
}

$clientPath = Get-MariaDbClient
$service = Get-Service -Name $serviceName
if ($DatabaseOnly) {
    if ($service.Status -ne 'Running') {
        throw 'Database-only repair requires the ComfyScapeMariaDB service to be running.'
    }
} else {
    $optionFile = Get-MariaDbOptionFile $clientPath
    if ($service.Status -ne 'Stopped') {
        Stop-Service -Name $serviceName -Force
        $service.WaitForStatus('Stopped', [TimeSpan]::FromSeconds(30))
    }
    Set-LocalOnlyBinding $optionFile
    Set-Service -Name $serviceName -StartupType Automatic
    Start-Service -Name $serviceName
    (Get-Service -Name $serviceName).WaitForStatus('Running', [TimeSpan]::FromSeconds(30))
}

$defaultsFile = Join-Path $toolDirectory ("mariadb-client-{0}.cnf" -f [Guid]::NewGuid().ToString('N'))
try {
    @(
        '[client]'
        'user=root'
        ('password="' + (ConvertTo-OptionFileValue $rootPassword) + '"')
        'host=127.0.0.1'
        'port=3306'
        'protocol=tcp'
    ) | Set-Content -LiteralPath $defaultsFile -Encoding ascii

    Wait-MariaDb $clientPath $defaultsFile
    $schemaCount = (Invoke-MariaDb $clientPath $defaultsFile "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='global';" | Select-Object -First 1).Trim()
    if ($schemaCount -eq '0') {
        $schemaPath = (Join-Path $WorkspaceRoot 'Server\db_exports\global.sql').Replace('\', '/')
        Write-Host 'Importing the initial game database schema...'
        Invoke-MariaDb $clientPath $defaultsFile "SOURCE $schemaPath;" | Out-Null
    }

    $membersTableCount = (Invoke-MariaDb $clientPath $defaultsFile "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='global' AND TABLE_NAME='members';" | Select-Object -First 1).Trim()
    if ($membersTableCount -ne '1') {
        throw 'The global database is missing its members table.'
    }
    $membersPrimaryKeyCount = (Invoke-MariaDb $clientPath $defaultsFile "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='global' AND TABLE_NAME='members' AND INDEX_NAME='PRIMARY' AND COLUMN_NAME='UID';" | Select-Object -First 1).Trim()
    if ($membersPrimaryKeyCount -eq '0') {
        Invoke-MariaDb $clientPath $defaultsFile 'ALTER TABLE `global`.`members` ADD PRIMARY KEY (`UID`);' | Out-Null
    }
    Invoke-MariaDb $clientPath $defaultsFile 'ALTER TABLE `global`.`members` MODIFY `UID` int(11) UNSIGNED NOT NULL AUTO_INCREMENT;' | Out-Null
    $membersAutoIncrementCount = (Invoke-MariaDb $clientPath $defaultsFile "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='global' AND TABLE_NAME='members' AND COLUMN_NAME='UID' AND EXTRA LIKE '%auto_increment%';" | Select-Object -First 1).Trim()
    if ($membersAutoIncrementCount -ne '1') {
        throw 'The members.UID column is not configured to generate account identifiers.'
    }

    $safeUser = ConvertTo-SqlString $applicationUser
    $safePassword = ConvertTo-SqlString $applicationPassword
    $accountSql = @"
DROP USER IF EXISTS 'root'@'%';
CREATE USER IF NOT EXISTS '$safeUser'@'localhost' IDENTIFIED VIA mysql_native_password USING PASSWORD('$safePassword');
CREATE USER IF NOT EXISTS '$safeUser'@'127.0.0.1' IDENTIFIED VIA mysql_native_password USING PASSWORD('$safePassword');
ALTER USER '$safeUser'@'localhost' IDENTIFIED VIA mysql_native_password USING PASSWORD('$safePassword');
ALTER USER '$safeUser'@'127.0.0.1' IDENTIFIED VIA mysql_native_password USING PASSWORD('$safePassword');
REVOKE ALL PRIVILEGES, GRANT OPTION FROM '$safeUser'@'localhost';
REVOKE ALL PRIVILEGES, GRANT OPTION FROM '$safeUser'@'127.0.0.1';
GRANT ALL PRIVILEGES ON ``global``.* TO '$safeUser'@'localhost';
GRANT ALL PRIVILEGES ON ``global``.* TO '$safeUser'@'127.0.0.1';
FLUSH PRIVILEGES;
"@
    Invoke-MariaDb $clientPath $defaultsFile $accountSql | Out-Null

    $binding = (Invoke-MariaDb $clientPath $defaultsFile "SELECT @@bind_address;" | Select-Object -First 1).Trim()
    if ($binding -ne '127.0.0.1') {
        throw "MariaDB reported unexpected bind_address '$binding'."
    }
    $compatibleAccountCount = (Invoke-MariaDb $clientPath $defaultsFile "SELECT COUNT(*) FROM mysql.user WHERE User='$safeUser' AND Host IN ('localhost','127.0.0.1') AND plugin='mysql_native_password';" | Select-Object -First 1).Trim()
    $globalPrivilegeCount = (Invoke-MariaDb $clientPath $defaultsFile "SELECT COUNT(*) FROM information_schema.USER_PRIVILEGES WHERE GRANTEE IN ('''$safeUser''@''localhost''','''$safeUser''@''127.0.0.1''') AND PRIVILEGE_TYPE <> 'USAGE';" | Select-Object -First 1).Trim()
    $otherSchemaCount = (Invoke-MariaDb $clientPath $defaultsFile "SELECT COUNT(*) FROM information_schema.SCHEMA_PRIVILEGES WHERE GRANTEE IN ('''$safeUser''@''localhost''','''$safeUser''@''127.0.0.1''') AND TABLE_SCHEMA <> 'global';" | Select-Object -First 1).Trim()
    if ($compatibleAccountCount -ne '2') {
        throw 'The application database accounts are not using the JDBC-compatible mysql_native_password plugin.'
    }
    if ($globalPrivilegeCount -ne '0' -or $otherSchemaCount -ne '0') {
        throw 'The application database account received privileges outside the global schema.'
    }
} finally {
    if (Test-Path -LiteralPath $defaultsFile) {
        Remove-Item -LiteralPath $defaultsFile -Force
    }
}

Write-Host 'MariaDB is ready: automatic Windows service, local-only on 127.0.0.1:3306.'
