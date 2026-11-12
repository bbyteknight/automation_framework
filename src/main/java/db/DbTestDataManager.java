package db;

import context.ScenarioContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks rows seeded directly into the database during a scenario (table + id column + id
 * value) and deletes whatever is still outstanding once the scenario ends — the DB equivalent of
 * {@link testdata.ApiTestDataManager}'s pattern for API-created entities, so a scenario that
 * seeds fixture data via {@link DbClient} never leaves it behind, even if it fails before any
 * explicit cleanup step runs.
 */
public class DbTestDataManager {

    private static final String TRACKED_KEY = "dbCleanupRecords";
    private static final Logger logger = LogManager.getLogger(DbTestDataManager.class);

    public void track(ScenarioContext scenarioContext, String table, String idColumn, Object idValue) {
        SqlIdentifiers.validate(table);
        SqlIdentifiers.validate(idColumn);
        trackedRecords(scenarioContext).add(new SeededRecord(table, idColumn, idValue));
    }

    /**
     * Deletes everything still tracked for this scenario, most-recently-seeded row first (in
     * case a later row references an earlier one via a foreign key). Safe to call even if
     * nothing was seeded. One row's cleanup failure doesn't stop the rest from being attempted.
     */
    public void cleanup(ScenarioContext scenarioContext) {
        List<SeededRecord> records = trackedRecords(scenarioContext);
        for (int i = records.size() - 1; i >= 0; i--) {
            SeededRecord record = records.get(i);
            try {
                DbClient.update("DELETE FROM " + record.table() + " WHERE " + record.idColumn() + " = ?", record.idValue());
            } catch (RuntimeException e) {
                logger.warn("⚠️ Failed to clean up seeded row {}.{} = {}: {}",
                        record.table(), record.idColumn(), record.idValue(), e.getMessage());
            }
        }
        records.clear();
    }

    @SuppressWarnings("unchecked")
    private List<SeededRecord> trackedRecords(ScenarioContext scenarioContext) {
        if (!scenarioContext.contains(TRACKED_KEY)) {
            scenarioContext.set(TRACKED_KEY, new ArrayList<SeededRecord>());
        }
        return scenarioContext.get(TRACKED_KEY, List.class);
    }

    private record SeededRecord(String table, String idColumn, Object idValue) {
    }
}
