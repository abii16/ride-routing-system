CREATE DATABASE IF NOT EXISTS ride_sharing_distributed;
USE ride_sharing_distributed;

-- Passengers Table
CREATE TABLE IF NOT EXISTS passengers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    latitude DOUBLE DEFAULT 0.0,
    longitude DOUBLE DEFAULT 0.0,
    is_login TINYINT(1) DEFAULT 0
);

-- Drivers Table
CREATE TABLE IF NOT EXISTS drivers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    latitude DOUBLE DEFAULT 0.0,
    longitude DOUBLE DEFAULT 0.0,
    is_available TINYINT(1) DEFAULT 1,
    is_login TINYINT(1) DEFAULT 0
);

-- Rides Table
CREATE TABLE IF NOT EXISTS rides (
    id INT AUTO_INCREMENT PRIMARY KEY,
    passenger_id INT NOT NULL,
    driver_id INT DEFAULT NULL,
    status VARCHAR(20) DEFAULT 'REQUESTED', -- REQUESTED, ACCEPTED, STARTED, COMPLETED, CANCELED
    start_latitude DOUBLE NOT NULL,
    start_longitude DOUBLE NOT NULL,
    dest_latitude DOUBLE NOT NULL,
    dest_longitude DOUBLE NOT NULL,
    start_address VARCHAR(255),
    dest_address VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (passenger_id) REFERENCES passengers(id),
    FOREIGN KEY (driver_id) REFERENCES drivers(id)
);

-- Create a Replication User (Optional, if you want manual MySQL replication later)
-- CREATE USER IF NOT EXISTS 'repl'@'%' IDENTIFIED BY 'password';
-- GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';
-- FLUSH PRIVILEGES;

SELECT "Database Setup Complete!" as status;
