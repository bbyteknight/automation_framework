package reporting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Local, deterministic failure triage: scans this run's Allure result files and assigns each
 * failed/broken scenario one of the root-cause categories also defined in
 * src/test/resources/allure/categories.json (the regexes below must be kept in sync with that
 * file by hand), then writes one consolidated summary for the whole run. No network call, no
 * LLM, no cost — this is what actually satisfies "every failed run includes a categorised
 * failure summary" on its own.
 *
 * ai.LlmFailureSummarizer reads this class's output file to optionally turn it into a
 * natural-language summary via an LLM — that part is opt-in and never runs automatically (see
 * its own javadoc and CLAUDE.md "Keeping AI out of CI").
 *
 * Wired automatically from hooks.Hooks#globalTeardown (@AfterAll), so a plain `mvn test` always
 * produces this summary for the Cucumber portion of the suite. That hook only sees whatever
 * target/allure-results/*-result.json files exist at Cucumber's own teardown time — if other
 * TestNG test classes in testng.xml are still running in another parallel thread, their
 * failures may not be included yet. For a summary covering the entire suite, re-run this
 * class's main() after `mvn test` finishes:
 *
 *   mvn exec:java -Dexec.mainClass=reporting.FailureTriageReport
 */
public final class FailureTriageReport {

    private static final Logger logger = LogManager.getLogger(FailureTriageReport.class);
    private static final Path RESULTS_DIR = Path.of("target", "allure-results");
    public static final Path SUMMARY_FILE = RESULTS_DIR.resolve("failure-triage-summary.json");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private FailureTriageReport() {
    }

    public static void main(String[] args) {
        generate();
    }

    // Mirrors src/test/resources/allure/categories.json's traceRegex patterns. DOTALL so "."
    // also matches the newlines inside a stack trace, since Pattern.matches() requires the
    // whole (multi-line) trace/message text to match.
    private enum Category {
        PRODUCT_BUG("Product bug", ".*AssertionError.*"),
        LOCATOR("Locator problems",
                ".*(NoSuchElementException|StaleElementReferenceException|"
                        + "ElementNotInteractableException|ElementClickInterceptedException|"
                        + "InvalidSelectorException).*|.*TimeoutException.*utils\\.WaitUtils.*"),
        TIMEOUT("Timeout problems",
                ".*(java\\.util\\.concurrent\\.TimeoutException|ScriptTimeoutException|"
                        + "RemoteWebDriver\\.(get|executeAsyncScript)|DriverCommandExecutor\\.execute|"
                        + "JdkHttpClient).*"),
        ENVIRONMENT("Environment problems",
                ".*(exceptions\\.ConfigurationException|exceptions\\.RetryExhaustedException|"
                        + "WebDriverException|SessionNotCreatedException|InvalidSessionIdException|"
                        + "ConnectException|SocketTimeoutException|UnknownHostException).*"),
        DATA("Data problems",
                ".*(exceptions\\.TestDataException|NumberFormatException|JsonProcessingException|"
                        + "JsonParseException).*"),
        UNKNOWN("Uncategorised", null);

        private final String label;
        private final Pattern pattern;

        Category(String label, String regex) {
            this.label = label;
            this.pattern = regex == null ? null : Pattern.compile(regex, Pattern.DOTALL);
        }
    }

    public static void generate() {
        List<ObjectNode> failures = readFailures();

        if (failures.isEmpty()) {
            logger.info("✅ Failure triage: no failed/broken scenarios found in {}.", RESULTS_DIR);
            return;
        }

        writeSummary(failures);
        logCounts(failures);
    }

    private static List<ObjectNode> readFailures() {
        List<ObjectNode> failures = new ArrayList<>();
        if (!Files.isDirectory(RESULTS_DIR)) {
            return failures;
        }

        try (DirectoryStream<Path> resultFiles = Files.newDirectoryStream(RESULTS_DIR, "*-result.json")) {
            for (Path resultFile : resultFiles) {
                ObjectNode failure = readFailure(resultFile);
                if (failure != null) {
                    failures.add(failure);
                }
            }
        } catch (IOException e) {
            logger.warn("⚠️ Could not scan Allure results for failure triage: {}", e.getMessage());
        }
        return failures;
    }

    private static ObjectNode readFailure(Path resultFile) {
        try {
            JsonNode result = OBJECT_MAPPER.readTree(resultFile.toFile());
            String status = result.path("status").asText("");
            if (!"failed".equals(status) && !"broken".equals(status)) {
                return null;
            }

            String name = result.path("name").asText(resultFile.getFileName().toString());
            String message = result.path("statusDetails").path("message").asText("");
            String trace = result.path("statusDetails").path("trace").asText("");
            Category category = categorize(trace.isBlank() ? message : trace);

            ObjectNode failure = OBJECT_MAPPER.createObjectNode();
            failure.put("scenario", name);
            failure.put("status", status);
            failure.put("category", category.label);
            failure.put("message", message);
            return failure;
        } catch (IOException e) {
            logger.warn("⚠️ Could not read Allure result {}: {}", resultFile, e.getMessage());
            return null;
        }
    }

    private static Category categorize(String text) {
        for (Category category : Category.values()) {
            if (category.pattern != null && category.pattern.matcher(text).matches()) {
                return category;
            }
        }
        return Category.UNKNOWN;
    }

    private static void writeSummary(List<ObjectNode> failures) {
        ArrayNode array = OBJECT_MAPPER.createArrayNode();
        failures.forEach(array::add);
        try {
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(SUMMARY_FILE.toFile(), array);
        } catch (IOException e) {
            logger.warn("⚠️ Could not write failure triage summary: {}", e.getMessage());
        }
    }

    private static void logCounts(List<ObjectNode> failures) {
        Map<String, Long> counts = failures.stream()
                .collect(Collectors.groupingBy(failure -> failure.get("category").asText(), Collectors.counting()));
        logger.warn("🔍 Failure triage ({} failed/broken, see {}): {}", failures.size(), SUMMARY_FILE, counts);
    }
}
