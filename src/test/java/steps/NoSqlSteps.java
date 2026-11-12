package steps;

import com.mongodb.client.model.Filters;
import db.MongoClientWrapper;
import db.RedisClient;
import exceptions.FrameworkException;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.testng.Assert;

import java.util.Map;

/**
 * Generic document (MongoDB) and key (Redis) validation steps, mirroring {@code steps.DbSteps}'
 * schema-agnostic shape: "collection"/"field"/"key" are plain strings rather than a hardcoded
 * domain, so the same steps work for any document store or cache a feature needs to validate.
 */
public class NoSqlSteps {

    // --- MongoDB: document validation ---

    @Given("a document is inserted into the {string} collection with:")
    public void aDocumentIsInsertedIntoTheCollectionWith(String collection, DataTable dataTable) {
        Document document = new Document(dataTable.asMap(String.class, String.class));
        MongoClientWrapper.insertOne(collection, document);
    }

    @Then("the {string} collection should contain a document where {string} is {string}")
    public void theCollectionShouldContainADocumentWhere(String collection, String field, String value) {
        Document document = MongoClientWrapper.findOne(collection, Filters.eq(field, value));
        Assert.assertNotNull(document, "Expected \"" + collection + "\" to contain a document where " + field + " = " + value);
    }

    @Then("the {string} collection should contain a document where:")
    public void theCollectionShouldContainADocumentWhereDataTable(String collection, DataTable dataTable) {
        Map<String, String> conditions = dataTable.asMap(String.class, String.class);
        Document document = MongoClientWrapper.findOne(collection, buildFilter(conditions));
        Assert.assertNotNull(document, "Expected \"" + collection + "\" to contain a document matching " + conditions);
    }

    @Then("the {string} field of the document in {string} where {string} is {string} should be {string}")
    public void theFieldOfTheDocumentShouldBe(String field, String collection, String whereField, String whereValue, String expected) {
        Document document = MongoClientWrapper.findOne(collection, Filters.eq(whereField, whereValue));
        if (document == null) {
            throw new FrameworkException("❗ No document found in \"" + collection + "\" where " + whereField + " = " + whereValue);
        }
        Assert.assertEquals(String.valueOf(document.get(field)), expected);
    }

    @And("the document is removed from the {string} collection where {string} is {string}")
    public void theDocumentIsRemovedFromTheCollectionWhere(String collection, String field, String value) {
        MongoClientWrapper.deleteMany(collection, Filters.eq(field, value));
    }

    private Bson buildFilter(Map<String, String> conditions) {
        Bson filter = null;
        for (Map.Entry<String, String> entry : conditions.entrySet()) {
            Bson condition = Filters.eq(entry.getKey(), entry.getValue());
            filter = filter == null ? condition : Filters.and(filter, condition);
        }
        return filter;
    }

    // --- Redis: key validation ---

    @Given("the {string} key is set to {string} in Redis")
    public void theKeyIsSetToInRedis(String key, String value) {
        RedisClient.set(key, value);
    }

    @Then("the {string} key should exist in Redis")
    public void theKeyShouldExistInRedis(String key) {
        Assert.assertTrue(RedisClient.exists(key), "Expected Redis key \"" + key + "\" to exist");
    }

    @Then("the {string} key should not exist in Redis")
    public void theKeyShouldNotExistInRedis(String key) {
        Assert.assertFalse(RedisClient.exists(key), "Expected Redis key \"" + key + "\" to not exist");
    }

    @Then("the {string} key in Redis should equal {string}")
    public void theKeyInRedisShouldEqual(String key, String expectedValue) {
        Assert.assertEquals(RedisClient.get(key), expectedValue);
    }

    @And("the {string} key is deleted from Redis")
    public void theKeyIsDeletedFromRedis(String key) {
        RedisClient.delete(key);
    }
}
