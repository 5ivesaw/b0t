@echo off
setlocal
cd /d "%~dp0"
where py.exe >nul 2>nul
if not errorlevel 1 (py.exe -3 waypoint_model.py & goto :done)
python.exe waypoint_model.py
:done
echo.
pause
