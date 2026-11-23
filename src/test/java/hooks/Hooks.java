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
import reporting.FailureTriageReport;
import testdata.ApiTestDataManager;
import utils.CloudStatusReporter;
import utils.DownloadManager;
import utils.DriverFactory;
import utils.JsErrorMonitor;
import utils.NetworkMockUtil;
import utils.NetworkPerformanceMonitor;
import utils.ScreenshotUtil;
import utils.ZipExtractionManager;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
        // Only sees whatever target/allure-results/*-result.json files exist right now — see
        // FailureTriageReport's own javadoc for re-running it after the full suite (including
        // any plain-TestNG classes running in another parallel thread) finishes.
        FailureTriageReport.generate();
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
        scenarioContext.set(JsErrorMonitor.CONTEXT_KEY, new JsErrorMonitor(DriverFactory.getDriver()));
        scenarioContext.set(NetworkPerformanceMonitor.CONTEXT_KEY, new NetworkPerformanceMonitor(DriverFactory.getDriver()));
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

    // Same reasoning as cleanupDownloads: must run before quitDriver() closes the BiDi
    // WebSocket the intercept lives on.
    @After("@web")
    public void cleanupNetworkMocks() {
        if (scenarioContext.contains(NetworkMockUtil.CONTEXT_KEY)) {
            scenarioContext.get(NetworkMockUtil.CONTEXT_KEY, NetworkMockUtil.class).close();
        }
    }

    // Declared last among the @web @After hooks, so Cucumber's reverse-declaration-order
    // execution runs this one FIRST — meaning if failOnJsError trips the throw below, the
    // scenario is already marked failed by the time tearDownUi (declared first, runs last)
    // takes its failure screenshot/page-source attachment.
    @After("@web")
    public void captureJsErrors(Scenario scenario) {
        if (!scenarioContext.contains(JsErrorMonitor.CONTEXT_KEY)) {
            return;
        }
        JsErrorMonitor jsErrorMonitor = scenarioContext.get(JsErrorMonitor.CONTEXT_KEY, JsErrorMonitor.class);
        jsErrorMonitor.close();

        List<String> jsErrors = jsErrorMonitor.unignoredJsErrors();
        boolean failOnJsError = ConfigReader.get("failOnJsError", "false").equalsIgnoreCase("true");
        boolean willFailForJsErrors = failOnJsError && !jsErrors.isEmpty();

        if ((scenario.isFailed() || willFailForJsErrors) && jsErrorMonitor.hasConsoleLog()) {
            scenario.attach(jsErrorMonitor.consoleLogText().getBytes(StandardCharsets.UTF_8),
                    "text/plain", "Browser Console Log");
        }

        if (willFailForJsErrors) {
            throw new AssertionError("❗ JavaScript error(s) detected in the browser console: " + jsErrors);
        } else if (!jsErrors.isEmpty()) {
            logger.warn("⚠️ {} JS error(s) detected in the browser console (failOnJsError is disabled): {}",
                    jsErrors.size(), jsErrors);
        }
    }

    // Default "slow" cutoff for the Allure report/log warning below — separate from the per-scenario
    // threshold steps.NetworkPerformanceSteps asserts with ("no request should take longer than
    // <n> seconds"), which is scenario-chosen and always enforced when used, regardless of this value.
    private static final long SLOW_REQUEST_REPORT_THRESHOLD_MS = 3000;
    private static final int SLOWEST_REQUESTS_TO_REPORT = 5;

    // Declared last among the @web @After hooks (after captureJsErrors), so it runs even earlier
    // in Cucumber's reverse-declaration-order execution — doesn't matter functionally (this only
    // reads the monitor's in-memory list), but keeps every BiDi-monitor cleanup hook grouped the
    // same way. Attaches a network performance report to Allure for every @web scenario (not just
    // failures) — the DoD is "slow or failing calls are visible in every web run", not only failed ones.
    @After("@web")
    public void captureNetworkPerformance(Scenario scenario) {
        if (!scenarioContext.contains(NetworkPerformanceMonitor.CONTEXT_KEY)) {
            return;
        }
        NetworkPerformanceMonitor networkPerformanceMonitor =
                scenarioContext.get(NetworkPerformanceMonitor.CONTEXT_KEY, NetworkPerformanceMonitor.class);
        networkPerformanceMonitor.close();

        List<NetworkPerformanceMonitor.RequestOutcome> slowest =
                networkPerformanceMonitor.slowest(SLOWEST_REQUESTS_TO_REPORT);
        List<NetworkPerformanceMonitor.RequestOutcome> failed = networkPerformanceMonitor.failedRequests();

        if (!slowest.isEmpty()) {
            scenario.attach(buildNetworkPerformanceReport(networkPerformanceMonitor, slowest, failed)
                            .getBytes(StandardCharsets.UTF_8),
                    "text/plain", "Network Performance");
        }

        List<NetworkPerformanceMonitor.RequestOutcome> slow =
                networkPerformanceMonitor.slowerThan(SLOW_REQUEST_REPORT_THRESHOLD_MS);
        if (!slow.isEmpty()) {
            logger.warn("🐢 {} request(s) slower than {}ms: {}", slow.size(), SLOW_REQUEST_REPORT_THRESHOLD_MS, slow);
        }
        if (!failed.isEmpty()) {
            logger.warn("🔴 {} failed network response(s) during this scenario: {}", failed.size(), failed);
        }
    }

    private String buildNetworkPerformanceReport(NetworkPerformanceMonitor networkPerformanceMonitor,
            List<NetworkPerformanceMonitor.RequestOutcome> slowest,
            List<NetworkPerformanceMonitor.RequestOutcome> failed) {
        StringBuilder report = new StringBuilder();
        report.append(networkPerformanceMonitor.completedRequests().size()).append(" request(s) captured.")
                .append(System.lineSeparator()).append(System.lineSeparator());

        report.append("Slowest requests:").append(System.lineSeparator());
        slowest.forEach(request -> report.append("  ").append(request).append(System.lineSeparator()));

        if (!failed.isEmpty()) {
            report.append(System.lineSeparator()).append("Failed responses (4xx/5xx or network error):")
                    .append(System.lineSeparator());
            failed.forEach(request -> report.append("  ").append(request).append(System.lineSeparator()));
        }
        return report.toString();
    }
}
