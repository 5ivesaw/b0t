@echo off
setlocal
title SawBotV1 Phase 4 Dummy Model
cd /d "%~dp0"
where py.exe >nul 2>nul
if not errorlevel 1 (
  py.exe -3 dummy_model.py --mode interactive
) else (
  python.exe dummy_model.py --mode interactive
)
echo.
pause
