package listeners;

import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import utils.DriverFactory;
import utils.ScreenshotUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * Attaches failure evidence (screenshot + page source) to the active Allure test for plain
 * TestNG tests (e.g. LoginTest), mirroring what hooks.Hooks does for Cucumber scenarios. Paired
 * with AllureTestNg (see base.BaseTest's @Listeners) which creates the actual Allure test result;
 * this listener only adds the extra evidence on top of it.
 */
public class TestListener implements ITestListener {

    private static final Logger logger = LogManager.getLogger(TestListener.class);

    @Override
    public void onTestStart(ITestResult result) {
        logger.info("🔹 Test Started: {}", result.getMethod().getMethodName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        logger.info("✅ Test Passed: {}", result.getName());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        logger.error("❌ Test Failed: {}", result.getName());

        WebDriver driver = DriverFactory.getDriver();
        if (driver == null) {
            return;
        }

        byte[] screenshot = ScreenshotUtil.captureScreenshotBytes(driver);
        Allure.addAttachment("Failure Screenshot", "image/png", new ByteArrayInputStream(screenshot), ".png");

        File savedScreenshot = ScreenshotUtil.saveScreenshotToFile(driver, result.getName());
        logger.info("📸 Failure screenshot saved: {}", savedScreenshot.getPath());

        String pageSource = driver.getPageSource();
        Allure.addAttachment("Page Source", "text/html",
                new ByteArrayInputStream(pageSource.getBytes(StandardCharsets.UTF_8)), ".html");
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        logger.warn("⚠️ Test Skipped: {}", result.getName());
    }

    @Override
    public void onStart(ITestContext context) {
        logger.info("🚀 Test Context Started: {}", context.getName());
    }

    @Override
    public void onFinish(ITestContext context) {
        logger.info("🏁 Test Context Finished: {}", context.getName());
    }
}
