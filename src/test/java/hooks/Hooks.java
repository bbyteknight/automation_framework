package hooks;

import config.ConfigReader;
import context.GlobalContext;
import context.ScenarioContext;
import drivers.MobileDriverFactory;
import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.AfterStep;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.Scenario;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import reporting.AllureReportSetup;
import testdata.ApiTestDataManager;
import utils.CloudStatusReporter;
import utils.DriverFactory;
import utils.ScreenshotUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class Hooks {

    private static final Logger logger = LogManager.getLogger(Hooks.class);

    private final ScenarioContext scenarioContext;
    private final ApiTestDataManager apiTestDataManager = new ApiTestDataManager();

    public Hooks(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @BeforeAll
    public static void globalSetup() {
        // Cucumber-only runs (e.g. RerunTestRunner via -Dtest=RerunTestRunner) never go
        // through BaseTest, so this is the only guaranteed place to load config for them.
        ConfigReader.loadProperties();
        AllureReportSetup.prepare();

        // Swap for a real auth call (e.g. REST Assured POST to an auth endpoint)
        // once a live API is available; for now this just surfaces the configured token.
        String token = ConfigReader.get("apiToken", "");
        GlobalContext.setAuthToken(token);
        if (token.isBlank()) {
            logger.warn("🔐 Global setup complete — no apiToken configured (API-authenticated scenarios will fail).");
        } else {
            logger.info("🔐 Global setup complete — auth token loaded for the run.");
        }
    }

    @AfterAll
    public static void globalTeardown() {
        GlobalContext.clear();
        logger.info("🔚 Global teardown complete — auth token cleared.");
    }

    @Before("@web")
    public void setUpWeb() {
        DriverFactory.initDriver(ConfigReader.get("browser"));
        DriverFactory.launchApp();
    }

    @Before("@android or @ios")
    public void setUpMobile(Scenario scenario) {
        String platform = scenario.getSourceTagNames().contains("@android") ? "android" : "ios";
        MobileDriverFactory.initDriver(platform);
    }

    // Fires after every Gherkin step (pass or fail), not just at scenario end, so the report
    // shows the UI's progression — page load, state right after each click/sendKeys — rather
    // than only a screenshot of the final failure.
    @AfterStep("@web or @android or @ios")
    public void attachStepScreenshot(Scenario scenario) {
        if (DriverFactory.getDriver() != null) {
            byte[] screenshot = ScreenshotUtil.captureScreenshotBytes(DriverFactory.getDriver());
            scenario.attach(screenshot, "image/png", "Step Screenshot");
        }
    }

    @After("@web or @android or @ios")
    public void tearDownUi(Scenario scenario) {
        if (scenario.isFailed() && DriverFactory.getDriver() != null) {
            WebDriver driver = DriverFactory.getDriver();

            byte[] screenshot = ScreenshotUtil.captureScreenshotBytes(driver);
            scenario.attach(screenshot, "image/png", "Failure Screenshot");

            // Also keep a copy on disk — easier to pull up locally than digging through the
            // Cucumber JSON report's embedded attachment.
            File savedScreenshot = ScreenshotUtil.saveScreenshotToFile(driver, scenario.getName());
            logger.info("📸 Failure screenshot saved: {}", savedScreenshot.getPath());

            // scenario.attach() is picked up by both the Cucumber HTML/JSON report and the
            // Allure Cucumber plugin (AllureCucumber7Jvm listens for Cucumber's embed events),
            // so one call here surfaces the page source as evidence in both reports.
            String pageSource = driver.getPageSource();
            scenario.attach(pageSource.getBytes(StandardCharsets.UTF_8), "text/html", "Page Source");
        }

        // Must run before quitDriver() — the BrowserStack status hook needs the session still open.
        if ("cloud".equalsIgnoreCase(ConfigReader.get("executionMode", "local")) && DriverFactory.getDriver() != null) {
            CloudStatusReporter.reportStatus(DriverFactory.getDriver(), !scenario.isFailed(), scenario.getName());
        }

        DriverFactory.quitDriver();
    }

    // Sweeps up any API-created test object the scenario tracked but didn't explicitly delete
    // itself (e.g. objects-data-driven.feature), so nothing leaks into the shared sandbox.
    @After("@api")
    public void cleanupApiTestData() {
        apiTestDataManager.cleanup(scenarioContext);
    }
}
