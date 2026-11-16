package steps;

import context.ScenarioContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.testng.Assert;
import utils.DriverFactory;
import utils.JsErrorMonitor;

import java.time.Duration;
import java.util.List;

/**
 * Drives {@code utils.JsErrorMonitor} (WebDriver BiDi's LogInspector module) to prove console
 * errors and uncaught JavaScript exceptions are actually captured. "the page throws" schedules
 * the error via {@code window.setTimeout(..., 0)} rather than throwing directly from
 * {@code executeScript} — a direct throw there would just raise a {@code WebDriverException} from
 * Selenium's own script call, not a real uncaught page-level exception, which is what
 * {@code JsErrorMonitor}/BiDi's {@code javascriptException} log entry is actually meant to catch.
 */
public class ConsoleLogSteps {

    private static final Duration CAPTURE_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration CAPTURE_POLL_INTERVAL = Duration.ofMillis(200);

    private final ScenarioContext scenarioContext;

    public ConsoleLogSteps(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @When("the page logs a console error {string}")
    public void thePageLogsAConsoleError(String message) {
        executeJs("console.error(arguments[0]);", message);
    }

    @When("the page throws an uncaught JavaScript error {string}")
    public void thePageThrowsAnUncaughtJavaScriptError(String message) {
        executeJs("window.setTimeout(function() { throw new Error(arguments[0]); }, 0);", message);
    }

    @Then("the browser console should report a JS error containing {string}")
    public void theBrowserConsoleShouldReportAJsErrorContaining(String expectedSubstring) {
        List<String> jsErrors = pollForJsError(expectedSubstring);
        Assert.assertTrue(jsErrors.stream().anyMatch(error -> error.contains(expectedSubstring)),
                "Expected a captured JS error containing \"" + expectedSubstring + "\" — captured: " + jsErrors);
    }

    // Chrome's BiDi javascriptException log entry (unlike its onConsoleEntry text) does not
    // currently surface the thrown Error's own message — only a generic "Uncaught Error"
    // (confirmed by a real run: a page throwing "new Error('Cannot read properties...')" is
    // captured as exactly "Uncaught Error", with the custom message dropped). Another concrete
    // instance of this card's "BiDi API still evolving" checklist item, so this step only
    // verifies an uncaught exception was captured at all, not its message content.
    @Then("the browser console should report an uncaught JS error")
    public void theBrowserConsoleShouldReportAnUncaughtJsError() {
        List<String> jsErrors = pollForAnyJsError();
        Assert.assertFalse(jsErrors.isEmpty(), "Expected at least one captured JS error, but none were captured");
    }

    @Then("the browser console should report no JS errors")
    public void theBrowserConsoleShouldReportNoJsErrors() {
        JsErrorMonitor jsErrorMonitor = scenarioContext.get(JsErrorMonitor.CONTEXT_KEY, JsErrorMonitor.class);
        List<String> jsErrors = jsErrorMonitor.unignoredJsErrors();
        Assert.assertTrue(jsErrors.isEmpty(),
                "Expected no (unignored) JS errors, but captured: " + jsErrors);
    }

    // Waits for delivery of the raw (possibly allow-listed) console entry itself, as opposed to
    // theBrowserConsoleShouldReportAJsErrorContaining's wait on the filtered jsErrors list — lets
    // a scenario prove an allow-listed error was still seen and logged, just not counted as a failure.
    @Then("the browser console log should contain {string}")
    public void theBrowserConsoleLogShouldContain(String expectedSubstring) {
        JsErrorMonitor jsErrorMonitor = scenarioContext.get(JsErrorMonitor.CONTEXT_KEY, JsErrorMonitor.class);
        Wait<WebDriver> wait = new FluentWait<>(DriverFactory.getDriver())
                .withTimeout(CAPTURE_TIMEOUT)
                .pollingEvery(CAPTURE_POLL_INTERVAL)
                .ignoring(TimeoutException.class);

        try {
            wait.until(driver -> jsErrorMonitor.consoleLogText().contains(expectedSubstring) ? Boolean.TRUE : null);
        } catch (TimeoutException ignored) {
            // Fall through — the assertion below reports what WAS captured.
        }
        Assert.assertTrue(jsErrorMonitor.consoleLogText().contains(expectedSubstring),
                "Expected the console log to contain \"" + expectedSubstring + "\" — captured: "
                        + jsErrorMonitor.consoleLogText());
    }

    private List<String> pollForJsError(String expectedSubstring) {
        JsErrorMonitor jsErrorMonitor = scenarioContext.get(JsErrorMonitor.CONTEXT_KEY, JsErrorMonitor.class);
        Wait<WebDriver> wait = new FluentWait<>(DriverFactory.getDriver())
                .withTimeout(CAPTURE_TIMEOUT)
                .pollingEvery(CAPTURE_POLL_INTERVAL)
                .ignoring(TimeoutException.class);

        try {
            wait.until(driver -> jsErrorMonitor.unignoredJsErrors().stream()
                    .anyMatch(error -> error.contains(expectedSubstring))
                    ? jsErrorMonitor.unignoredJsErrors()
                    : null);
        } catch (TimeoutException ignored) {
            // Fall through — the caller's own assertion below reports a clearer failure message
            // (including whatever WAS captured) than a bare FluentWait timeout would.
        }
        return jsErrorMonitor.unignoredJsErrors();
    }

    private List<String> pollForAnyJsError() {
        JsErrorMonitor jsErrorMonitor = scenarioContext.get(JsErrorMonitor.CONTEXT_KEY, JsErrorMonitor.class);
        Wait<WebDriver> wait = new FluentWait<>(DriverFactory.getDriver())
                .withTimeout(CAPTURE_TIMEOUT)
                .pollingEvery(CAPTURE_POLL_INTERVAL)
                .ignoring(TimeoutException.class);

        try {
            wait.until(driver -> jsErrorMonitor.unignoredJsErrors().isEmpty() ? null : jsErrorMonitor.unignoredJsErrors());
        } catch (TimeoutException ignored) {
            // Fall through — the caller's own assertion reports the (empty) result clearly.
        }
        return jsErrorMonitor.unignoredJsErrors();
    }

    private void executeJs(String script, Object... args) {
        ((JavascriptExecutor) DriverFactory.getDriver()).executeScript(script, args);
    }
}
