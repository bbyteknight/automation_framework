package utils;

import config.ConfigReader;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.module.Network;
import org.openqa.selenium.bidi.network.BeforeRequestSent;
import org.openqa.selenium.bidi.network.FetchError;
import org.openqa.selenium.bidi.network.ResponseDetails;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Wraps WebDriver BiDi's Network module ({@code org.openqa.selenium.bidi.module.Network}) to
 * passively observe request/response timing and status for every network call a {@code @web}
 * scenario makes — page navigations, XHR/fetch, images, everything — without ever intercepting or
 * blocking a single one. Unlike {@link NetworkMockUtil} (same module, but via {@code addIntercept}
 * on a blocking phase), this only subscribes to {@code onBeforeRequestSent}/
 * {@code onResponseCompleted}/{@code onFetchError}, which fire as plain notifications regardless
 * of any intercept — so it works even in an environment where blocking interception itself hits
 * the browser/Selenium BiDi-version mismatch documented on {@code NetworkMockUtil}.
 *
 * <p>Request/response pairs are correlated by BiDi's own {@code requestId}, and duration is each
 * event's own {@code timestamp} field (an epoch-ms UTC timestamp per the BiDi spec, not
 * {@code RequestData.getTimings()}'s page-navigation-relative {@code FetchTimingInfo} values,
 * which are awkward to turn back into a wall-clock duration and unnecessary for this purpose).
 *
 * <p>Requests that never reach {@code onResponseCompleted} (page unloaded mid-request, a
 * BiDi-version gap like the one on {@code NetworkMockUtil}, etc.) are simply never recorded here
 * — there is no way to time something that never finished, and nothing meaningful to report for
 * it.
 *
 * <p>Known third-party noise (e.g. a test site's own analytics beacon failing DNS resolution in a
 * sandboxed/offline-ish environment — confirmed on a real run: the-internet.herokuapp.com fires
 * an Optimizely beacon on every page load that fails with {@code ERR_NAME_NOT_RESOLVED} here) is
 * filtered out of {@link #failedRequests()} via a configurable allow-list ({@code
 * networkErrorAllowList} — comma-separated, case-insensitive substrings matched against the URL),
 * same convention as {@code JsErrorMonitor}'s {@code jsErrorAllowList}. Allow-listed failures are
 * still counted in {@link #completedRequests()}/the Allure report's total, just not treated as a
 * failure.
 */
public class NetworkPerformanceMonitor implements AutoCloseable {

    /** ScenarioContext key both steps.NetworkPerformanceSteps and hooks.Hooks use to find/close this instance. */
    public static final String CONTEXT_KEY = "networkPerformanceMonitor";

    private final Network network;
    private final Map<String, Long> requestStartedAt = new ConcurrentHashMap<>();
    private final List<RequestOutcome> completed = new CopyOnWriteArrayList<>();
    private final Set<String> allowList;

    public NetworkPerformanceMonitor(WebDriver driver) {
        this.allowList = loadAllowList();
        this.network = new Network(driver);
        network.onBeforeRequestSent(this::recordRequestStarted);
        network.onResponseCompleted(this::recordResponseCompleted);
        network.onFetchError(this::recordFetchError);
    }

    private static Set<String> loadAllowList() {
        String raw = ConfigReader.get("networkErrorAllowList", "");
        if (raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toUnmodifiableSet());
    }

    private boolean isAllowListed(String url) {
        if (url == null) {
            return false;
        }
        String lowerUrl = url.toLowerCase();
        return allowList.stream().anyMatch(lowerUrl::contains);
    }

    private void recordRequestStarted(BeforeRequestSent event) {
        requestStartedAt.put(event.getRequest().getRequestId(), event.getTimestamp());
    }

    private void recordResponseCompleted(ResponseDetails event) {
        long durationMs = durationSince(event.getRequest().getRequestId(), event.getTimestamp());
        completed.add(new RequestOutcome(event.getRequest().getUrl(), event.getResponseData().getStatus(),
                durationMs, null));
    }

    private void recordFetchError(FetchError event) {
        long durationMs = durationSince(event.getRequest().getRequestId(), event.getTimestamp());
        completed.add(new RequestOutcome(event.getRequest().getUrl(), -1, durationMs, event.getErrorText()));
    }

    private long durationSince(String requestId, long completedAtMs) {
        Long startedAtMs = requestStartedAt.remove(requestId);
        return startedAtMs == null ? -1 : completedAtMs - startedAtMs;
    }

    /** Every request this monitor saw complete (successfully or not), in completion order. */
    public List<RequestOutcome> completedRequests() {
        return List.copyOf(completed);
    }

    /** Requests whose duration exceeded {@code thresholdMs} (duration-unknown entries are never "slow"). */
    public List<RequestOutcome> slowerThan(long thresholdMs) {
        return completed.stream().filter(r -> r.durationMs() >= 0 && r.durationMs() > thresholdMs)
                .collect(Collectors.toList());
    }

    /** HTTP 4xx/5xx responses and network-level failures (DNS, connection refused, etc.), minus the allow-list. */
    public List<RequestOutcome> failedRequests() {
        return completed.stream()
                .filter(RequestOutcome::isFailure)
                .filter(request -> !isAllowListed(request.url()))
                .collect(Collectors.toList());
    }

    /** The {@code limit} slowest completed requests, slowest first (duration-unknown entries sort last). */
    public List<RequestOutcome> slowest(int limit) {
        return completed.stream()
                .sorted(Comparator.comparingLong(RequestOutcome::durationMs).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public void close() {
        network.close();
    }

    /** {@code status} is -1 and {@code networkError} is non-null for a network-level failure (no HTTP response at all). */
    public record RequestOutcome(String url, int status, long durationMs, String networkError) {
        public boolean isHttpError() {
            return status >= 400;
        }

        public boolean isFailure() {
            return isHttpError() || networkError != null;
        }

        @Override
        public String toString() {
            String durationText = durationMs < 0 ? "?ms" : durationMs + "ms";
            String statusText = networkError != null ? "NETWORK ERROR: " + networkError : "HTTP " + status;
            return "[" + durationText + "] " + statusText + " " + url;
        }
    }
}
