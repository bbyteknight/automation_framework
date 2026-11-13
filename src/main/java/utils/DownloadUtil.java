package utils;

import config.ConfigReader;
import exceptions.FrameworkException;
import org.openqa.selenium.HasDownloads;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

/**
 * Retrieves downloaded files regardless of where the browser session actually ran.
 *
 * Local runs (executionMode=local) write straight to {@code downloadDir} (DriverFactory sets
 * Chrome/Firefox/Edge's download prefs to that folder), so the file is already on this machine —
 * {@link #waitForDownload} just polls the filesystem for it.
 *
 * Grid/cloud runs (executionMode=grid|cloud|remote) download to the remote node's own filesystem,
 * unreachable from here. DriverFactory enables Selenium 4's {@code se:downloadsEnabled} capability
 * on those sessions, which exposes {@link HasDownloads} — {@link #waitForDownload} polls
 * {@link HasDownloads#getDownloadableFiles()} instead, then pulls the file to {@code downloadDir}
 * via {@link HasDownloads#downloadFile}.
 */
public class DownloadUtil {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(500);

    private DownloadUtil() {
    }

    public static Path downloadDir() {
        Path dir = Paths.get(ConfigReader.get("downloadDir", "target/downloads"));
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new FrameworkException("❗ Failed to create download directory: " + dir, e);
        }
        return dir;
    }

    /** Names of files currently sitting in the Grid/cloud node's download directory for this session. */
    public static List<String> listRemoteFiles(WebDriver driver) {
        return asDownloads(driver).getDownloadableFiles();
    }

    /** Pulls one file off the remote node into the local {@link #downloadDir()} and returns its local path. */
    public static Path downloadRemoteFile(WebDriver driver, String fileName) {
        Path destDir = downloadDir();
        try {
            asDownloads(driver).downloadFile(fileName, destDir);
        } catch (IOException e) {
            throw new FrameworkException("❗ Failed to download remote file: " + fileName, e);
        }
        return destDir.resolve(fileName);
    }

    /** Clears every file the remote node is holding for this session. Safe to call even if none were downloaded. */
    public static void deleteRemoteFiles(WebDriver driver) {
        asDownloads(driver).deleteDownloadableFiles();
    }

    public static Path waitForDownload(WebDriver driver, String fileName) {
        return waitForDownload(driver, fileName, DEFAULT_TIMEOUT);
    }

    /**
     * Blocks until {@code fileName} has finished downloading, then returns its local path — on
     * Grid/cloud this includes pulling it over from the remote node first. Local, in-progress
     * downloads are recognized by their browser-added ".crdownload"/".part" suffix and skipped
     * until that suffix disappears, so callers never read a partially-written file.
     */
    public static Path waitForDownload(WebDriver driver, String fileName, Duration timeout) {
        boolean remote = isRemoteExecution();
        Wait<WebDriver> wait = new FluentWait<>(driver)
                .withTimeout(timeout)
                .pollingEvery(POLL_INTERVAL);

        Path[] resolved = new Path[1];
        try {
            wait.until(currentDriver -> {
                if (remote) {
                    if (listRemoteFiles(currentDriver).contains(fileName)) {
                        resolved[0] = downloadRemoteFile(currentDriver, fileName);
                        return true;
                    }
                    return false;
                }
                Path candidate = downloadDir().resolve(fileName);
                if (Files.exists(candidate) && !isStillDownloading(candidate)) {
                    resolved[0] = candidate;
                    return true;
                }
                return false;
            });
        } catch (TimeoutException e) {
            throw new FrameworkException("❗ Timed out after " + timeout.getSeconds()
                    + "s waiting for download to complete: " + fileName, e);
        }
        return resolved[0];
    }

    private static boolean isStillDownloading(Path file) {
        String name = file.getFileName().toString();
        Path incompleteChrome = file.resolveSibling(name + ".crdownload");
        Path incompleteFirefox = file.resolveSibling(name + ".part");
        return Files.exists(incompleteChrome) || Files.exists(incompleteFirefox);
    }

    private static boolean isRemoteExecution() {
        String mode = ConfigReader.get("executionMode", "local").toLowerCase().trim();
        return mode.equals("grid") || mode.equals("cloud") || mode.equals("remote");
    }

    private static HasDownloads asDownloads(WebDriver driver) {
        if (driver instanceof HasDownloads hasDownloads) {
            return hasDownloads;
        }
        throw new FrameworkException("❗ Current driver session does not support remote downloads "
                + "(se:downloadsEnabled was not enabled for this session)");
    }
}
