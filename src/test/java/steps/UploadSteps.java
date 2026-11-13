package steps;

import config.ConfigReader;
import context.ScenarioContext;
import exceptions.FrameworkException;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;
import pages.UploadPage;
import utils.DriverFactory;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

/**
 * Drives the-internet's /upload page with a fixture file from {@code resources/testdata/uploads}
 * — identical steps whether the session is local or on a Grid node, since DriverFactory's
 * LocalFileDetector (see RemoteWebDriver.setFileDetector) ships the local file to the remote node
 * transparently before Selenium types its path into the file input.
 */
public class UploadSteps {

    private static final String UPLOADS_RESOURCE_DIR = "testdata/uploads/";

    private final ScenarioContext scenarioContext;

    public UploadSteps(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("the user is on the file upload page")
    public void theUserIsOnTheFileUploadPage() {
        String uploadPageUrl = ConfigReader.get("uploadPageUrl", "https://the-internet.herokuapp.com/upload");
        UploadPage uploadPage = new UploadPage(DriverFactory.getDriver());
        uploadPage.open(uploadPageUrl);
        scenarioContext.set("uploadPage", uploadPage);
    }

    @When("the user uploads the file {string}")
    public void theUserUploadsTheFile(String fileName) {
        UploadPage uploadPage = scenarioContext.get("uploadPage", UploadPage.class);
        uploadPage.selectFile(uploadFixturePath(fileName));
        uploadPage.submit();
    }

    @Then("the uploaded file name should be displayed as {string}")
    public void theUploadedFileNameShouldBeDisplayedAs(String expectedFileName) {
        UploadPage uploadPage = scenarioContext.get("uploadPage", UploadPage.class);
        Assert.assertEquals(uploadPage.getUploadedFileName(), expectedFileName);
    }

    private String uploadFixturePath(String fileName) {
        String resourcePath = UPLOADS_RESOURCE_DIR + fileName;
        URL resource = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
        if (resource == null) {
            throw new FrameworkException("❗ Upload fixture not found on classpath: " + resourcePath);
        }
        try {
            return Paths.get(resource.toURI()).toAbsolutePath().toString();
        } catch (URISyntaxException e) {
            throw new FrameworkException("❌ Could not resolve upload fixture path: " + resourcePath, e);
        }
    }
}
