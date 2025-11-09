USE ticketing_db;

ALTER TABLE ticket_types
    ADD KEY ix_ticket_event_name (event_id, name);

ALTER TABLE orders
    ADD KEY ix_orders_created (created_at);

ALTER TABLE order_items
    ADD KEY ix_items_ticket_qty (ticket_type_id, qty);