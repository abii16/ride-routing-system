# Network Bootstrap Installer for Distributed Ride Share System
# Run this on PC B and PC C to automatically download and setup everything

param(
    [Parameter(Mandatory=$true)]
    [string]$ServerIP  # IP address of PC A
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Distributed Ride Share - Network Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Connecting to PC A at: $ServerIP" -ForegroundColor Yellow
Write-Host ""

# Create installation directory
$InstallDir = "C:\RideShare"
if (!(Test-Path $InstallDir)) {
    New-Item -ItemType Directory -Path $InstallDir | Out-Null
}

# Step 1: Download Application Files
Write-Host "[1/4] Downloading application from PC A..." -ForegroundColor Green
$AppUrl = "http://${ServerIP}:8080/download/app.zip"
$AppZip = "$InstallDir\app.zip"

try {
    Invoke-WebRequest -Uri $AppUrl -OutFile $AppZip -UseBasicParsing
    Expand-Archive -Path $AppZip -DestinationPath "$InstallDir\app" -Force
    Remove-Item $AppZip
    Write-Host "   ✓ Application downloaded" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Failed to download application" -ForegroundColor Red
    Write-Host "   Error: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Alternative: Manual network share method" -ForegroundColor Yellow
    Write-Host "1. On PC A, share the folder: C:\xampp\htdocs\disributed" -ForegroundColor Yellow
    Write-Host "2. On this PC, run:" -ForegroundColor Yellow
    Write-Host "   xcopy /E /I \\$ServerIP\disributed C:\RideShare\app" -ForegroundColor Cyan
    exit 1
}

# Step 2: Download Portable Java
Write-Host "[2/4] Downloading Portable Java..." -ForegroundColor Green
$JavaUrl = "https://download.oracle.com/java/17/latest/jdk-17_windows-x64_bin.zip"
# Alternative: Use Adoptium
$JavaUrl = "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse"
$JavaZip = "$InstallDir\java.zip"

try {
    Write-Host "   Downloading from Adoptium (this may take a few minutes)..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri $JavaUrl -OutFile $JavaZip -UseBasicParsing
    Expand-Archive -Path $JavaZip -DestinationPath "$InstallDir" -Force
    # Rename to standard name
    $JavaFolder = Get-ChildItem "$InstallDir\jdk*" | Select-Object -First 1
    if ($JavaFolder) {
        Rename-Item $JavaFolder.FullName "$InstallDir\jdk"
    }
    Remove-Item $JavaZip
    Write-Host "   ✓ Java installed" -ForegroundColor Green
} catch {
    Write-Host "   ⚠ Java download failed (will try system Java)" -ForegroundColor Yellow
}

# Step 3: Download Portable MySQL
Write-Host "[3/4] Setting up Portable MySQL..." -ForegroundColor Green
$MySQLUrl = "https://dev.mysql.com/get/Downloads/MySQL-8.0/mysql-8.0.40-winx64.zip"
$MySQLZip = "$InstallDir\mysql.zip"

try {
    Write-Host "   Downloading MySQL (this may take 5-10 minutes)..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri $MySQLUrl -OutFile $MySQLZip -UseBasicParsing
    Expand-Archive -Path $MySQLZip -DestinationPath "$InstallDir" -Force
    $MySQLFolder = Get-ChildItem "$InstallDir\mysql*" | Select-Object -First 1
    if ($MySQLFolder) {
        Rename-Item $MySQLFolder.FullName "$InstallDir\mysql"
    }
    Remove-Item $MySQLZip
    
    # Initialize MySQL
    Write-Host "   Initializing MySQL database..." -ForegroundColor Yellow
    & "$InstallDir\mysql\bin\mysqld.exe" --initialize-insecure --datadir="$InstallDir\mysql\data" 2>$null
    Write-Host "   ✓ MySQL installed" -ForegroundColor Green
} catch {
    Write-Host "   ⚠ MySQL download failed" -ForegroundColor Yellow
    Write-Host "   You'll need to install XAMPP manually or copy MySQL from PC A" -ForegroundColor Yellow
}

# Step 4: Create startup script
Write-Host "[4/4] Creating startup script..." -ForegroundColor Green

$StartupScript = @"
@echo off
title Ride Share - Auto-Syncing Server

echo Starting MySQL...
start /MIN `"`" `"$InstallDir\mysql\bin\mysqld.exe`" --console --datadir=`"$InstallDir\mysql\data`"
timeout /t 5 /nobreak > nul

echo Starting Ride Share System...
cd /d `"$InstallDir\app`"
call start-portable.bat

pause
"@

$StartupScript | Out-File "$InstallDir\START.bat" -Encoding ASCII

Write-Host "   ✓ Startup script created" -ForegroundColor Green

# Step 5: Configure firewall
Write-Host ""
Write-Host "Configuring Windows Firewall..." -ForegroundColor Green
try {
    netsh advfirewall firewall add rule name="RideShare Discovery" dir=in action=allow protocol=UDP localport=8888 2>$null
    netsh advfirewall firewall add rule name="RideShare Sync" dir=in action=allow protocol=TCP localport=5002 2>$null
    netsh advfirewall firewall add rule name="RideShare Web" dir=in action=allow protocol=TCP localport=8080 2>$null
    Write-Host "   ✓ Firewall configured" -ForegroundColor Green
} catch {
    Write-Host "   ⚠ Firewall configuration requires admin rights" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Installation Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "To start the server, run:" -ForegroundColor Yellow
Write-Host "   $InstallDir\START.bat" -ForegroundColor Cyan
Write-Host ""
Write-Host "The server will automatically:" -ForegroundColor Yellow
Write-Host "  • Discover PC A at $ServerIP" -ForegroundColor White
Write-Host "  • Sync all data automatically" -ForegroundColor White
Write-Host "  • Provide backup if PC A fails" -ForegroundColor White
Write-Host ""

# Ask if user wants to start now
$response = Read-Host "Start the server now? (Y/N)"
if ($response -eq 'Y' -or $response -eq 'y') {
    Start-Process "$InstallDir\START.bat"
}
"@

$InstallDir\app\setup_database.sql
