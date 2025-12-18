# ✅ TODO List
## Distributed Ride-Sharing Dispatch System
(Java + Gebeta Maps + XAMPP/MySQL)

---

## 🧠 Stage 1: Concept & Requirements
- [ ] Write clear problem statement
- [ ] Identify actors (Passenger, Driver, Admin)
- [ ] Define client–server architecture
- [ ] Decide communication method (Java Sockets or RMI)
- [ ] Finalize technology stack:
  - [ ] Java (Server & Clients)
  - [ ] MySQL via XAMPP
  - [ ] Gebeta Maps
- [ ] Limit features to medium level (no payments, no accounts recovery)

---

## 🏗️ Stage 2: System Design
- [ ] Draw system architecture diagram
- [ ] Create use-case diagram
- [ ] Design database schema
- [ ] Define entities:
  - [ ] Passenger
  - [ ] Driver
  - [ ] Ride
- [ ] Define ride lifecycle (REQUESTED, ASSIGNED, COMPLETED)
- [ ] Design communication protocol (message formats)
- [ ] Decide synchronization mechanism for driver assignment

---

## 🗄️ Stage 3: Database Implementation (XAMPP / MySQL)
- [ ] Install XAMPP
- [ ] Start Apache and MySQL services
- [ ] Create database `ride_sharing_system`
- [x] Create tables (Schema script created in `database/schema.sql`):
  - [x] passengers
  - [x] drivers
  - [x] rides
- [ ] Implement driver availability status
- [ ] Insert test data
- [x] Connect Java server to MySQL using JDBC
- [x] Verify database operations (insert, update, select)

---

## 🖥️ Stage 4: Server Development (Java)
- [x] Create Java server project
- [x] Implement socket/RMI server
- [x] Accept multiple client connections
- [x] Implement multithreading (one thread per client)
- [ ] Handle passenger ride requests
- [ ] Track available drivers
- [ ] Calculate nearest driver
- [ ] Assign driver to passenger
- [ ] Prevent multiple assignments (synchronization)
- [ ] Update ride status in database

---

## 👨‍💻 Stage 5: Client Development
### Passenger Client
- [x] Create passenger client interface (CMD)
- [ ] Passenger identification/login
- [ ] Send location to server
- [ ] Send ride request
- [ ] Receive assigned driver details

### Driver Client
- [x] Create driver client interface (CMD)
- [ ] Driver login
- [ ] Send current location
- [ ] Update availability status
- [ ] Receive ride assignment
- [ ] Update ride status (started/completed)

---

## 🗺️ Stage 6: Gebeta Maps Integration
- [ ] Create HTML + JavaScript map interface
- [ ] Load Gebeta Maps tiles
- [ ] Display passenger markers
- [ ] Display driver markers
- [ ] Update markers dynamically
- [ ] Show assigned passenger–driver link
- [ ] Test map rendering with sample coordinates

---

## 🔗 Stage 7: Integration & Communication
- [ ] Connect Java server with map interface
- [ ] Expose location and ride data
- [ ] Fetch data using JavaScript
- [ ] Sync map data with database
- [ ] Ensure real-time updates
- [ ] Test full communication flow:
  - [ ] Passenger → Server
  - [ ] Driver → Server
  - [ ] Server → Database
  - [ ] Server → Map

---

## 🧪 Stage 8: Testing & Validation
- [ ] Run server on one university computer
- [ ] Run passenger and driver clients on different computers
- [ ] Test multiple passenger requests at the same time
- [ ] Verify driver is assigned only once
- [ ] Check database consistency
- [ ] Handle client disconnections safely
- [ ] Fix bugs and edge cases

---

## 🎬 Stage 9: Demo & Presentation
- [ ] Prepare demo scenario (2 passengers, 2 drivers)
- [ ] Demonstrate concurrent requests
- [ ] Show synchronization in action
- [ ] Explain distributed architecture
- [ ] Show Gebeta Maps visualization
- [ ] Prepare viva answers

---

## 📄 Stage 10: Documentation & Submission
- [ ] Write project abstract
- [ ] Describe system architecture
- [ ] Explain algorithms (driver selection, synchronization)
- [ ] Explain distributed system concepts used
- [ ] Add screenshots
- [ ] Final review
- [ ] Submit project 🎓

---

## ✅ Final Checklist
- [ ] Distributed system confirmed
- [ ] Server–client separation clear
- [ ] Database integrated
- [ ] Gebeta Maps working
- [ ] Medium-level complexity maintained
- [ ] Ready for university evaluation
