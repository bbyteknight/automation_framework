package utils;

import config.ConfigReader;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.log.ConsoleLogEntry;
import org.openqa.selenium.bidi.log.JavascriptLogEntry;
import org.openqa.selenium.bidi.log.LogLevel;
import org.openqa.selenium.bidi.module.LogInspector;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Wraps WebDriver BiDi's {@code LogInspector} module to capture the browser's own console output
 * (console.log/warn/error/info) and uncaught JavaScript exceptions for a scenario — without
 * touching the page under test. DriverFactory requests the {@code webSocketUrl} capability on
 * every session (see utils.NetworkMockUtil for the same prerequisite), so BiDi is available here
 * too, local or remote.
 *
 * <p>Two independent BiDi log event types are subscribed to: {@code onConsoleEntry} (explicit
 * {@code console.*} calls — only its {@code ERROR}-level entries, i.e. {@code console.error()},
 * count as a JS error here) and {@code onJavaScriptException} (uncaught runtime exceptions, which
 * never go through {@code console.*} at all). Both are recorded for the full console-log
 * attachment; only these two feed {@link #unignoredJsErrors()}.
 *
 * <p>Known-harmless noise (e.g. a sandboxed test page's own broken third-party script, or a
 * browser extension injecting its own console error) is filtered via a configurable allow-list
 * ({@code jsErrorAllowList} — comma-separated, case-insensitive substrings matched against the
 * error text) rather than hardcoded here, so each environment/suite can tune it without a code
 * change. {@code failOnJsError} (hooks.Hooks reads it, not this class) decides whether a
 * non-empty {@link #unignoredJsErrors()} should actually fail the scenario.
 */
public class JsErrorMonitor implements AutoCloseable {

    /** ScenarioContext key hooks.Hooks uses to find/close/read this instance. */
    public static final String CONTEXT_KEY = "jsErrorMonitor";

    private final LogInspector logInspector;
    private final List<String> consoleLogLines = new CopyOnWriteArrayList<>();
    private final List<String> jsErrors = new CopyOnWriteArrayList<>();
    private final Set<String> allowList;

    public JsErrorMonitor(WebDriver driver) {
        this.allowList = loadAllowList();
        this.logInspector = new LogInspector(driver);
        logInspector.onConsoleEntry(this::recordConsoleEntry);
        logInspector.onJavaScriptException(this::recordJsException);
    }

    private void recordConsoleEntry(ConsoleLogEntry entry) {
        consoleLogLines.add("[" + entry.getLevel() + "] console." + entry.getMethod() + ": " + entry.getText());
        if (entry.getLevel() == LogLevel.ERROR && !isAllowListed(entry.getText())) {
            jsErrors.add(entry.getText());
        }
    }

    private void recordJsException(JavascriptLogEntry entry) {
        consoleLogLines.add("[EXCEPTION] " + entry.getText());
        if (!isAllowListed(entry.getText())) {
            jsErrors.add(entry.getText());
        }
    }

    private boolean isAllowListed(String text) {
        if (text == null) {
            return false;
        }
        String lowerText = text.toLowerCase();
        return allowList.stream().anyMatch(lowerText::contains);
    }

    private static Set<String> loadAllowList() {
        String raw = ConfigReader.get("jsErrorAllowList", "");
        if (raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean hasConsoleLog() {
        return !consoleLogLines.isEmpty();
    }

    /** Every captured console/exception line, in capture order — one per line, for a text attachment. */
    public String consoleLogText() {
        return String.join(System.lineSeparator(), consoleLogLines);
    }

    /** Error-level console entries and uncaught JS exceptions, minus anything matching the allow-list. */
    public List<String> unignoredJsErrors() {
        return Collections.unmodifiableList(jsErrors);
    }

    @Override
    public void close() {
        logInspector.close();
    }
}
