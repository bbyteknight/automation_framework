package utils;

import context.ScenarioContext;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks temporary extraction folders created during a scenario (e.g. via {@link ZipUtil#unzip})
 * and deletes them all once the scenario ends, so asserting on an archive's extracted contents
 * never leaves stray directories under {@code target/} behind — the same
 * track/cleanup-per-scenario pattern as {@code db.DbTestDataManager}/{@code testdata.ApiTestDataManager}.
 */
public class ZipExtractionManager {

    private static final String TRACKED_DIRS_KEY = "zipExtractionDirs";
    private static final Logger logger = LogManager.getLogger(ZipExtractionManager.class);

    public void track(ScenarioContext scenarioContext, String extractionDirPath) {
        trackedDirs(scenarioContext).add(extractionDirPath);
    }

    /** Deletes every extraction folder still tracked for this scenario. Safe to call even if none were created. */
    public void cleanup(ScenarioContext scenarioContext) {
        List<String> dirs = trackedDirs(scenarioContext);
        for (String dirPath : dirs) {
            try {
                FileUtils.deleteDirectory(new File(dirPath));
            } catch (IOException e) {
                logger.warn("⚠️ Failed to clean up temporary extraction folder {}: {}", dirPath, e.getMessage());
            }
        }
        dirs.clear();
    }

    @SuppressWarnings("unchecked")
    private List<String> trackedDirs(ScenarioContext scenarioContext) {
        if (!scenarioContext.contains(TRACKED_DIRS_KEY)) {
            scenarioContext.set(TRACKED_DIRS_KEY, new ArrayList<String>());
        }
        return scenarioContext.get(TRACKED_DIRS_KEY, List.class);
    }
}
