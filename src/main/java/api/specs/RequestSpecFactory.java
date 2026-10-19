package api.specs;

import api.clients.AuthManager;
import api.filters.ApiLoggingFilter;
import config.ConfigReader;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * Builds reusable REST Assured request specs so individual API clients/tests
 * don't each hardcode baseUri, content type, or auth headers.
 */
public final class RequestSpecFactory {

    private RequestSpecFactory() {
    }

    /** Unauthenticated base spec: baseUri (from ConfigReader) + JSON content type + request/response logging. */
    public static RequestSpecification baseSpec() {
        return baseSpec(ConfigReader.get("apiBaseUrl"));
    }

    /** Same as {@link #baseSpec()}, but against an explicit base URI (for clients targeting a different host). */
    public static RequestSpecification baseSpec(String baseUri) {
        return new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new ApiLoggingFilter())
                .build();
    }

    /** Same as {@link #baseSpec()}, plus a Bearer token resolved via AuthManager. */
    public static RequestSpecification authenticatedSpec() {
        return new RequestSpecBuilder()
                .addRequestSpecification(baseSpec())
                .addHeader("Authorization", "Bearer " + AuthManager.getAccessToken())
                .build();
    }
}
