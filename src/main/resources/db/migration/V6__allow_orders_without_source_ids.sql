-- Orders and order items created through the API have no CSV source id; only imported rows do.
-- The UNIQUE constraints stay: Postgres allows any number of NULLs, so imported ids are still unique.
ALTER TABLE orders ALTER COLUMN source_order_id DROP NOT NULL;
ALTER TABLE order_item ALTER COLUMN source_order_details_id DROP NOT NULL;
