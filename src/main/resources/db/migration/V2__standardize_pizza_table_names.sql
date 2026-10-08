-- Standardize catalog naming: prefix pizza-only reference tables with `pizza_`
ALTER TABLE category RENAME TO pizza_category;
ALTER TABLE pizza_category RENAME CONSTRAINT category_pkey TO pizza_category_pkey;
ALTER TABLE pizza_category RENAME CONSTRAINT category_name_key TO pizza_category_name_key;
ALTER SEQUENCE category_id_seq RENAME TO pizza_category_id_seq;

ALTER TABLE pizza_type RENAME COLUMN category_id TO pizza_category_id;
ALTER TABLE pizza_type RENAME CONSTRAINT pizza_type_category_id_fkey TO pizza_type_pizza_category_id_fkey;
ALTER INDEX idx_pizza_type_category_id RENAME TO idx_pizza_type_pizza_category_id;

ALTER TABLE ingredient RENAME TO pizza_ingredient;
ALTER TABLE pizza_ingredient RENAME CONSTRAINT ingredient_pkey TO pizza_ingredient_pkey;
ALTER TABLE pizza_ingredient RENAME CONSTRAINT ingredient_name_key TO pizza_ingredient_name_key;
ALTER SEQUENCE ingredient_id_seq RENAME TO pizza_ingredient_id_seq;

ALTER TABLE pizza_type_ingredient RENAME COLUMN ingredient_id TO pizza_ingredient_id;
ALTER TABLE pizza_type_ingredient RENAME CONSTRAINT pizza_type_ingredient_ingredient_id_fkey TO pizza_type_ingredient_pizza_ingredient_id_fkey;
