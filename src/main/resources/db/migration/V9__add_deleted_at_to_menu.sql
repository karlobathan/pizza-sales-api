-- Soft delete for the menu: DELETE /api/pizza-types/{id} and /api/pizzas/{id} set deleted_at instead of removing the row.
-- The menu endpoints hide deleted rows, but existing orders still reference (and show) the pizzas they were placed with,
-- and the rows keep their codes, so a re-import doesn't recreate them.
ALTER TABLE pizza_type ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE pizza ADD COLUMN deleted_at TIMESTAMPTZ;

-- a pizza type can have one active pizza per size; a deleted size must not block creating that size again
ALTER TABLE pizza DROP CONSTRAINT pizza_pizza_type_id_size_key;
CREATE UNIQUE INDEX pizza_pizza_type_id_size_active_key ON pizza (pizza_type_id, size) WHERE deleted_at IS NULL;
