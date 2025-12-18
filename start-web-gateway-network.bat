@echo off
echo Starting Web Gateway with Network Access...
java -cp ".;lib\mysql-connector-j-9.2.0.jar" services.gateway.WebGatewayServer
pause
