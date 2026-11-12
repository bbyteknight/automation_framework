-- Proves db.ContainerDatabaseSuiteListener actually ran its schema/seed step against the real,
-- throwaway container (see db.ContainerDatabaseTest) — not tied to any real application table.
CREATE TABLE IF NOT EXISTS container_health_check (
    id VARCHAR(50) PRIMARY KEY,
    note VARCHAR(255)
)
