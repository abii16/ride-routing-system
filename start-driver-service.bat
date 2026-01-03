@echo off
REM Start Driver Service (Port 5001 for clients, Port 5003 for API)
REM This service must be started SECOND (after Database Service)

echo ========================================
echo STARTING DRIVER SERVICE
echo ========================================
echo Client Port: 5001 (for driver clients)
echo API Port: 5003 (for Dispatch Server queries)
echo Role: Manages driver connections and availability
echo ========================================
echo.

echo Make sure Database Service is running on port 5002
echo.
pause

call "%~dp0build-java.bat"
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

java -cp out;lib\mysql-connector-j-9.2.0.jar services.driver.DriverServiceServer

pause
