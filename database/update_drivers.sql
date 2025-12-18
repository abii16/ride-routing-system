USE ride_sharing_distributed;

-- Update drivers table to include all new requested fields
ALTER TABLE drivers 
ADD COLUMN full_name VARCHAR(100),
ADD COLUMN dob DATE,
ADD COLUMN gender VARCHAR(20),
ADD COLUMN nationality VARCHAR(50),
ADD COLUMN id_number VARCHAR(50),
ADD COLUMN email VARCHAR(100),
ADD COLUMN address TEXT,
ADD COLUMN license_number VARCHAR(50),
ADD COLUMN license_type VARCHAR(50),
ADD COLUMN license_issue_date DATE,
ADD COLUMN license_expiry_date DATE,
ADD COLUMN vehicle_type VARCHAR(50),
ADD COLUMN vehicle_model VARCHAR(50),
ADD COLUMN vehicle_year INT,
ADD COLUMN license_plate VARCHAR(30),
ADD COLUMN status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING';

-- Modify password to be longer just in case
ALTER TABLE drivers MODIFY password VARCHAR(255);

-- Ensure is_available defaults to false until approved
ALTER TABLE drivers MODIFY is_available BOOLEAN DEFAULT FALSE;
