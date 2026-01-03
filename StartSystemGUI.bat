@echo off
title Ride Sharing System Launcher (GUI Mode)

echo ===================================================
echo   DISTRIBUTED RIDE SHARING SYSTEM LAUNCHER (GUI)
echo ===================================================
echo.
echo [1/3] Starting RideSharingServer...
start "Ride Sharing SERVER" cmd /k "java -cp ".;lib/mysql-connector-j-9.2.0.jar" server.RideSharingServer"
"%SystemRoot%\System32\timeout.exe" /t 2 /nobreak >nul

echo [2/3] Launching Web Map...
start "" "c:\Users\PC\Documents\disributed\web\index.html"
"%SystemRoot%\System32\timeout.exe" /t 1 /nobreak >nul

echo [3/3] Starting GUI Clients...
start "PASSENGER GUI" javaw client.PassengerGUI
start "DRIVER GUI" javaw client.DriverGUI

echo.
echo System is running!
echo  - Server is processing requests.
echo  - Map should be open in your browser.
echo  - Two Java GUI windows should appear for Passenger and Driver.
echo.
echo To stop, close the windows.
pause
