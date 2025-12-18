@echo off
title Ride Sharing System Launcher

echo ===================================================
echo   DISTRIBUTED RIDE SHARING SYSTEM LAUNCHER
echo ===================================================
echo.
echo [1/3] Starting RideSharingServer...
start "Ride Sharing SERVER" cmd /k "java -cp ".;lib/mysql-connector-j-9.2.0.jar" server.RideSharingServer"
timeout /t 2 >nul

echo [2/3] Launching Web Map...
start "" "c:\Users\PC\Documents\disributed\web\index.html"
timeout /t 1 >nul

echo [3/3] Starting Clients...
start "PASSENGER Client" cmd /k "java client.PassengerClient"
start "DRIVER Client" cmd /k "java client.DriverClient"

echo.
echo System is running!
echo  - Server is processing requests.
echo  - Map should be open in your browser.
echo  - Two terminals are open for Passenger and Driver.
echo.
echo To stop, close all terminal windows.
pause
