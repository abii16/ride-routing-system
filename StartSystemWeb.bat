@echo off
title Ride Sharing System Launcher (WEB Mode)

echo ===================================================
echo   DISTRIBUTED RIDE SHARING SYSTEM LAUNCHER (WEB)
echo ===================================================
echo.
echo [1/3] Starting RideSharingServer (Hybrid Socket/HTTP)...
start "Ride Sharing SERVER" cmd /k "java -cp ".;lib/mysql-connector-j-9.2.0.jar" server.RideSharingServer"
"%SystemRoot%\System32\timeout.exe" /t 3 /nobreak >nul

echo [2/3] Launching Web Map Dashboard...
start "" "c:\Users\PC\Documents\disributed\web\index.html"
"%SystemRoot%\System32\timeout.exe" /t 1 /nobreak >nul

echo [3/3] Launching Web Clients...
start "" "c:\Users\PC\Documents\disributed\web\passenger.html"
start "" "c:\Users\PC\Documents\disributed\web\driver.html"

echo.
echo System is running!
echo  - Server is processing requests on Port 5000.
echo  - 3 Browser Tabs should be open (Map, Passenger, Driver).
echo.
echo NOTE: Since these are local HTML files, you might need to
echo allow CORS or use a proper web server extension if Fetch fails.
echo.
pause
