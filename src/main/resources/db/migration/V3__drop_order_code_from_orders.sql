-- Drop the app-generated public order code (e.g. "PZA-7K9X2M"); orders are identified by id and source_order_id
-- Dropping the column also drops its UNIQUE constraint (orders_order_code_key)
ALTER TABLE orders DROP COLUMN order_code;
