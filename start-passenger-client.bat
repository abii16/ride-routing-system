@echo off
REM Start Passenger Client GUI
REM Can run on ANY machine (just change DISPATCH_HOST in code if needed)

echo ========================================
echo STARTING PASSENGER CLIENT
echo ========================================
echo Connects to: Dispatch Server (port 5000)
echo ========================================
echo.

java -cp . clients.passenger.PassengerClientGUI

pause
