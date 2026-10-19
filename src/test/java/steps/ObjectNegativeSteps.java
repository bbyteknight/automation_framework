package steps;

import api.clients.ObjectClient;
import api.clients.StatusCodeClient;
import context.ScenarioContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.testng.Assert;

public class ObjectNegativeSteps {

    private final ScenarioContext scenarioContext;
    private final ObjectClient objectClient = new ObjectClient();
    private final StatusCodeClient statusCodeClient = new StatusCodeClient();

    public ObjectNegativeSteps(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @When("the user requests an object with id {string}")
    public void theUserRequestsAnObjectWithId(String id) {
        scenarioContext.set("lastResponse", objectClient.getObjectByIdRaw(id));
    }

    @When("the user submits a malformed create-object request")
    public void theUserSubmitsAMalformedCreateObjectRequest() {
        scenarioContext.set("lastResponse", objectClient.createObjectWithRawBody("{this is not valid json"));
    }

    @When("the user submits a create-object request with no body")
    public void theUserSubmitsACreateObjectRequestWithNoBody() {
        scenarioContext.set("lastResponse", objectClient.createObjectWithNoBody());
    }

    @When("the user makes a request to a protected endpoint without credentials")
    public void theUserMakesARequestToAProtectedEndpointWithoutCredentials() {
        scenarioContext.set("lastResponse", statusCodeClient.getStatus(401));
    }

    @Then("the response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int expectedStatus) {
        Response response = scenarioContext.get("lastResponse", Response.class);
        Assert.assertEquals(response.statusCode(), expectedStatus);
    }

    @And("the response should contain an error message")
    public void theResponseShouldContainAnErrorMessage() {
        Response response = scenarioContext.get("lastResponse", Response.class);
        Assert.assertNotNull(response.jsonPath().getString("error"), "Expected an 'error' field in the response body");
    }
}
