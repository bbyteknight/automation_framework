package api.filters;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
        logger.info("API Request -> {} {}{}",
                requestSpec.getMethod(),
                requestSpec.getURI(),
                requestSpec.getBody() != null ? "\nRequest Body: " + requestSpec.getBody() : "");

        Response response = ctx.next(requestSpec, responseSpec);

        logger.info("API Response <- {} ({} ms)\nResponse Body: {}",
                response.getStatusCode(),
                response.getTime(),
                response.getBody().asPrettyString());

        return response;
    }
}
