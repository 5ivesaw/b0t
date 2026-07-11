@echo off
setlocal
cd /d "%~dp0"
py -3 -m pip install -r requirements-training.txt || exit /b 1
py -3 generate_teacher_data.py || exit /b 1
py -3 train_waypoint.py || exit /b 1
py -3 evaluate_waypoint.py || exit /b 1
echo.
echo Waypoint dataset, checkpoint, and evaluation rebuilt.
pause
