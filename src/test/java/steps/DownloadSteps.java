package steps;

import config.ConfigReader;
import context.ScenarioContext;
import exceptions.FrameworkException;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;
import pages.DownloadPage;
import utils.DownloadManager;
import utils.DownloadUtil;
import utils.DriverFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Downloads a real file from the-internet's /download page (the same public site login.feature
 * already targets) and retrieves it the right way for wherever the session actually ran —
 * {@code utils.DownloadUtil} pulls it over the Grid/cloud {@code se:downloadsEnabled} API when
 * remote, or reads it straight off disk when local. {@code utils.DownloadManager} tracks the
 * resulting local path so it's cleaned up once the scenario ends.
 */
public class DownloadSteps {

    private static final String DOWNLOADED_FILE_KEY = "downloadedFilePath";

    private final ScenarioContext scenarioContext;
    private final DownloadManager downloadManager = new DownloadManager();

    public DownloadSteps(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("the user is on the file download page")
    public void theUserIsOnTheFileDownloadPage() {
        String downloadPageUrl = ConfigReader.get("downloadPageUrl", "https://the-internet.herokuapp.com/download");
        DownloadPage downloadPage = new DownloadPage(DriverFactory.getDriver());
        downloadPage.open(downloadPageUrl);
        scenarioContext.set("downloadPage", downloadPage);
    }

    @When("the user downloads the file {string}")
    public void theUserDownloadsTheFile(String fileName) {
        DownloadPage downloadPage = scenarioContext.get("downloadPage", DownloadPage.class);
        downloadPage.downloadFile(fileName);
        downloadAndTrack(fileName);
    }

    // The /download page is a shared public demo app — which files are actually listed there
    // varies run to run — so the sample scenario picks whatever's first rather than a hardcoded
    // name. Point steps.DownloadSteps.theUserDownloadsTheFile at a known file name once a real
    // export/download feature exists.
    @When("the user downloads the first available file")
    public void theUserDownloadsTheFirstAvailableFile() {
        DownloadPage downloadPage = scenarioContext.get("downloadPage", DownloadPage.class);
        String fileName = downloadPage.firstAvailableFileName();
        downloadPage.downloadFile(fileName);
        downloadAndTrack(fileName);
    }

    @Then("the downloaded file should exist")
    public void theDownloadedFileShouldExist() {
        Path downloadedFile = currentDownloadedFile();
        Assert.assertTrue(Files.exists(downloadedFile), "Expected downloaded file to exist: " + downloadedFile);
    }

    @Then("the downloaded file should not be empty")
    public void theDownloadedFileShouldNotBeEmpty() {
        Path downloadedFile = currentDownloadedFile();
        try {
            Assert.assertTrue(Files.size(downloadedFile) > 0, "Expected downloaded file to be non-empty: " + downloadedFile);
        } catch (IOException e) {
            throw new FrameworkException("❗ Failed to read downloaded file size: " + downloadedFile, e);
        }
    }

    private void downloadAndTrack(String fileName) {
        Path downloadedFile = DownloadUtil.waitForDownload(DriverFactory.getDriver(), fileName);
        downloadManager.track(scenarioContext, downloadedFile);
        scenarioContext.set(DOWNLOADED_FILE_KEY, downloadedFile.toString());
    }

    private Path currentDownloadedFile() {
        return Path.of(scenarioContext.get(DOWNLOADED_FILE_KEY, String.class));
    }
}
