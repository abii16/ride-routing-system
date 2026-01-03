@echo off
echo STARTING FULL SYSTEM (Including Web Gateway)
echo.

call "%~dp0build-java.bat"
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

echo 1. Database Service
start "Database" cmd /k "java -cp out;lib\mysql-connector-j-9.2.0.jar services.database.DatabaseServiceServer"
"%SystemRoot%\System32\timeout.exe" /t 2 /nobreak >nul

echo 2. Driver Service
start "Driver Service" cmd /k "java -cp out;lib\mysql-connector-j-9.2.0.jar services.driver.DriverServiceServer"
"%SystemRoot%\System32\timeout.exe" /t 2 /nobreak >nul

echo 3. Dispatch Server
start "Dispatch Server" cmd /k "java -cp out;lib\mysql-connector-j-9.2.0.jar services.dispatch.DispatchServer"
"%SystemRoot%\System32\timeout.exe" /t 2 /nobreak >nul

echo 4. WEB GATEWAY (Port 8080)
start "Web Gateway" cmd /k "java -cp out;lib\mysql-connector-j-9.2.0.jar services.gateway.WebGatewayServer"
"%SystemRoot%\System32\timeout.exe" /t 2 /nobreak >nul

echo.
echo SYSTEM STARTED!
echo Access the Web App at: http://localhost:8080/
echo.
if "%NO_PAUSE%"=="" (
	pause
)
