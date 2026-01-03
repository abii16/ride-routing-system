@echo off
REM Start Database Service (Port 5002)
REM This service must be started FIRST

echo ========================================
echo STARTING DATABASE SERVICE
echo ========================================
echo Port: 5002
echo Role: Handles all database operations
echo ========================================
echo.

REM Check if MySQL is running
echo Checking if MySQL is running...
netstat -an | find ":3306" >nul
if %ERRORLEVEL% NEQ 0 (
    echo WARNING: MySQL does not appear to be running on port 3306
    echo Please start XAMPP MySQL before running this service
    echo.
    pause
    exit /b 1
)

echo MySQL is running.
echo Starting Database Service...
echo.

call "%~dp0build-java.bat"
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

java -cp out;lib\mysql-connector-j-9.2.0.jar services.database.DatabaseServiceServer

pause
