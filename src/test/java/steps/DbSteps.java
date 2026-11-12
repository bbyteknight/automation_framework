package steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import context.ScenarioContext;
import db.DbClient;
import db.QueryLoader;
import db.SqlIdentifiers;
import exceptions.FrameworkException;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generic, table/column-driven database assertions backed by {@link DbClient} and the SQL
 * templates under {@code resources/queries}. "Table"/"column" are plain strings rather than a
 * hardcoded schema, so the same steps work for any table a feature needs to validate. The
 * "compare to API response" steps join on an {@code id} column by convention — the common case
 * of looking up the DB record a just-created API resource would have produced.
 */
public class DbSteps {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ScenarioContext scenarioContext;

    public DbSteps(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    // Only ensures the table exists — deliberately never truncates it, since synced_objects is
    // a single shared table and scenarios can run concurrently (suite's parallel="methods"
    // mode). Each scenario creates its own uniquely-named/ID'd object, so rows from other
    // scenarios/runs are simply ignored by the WHERE-filtered assertions below.
    @Given("the synced_objects table is ready")
    public void theSyncedObjectsTableIsReady() {
        DbClient.update(QueryLoader.load("create_synced_objects_table"));
    }

    @And("the created object is synced to the database")
    public void theCreatedObjectIsSyncedToTheDatabase() {
        Map<String, Object> apiObject = toMap(scenarioContext.get("createdObject"));
        DbClient.update(QueryLoader.load("insert_synced_object"), apiObject.get("id"), apiObject.get("name"));
    }

    @Then("the {string} table should contain a record where {string} is {string}")
    public void theTableShouldContainARecordWhere(String table, String column, String value) {
        Map<String, Object> row = findSingleRow(table, Map.of(column, value));
        Assert.assertNotNull(row, "Expected \"" + table + "\" to contain a record where " + column + " = " + value);
    }

    @Then("the {string} table should contain a record where:")
    public void theTableShouldContainARecordWhereDataTable(String table, DataTable dataTable) {
        Map<String, String> conditions = dataTable.asMap(String.class, String.class);
        Map<String, Object> row = findSingleRow(table, conditions);
        Assert.assertNotNull(row, "Expected \"" + table + "\" to contain a record matching " + conditions);
    }

    @Then("the {string} table should contain a synced record for {string}")
    public void theTableShouldContainASyncedRecordFor(String table, String contextKey) {
        Map<String, Object> apiObject = toMap(scenarioContext.get(contextKey));
        String id = String.valueOf(apiObject.get("id"));
        Map<String, Object> row = findSingleRow(table, Map.of("id", id));
        Assert.assertNotNull(row, "Expected \"" + table + "\" to contain a synced record for id " + id);
    }

    @Then("the {string} column in the {string} table should be {string} where {string} is {string}")
    public void theColumnInTableShouldBeWhere(String column, String table, String expected, String whereColumn, String whereValue) {
        Map<String, Object> row = requireSingleRow(table, Map.of(whereColumn, whereValue));
        Assert.assertEquals(String.valueOf(row.get(column)), expected);
    }

    @Then("the {string} column in the {string} table should equal the {string} field of {string}")
    public void theColumnShouldEqualApiField(String dbColumn, String table, String apiField, String contextKey) {
        Map<String, Object> apiObject = toMap(scenarioContext.get(contextKey));
        String id = String.valueOf(apiObject.get("id"));
        Map<String, Object> row = requireSingleRow(table, Map.of("id", id));

        Assert.assertEquals(String.valueOf(row.get(dbColumn)), String.valueOf(apiObject.get(apiField)),
                dbColumn + " in \"" + table + "\" did not match \"" + apiField + "\" from " + contextKey);
    }

    private Map<String, Object> requireSingleRow(String table, Map<String, String> conditions) {
        Map<String, Object> row = findSingleRow(table, conditions);
        if (row == null) {
            throw new FrameworkException("❗ No record found in \"" + table + "\" matching " + conditions);
        }
        return row;
    }

    private Map<String, Object> findSingleRow(String table, Map<String, String> conditions) {
        SqlIdentifiers.validate(table);
        conditions.keySet().forEach(SqlIdentifiers::validate);

        List<String> fragments = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        for (Map.Entry<String, String> entry : conditions.entrySet()) {
            fragments.add(entry.getKey() + " = ?");
            params.add(entry.getValue());
        }

        String sql = QueryLoader.load("select_by_conditions")
                .replace("${table}", table)
                .replace("${conditions}", String.join(" AND ", fragments));

        List<Map<String, Object>> rows = DbClient.query(sql, params.toArray());
        if (rows.size() > 1) {
            throw new FrameworkException("❗ Expected at most one record in \"" + table + "\" matching "
                    + conditions + " but found " + rows.size());
        }
        return rows.isEmpty() ? null : rows.get(0);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object apiObject) {
        if (apiObject == null) {
            throw new FrameworkException("❗ No value found in the scenario context to compare against the database");
        }
        return OBJECT_MAPPER.convertValue(apiObject, Map.class);
    }

    @Then("the seeded order should exist in the {string} table with status {string}")
    public void theSeededOrderShouldExistWithStatus(String table, String expectedStatus) {
        String orderId = scenarioContext.get("seededOrderId", String.class);
        Map<String, Object> row = requireSingleRow(table, Map.of("id", orderId));
        Assert.assertEquals(String.valueOf(row.get("status")), expectedStatus);
    }

    @When("the seeded order's status is updated to {string}")
    public void theSeededOrdersStatusIsUpdatedTo(String status) {
        String orderId = scenarioContext.get("seededOrderId", String.class);
        DbClient.update("UPDATE orders SET status = ? WHERE id = ?", status, orderId);
    }
}
