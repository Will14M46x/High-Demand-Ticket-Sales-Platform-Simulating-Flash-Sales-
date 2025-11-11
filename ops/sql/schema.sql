-- Create fresh database
DROP DATABASE IF EXISTS ticketing_db;
CREATE DATABASE ticketing_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ticketing_db;

CREATE TABLE customers (
                           id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                           email VARCHAR(255) NULL,
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           PRIMARY KEY (id),
                           KEY ix_customers_email (email)
) ENGINE=InnoDB;

CREATE TABLE venues (
                        id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                        name VARCHAR(255) NOT NULL,
                        address VARCHAR(255),
                        city VARCHAR(128),
                        country VARCHAR(64),
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (id),
                        UNIQUE KEY ux_venues_name (name)
) ENGINE=InnoDB;

CREATE TABLE events (
                        id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                        venue_id BIGINT UNSIGNED NOT NULL,
                        name VARCHAR(255) NOT NULL,
                        starts_at DATETIME NOT NULL,
                        ends_at DATETIME NULL,
                        status ENUM('DRAFT','ON_SALE','SUSPENDED','ENDED') NOT NULL DEFAULT 'DRAFT',
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        PRIMARY KEY (id),
                        KEY ix_events_status_starts (status, starts_at),
                        CONSTRAINT fk_events_venue FOREIGN KEY (venue_id) REFERENCES venues(id)
) ENGINE=InnoDB;

CREATE TABLE ticket_types (
                              id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                              event_id BIGINT UNSIGNED NOT NULL,
                              name VARCHAR(100) NOT NULL,
                              price_cents INT UNSIGNED NOT NULL,
                              currency CHAR(3) NOT NULL DEFAULT 'EUR',
                              capacity_total INT UNSIGNED NOT NULL,
                              capacity_sold INT UNSIGNED NOT NULL DEFAULT 0,
                              capacity_held INT UNSIGNED NOT NULL DEFAULT 0,
                              version INT UNSIGNED NOT NULL DEFAULT 0,
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              PRIMARY KEY (id),
                              KEY ix_tickets_event (event_id),
                              CONSTRAINT ck_capacity CHECK (capacity_sold + capacity_held <= capacity_total),
                              CONSTRAINT fk_ticket_event FOREIGN KEY (event_id) REFERENCES events(id)
) ENGINE=InnoDB;

CREATE TABLE inventory_ledger (
                                  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                  ticket_type_id BIGINT UNSIGNED NOT NULL,
                                  delta_sold INT NOT NULL,
                                  reason ENUM('FINALIZE_ORDER','MANUAL_ADJUST') NOT NULL,
                                  related_order_id BIGINT UNSIGNED NULL,
                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  PRIMARY KEY (id),
                                  KEY ix_ledger_ticket (ticket_type_id, created_at),
                                  CONSTRAINT fk_ledger_ticket FOREIGN KEY (ticket_type_id) REFERENCES ticket_types(id)
) ENGINE=InnoDB;

CREATE TABLE orders (
                        id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                        customer_id BIGINT UNSIGNED NOT NULL,
                        status ENUM('PENDING_PAYMENT','PAID','CANCELLED','EXPIRED') NOT NULL,
                        total_cents INT UNSIGNED NOT NULL,
                        currency CHAR(3) NOT NULL DEFAULT 'EUR',
                        idempotency_key CHAR(36) NOT NULL,
                        payment_ref VARCHAR(128) NULL,
                        payment_payload JSON NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        PRIMARY KEY (id),
                        UNIQUE KEY ux_orders_idempotency (idempotency_key),
                        KEY ix_orders_customer_status (customer_id, status),
                        CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
) ENGINE=InnoDB;

CREATE TABLE order_items (
                             id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                             order_id BIGINT UNSIGNED NOT NULL,
                             ticket_type_id BIGINT UNSIGNED NOT NULL,
                             qty INT UNSIGNED NOT NULL,
                             price_cents INT UNSIGNED NOT NULL,
                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             PRIMARY KEY (id),
                             KEY ix_items_order (order_id),
                             KEY ix_items_ticket (ticket_type_id),
                             CONSTRAINT fk_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
                             CONSTRAINT fk_items_ticket FOREIGN KEY (ticket_type_id) REFERENCES ticket_types(id)
) ENGINE=InnoDB;

CREATE TABLE outbox_events (
                               id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                               aggregate VARCHAR(64) NOT NULL,
                               aggregate_id BIGINT UNSIGNED NOT NULL,
                               type VARCHAR(128) NOT NULL,
                               payload JSON NOT NULL,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               published_at TIMESTAMP NULL,
                               PRIMARY KEY (id),
                               KEY ix_outbox_pub (published_at)
) ENGINE=InnoDB;

-- Optional: reservation audit table for hold/confirm/release/expire trail
CREATE TABLE reservation_holds (
                                   id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                   hold_id CHAR(36) NOT NULL,
                                   customer_id BIGINT UNSIGNED NOT NULL,
                                   ticket_type_id BIGINT UNSIGNED NOT NULL,
                                   qty INT UNSIGNED NOT NULL,
                                   action ENUM('HOLD','CONFIRM','RELEASE','EXPIRE') NOT NULL,
                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   PRIMARY KEY (id),
                                   UNIQUE KEY uk_hold_id (hold_id),
                                   KEY ix_customer_id (customer_id),
                                   KEY ix_ticket_type_id (ticket_type_id),
                                   KEY ix_created_at (created_at),
                                   CONSTRAINT fk_holds_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
                                   CONSTRAINT fk_holds_ticket_type FOREIGN KEY (ticket_type_id) REFERENCES ticket_types(id)
) ENGINE=InnoDB;

-- Seed minimal data
INSERT INTO venues (name, city, country) VALUES ('Main Hall', 'Limerick', 'IE');
INSERT INTO events (venue_id, name, starts_at, status) VALUES (1, 'Concert A', NOW() + INTERVAL 7 DAY, 'ON_SALE');
INSERT INTO ticket_types (event_id, name, price_cents, capacity_total, capacity_sold, capacity_held)
VALUES (1, 'General Admission', 7500, 1000, 0, 0);
