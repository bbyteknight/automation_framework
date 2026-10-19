package testdata;

import api.clients.ObjectClient;
import context.ScenarioContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks API entities created during a scenario (e.g. via ObjectClient.createObject) and deletes
 * whatever is still outstanding once the scenario ends — so a scenario that creates data through
 * the API never leaks it into the shared sandbox, even if it fails before its own explicit cleanup
 * step runs. A scenario that explicitly deletes what it created (untrack) simply leaves nothing
 * for the end-of-scenario sweep to do.
 */
public class ApiTestDataManager {

    private static final String TRACKED_IDS_KEY = "apiCleanupObjectIds";
    private static final Logger logger = LogManager.getLogger(ApiTestDataManager.class);

    private final ObjectClient objectClient = new ObjectClient();

    public void track(ScenarioContext scenarioContext, String objectId) {
        trackedIds(scenarioContext).add(objectId);
    }

    public void untrack(ScenarioContext scenarioContext, String objectId) {
        trackedIds(scenarioContext).remove(objectId);
    }

    /** Deletes everything still tracked for this scenario. Safe to call even if nothing was created. */
    public void cleanup(ScenarioContext scenarioContext) {
        List<String> ids = trackedIds(scenarioContext);
        for (String id : ids) {
            try {
                objectClient.deleteObject(id);
            } catch (RuntimeException e) {
                logger.warn("⚠️ Failed to clean up test object {} via API: {}", id, e.getMessage());
            }
        }
        ids.clear();
    }

    @SuppressWarnings("unchecked")
    private List<String> trackedIds(ScenarioContext scenarioContext) {
        if (!scenarioContext.contains(TRACKED_IDS_KEY)) {
            scenarioContext.set(TRACKED_IDS_KEY, new ArrayList<String>());
        }
        return scenarioContext.get(TRACKED_IDS_KEY, List.class);
    }
}
