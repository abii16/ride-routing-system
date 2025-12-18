# 🚀 Complete Setup Guide
## Distributed Ride-Sharing Dispatch System

Follow these steps EXACTLY to set up the system.

---

## Step 1: Install Prerequisites

### 1.1 Install JDK
1. Download JDK 8 or higher from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://adoptium.net/)
2. Run installer
3. **Add to PATH:** 
   - Windows: System Properties → Environment Variables → Path → Add `C:\Program Files\Java\jdk-xx\bin`
4. **Verify:**
   ```cmd
   java -version
   javac -version
   ```

### 1.2 Install XAMPP
1. Download from [https://www.apachefriends.org/](https://www.apachefriends.org/)
2. Install to `C:\xampp`
3. Open XAMPP Control Panel
4. Start **MySQL** service
5. **Verify:** Browse to http://localhost/phpmyadmin

### 1.3 Download MySQL Connector
1. Go to [MySQL Connector/J Download](https://dev.mysql.com/downloads/connector/j/)
2. Download **Platform Independent** ZIP
3. Extract `mysql-connector-j-9.2.0.jar`
4. Place in `c:\Users\SW\Downloads\AyuGram Desktop\disributed\disributed\lib\`

---

## Step 2: Setup Database

### 2.1 Using phpMyAdmin
1. Open http://localhost/phpmyadmin
2. Click "Import" tab
3. Choose file: `database/schema.sql`
4. Click "Go"
5. **Verify:** You should see database `ride_sharing_distributed` with 4 tables

### 2.2 Using MySQL Command Line
```cmd
cd "c:\Users\SW\Downloads\AyuGram Desktop\disributed\disributed"
mysql -u root -p < database\schema.sql
```
(Press Enter when prompted for password - XAMPP default has no password)

---

## Step 3: Build the System

```cmd
cd "c:\Users\SW\Downloads\AyuGram Desktop\disributed\disributed"
build.bat
```

**Expected Output:**
```
[1/5] Checking MySQL connector...
[2/5] Compiling common classes...
[3/5] Compiling service classes...
[4/5] Compiling client classes...
[5/5] Build complete!

BUILD SUCCESSFUL
```

**If you see errors:**
- "javac not recognized" → JDK not in PATH (see Step 1.1)
- "package com.mysql.cj.jdbc does not exist" → MySQL connector not in lib folder (see Step 1.3)

---

## Step 4: Run the Distributed System

### 4.1 Start Services (IN ORDER)

**Terminal 1: Database Service**
```cmd
cd "c:\Users\SW\Downloads\AyuGram Desktop\disributed\disributed"
start-database-service.bat
```
Wait for: `[DatabaseService] Ready to accept connections`

**Terminal 2: Driver Service**
```cmd
cd "c:\Users\SW\Downloads\AyuGram Desktop\disributed\disributed"
start-driver-service.bat
```
Wait for: `[DriverService] API server ready on port 5003`

**Terminal 3: Dispatch Server**
```cmd
cd "c:\Users\SW\Downloads\AyuGram Desktop\disributed\disributed"
start-dispatch-server.bat
```
Wait for: `[DispatchServer] Ready to accept passenger connections`

### 4.2 Start Clients

**Terminal 4: Driver Client**
```cmd
cd "c:\Users\SW\Downloads\AyuGram Desktop\disributed\disributed"
start-driver-client.bat
```

A GUI window will appear:
1. Enter Username: `driver1`
2. Enter Password: `pass123`
3. Enter Phone: `0911111111`
4. Latitude: `9.0060` (Addis Ababa)
5. Longitude: `38.7640`
6. Click **Register**
7. Check "Available for Rides"

**Terminal 5: Passenger Client**
```cmd
cd "c:\Users\SW\Downloads\AyuGram Desktop\disributed\disributed"
start-passenger-client.bat
```

A GUI window will appear:
1. Enter Username: `alice`
2. Enter Password: `pass123`
3. Enter Phone: `0922222222`
4. Click **Register** or **Login**
5. Fill in coordinates:
   - Pickup Latitude: `9.0054`
   - Pickup Longitude: `38.7636`
   - Destination Latitude: `9.0100`
   - Destination Longitude: `38.7700`
6. Click **Request Ride**

### 4.3 Open Web Map
1. Open web browser
2. Navigate to: `file:///c:/Users/SW/Downloads/AyuGram Desktop/disributed/disributed/web/map.html`
3. You should see the map with passenger and driver markers

---

## Step 5: Test the System

### Test 1: Single Ride Assignment
1. **Driver:** Logged in, available
2. **Passenger:** Click "Request Ride"
3. **Expected:**
   - Driver gets popup: "New Ride Assignment!"
   - Passenger gets message: "Driver assigned! Driver: driver1"
   - Database has new ride record
   - Map shows both markers

### Test 2: Concurrent Requests (Synchronization Test)
1. **Driver:** 1 driver logged in, available
2. **Passengers:** Start 3 passenger clients
3. **All 3 click "Request Ride" at the SAME time**
4. **Expected:**
   - Only 1 passenger gets the driver
   - Other 2 get "No drivers available"
   - ✅ **This proves synchronization works!**

### Test 3: Driver Disconnect
1. Ongoing ride assigned
2. **Close driver client forcefully** (X button)
3. **Expected:**
   - Driver Service logs: "Driver disconnected"
   - Driver marked unavailable in memory
   - System remains stable

---

## Step 6: Multi-Machine Setup (Optional)

### To Run on Different Computers:

#### Machine 1 (Server - runs ALL services)
IP: `192.168.1.100` (example)

1. Run all 3 services:
   ```cmd
   start-database-service.bat
   start-driver-service.bat
   start-dispatch-server.bat
   ```

2. Allow firewall:
   ```cmd
   netsh advfirewall firewall add rule name="DispatchServer" dir=in action=allow protocol=TCP localport=5000
   netsh advfirewall firewall add rule name="DriverService" dir=in action=allow protocol=TCP localport=5001
   netsh advfirewall firewall add rule name="DriverServiceAPI" dir=in action=allow protocol=TCP localport=5003
   netsh advfirewall firewall add rule name="DatabaseService" dir=in action=allow protocol=TCP localport=5002
   ```

#### Machine 2 (Driver Client)
1. Edit `clients/driver/DriverClientGUI.java`:
   ```java
   private static final String DRIVER_SERVICE_HOST = "192.168.1.100";
   ```

2. Recompile:
   ```cmd
   javac -cp . -d . clients\driver\*.java
   ```

3. Run:
   ```cmd
   start-driver-client.bat
   ```

#### Machine 3 (Passenger Client)
1. Edit `clients/passenger/PassengerClientGUI.java`:
   ```java
   private static final String DISPATCH_HOST = "192.168.1.100";
   ```

2. Recompile:
   ```cmd
   javac -cp . -d . clients\passenger\*.java
   ```

3. Run:
   ```cmd
   start-passenger-client.bat
   ```

#### Machine 4 (Web Viewer)
1. Copy `web/` folder to this machine
2. Edit `web/js/map.js` (if using server-side data export)
3. Open `web/map.html` in browser

---

## Verification Checklist

Before demo/evaluation, verify:

- [ ] All 3 services start without errors
- [ ] Driver client connects successfully
- [ ] Passenger client connects successfully
- [ ] Ride request assigns driver
- [ ] Driver receives ride notification
- [ ] Database has ride record
- [ ] Web map shows locations
- [ ] Concurrent requests handled correctly (only 1 gets driver)
- [ ] Driver disconnect handled gracefully

---

## Common Issues & Solutions

### Issue: "Port already in use"
**Solution:** Another instance is running. Find and stop it:
```cmd
netstat -ano | findstr :5000
taskkill /PID <process_id> /F
```

### Issue: GUI doesn't appear
**Solution:** Missing Java Swing libraries. Reinstall JDK with desktop libraries.

### Issue: "Access denied" to MySQL
**Solution:** XAMPP MySQL credentials:
- Username: `root`
- Password: (empty)
- Edit `services/database/DatabaseManager.java` if different

### Issue: Map doesn't update
**Solution:** 
1. Check if Database Service is exporting `web/data.json`
2. Clear browser cache
3. Check browser console for errors (F12)

---

## Performance Tips

### For Demo
1. **Use sample coordinates in Addis Ababa region**
   - Passenger: 9.0054, 38.7636 (Bole)
   - Driver: 9.0060, 38.7640 (nearby)
   - Destination: 9.0100, 38.7700 (2km away)

2. **Prepare talking points:**
   - "3 independent services demonstrate distributed architecture"
   - "ReentrantLock prevents race conditions"
   - "Can run on different machines"
   - "JSON-based communication is language-agnostic"

3. **Demo sequence:**
   1. Show architecture diagram (ARCHITECTURE.md)
   2. Start services (explain each port and role)
   3. Start 1 driver
   4. Start 1 passenger
   5. Request ride → Show successful assignment
   6. Start 2 more passengers
   7. All request simultaneously → Show only 1 gets driver
   8. Open web map → Show real-time visualization

---

## Next Steps After Setup

1. **Read ARCHITECTURE.md** for deep understanding
2. **Read README.md** for viva defense answers
3. **Test all scenarios** from README
4. **Practice explaining** distributed concepts
5. **Prepare for questions:**
   - Why is this distributed?
   - How does synchronization work?
   - What happens when a service crashes?
   - How would you scale this?

---

**You're all set! This is a university-grade distributed system ready for evaluation.** 🎓
