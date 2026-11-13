package utils;

import context.ScenarioContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks local files pulled down via {@link DownloadUtil#waitForDownload} during a scenario and
 * deletes them once it ends, so asserting on a download never leaves stray files under
 * {@code downloadDir} behind — the same track/cleanup-per-scenario pattern as
 * {@code ZipExtractionManager}/{@code db.DbTestDataManager}/{@code testdata.ApiTestDataManager}.
 *
 * On a Grid/cloud session, cleanup also clears whatever the remote node is still holding for this
 * session ({@link DownloadUtil#deleteRemoteFiles}), so a scenario that downloaded a file but never
 * pulled it over doesn't leave it on the node either.
 */
public class DownloadManager {

    private static final String TRACKED_FILES_KEY = "trackedDownloads";
    private static final Logger logger = LogManager.getLogger(DownloadManager.class);

    public void track(ScenarioContext scenarioContext, Path downloadedFile) {
        trackedFiles(scenarioContext).add(downloadedFile.toString());
    }

    /** Deletes every file tracked for this scenario, local and (if any) remote. Safe to call even if none were downloaded. */
    public void cleanup(ScenarioContext scenarioContext) {
        List<String> files = trackedFiles(scenarioContext);
        for (String filePath : files) {
            try {
                Files.deleteIfExists(Path.of(filePath));
            } catch (IOException e) {
                logger.warn("⚠️ Failed to clean up downloaded file {}: {}", filePath, e.getMessage());
            }
        }
        files.clear();

        WebDriver driver = DriverFactory.getDriver();
        if (driver != null) {
            try {
                DownloadUtil.deleteRemoteFiles(driver);
            } catch (RuntimeException e) {
                // Not every @web session has se:downloadsEnabled (e.g. a local run never needs it) —
                // nothing to clear remotely in that case, so this is expected, not a real failure.
                logger.debug("Skipping remote download cleanup: {}", e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> trackedFiles(ScenarioContext scenarioContext) {
        if (!scenarioContext.contains(TRACKED_FILES_KEY)) {
            scenarioContext.set(TRACKED_FILES_KEY, new ArrayList<String>());
        }
        return scenarioContext.get(TRACKED_FILES_KEY, List.class);
    }
}
