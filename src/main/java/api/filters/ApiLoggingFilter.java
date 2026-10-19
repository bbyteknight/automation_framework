package api.filters;

import io.qameta.allure.Allure;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.StringHelper;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Logs every API request/response through Log4j (console + target/logs, thread-aware,
 * same pipeline as the rest of the framework) instead of RestAssured's own
 * stdout-only request/response logging, and attaches the same details to the active
 * Allure test (Allure.addAttachment correlates to whichever test is running on this
 * thread, so no Scenario/test reference needs to be threaded through API client code).
 */
public class ApiLoggingFilter implements Filter {

    private static final Logger logger = LogManager.getLogger(ApiLoggingFilter.class);

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                            FilterableResponseSpecification responseSpec,
                            FilterContext ctx) {
        String headers = maskedHeaders(StreamSupport.stream(requestSpec.getHeaders().spliterator(), false));
        String requestBody = requestSpec.getBody() != null ? requestSpec.getBody().toString() : "";

        logger.info("API Request -> {} {}\nHeaders: {}{}",
                requestSpec.getMethod(), requestSpec.getURI(), headers,
                requestBody.isBlank() ? "" : "\nRequest Body: " + requestBody);

        Allure.addAttachment("API Request -> " + requestSpec.getMethod() + " " + requestSpec.getURI(),
                "text/plain",
                "Headers: " + headers + (requestBody.isBlank() ? "" : "\n\nBody:\n" + requestBody),
                ".txt");

        Response response = ctx.next(requestSpec, responseSpec);

        String responseBody = response.getBody().asPrettyString();
        logger.info("API Response <- {} ({} ms)\nResponse Body: {}",
                response.getStatusCode(), response.getTime(), responseBody);

        Allure.addAttachment("API Response <- " + response.getStatusCode(), "text/plain", responseBody, ".txt");

        return response;
    }

    /** Logs every header's name, but masks the value of any that typically carry a credential
     * (Authorization, API keys, etc.) so a request log never leaks a live token. */
    private String maskedHeaders(Stream<Header> headers) {
        return headers
                .map(header -> header.getName() + ": " + (isSensitive(header.getName())
                        ? StringHelper.mask(header.getValue(), 4)
                        : header.getValue()))
                .collect(Collectors.joining(", "));
    }

    private boolean isSensitive(String headerName) {
        String name = headerName.toLowerCase();
        return name.equals("authorization") || name.contains("api-key") || name.contains("api_key");
    }
}
