@echo off
REM Build script for Distributed Ride-Sharing System
REM Compiles all Java source files

echo ========================================
echo BUILDING DISTRIBUTED RIDE-SHARING SYSTEM
echo ========================================
echo.

REM Check if JDK is installed
where javac >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: javac not found in PATH
    echo Please install JDK and add it to your PATH
    pause
    exit /b 1
)

echo [1/5] Checking MySQL connector...
if not exist "lib\mysql-connector-j-9.2.0.jar" (
    echo WARNING: MySQL connector not found in lib directory
    echo Please download mysql-connector-j-9.2.0.jar and place it in the lib folder
    pause
)

echo [2/5] Compiling common classes...
javac -d . common\*.java
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to compile common classes
    pause
    exit /b 1
)

echo [3/5] Compiling service classes...
javac -cp ".;lib\mysql-connector-j-9.2.0.jar" -d . services\database\*.java
javac -cp ".;lib\mysql-connector-j-9.2.0.jar" -d . services\driver\*.java
javac -cp ".;lib\mysql-connector-j-9.2.0.jar" -d . services\dispatch\*.java
javac -cp ".;lib\mysql-connector-j-9.2.0.jar" -d . services\gateway\*.java
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to compile service classes
    pause
    exit /b 1
)

echo [4/5] Compiling client classes...
javac -cp . -d . clients\passenger\*.java
javac -cp . -d . clients\driver\*.java
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to compile client classes
    pause
    exit /b 1
)

echo [5/5] Build complete!
echo.
echo ========================================
echo BUILD SUCCESSFUL
echo ========================================
echo.
echo All Java files compiled successfully.
echo You can now run the distributed system using the start-*.bat scripts.
echo.
echo Run order:
echo   1. start-database-service.bat
echo   2. start-driver-service.bat
echo   3. start-dispatch-server.bat
echo   4. start-passenger-client.bat (on any machine)
echo   5. start-driver-client.bat (on any machine)
echo   6. Open web\map.html in browser
echo.
pause
