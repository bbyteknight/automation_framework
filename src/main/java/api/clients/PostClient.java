package api.clients;

import api.models.request.CreatePostRequest;
import api.models.response.PostResponse;
import api.specs.RequestSpecFactory;
import api.specs.ResponseSpecFactory;
import io.restassured.RestAssured;

import java.util.Arrays;
import java.util.List;

/**
 * Wraps the /posts endpoints. Step definitions call these methods and get
 * back POJOs — no RestAssured.given()/.when()/.then() in step def classes.
 */
public class PostClient {

    private static final String SCHEMA = "schemas/post-schema.json";

    public List<PostResponse> getAllPosts() {
        return Arrays.asList(
                RestAssured.given().spec(RequestSpecFactory.baseSpec())
                        .get("/posts")
                        .then().statusCode(200)
                        .extract().as(PostResponse[].class)
        );
    }

    public PostResponse getPostById(int id) {
        return RestAssured.given().spec(RequestSpecFactory.baseSpec())
                .get("/posts/{id}", id)
                .then().statusCode(200)
                .spec(ResponseSpecFactory.matchesSchema(SCHEMA))
                .extract().as(PostResponse.class);
    }

    public PostResponse createPost(CreatePostRequest request) {
        return RestAssured.given().spec(RequestSpecFactory.baseSpec())
                .body(request)
                .post("/posts")
                .then().statusCode(201)
                .spec(ResponseSpecFactory.matchesSchema(SCHEMA))
                .extract().as(PostResponse.class);
    }

    public PostResponse updatePost(int id, CreatePostRequest request) {
        return RestAssured.given().spec(RequestSpecFactory.baseSpec())
                .body(request)
                .put("/posts/{id}", id)
                .then().statusCode(200)
                .spec(ResponseSpecFactory.matchesSchema(SCHEMA))
                .extract().as(PostResponse.class);
    }

    public void deletePost(int id) {
        RestAssured.given().spec(RequestSpecFactory.baseSpec())
                .delete("/posts/{id}", id)
                .then().statusCode(200);
    }
}
