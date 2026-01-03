@echo off
REM Master start script - starts all services in separate windows
REM Use this for easy startup during development/demo

echo ========================================
echo STARTING DISTRIBUTED RIDE-SHARING SYSTEM
echo ========================================
echo.
echo This will start all 3 services in separate windows.
echo.
echo Services:
echo   1. Database Service (Port 5002)
echo   2. Driver Service (Port 5001, 5003)
echo   3. Dispatch Server (Port 5000)
echo.
echo You can then manually start clients:
echo   - start-passenger-client.bat
echo   - start-driver-client.bat
echo.
pause

echo Starting Database Service...
start "Database Service (Port 5002)" cmd /k "start-database-service.bat"
"%SystemRoot%\System32\timeout.exe" /t 3 /nobreak >nul

echo Starting Driver Service...
start "Driver Service (Port 5001, 5003)" cmd /k "start-driver-service.bat"
"%SystemRoot%\System32\timeout.exe" /t 3 /nobreak >nul

echo Starting Dispatch Server...
start "Dispatch Server (Port 5000)" cmd /k "start-dispatch-server.bat"
"%SystemRoot%\System32\timeout.exe" /t 2 /nobreak >nul

echo.
echo ========================================
echo ALL SERVICES STARTED
echo ========================================
echo.
echo Services are running in separate windows.
echo.
echo Next steps:
echo   1. Run start-driver-client.bat (in a new terminal)
echo   2. Run start-passenger-client.bat (in a new terminal)
echo   3. Open web\map.html in your browser
echo.
echo To stop all services:
echo   - Close each service window
echo   - Or press Ctrl+C in each window
echo.
pause
