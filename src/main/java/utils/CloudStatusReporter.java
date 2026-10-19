package utils;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * Marks a scenario's pass/fail status on the BrowserStack dashboard via the
 * "browserstack_executor" JS hook, which BrowserStack's Appium server intercepts instead of
 * actually executing as a script. Must be called while the session is still open — i.e. before
 * DriverFactory.quitDriver() — since the status is attached to the active session.
 */
public final class CloudStatusReporter {

    private CloudStatusReporter() {
    }

    public static void reportStatus(WebDriver driver, boolean passed, String reason) {
        String status = passed ? "passed" : "failed";
        String escapedReason = reason == null ? "" : reason.replace("\"", "'").replace("\n", " ");
        String script = "browserstack_executor: {\"action\": \"setSessionStatus\", \"arguments\": "
                + "{\"status\": \"" + status + "\", \"reason\": \"" + escapedReason + "\"}}";
        ((JavascriptExecutor) driver).executeScript(script);
    }
}
