# 🗺️ Project Roadmap
## Distributed Ride-Sharing Dispatch System
(Java + Gebeta Maps + XAMPP/MySQL)

---

## 📍 Stage 1: Concept & Requirements (
**Goal:** Understand the problem and define scope

- Define problem statement (ride-sharing dispatch)
- Identify actors:
  - Passenger
  - Driver
  - Admin
- Define system architecture (client–server)
- Decide technology stack:
  - Java (Server & Clients)
  - Java Sockets / RMI
  - MySQL (XAMPP)
  - Gebeta Maps
- Finalize medium-level feature scope (no payment system)

---

## 🏗️ Stage 2: System Design 
**Goal:** Design how components interact

- Design architecture diagram
- Design use-case diagram
- Design database schema:
  - Users
  - Drivers
  - Passengers
  - Rides
- Define communication protocol between clients and server
- Decide synchronization strategy for driver assignment

---

## 🗄️ Stage 3: Database Implementation 
**Goal:** Set up persistent storage

- Install and configure XAMPP
- Create MySQL database
- Implement tables:
  - passengers
  - drivers
  - rides
- Set driver availability logic
- Test database with sample data
- Connect Java server using JDBC

---

## 🖥️ Stage 4: Server Development 
**Goal:** Build the core distributed logic

- Create Java server application
- Accept multiple client connections
- Implement multithreading
- Handle passenger ride requests
- Track driver availability
- Assign nearest available driver
- Prevent multiple assignments using synchronization
- Update ride status in database

---

## 👨‍💻 Stage 5: Client Development 
**Goal:** Build user-facing applications

### Passenger Client
- Passenger identification/login
- Send location and ride request
- Receive assigned driver details

### Driver Client
- Driver login
- Update current location
- Update availability status
- Receive ride assignment

---

## 🗺️ Stage 6: Gebeta Maps Integration 
**Goal:** Visualize locations and rides

- Integrate Gebeta Maps using HTML & JavaScript
- Display passenger locations
- Display driver locations
- Update markers dynamically
- Show assigned driver–passenger relationship

---

## 🔗 Stage 7: Integration & Communication 
**Goal:** Connect all components together

- Connect Java server with map interface
- Sync map data with database
- Ensure real-time updates 
- Test communication between:
  - Passenger → Server
  - Driver → Server
  - Server → Database
  - Server → Map

---

## 🧪 Stage 8: Testing & Validation 
**Goal:** Ensure correctness and stability

- Run server on one university computer
- Run multiple clients on different computers
- Test concurrent passenger requests
- Verify driver assignment correctness
- Check database consistency
- Handle disconnections gracefully

---

## 🎬 Stage 9: Demo & Presentation 
**Goal:** Prepare for evaluation

- Prepare live demo scenario
- Explain distributed nature of system
- Demonstrate synchronization
- Show map visualization
- Prepare answers for viva questions

---

## 📄 Stage 10: Documentation 
**Goal:** Final academic submission
- Write project report
- Explain algorithms and design decisions
- Highlight distributed system concepts
- Final review and submission 🎓

---

## ✅ Final Outcome
- Fully working distributed system
- Clear separation of server and clients
- Passenger-based ride request model
- University-ready medium-level project
