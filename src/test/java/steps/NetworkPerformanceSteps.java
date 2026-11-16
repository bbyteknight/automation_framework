package steps;

import context.ScenarioContext;
import io.cucumber.java.en.Then;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.testng.Assert;
import utils.DriverFactory;
import utils.NetworkPerformanceMonitor;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Asserts against {@code utils.NetworkPerformanceMonitor}'s passively captured request/response
 * timing and status for the current scenario — every {@code @web} scenario already has one
 * running (wired by hooks.Hooks), so these steps need no setup step of their own, unlike
 * steps.NetworkMockSteps' "network mocking is enabled".
 */
public class NetworkPerformanceSteps {

    private static final Duration CAPTURE_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration CAPTURE_POLL_INTERVAL = Duration.ofMillis(200);

    private final ScenarioContext scenarioContext;

    public NetworkPerformanceSteps(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Then("no request should take longer than {int} seconds")
    public void noRequestShouldTakeLongerThanSeconds(int maxSeconds) {
        long maxMs = TimeUnit.SECONDS.toMillis(maxSeconds);
        List<NetworkPerformanceMonitor.RequestOutcome> slow = networkPerformanceMonitor().slowerThan(maxMs);
        Assert.assertTrue(slow.isEmpty(),
                "Expected no request slower than " + maxSeconds + "s, but found: " + slow);
    }

    @Then("no network response should be a 4xx or 5xx error")
    public void noNetworkResponseShouldBeA4xxOr5xxError() {
        List<NetworkPerformanceMonitor.RequestOutcome> failed = networkPerformanceMonitor().failedRequests();
        Assert.assertTrue(failed.isEmpty(), "Expected no failed network responses, but found: " + failed);
    }

    // Polls briefly — onResponseCompleted can arrive slightly after the driver's own get()/
    // navigation call already returned, so reading the monitor the instant the step starts could
    // race a real failed response that just hasn't been delivered over the BiDi socket yet.
    @Then("a 4xx or 5xx response should have been detected")
    public void aResponseShouldHaveBeenDetected() {
        NetworkPerformanceMonitor networkPerformanceMonitor = networkPerformanceMonitor();
        Wait<WebDriver> wait = new FluentWait<>(DriverFactory.getDriver())
                .withTimeout(CAPTURE_TIMEOUT)
                .pollingEvery(CAPTURE_POLL_INTERVAL)
                .ignoring(TimeoutException.class);

        try {
            wait.until(driver -> networkPerformanceMonitor.failedRequests().isEmpty() ? null : Boolean.TRUE);
        } catch (TimeoutException ignored) {
            // Fall through — the assertion below reports a clearer failure message.
        }
        Assert.assertFalse(networkPerformanceMonitor.failedRequests().isEmpty(),
                "Expected at least one failed network response, but none were captured");
    }

    private NetworkPerformanceMonitor networkPerformanceMonitor() {
        return scenarioContext.get(NetworkPerformanceMonitor.CONTEXT_KEY, NetworkPerformanceMonitor.class);
    }
}
