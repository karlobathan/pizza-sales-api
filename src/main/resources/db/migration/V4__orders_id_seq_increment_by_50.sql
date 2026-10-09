-- Match the JPA sequence allocationSize on Order so Hibernate can reserve ids in blocks of 50
-- (one nextval per 50 inserts) instead of one round-trip per row during the orders import
ALTER SEQUENCE orders_id_seq INCREMENT BY 50;
