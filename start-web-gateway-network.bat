@echo off
echo Starting Web Gateway with Network Access...
call "%~dp0build-java.bat"
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

java -cp "out;lib\mysql-connector-j-9.2.0.jar" services.gateway.WebGatewayServer
pause
