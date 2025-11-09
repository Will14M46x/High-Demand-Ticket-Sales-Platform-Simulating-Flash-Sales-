USE ticketing_db;

INSERT INTO customers(email, display_name)
VALUES ('dev@example.com', 'Dev Person');

INSERT INTO venues(name, city, country) VALUES ('Starlight Arena','Dublin','IE');

INSERT INTO events(venue_id, name, starts_at, status)
VALUES (1, 'Nova Tour – Opening Night', '2026-01-15 20:00:00', 'ON_SALE');

INSERT INTO ticket_types(event_id, name, price_cents, currency, capacity_total)
VALUES (1, 'General Admission', 12000, 'EUR', 5000),
       (1, 'VIP',               30000, 'EUR',  250);
