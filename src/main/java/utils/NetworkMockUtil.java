package utils;

import org.openqa.selenium.UsernameAndPassword;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.module.Network;
import org.openqa.selenium.bidi.network.AddInterceptParameters;
import org.openqa.selenium.bidi.network.BytesValue;
import org.openqa.selenium.bidi.network.ContinueRequestParameters;
import org.openqa.selenium.bidi.network.Header;
import org.openqa.selenium.bidi.network.InterceptPhase;
import org.openqa.selenium.bidi.network.ProvideResponseParameters;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps WebDriver BiDi's Network module ({@code org.openqa.selenium.bidi.module.Network}) to mock
 * backend responses for edge-case testing (e.g. "the backend returns a 500") without touching any
 * real backend. DriverFactory requests the {@code webSocketUrl} capability on every session, local
 * or remote, so BiDi is available either way. Shape matches Selenium's own
 * {@code NetworkCommandsTest} (addIntercept(BEFORE_REQUEST_SENT) + onBeforeRequestSent +
 * provideResponse / addIntercept(AUTH_REQUIRED) + onAuthRequired + continueWithAuth).
 *
 * <b>BiDi API still evolving:</b> this Selenium version (see {@code selenium.version} in pom.xml)
 * ships two parallel network-interception APIs — {@code org.openqa.selenium.bidi.module.Network}
 * (used here: stable callback-based shape, documented in Selenium's own BiDi guide and test suite)
 * and a newer {@code org.openqa.selenium.bidi.protocol.module.Network} (event-object based, still
 * settling). Re-check both package names against whatever Selenium version is installed before
 * upgrading — a class disappearing from one package and reappearing in the other is expected.
 * Also re-check the installed Chrome/Firefox/Edge build: each browser's own BiDi network
 * implementation is independently still maturing, so a request that should be intercepted can
 * simply never fire the expected event on a bleeding-edge or mismatched browser build — that
 * looks identical to a script timeout from the Java side and is a browser-support gap, not a bug
 * in this class.
 *
 * Only one {@code BEFORE_REQUEST_SENT} intercept is ever registered (added lazily on the first
 * {@link #mockResponse} call), shared by every mock registered afterwards — a single dispatcher
 * checks the request's URL against every mock and, crucially, calls {@code continueRequest} for
 * anything that doesn't match. Skipping that fallback would strand every *other* request on the
 * page (including its own navigation) waiting on a continuation that never comes.
 */
public class NetworkMockUtil implements AutoCloseable {

    /** ScenarioContext key both steps.NetworkMockSteps and hooks.Hooks use to find/close this instance. */
    public static final String CONTEXT_KEY = "networkMockUtil";

    private final Network network;
    private final List<String> interceptIds = new ArrayList<>();
    private final Map<String, MockedResponse> mocksByUrl = new LinkedHashMap<>();
    private boolean requestInterceptRegistered;

    public NetworkMockUtil(WebDriver driver) {
        this.network = new Network(driver);
    }

    /**
     * Fabricates the response for the request to exactly {@code url} — the real backend never
     * sees it. {@code body} may be {@code null} for an empty body (e.g. a bare 500 with no
     * payload).
     */
    public void mockResponse(String url, int statusCode, String body) {
        mocksByUrl.put(url, new MockedResponse(statusCode, body));
        if (requestInterceptRegistered) {
            return;
        }
        requestInterceptRegistered = true;
        interceptIds.add(network.addIntercept(new AddInterceptParameters(InterceptPhase.BEFORE_REQUEST_SENT)));
        network.onBeforeRequestSent(beforeRequestSent -> {
            String requestId = beforeRequestSent.getRequest().getRequestId();
            MockedResponse mock = mocksByUrl.get(beforeRequestSent.getRequest().getUrl());
            if (mock == null) {
                network.continueRequest(new ContinueRequestParameters(requestId));
                return;
            }
            network.provideResponse(new ProvideResponseParameters(requestId)
                    .statusCode(mock.statusCode)
                    .reasonPhrase(reasonPhrase(mock.statusCode))
                    .headers(List.of(new Header("Content-Type", new BytesValue(BytesValue.Type.STRING, "application/json"))))
                    .body(new BytesValue(BytesValue.Type.STRING, mock.body == null ? "" : mock.body)));
        });
    }

    /**
     * Supplies basic-auth credentials via BiDi, so no native browser credential prompt ever
     * appears. Only intercepts {@link InterceptPhase#AUTH_REQUIRED} (fires solely on a real 401 +
     * WWW-Authenticate challenge), so it never needs the continue-unmatched fallback above.
     */
    public void handleBasicAuth(String username, String password) {
        interceptIds.add(network.addIntercept(new AddInterceptParameters(InterceptPhase.AUTH_REQUIRED)));
        network.onAuthRequired(responseDetails ->
                network.continueWithAuth(responseDetails.getRequest().getRequestId(),
                        new UsernameAndPassword(username, password)));
    }

    private String reasonPhrase(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            case 503 -> "Service Unavailable";
            default -> "Mocked";
        };
    }

    @Override
    public void close() {
        interceptIds.forEach(network::removeIntercept);
        network.close();
    }

    private record MockedResponse(int statusCode, String body) {
    }
}
