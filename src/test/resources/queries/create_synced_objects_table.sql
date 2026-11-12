-- Sample table for database_validation.feature: stands in for whatever table a real backend
-- would persist an API-created record into, so the "compare API response to DB" steps have
-- something real to query against without requiring a live downstream service.
CREATE TABLE IF NOT EXISTS synced_objects (
    id VARCHAR(100) PRIMARY KEY,
    object_name VARCHAR(255),
    synced_at TIMESTAMP
)
