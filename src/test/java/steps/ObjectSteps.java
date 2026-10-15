package steps;

import api.clients.ObjectClient;
import api.models.request.CreateObjectRequest;
import api.models.response.ObjectResponse;
import context.ScenarioContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ObjectSteps {

    private final ScenarioContext scenarioContext;
    private final ObjectClient objectClient = new ObjectClient();

    public ObjectSteps(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @When("the user retrieves all objects")
    public void theUserRetrievesAllObjects() {
        scenarioContext.set("allObjects", objectClient.getAllObjects());
    }

    @Then("the list of objects should not be empty")
    public void theListOfObjectsShouldNotBeEmpty() {
        List<?> objects = scenarioContext.get("allObjects", List.class);
        Assert.assertFalse(objects.isEmpty(), "Expected at least one object from GET /objects");
    }

    @When("the user creates an object with name {string} and data")
    public void theUserCreatesAnObjectWithNameAndData(String name, DataTable dataTable) {
        CreateObjectRequest request = CreateObjectRequest.builder()
                .name(name)
                .data(toTypedMap(dataTable))
                .build();
        scenarioContext.set("createdObject", objectClient.createObject(request));
    }

    @Then("the object should be created successfully")
    public void theObjectShouldBeCreatedSuccessfully() {
        ObjectResponse created = scenarioContext.get("createdObject", ObjectResponse.class);
        Assert.assertNotNull(created.getId(), "Created object should have an id");
    }

    @When("the user retrieves the created object")
    public void theUserRetrievesTheCreatedObject() {
        ObjectResponse created = scenarioContext.get("createdObject", ObjectResponse.class);
        scenarioContext.set("fetchedObject", objectClient.getObjectById(created.getId()));
    }

    @Then("the retrieved object should have name {string}")
    public void theRetrievedObjectShouldHaveName(String expectedName) {
        ObjectResponse fetched = scenarioContext.get("fetchedObject", ObjectResponse.class);
        Assert.assertEquals(fetched.getName(), expectedName);
    }

    @When("the user updates the created object with name {string} and data")
    public void theUserUpdatesTheCreatedObjectWithNameAndData(String name, DataTable dataTable) {
        ObjectResponse created = scenarioContext.get("createdObject", ObjectResponse.class);
        CreateObjectRequest request = CreateObjectRequest.builder()
                .name(name)
                .data(toTypedMap(dataTable))
                .build();
        scenarioContext.set("createdObject", objectClient.updateObject(created.getId(), request));
    }

    @Then("the object should be updated successfully")
    public void theObjectShouldBeUpdatedSuccessfully() {
        ObjectResponse updated = scenarioContext.get("createdObject", ObjectResponse.class);
        Assert.assertEquals(updated.getData().get("color"), "silver");
    }

    @When("the user deletes the created object")
    public void theUserDeletesTheCreatedObject() {
        ObjectResponse created = scenarioContext.get("createdObject", ObjectResponse.class);
        objectClient.deleteObject(created.getId());
        scenarioContext.set("deletedObjectId", created.getId());
    }

    @Then("the object should be deleted successfully")
    public void theObjectShouldBeDeletedSuccessfully() {
        Assert.assertNotNull(scenarioContext.get("deletedObjectId", String.class));
    }

    @And("the deleted object should no longer exist")
    public void theDeletedObjectShouldNoLongerExist() {
        String id = scenarioContext.get("deletedObjectId", String.class);
        Assert.assertEquals(objectClient.getObjectByIdRaw(id).statusCode(), 404);
    }

    @And("the created object name should be {string}")
    public void theCreatedObjectNameShouldBe(String expectedName) {
        ObjectResponse created = scenarioContext.get("createdObject", ObjectResponse.class);
        Assert.assertEquals(created.getName(), expectedName);
    }

    @And("the created object price should be {double}")
    public void theCreatedObjectPriceShouldBe(double expectedPrice) {
        ObjectResponse created = scenarioContext.get("createdObject", ObjectResponse.class);
        double actualPrice = ((Number) created.getData().get("price")).doubleValue();
        Assert.assertEquals(actualPrice, expectedPrice, 0.001);
    }

    private Map<String, Object> toTypedMap(DataTable dataTable) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : dataTable.asMap(String.class, String.class).entrySet()) {
            result.put(entry.getKey(), parseValue(entry.getValue()));
        }
        return result;
    }

    private Object parseValue(String raw) {
        if (raw.matches("-?\\d+")) {
            return Integer.parseInt(raw);
        }
        if (raw.matches("-?\\d+\\.\\d+")) {
            return Double.parseDouble(raw);
        }
        return raw;
    }
}
