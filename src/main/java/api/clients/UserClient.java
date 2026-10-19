package api.clients;

import api.models.request.CreateUserRequest;
import api.models.response.UserResponse;
import api.specs.RequestSpecFactory;
import api.specs.ResponseSpecFactory;
import io.restassured.RestAssured;

import java.util.Arrays;
import java.util.List;

/**
 * Wraps the /users endpoints. Step definitions call these methods and get
 * back POJOs — no RestAssured.given()/.when()/.then() in step def classes.
 */
public class UserClient {

    private static final String SCHEMA = "schemas/user-schema.json";

    public List<UserResponse> getAllUsers() {
        return Arrays.asList(
                RestAssured.given().spec(RequestSpecFactory.baseSpec())
                        .get("/users")
                        .then().statusCode(200)
                        .extract().as(UserResponse[].class)
        );
    }

    public UserResponse getUserById(int id) {
        return RestAssured.given().spec(RequestSpecFactory.baseSpec())
                .get("/users/{id}", id)
                .then().statusCode(200)
                .spec(ResponseSpecFactory.matchesSchema(SCHEMA))
                .extract().as(UserResponse.class);
    }

    public UserResponse createUser(CreateUserRequest request) {
        return RestAssured.given().spec(RequestSpecFactory.baseSpec())
                .body(request)
                .post("/users")
                .then().statusCode(201)
                .spec(ResponseSpecFactory.matchesSchema(SCHEMA))
                .extract().as(UserResponse.class);
    }

    public void deleteUser(int id) {
        RestAssured.given().spec(RequestSpecFactory.baseSpec())
                .delete("/users/{id}", id)
                .then().statusCode(200);
    }
}
