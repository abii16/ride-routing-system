@echo off
REM Start Driver Client GUI
REM Can run on ANY machine (just change DRIVER_SERVICE_HOST in code if needed)

echo ========================================
echo STARTING DRIVER CLIENT
echo ========================================
echo Connects to: Driver Service (port 5001)
echo ========================================
echo.

java -cp . clients.driver.DriverClientGUI

pause
