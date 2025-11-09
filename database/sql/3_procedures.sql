USE ticketing_db;
DELIMITER //

CREATE PROCEDURE finalize_tickets(
    IN p_ticket_type_id BIGINT,
    IN p_qty INT,
    IN p_related_order_id BIGINT
)
BEGIN
  DECLARE v_total INT;
  DECLARE v_sold  INT;

SELECT capacity_total, capacity_sold
INTO v_total, v_sold
FROM ticket_types
WHERE id = p_ticket_type_id
    FOR UPDATE;

IF (v_total - v_sold) < p_qty THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient inventory';
END IF;

UPDATE ticket_types
SET capacity_sold = capacity_sold + p_qty
WHERE id = p_ticket_type_id;

INSERT INTO inventory_ledger(ticket_type_id, delta_sold, reason, related_order_id)
VALUES (p_ticket_type_id, p_qty, 'FINALIZE_ORDER', p_related_order_id);
END//

DELIMITER ;
