package api.filters;

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
 * stdout-only request/response logging.
 *
 * This is also the hook point for attaching request/response details to a test
 * report: once Allure replaces ExtentReports, add Allure.addAttachment(...) calls
 * here so every API call shows up in the report without touching callers.
 */
public class ApiLoggingFilter implements Filter {

    private static final Logger logger = LogManager.getLogger(ApiLoggingFilter.class);

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                            FilterableResponseSpecification responseSpec,
                            FilterContext ctx) {
        logger.info("API Request -> {} {}\nHeaders: {}{}",
                requestSpec.getMethod(),
                requestSpec.getURI(),
                maskedHeaders(StreamSupport.stream(requestSpec.getHeaders().spliterator(), false)),
                requestSpec.getBody() != null ? "\nRequest Body: " + requestSpec.getBody() : "");

        Response response = ctx.next(requestSpec, responseSpec);

        logger.info("API Response <- {} ({} ms)\nResponse Body: {}",
                response.getStatusCode(),
                response.getTime(),
                response.getBody().asPrettyString());

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
