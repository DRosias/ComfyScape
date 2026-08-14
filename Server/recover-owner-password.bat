@echo off
setlocal EnableExtensions DisableDelayedExpansion
set "WORKSPACE_ROOT=%~dp0"
set "MARIADB_SERVICE=ComfyScapeMariaDB"
cd /d "%WORKSPACE_ROOT%"

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%WORKSPACE_ROOT%setup-local.ps1" -WorkspaceRoot "%WORKSPACE_ROOT%." || goto :failed
call :load_mysql_environment || goto :failed
call :refuse_if_game_is_running || goto :failed
call :ensure_mariadb_service || goto :failed
call :wait_for_database || goto :failed

cd /d "%WORKSPACE_ROOT%Server"
call .\mvnw.cmd package -DskipTests || goto :failed
set "RECOVERY_JAR="
for %%J in (target\*-jar-with-dependencies.jar) do set "RECOVERY_JAR=%%~fJ"
if "%RECOVERY_JAR%"=="" (
    echo The server recovery JAR was not created.
    goto :failed
)
if not exist "%RECOVERY_JAR%" (
    echo The server recovery JAR was not created.
    goto :failed
)

java -cp "%RECOVERY_JAR%" core.tools.OwnerPasswordRecovery "%WORKSPACE_ROOT%config\default.conf"
if errorlevel 1 goto :failed
echo.
echo You can now sign in as igneusowner with the new password.
pause
exit /b 0

:load_mysql_environment
for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%WORKSPACE_ROOT%mysql.env") do set "%%A=%%B"
if "%MYSQL_DATABASE%"=="" exit /b 1
if "%MYSQL_USER%"=="" exit /b 1
if "%MYSQL_PASSWORD%"=="" exit /b 1
if "%MYSQL_ROOT_PASSWORD%"=="" exit /b 1
exit /b 0

:refuse_if_game_is_running
powershell.exe -NoProfile -Command "$client = New-Object Net.Sockets.TcpClient; try { $task = $client.ConnectAsync('127.0.0.1', 43595); if ($task.Wait(500) -and $client.Connected) { exit 0 }; exit 1 } catch { exit 1 } finally { $client.Dispose() }" >nul 2>&1
if errorlevel 1 exit /b 0
echo The game server is running on TCP 43595. Stop it before recovering the owner password.
exit /b 1

:ensure_mariadb_service
sc.exe query "%MARIADB_SERVICE%" >nul 2>&1
if errorlevel 1 (
    echo MariaDB is not installed. Windows will request administrator approval once.
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%WORKSPACE_ROOT%install-mariadb.ps1" -WorkspaceRoot "%WORKSPACE_ROOT%."
    if errorlevel 1 exit /b 1
)
sc.exe query "%MARIADB_SERVICE%" | findstr /C:"STATE" | findstr /C:"RUNNING" >nul 2>&1
if not errorlevel 1 exit /b 0
sc.exe start "%MARIADB_SERVICE%" >nul 2>&1
if not errorlevel 1 exit /b 0
echo MariaDB is stopped. Windows will request administrator approval to start it.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%WORKSPACE_ROOT%install-mariadb.ps1" -WorkspaceRoot "%WORKSPACE_ROOT%."
if errorlevel 1 exit /b 1
exit /b 0

:wait_for_database
for /L %%I in (1,1,60) do (
    powershell.exe -NoProfile -Command "$client = New-Object Net.Sockets.TcpClient; try { $task = $client.ConnectAsync('127.0.0.1', 3306); if ($task.Wait(1000) -and $client.Connected) { exit 0 }; exit 1 } catch { exit 1 } finally { $client.Dispose() }" >nul 2>&1
    if not errorlevel 1 exit /b 0
    timeout /t 2 /nobreak >nul
)
echo MariaDB did not become available on 127.0.0.1:3306 within two minutes.
exit /b 1

:failed
echo.
echo Owner password recovery did not complete. Review the message above.
pause
exit /b 1
