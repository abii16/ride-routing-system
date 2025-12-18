-- Run this on the MASTER PC (PC A)
-- Creates a user 'replica_user' that slave servers can use to login.

CREATE USER 'replica_user'@'%' IDENTIFIED BY 'password';
GRANT REPLICATION SLAVE ON *.* TO 'replica_user'@'%';
FLUSH PRIVILEGES;

-- Show status to get Coordinates (File and Position)
SHOW MASTER STATUS;
