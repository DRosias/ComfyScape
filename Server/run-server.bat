@echo off
setlocal EnableExtensions DisableDelayedExpansion
set "WORKSPACE_ROOT=%~dp0"
set "MARIADB_SERVICE=ComfyScapeMariaDB"
cd /d "%WORKSPACE_ROOT%"

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%WORKSPACE_ROOT%setup-local.ps1" -WorkspaceRoot "%WORKSPACE_ROOT%." || exit /b 1
call :load_mysql_environment || exit /b 1
call :ensure_mariadb_service || exit /b 1
call :wait_for_database || exit /b 1

rem Used only if igneusowner does not yet exist. It is never written to disk.
set "COMFYSCAPE_BOOTSTRAP_OWNER_PASSWORD=changeme"

cd /d "%WORKSPACE_ROOT%Server"
if NOT exist hasRan.txt (
    call .\mvnw.cmd clean || exit /b 1
    copy NUL hasRan.txt >nul
)
call .\mvnw.cmd package -DskipTests || exit /b 1
xcopy /Y target\*-with-dependencies.jar server.jar* >nul || exit /b 1

echo.
echo MariaDB is listening only on 127.0.0.1:3306 and will remain running after the game exits.
echo On the first launch, sign in as igneusowner and change the bootstrap password with ::resetpassword before opening TCP 43595 publicly.
java -jar server.jar "%WORKSPACE_ROOT%config\default.conf"
exit /b %ERRORLEVEL%

:load_mysql_environment
for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%WORKSPACE_ROOT%mysql.env") do set "%%A=%%B"
if "%MYSQL_DATABASE%"=="" (
    echo mysql.env is missing MYSQL_DATABASE.
    exit /b 1
)
if "%MYSQL_USER%"=="" (
    echo mysql.env is missing MYSQL_USER.
    exit /b 1
)
if "%MYSQL_PASSWORD%"=="" (
    echo mysql.env is missing MYSQL_PASSWORD.
    exit /b 1
)
if "%MYSQL_ROOT_PASSWORD%"=="" (
    echo mysql.env is missing MYSQL_ROOT_PASSWORD.
    exit /b 1
)
exit /b 0

:ensure_mariadb_service
sc.exe query "%MARIADB_SERVICE%" >nul 2>&1
if errorlevel 1 (
    echo MariaDB is not installed. Windows will request administrator approval once.
    call :run_mariadb_helper || exit /b 1
)
sc.exe query "%MARIADB_SERVICE%" | findstr /C:"STATE" | findstr /C:"RUNNING" >nul 2>&1
if not errorlevel 1 exit /b 0

sc.exe start "%MARIADB_SERVICE%" >nul 2>&1
if not errorlevel 1 exit /b 0

echo MariaDB is stopped. Windows will request administrator approval to start it.
call :run_mariadb_helper || exit /b 1
exit /b 0

:run_mariadb_helper
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%WORKSPACE_ROOT%install-mariadb.ps1" -WorkspaceRoot "%WORKSPACE_ROOT%."
if errorlevel 1 (
    echo MariaDB setup did not complete. Review the message above, then run run-server.bat again.
    exit /b 1
)
exit /b 0

:wait_for_database
for /L %%I in (1,1,60) do (
    powershell.exe -NoProfile -Command "$client = New-Object Net.Sockets.TcpClient; try { $task = $client.ConnectAsync('127.0.0.1', 3306); if ($task.Wait(1000) -and $client.Connected) { exit 0 }; exit 1 } catch { exit 1 } finally { $client.Dispose() }" >nul 2>&1
    if not errorlevel 1 exit /b 0
    timeout /t 2 /nobreak >nul
)
echo MariaDB did not become available on 127.0.0.1:3306 within two minutes.
exit /b 1
