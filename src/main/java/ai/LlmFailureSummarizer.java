package ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import config.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reporting.FailureTriageReport;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Optional, manual-only step: turns reporting.FailureTriageReport's local categorisation
 * ({@code target/allure-results/failure-triage-summary.json}) into a short natural-language
 * summary via an LLM. Deliberately NOT wired into hooks.Hooks or any Maven test phase — this is
 * a development-time aid a human runs by hand after a suite finishes, never a runtime
 * dependency of the test run itself:
 *
 * <pre>mvn exec:java -Dexec.mainClass=ai.LlmFailureSummarizer</pre>
 *
 * See CLAUDE.md "Keeping AI out of CI" — this project's rule is that AI-assisted tooling stays
 * out of CI and out of a run's pass/fail; the free, deterministic triage in
 * FailureTriageReport already satisfies this layer's Definition of Done on its own.
 *
 * Off by default and fails soft: with {@code aiFailureSummaryEnabled=false} (the default, see
 * dev.properties) or any of {@code llmApiUrl}/{@code llmApiKey}/{@code llmModel} unset, this
 * logs why it's skipping and returns rather than erroring — a convenience report should never
 * break a build. Targets a generic OpenAI-compatible {@code /chat/completions} request/response
 * shape, since that's widely supported (OpenAI itself, Azure OpenAI, many self-hosted LLM
 * gateways); point {@code llmApiUrl} at whichever endpoint your team is authorised to use, and
 * adjust {@link #buildRequestBody} / {@link #extractReply} here if that endpoint's schema
 * differs. {@code llmApiKey} is a secret — set it via an environment variable or a local
 * gitignored {@code .env} file (LLM_API_KEY), never in a committed {@code .properties} file;
 * see SECRETS.md.
 */
public final class LlmFailureSummarizer {

    private static final Logger logger = LogManager.getLogger(LlmFailureSummarizer.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path SUMMARY_OUTPUT = Path.of("target", "allure-results", "failure-triage-ai-summary.md");
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private LlmFailureSummarizer() {
    }

    public static void main(String[] args) {
        if (!ConfigReader.get("aiFailureSummaryEnabled", "false").equalsIgnoreCase("true")) {
            logger.info("🤖 AI failure summary skipped: aiFailureSummaryEnabled=false (see dev.properties).");
            return;
        }

        String apiUrl = ConfigReader.get("llmApiUrl", "");
        String apiKey = ConfigReader.get("llmApiKey", "");
        String model = ConfigReader.get("llmModel", "");
        if (apiUrl.isBlank() || apiKey.isBlank() || model.isBlank()) {
            logger.info("🤖 AI failure summary skipped: llmApiUrl/llmApiKey/llmModel are not all configured.");
            return;
        }

        if (!Files.exists(FailureTriageReport.SUMMARY_FILE)) {
            logger.info("🤖 AI failure summary skipped: {} not found — run the suite first.",
                    FailureTriageReport.SUMMARY_FILE);
            return;
        }

        try {
            String failuresJson = Files.readString(FailureTriageReport.SUMMARY_FILE, StandardCharsets.UTF_8);
            String reply = callLlm(apiUrl, apiKey, model, failuresJson);
            Files.writeString(SUMMARY_OUTPUT, reply, StandardCharsets.UTF_8);
            logger.info("🤖 AI failure summary written to {}", SUMMARY_OUTPUT);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.warn("⚠️ AI failure summary call failed, skipping: {}", e.getMessage());
        }
    }

    private static String callLlm(String apiUrl, String apiKey, String model, String failuresJson)
            throws IOException, InterruptedException {
        String requestBody = OBJECT_MAPPER.writeValueAsString(buildRequestBody(model, failuresJson));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        return extractReply(response.body());
    }

    /** OpenAI-compatible chat-completions request body — adjust here if your endpoint differs. */
    private static ObjectNode buildRequestBody(String model, String failuresJson) {
        String prompt = "Here is a categorised list of test failures from one automated test run, as JSON "
                + "(each entry has scenario, status, category, message). For each category present, give a "
                + "one-paragraph likely root cause and a suggested next step. Be concise.\n\n" + failuresJson;

        ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("model", model);
        ObjectNode message = body.putArray("messages").addObject();
        message.put("role", "user");
        message.put("content", prompt);
        return body;
    }

    /** OpenAI-compatible chat-completions response shape — adjust here if your endpoint differs. */
    private static String extractReply(String responseBody) throws IOException {
        JsonNode content = OBJECT_MAPPER.readTree(responseBody).at("/choices/0/message/content");
        return content.isMissingNode() ? responseBody : content.asText();
    }
}
