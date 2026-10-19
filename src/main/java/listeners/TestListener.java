package listeners;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import reporting.ExtentTestManager;
import reporting.ReportManager;
import utils.DateHelper;
import utils.DriverFactory;
import utils.ScreenshotUtil;

public class TestListener implements ITestListener {

    private static final Logger logger = LogManager.getLogger(TestListener.class);
    private static final String REPORTS_DIR = "test-output/reports/";

    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getMethod().getMethodName();

        String reportFileName = REPORTS_DIR + testName + "_" + DateHelper.timestamp() + ".html";

        ReportManager.getInstance(reportFileName);
        ExtentTestManager.setTest(ReportManager.createTest(testName));
        logger.info("🔹 Test Started: {}", testName);
        try {
            ExtentTestManager.getTest().log(Status.INFO, "Test Started: " + testName);
        } catch (Exception e) {
            logger.error("❗ Failed to log test start: {}", e.getMessage());
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        logger.info("✅ Test Passed: {}", result.getName());
        try {
            ExtentTestManager.getTest().log(Status.PASS, "Test Passed: " + result.getName());
        } catch (Exception e) {
            logger.error("❗ Failed to log test success: {}", e.getMessage());
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        WebDriver driver = DriverFactory.getDriver();
        ExtentTest test = ExtentTestManager.getTest();

        if (driver != null) {
            ScreenshotUtil.addScreenshotToReport(driver, test, "❌ Test Failed Screenshot", "fail");
            ScreenshotUtil.saveScreenshotToFile(driver, result.getName());
        }

        test.fail(result.getThrowable());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        logger.warn("⚠️ Test Skipped: {}", result.getName());
        try {
            ExtentTestManager.getTest().log(Status.SKIP, "Test Skipped: " + result.getThrowable());
        } catch (Exception e) {
            logger.error("❗ Failed to log test skip: {}", e.getMessage());
        }
    }

    @Override
    public void onStart(ITestContext context) {
        logger.info("🚀 Test Context Started: {}", context.getName());
    }

    @Override
    public void onFinish(ITestContext context) {
        logger.info("🏁 Test Context Finished: {}", context.getName());
        ReportManager.flushReports(); // Ensure we flush the report to disk
    }
}
