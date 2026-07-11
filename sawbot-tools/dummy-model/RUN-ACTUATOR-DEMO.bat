@echo off
setlocal
title SawBotV1 Phase 4 Actuator Demo
cd /d "%~dp0"
where py.exe >nul 2>nul
if not errorlevel 1 (
  py.exe -3 dummy_model.py --mode demo
) else (
  python.exe dummy_model.py --mode demo
)
echo.
pause
