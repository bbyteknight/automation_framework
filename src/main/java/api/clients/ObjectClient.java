package api.clients;

import api.models.request.CreateObjectRequest;
import api.models.request.PatchObjectRequest;
import api.models.response.DeleteResponse;
import api.models.response.ObjectResponse;
import api.specs.RequestSpecFactory;
import api.specs.ResponseSpecFactory;
import config.ConfigReader;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Arrays;
import java.util.List;

/**
 * Wraps the /objects endpoints on api.restful-api.dev. Step definitions call
 * these methods and get back POJOs — no RestAssured.given()/.when()/.then()
 * in step def classes.
 *
 * The *Raw methods return the unasserted Response for negative-path tests
 * (invalid id, malformed body, missing body) where the caller needs to
 * inspect the status code/error body itself rather than have this client
 * assert a 2xx and throw.
 */
public class ObjectClient {

    private static final String BASE_PATH = "/objects";
    private static final String SCHEMA = "schemas/object-schema.json";

    private RequestSpecification spec() {
        return RequestSpecFactory.baseSpec(ConfigReader.get("restfulApiBaseUrl"));
    }

    public List<ObjectResponse> getAllObjects() {
        return Arrays.asList(
                RestAssured.given().spec(spec())
                        .get(BASE_PATH)
                        .then().statusCode(200)
                        .extract().as(ObjectResponse[].class)
        );
    }

    public ObjectResponse getObjectById(String id) {
        return RestAssured.given().spec(spec())
                .get(BASE_PATH + "/{id}", id)
                .then().statusCode(200)
                .spec(ResponseSpecFactory.matchesSchema(SCHEMA))
                .extract().as(ObjectResponse.class);
    }

    public Response getObjectByIdRaw(String id) {
        return RestAssured.given().spec(spec())
                .get(BASE_PATH + "/{id}", id);
    }

    public ObjectResponse createObject(CreateObjectRequest request) {
        return RestAssured.given().spec(spec())
                .body(request)
                .post(BASE_PATH)
                .then().statusCode(200)
                .spec(ResponseSpecFactory.matchesSchema(SCHEMA))
                .extract().as(ObjectResponse.class);
    }

    public Response createObjectWithRawBody(String rawJsonBody) {
        return RestAssured.given().spec(spec())
                .body(rawJsonBody)
                .post(BASE_PATH);
    }

    public Response createObjectWithNoBody() {
        return RestAssured.given().spec(spec())
                .post(BASE_PATH);
    }

    public ObjectResponse updateObject(String id, CreateObjectRequest request) {
        return RestAssured.given().spec(spec())
                .body(request)
                .put(BASE_PATH + "/{id}", id)
                .then().statusCode(200)
                .spec(ResponseSpecFactory.matchesSchema(SCHEMA))
                .extract().as(ObjectResponse.class);
    }

    public ObjectResponse patchObject(String id, PatchObjectRequest request) {
        return RestAssured.given().spec(spec())
                .body(request)
                .patch(BASE_PATH + "/{id}", id)
                .then().statusCode(200)
                .spec(ResponseSpecFactory.matchesSchema(SCHEMA))
                .extract().as(ObjectResponse.class);
    }

    public DeleteResponse deleteObject(String id) {
        return RestAssured.given().spec(spec())
                .delete(BASE_PATH + "/{id}", id)
                .then().statusCode(200)
                .extract().as(DeleteResponse.class);
    }
}
