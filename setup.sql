CREATE DATABASE IF NOT EXISTS ride_sharing_distributed;
USE ride_sharing_distributed;

-- 1. Passengers Table
CREATE TABLE IF NOT EXISTS passengers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    latitude DOUBLE,
    longitude DOUBLE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Drivers Table
CREATE TABLE IF NOT EXISTS drivers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    latitude DOUBLE,
    longitude DOUBLE,
    is_available BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Rides Table
CREATE TABLE IF NOT EXISTS rides (
    id INT AUTO_INCREMENT PRIMARY KEY,
    passenger_id INT NOT NULL,
    driver_id INT,
    start_latitude DOUBLE,
    start_longitude DOUBLE,
    dest_latitude DOUBLE,
    dest_longitude DOUBLE,
    status VARCHAR(20) DEFAULT 'REQUESTED', 
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (passenger_id) REFERENCES passengers(id),
    FOREIGN KEY (driver_id) REFERENCES drivers(id)
);

-- 4. Ride Status History
CREATE TABLE IF NOT EXISTS ride_status_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ride_id INT NOT NULL,
    status VARCHAR(20),
    latitude DOUBLE,
    longitude DOUBLE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ride_id) REFERENCES rides(id)
);

-- 5. Admins Table (NEW)
CREATE TABLE IF NOT EXISTS admins (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Initial Data


-- Default Admin
INSERT IGNORE INTO admins (username, password) VALUES ('admin', 'admin123');
