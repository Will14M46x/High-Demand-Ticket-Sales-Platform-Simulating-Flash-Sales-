/* This script sets up the entire database environment.
  Run this full script in MySQL Workbench to create all
  schemas, tables, and sample data.
*/

-- --- 1. Inventory Service ---
CREATE DATABASE IF NOT EXISTS inventory_db;
USE inventory_db;

CREATE TABLE events (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        name VARCHAR(255) NOT NULL,
                        total_tickets INT NOT NULL,
                        available_tickets INT NOT NULL,
                        version INT NOT NULL DEFAULT 0,
                        sale_start_time DATETIME NOT NULL,
                        CONSTRAINT chk_tickets CHECK (available_tickets <= total_tickets)
);

-- Insert sample event data
INSERT INTO events (name, total_tickets, available_tickets, sale_start_time)
VALUES ('Flash Sale Concert', 1000, 1000, '2025-12-01 10:00:00');


-- --- 2. Booking Service ---
CREATE DATABASE IF NOT EXISTS order_db;
USE order_db;

CREATE TABLE orders (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        user_id BIGINT NOT NULL,
                        event_id BIGINT NOT NULL,
                        quantity INT NOT NULL,
                        total_amount DECIMAL(10, 2) NOT NULL,
                        status VARCHAR(50) NOT NULL,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
