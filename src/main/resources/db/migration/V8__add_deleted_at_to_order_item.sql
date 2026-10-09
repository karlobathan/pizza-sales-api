-- Soft delete for order items: PUT /api/orders/{id} sets deleted_at on the items it replaces instead of removing them,
-- and the API hides them. The rows keep their source_order_details_id, so a re-import doesn't add them back.
ALTER TABLE order_item ADD COLUMN deleted_at TIMESTAMPTZ;
