package api.clients;

import api.specs.RequestSpecFactory;
import io.restassured.RestAssured;
import io.restassured.response.Response;

/**
 * Wraps httpbin.org's /status/{code} echo endpoint.
 *
 * Used only to exercise the framework's handling of a 401 response:
 * api.restful-api.dev (used by ObjectClient) has no authentication at all,
 * so it cannot produce a real 401 — this hits a dedicated public endpoint
 * built for returning arbitrary status codes instead of faking one against
 * an API that doesn't have that behavior.
 */
public class StatusCodeClient {

    private static final String BASE_URI = "https://httpbin.org";

    public Response getStatus(int statusCode) {
        return RestAssured.given().spec(RequestSpecFactory.baseSpec(BASE_URI))
                .get("/status/{code}", statusCode);
    }
}
