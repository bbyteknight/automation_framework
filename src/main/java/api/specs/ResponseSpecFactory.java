package api.specs;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.ResponseSpecification;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

/** Common response assertions shared across API tests. */
public final class ResponseSpecFactory {

    private ResponseSpecFactory() {
    }

    public static ResponseSpecification expectStatus(int statusCode) {
        return new ResponseSpecBuilder()
                .expectStatusCode(statusCode)
                .build();
    }

    public static ResponseSpecification expectJson(int statusCode) {
        return new ResponseSpecBuilder()
                .expectStatusCode(statusCode)
                .expectContentType(ContentType.JSON)
                .build();
    }

    /**
     * Validates the response body against a JSON Schema file on the classpath,
     * e.g. matchesSchema("schemas/post-schema.json") for a file under
     * src/test/resources/schemas.
     */
    public static ResponseSpecification matchesSchema(String schemaClasspathPath) {
        return new ResponseSpecBuilder()
                .expectBody(matchesJsonSchemaInClasspath(schemaClasspathPath))
                .build();
    }
}
