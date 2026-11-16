package steps;

import context.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import utils.DriverFactory;
import utils.NetworkMockUtil;

import java.util.Map;

/**
 * Drives {@code utils.NetworkMockUtil} (WebDriver BiDi's Network module) to simulate backend
 * failures without touching any real backend. Since no page in this framework makes its own
 * browser-side API call, {@code the browser fetches "<url>"} issues one itself via
 * {@code fetch()} (async, read through {@code executeAsyncScript}) against a same-origin path on
 * the already-loaded page (cross-origin would hit CORS, unrelated to what's being tested here) —
 * point it at a real app's own API call once one exists.
 */
public class NetworkMockSteps {

    private static final String FETCH_RESULT_KEY = "fetchResult";

    // language=javascript
    private static final String FETCH_SCRIPT = """
            var callback = arguments[arguments.length - 1];
            fetch(arguments[0]).then(function(response) {
                return response.text().then(function(body) {
                    callback({status: response.status, body: body});
                });
            }).catch(function(error) {
                callback({status: -1, body: String(error)});
            });
            """;

    private final ScenarioContext scenarioContext;

    public NetworkMockSteps(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("network mocking is enabled")
    public void networkMockingIsEnabled() {
        scenarioContext.set(NetworkMockUtil.CONTEXT_KEY, new NetworkMockUtil(DriverFactory.getDriver()));
    }

    @Given("the API {string} returns status {int}")
    public void theApiReturnsStatus(String url, int statusCode) {
        networkMockUtil().mockResponse(url, statusCode, null);
    }

    @Given("the API {string} returns status {int} with this mock body:")
    public void theApiReturnsStatusWithThisMockBody(String url, int statusCode, String mockBody) {
        networkMockUtil().mockResponse(url, statusCode, mockBody);
    }

    @Given("basic auth is handled with username {string} and password {string}")
    public void basicAuthIsHandledWith(String username, String password) {
        networkMockUtil().handleBasicAuth(username, password);
    }

    @When("the user visits {string}")
    public void theUserVisits(String url) {
        DriverFactory.getDriver().get(url);
    }

    @When("the browser fetches {string}")
    public void theBrowserFetches(String url) {
        JavascriptExecutor executor = (JavascriptExecutor) DriverFactory.getDriver();
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) executor.executeAsyncScript(FETCH_SCRIPT, url);
        scenarioContext.set(FETCH_RESULT_KEY, result);
    }

    @Then("the fetch response status should be {int}")
    public void theFetchResponseStatusShouldBe(int expectedStatus) {
        long actualStatus = (long) fetchResult().get("status");
        Assert.assertEquals(actualStatus, (long) expectedStatus);
    }

    @Then("the fetch response body should contain {string}")
    public void theFetchResponseBodyShouldContain(String expectedSubstring) {
        String body = (String) fetchResult().get("body");
        Assert.assertTrue(body.contains(expectedSubstring),
                "Expected fetch response body to contain \"" + expectedSubstring + "\" — actual: " + body);
    }

    @Then("the page should show the text {string}")
    public void thePageShouldShowTheText(String expectedText) {
        String pageSource = DriverFactory.getDriver().getPageSource();
        Assert.assertTrue(pageSource.contains(expectedText),
                "Expected page to contain \"" + expectedText + "\"");
    }

    private NetworkMockUtil networkMockUtil() {
        return scenarioContext.get(NetworkMockUtil.CONTEXT_KEY, NetworkMockUtil.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchResult() {
        return scenarioContext.get(FETCH_RESULT_KEY, Map.class);
    }
}
