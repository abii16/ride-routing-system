-- Distributed Ride-Sharing Dispatch System Database Schema
-- MySQL/XAMPP Database

-- Create database
CREATE DATABASE IF NOT EXISTS ride_sharing_distributed;
USE ride_sharing_distributed;

-- Table: passengers
CREATE TABLE IF NOT EXISTS passengers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    latitude DOUBLE DEFAULT NULL,
    longitude DOUBLE DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_location (latitude, longitude)
);

-- Table: drivers
CREATE TABLE IF NOT EXISTS drivers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    latitude DOUBLE DEFAULT NULL,
    longitude DOUBLE DEFAULT NULL,
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_available (is_available),
    INDEX idx_location (latitude, longitude)
);

-- Table: rides
CREATE TABLE IF NOT EXISTS rides (
    id INT AUTO_INCREMENT PRIMARY KEY,
    passenger_id INT NOT NULL,
    driver_id INT NOT NULL,
    start_latitude DOUBLE NOT NULL,
    start_longitude DOUBLE NOT NULL,
    dest_latitude DOUBLE DEFAULT NULL,
    dest_longitude DOUBLE DEFAULT NULL,
    status ENUM('REQUESTED', 'ASSIGNED', 'STARTED', 'COMPLETED', 'CANCELLED') DEFAULT 'REQUESTED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    FOREIGN KEY (passenger_id) REFERENCES passengers(id) ON DELETE CASCADE,
    FOREIGN KEY (driver_id) REFERENCES drivers(id) ON DELETE CASCADE,
    INDEX idx_status (status),
    INDEX idx_passenger (passenger_id),
    INDEX idx_driver (driver_id)
);

-- Table: ride_status_history
CREATE TABLE IF NOT EXISTS ride_status_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ride_id INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    latitude DOUBLE DEFAULT NULL,
    longitude DOUBLE DEFAULT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ride_id) REFERENCES rides(id) ON DELETE CASCADE,
    INDEX idx_ride (ride_id),
    INDEX idx_timestamp (timestamp)
);

-- Insert sample test data
INSERT IGNORE INTO passengers (username, password, phone, latitude, longitude) VALUES
('alice', 'pass123', '0911111111', 9.0054, 38.7636),
('bob', 'pass123', '0922222222', 9.0100, 38.7700),
('charlie', 'pass123', '0933333333', 9.0080, 38.7650);

INSERT IGNORE INTO drivers (username, password, phone, latitude, longitude, is_available) VALUES
('driver1', 'pass123', '0944444444', 9.0060, 38.7640, TRUE),
('driver2', 'pass123', '0955555555', 9.0090, 38.7680, TRUE),
('driver3', 'pass123', '0966666666', 9.0070, 38.7660, TRUE);

-- Display tables
SHOW TABLES;

-- Display sample data
SELECT 'Passengers:' as '';
SELECT * FROM passengers;

SELECT 'Drivers:' as '';
SELECT * FROM drivers;

SELECT 'Database setup complete!' as '';
