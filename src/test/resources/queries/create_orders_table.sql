-- Sample table for the @seed-orders tag (hooks.DbSeedHooks): stands in for whatever table a
-- real application would read its "orders" from, so DB-based seed/cleanup has something real
-- to insert into and delete from without requiring a live application database.
CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(100) PRIMARY KEY,
    customer_name VARCHAR(255),
    status VARCHAR(50),
    created_at TIMESTAMP
)
