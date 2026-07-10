@echo off
setlocal
set "APP_HOME=%~dp0"
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%APP_HOME%tools\gradle-bootstrap.ps1" %*
exit /b %ERRORLEVEL%
