package steps;

import context.ScenarioContext;
import exceptions.FrameworkException;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import utils.ZipExtractionManager;
import utils.ZipUtil;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * Generic, schema-agnostic steps for creating, extracting, and validating zip archives via
 * {@code utils.ZipUtil} — built from a small fixture directory ({@code resources/files/sample_source})
 * rather than any real downloaded file, since the framework has no browser-download plumbing today;
 * swap in a real downloaded file's path wherever a scenario actually has one.
 */
public class FileSteps {

    private static final String ZIP_PATH_KEY = "currentZipPath";
    private static final String EXTRACTION_DIR_KEY = "currentExtractionDir";
    private static final String SAMPLE_SOURCE_RESOURCE = "files/sample_source";

    private final ScenarioContext scenarioContext;
    private final ZipExtractionManager zipExtractionManager = new ZipExtractionManager();

    public FileSteps(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("a zip archive is created from the sample source directory")
    public void aZipArchiveIsCreatedFromTheSampleSourceDirectory() {
        createZip(null);
    }

    @Given("a password-protected zip archive is created from the sample source directory with password {string}")
    public void aPasswordProtectedZipArchiveIsCreatedWithPassword(String password) {
        createZip(password);
    }

    @Then("the zip archive should contain file {string}")
    public void theZipArchiveShouldContainFile(String fileName) {
        assertZipContainsFile(fileName);
    }

    @Then("the downloaded zip should contain file {string}")
    public void theDownloadedZipShouldContainFile(String fileName) {
        assertZipContainsFile(fileName);
    }

    @Then("the zip archive should contain {int} file(s)")
    public void theZipArchiveShouldContainFileCount(int expectedCount) {
        int actual = ZipUtil.fileCount(currentZipPath());
        Assert.assertEquals(actual, expectedCount, "Unexpected file count in archive " + currentZipPath());
    }

    @When("the zip archive is extracted")
    public void theZipArchiveIsExtracted() {
        extractZip(null);
    }

    @When("the zip archive is extracted with password {string}")
    public void theZipArchiveIsExtractedWithPassword(String password) {
        extractZip(password);
    }

    @Then("the extracted files should contain {string}")
    public void theExtractedFilesShouldContain(String fileName) {
        File extractionDir = new File(currentExtractionDir());
        boolean found = FileUtils.listFiles(extractionDir, null, true).stream()
                .anyMatch(file -> file.getName().equals(fileName));
        Assert.assertTrue(found, "Expected to find \"" + fileName + "\" under " + extractionDir.getPath());
    }

    private void createZip(String password) {
        String zipPath = "target/zip-fixtures/sample-" + UUID.randomUUID() + ".zip";
        ZipUtil.zip(sampleSourceDir(), zipPath, password);
        scenarioContext.set(ZIP_PATH_KEY, zipPath);
    }

    private void extractZip(String password) {
        String extractionDir = "target/zip-extractions/" + UUID.randomUUID();
        ZipUtil.unzip(currentZipPath(), extractionDir, password);
        zipExtractionManager.track(scenarioContext, extractionDir);
        scenarioContext.set(EXTRACTION_DIR_KEY, extractionDir);
    }

    private void assertZipContainsFile(String fileName) {
        List<String> contents = ZipUtil.listContents(currentZipPath());
        boolean found = contents.stream().anyMatch(entry -> entry.endsWith(fileName));
        Assert.assertTrue(found, "Expected archive " + currentZipPath()
                + " to contain \"" + fileName + "\" — actual entries: " + contents);
    }

    private String currentZipPath() {
        return scenarioContext.get(ZIP_PATH_KEY, String.class);
    }

    private String currentExtractionDir() {
        return scenarioContext.get(EXTRACTION_DIR_KEY, String.class);
    }

    private String sampleSourceDir() {
        URL resource = Thread.currentThread().getContextClassLoader().getResource(SAMPLE_SOURCE_RESOURCE);
        if (resource == null) {
            throw new FrameworkException("❗ Sample source directory not found on classpath: " + SAMPLE_SOURCE_RESOURCE);
        }
        try {
            return Paths.get(resource.toURI()).toString();
        } catch (URISyntaxException e) {
            throw new FrameworkException("❌ Could not resolve sample source directory path", e);
        }
    }
}
