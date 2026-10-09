-- Soft delete: DELETE /api/orders/{id} sets deleted_at instead of removing the row, and the API hides deleted orders.
-- The row and its order_item rows are kept, so a re-import sees the source id and doesn't recreate the order.
ALTER TABLE orders ADD COLUMN deleted_at TIMESTAMPTZ;
