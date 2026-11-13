package hooks;

import config.ConfigReader;
import context.GlobalContext;
import context.ScenarioContext;
import db.DbClient;
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
import utils.DownloadManager;
import utils.DriverFactory;
import utils.ScreenshotUtil;
import utils.ZipExtractionManager;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class Hooks {

    private static final Logger logger = LogManager.getLogger(Hooks.class);

    private final ScenarioContext scenarioContext;
    private final ApiTestDataManager apiTestDataManager = new ApiTestDataManager();
    private final ZipExtractionManager zipExtractionManager = new ZipExtractionManager();
    private final DownloadManager downloadManager = new DownloadManager();

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
        warnOnLeftoverSeedData();
        // Safe even if no scenario ever queried the database — closeAll() no-ops when the
        // pool was never initialized, so this doesn't force a DB connection on every run.
        DbClient.closeAll();
        logger.info("🔚 Global teardown complete — auth token cleared, DB pool closed.");
    }

    // Final safety net for "scenarios leave no leftover data": if every @seed-orders scenario's
    // own @After("@seed-orders") cleanup ran (hooks.DbSeedHooks), "orders" is empty by now. A
    // non-empty table here means a scenario crashed badly enough to skip even that cleanup (e.g.
    // a JVM-level failure) — surfaced as a loud warning rather than silently left behind.
    private static void warnOnLeftoverSeedData() {
        try {
            int leftover = DbClient.query("SELECT * FROM orders").size();
            if (leftover > 0) {
                logger.warn("⚠️ {} leftover row(s) in \"orders\" after the run — a @seed-orders "
                        + "scenario's cleanup hook may not have run.", leftover);
            }
        } catch (RuntimeException e) {
            // "orders" was never created this run (no @seed-orders scenario executed) — nothing to check.
            logger.debug("Skipping leftover-seed-data check: {}", e.getMessage());
        }
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

    @Before("@desktop")
    public void setUpDesktop() {
        DriverFactory.initDesktopDriver();
    }

    // Fires after every Gherkin step (pass or fail), not just at scenario end, so the report
    // shows the UI's progression — page load, state right after each click/sendKeys — rather
    // than only a screenshot of the final failure.
    //
    // @desktop is deliberately excluded here (unlike @After below): NovaWindows' PowerShell-backed
    // SendKeys is reproducibly destabilized by a screenshot capture landing between two keyboard
    // actions (observed as "SendWait ... Access is denied" on the next sendKeys, or a click
    // silently not registering) — so desktop scenarios only get a screenshot on failure, not after
    // every step.
    @AfterStep("@web or @android or @ios")
    public void attachStepScreenshot(Scenario scenario) {
        if (DriverFactory.getDriver() != null) {
            byte[] screenshot = ScreenshotUtil.captureScreenshotBytes(DriverFactory.getDriver());
            scenario.attach(screenshot, "image/png", "Step Screenshot");
        }
    }

    @After("@web or @android or @ios or @desktop")
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

    // No tag filter: any scenario on any platform could extract a zip via steps.FileSteps, and
    // cleanup() is a no-op when nothing was tracked, so this is safe/cheap to run for every
    // scenario rather than only @api ones.
    @After
    public void cleanupZipExtractions() {
        zipExtractionManager.cleanup(scenarioContext);
    }

    // Declared after tearDownUi, so Cucumber's reverse-declaration-order @After execution runs
    // this one first — the driver (and its Grid/cloud session) must still be open for
    // DownloadManager.cleanup() to clear any file the remote node is still holding.
    @After("@web")
    public void cleanupDownloads() {
        downloadManager.cleanup(scenarioContext);
    }
}
