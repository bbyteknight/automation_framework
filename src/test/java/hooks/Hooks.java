package hooks;

import config.ConfigReader;
import context.GlobalContext;
import drivers.MobileDriverFactory;
import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.Scenario;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.DriverFactory;
import utils.ScreenshotUtil;

public class Hooks {

    private static final Logger logger = LogManager.getLogger(Hooks.class);

    @BeforeAll
    public static void globalSetup() {
        // Cucumber-only runs (e.g. RerunTestRunner via -Dtest=RerunTestRunner) never go
        // through BaseTest, so this is the only guaranteed place to load config for them.
        ConfigReader.loadProperties();

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

    @After("@web or @android or @ios")
    public void tearDownUi(Scenario scenario) {
        if (scenario.isFailed() && DriverFactory.getDriver() != null) {
            byte[] screenshot = ScreenshotUtil.captureScreenshotBytes(DriverFactory.getDriver());
            scenario.attach(screenshot, "image/png", "Failure Screenshot");
        }
        DriverFactory.quitDriver();
    }
}
