@echo off
title SawBotV1 Latest Telemetry Test
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0test-latest-telemetry.ps1"
echo.
pause
