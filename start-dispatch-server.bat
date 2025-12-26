@echo off
REM Start Dispatch Server (Port 5000)
REM This service must be started THIRD (after Database and Driver services)

echo ========================================
echo STARTING DISPATCH SERVER
echo ========================================
echo Port: 5000 (for passenger clients)
echo Role: Orchestrates ride requests and driver assignment
echo ========================================
echo.

echo Make sure these services are running:
echo   - Database Service (port 5002)
echo   - Driver Service (ports 5001, 5003)
echo.
pause

java -cp .;lib\mysql-connector-j-9.2.0.jar services.dispatch.DispatchServer

pause
