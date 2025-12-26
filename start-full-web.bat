@echo off
echo STARTING FULL SYSTEM (Including Web Gateway)
echo.

echo 1. Database Service
start "Database" cmd /k "java -cp .;lib\mysql-connector-j-9.2.0.jar services.database.DatabaseServiceServer"
timeout /t 2

echo 2. Driver Service
start "Driver Service" cmd /k "java -cp .;lib\mysql-connector-j-9.2.0.jar services.driver.DriverServiceServer"
timeout /t 2

echo 3. Dispatch Server
start "Dispatch Server" cmd /k "java -cp .;lib\mysql-connector-j-9.2.0.jar services.dispatch.DispatchServer"
timeout /t 2

echo 4. WEB GATEWAY (Port 8080)
start "Web Gateway" cmd /k "java -cp .;lib\mysql-connector-j-9.2.0.jar services.gateway.WebGatewayServer"
timeout /t 2

echo.
echo SYSTEM STARTED!
echo Access the Web App at: http://localhost:8080/
echo.
pause
