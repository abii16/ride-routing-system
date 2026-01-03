@echo off
setlocal enabledelayedexpansion

echo ========================================
echo BUILDING JAVA SOURCES
echo ========================================
echo.

REM Ensure we run from repo root
cd /d "%~dp0"

set "OUT_DIR=out"
set "LIB_JAR=lib\mysql-connector-j-9.2.0.jar"
set "CP=.;%LIB_JAR%"

if not exist "%LIB_JAR%" (
  echo ERROR: Missing %LIB_JAR%
  echo See lib\DOWNLOAD_CONNECTOR.md
  exit /b 1
)

if exist "%OUT_DIR%" rmdir /s /q "%OUT_DIR%"
mkdir "%OUT_DIR%" >nul

set "SRC_LIST=%TEMP%\java_sources_%RANDOM%%RANDOM%.txt"
del /q "%SRC_LIST%" 2>nul

for /r %%F in (*.java) do (
  echo %%F>>"%SRC_LIST%"
)

REM Compile
javac -encoding UTF-8 -cp "%CP%" -d "%OUT_DIR%" @"%SRC_LIST%"
set "JAVAC_ERR=%ERRORLEVEL%"
del /q "%SRC_LIST%" 2>nul

if not "%JAVAC_ERR%"=="0" (
  echo.
  echo ERROR: Java compilation failed.
  exit /b %JAVAC_ERR%
)

echo.
echo Build OK. Classes written to %OUT_DIR%\
exit /b 0
