CREATE TABLE category (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Reference data: pizza types (recipes), decoupled from size/price (see `pizza`)
CREATE TABLE pizza_type (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE, -- natural/source id, kept for idempotent re-import
    name        VARCHAR(100) NOT NULL,
    category_id BIGINT       NOT NULL REFERENCES category(id) ON DELETE RESTRICT
);

CREATE TABLE ingredient (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- Many-to-many: a pizza_type has many ingredients, an ingredient appears in many pizza_types
CREATE TABLE pizza_type_ingredient (
    pizza_type_id BIGINT NOT NULL REFERENCES pizza_type(id) ON DELETE CASCADE,
    ingredient_id BIGINT NOT NULL REFERENCES ingredient(id) ON DELETE RESTRICT,
    PRIMARY KEY (pizza_type_id, ingredient_id)
);

-- A sellable pizza: one size/price variant of a pizza_type
CREATE TABLE pizza (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(50)   NOT NULL UNIQUE, -- natural/source id, kept for idempotent re-import
    pizza_type_id BIGINT        NOT NULL REFERENCES pizza_type(id) ON DELETE RESTRICT,
    size          VARCHAR(10)   NOT NULL, -- S, M, L, XL, XXL
    price         NUMERIC(6, 2) NOT NULL CHECK (price >= 0),
    UNIQUE (pizza_type_id, size)
);

-- Table named "orders", not "order" (ORDER is a reserved SQL keyword)
CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    order_code      VARCHAR(12) NOT NULL UNIQUE, -- human-readable public id, e.g. "PZA-7K9X2M"; app-generated
    source_order_id BIGINT NOT NULL UNIQUE, -- natural/source id, kept for idempotent re-import
    order_date      DATE   NOT NULL,
    order_time      TIME   NOT NULL
);

CREATE TABLE order_item (
    id                       BIGSERIAL PRIMARY KEY,
    source_order_details_id BIGINT  NOT NULL UNIQUE, -- natural/source id, kept for idempotent re-import
    order_id                 BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    pizza_id                 BIGINT NOT NULL REFERENCES pizza(id) ON DELETE RESTRICT,
    quantity                  INTEGER NOT NULL CHECK (quantity > 0)
);

-- Postgres doesn't auto-index FK columns; these support joins and the orders-by-date-range lookup
CREATE INDEX idx_pizza_type_category_id ON pizza_type (category_id);
CREATE INDEX idx_orders_order_date ON orders (order_date);
CREATE INDEX idx_order_item_order_id ON order_item (order_id);
CREATE INDEX idx_order_item_pizza_id ON order_item (pizza_id);
CREATE INDEX idx_pizza_pizza_type_id ON pizza (pizza_type_id);
