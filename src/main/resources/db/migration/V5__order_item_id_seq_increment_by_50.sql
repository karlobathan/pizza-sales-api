-- Match the JPA sequence allocationSize on OrderItem so Hibernate can reserve ids in blocks of 50
-- (one nextval per 50 inserts) instead of one round-trip per row during the order details import
ALTER SEQUENCE order_item_id_seq INCREMENT BY 50;
