package db;

import config.ConfigReader;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Exercises DbClient's real pool/query/update code paths against whatever database is already
 * configured (dbUrl — H2 in-memory by default on dev, see dev.properties). Deliberately reuses
 * DbClient's shared pool rather than pointing at its own dbUrl: DbClient is a process-wide
 * static singleton, and overriding dbUrl / calling closeAll() here would race the @database
 * Cucumber scenarios (steps.DbSteps) that may run concurrently under the suite's
 * parallel="methods" mode. Using its own uniquely-named table keeps this test from colliding
 * with any other table in play, including on a real database if dbUrl is overridden.
 */
public class DbClientTest {

    private static final String TABLE = "db_client_test_users";

    @BeforeClass(alwaysRun = true)
    public void setUpDatabase() {
        // Neither Hooks.globalSetup (Cucumber-only @BeforeAll) nor BaseTest (plain-TestNG,
        // only wraps LoginTest) is guaranteed to have run yet under the suite's parallel
        // execution — loadProperties() is idempotent, so calling it here directly is safe.
        ConfigReader.loadProperties();
        DbClient.update("CREATE TABLE IF NOT EXISTS " + TABLE + " (id INT PRIMARY KEY, username VARCHAR(50), active BOOLEAN)");
        DbClient.update("DELETE FROM " + TABLE);
    }

    @AfterClass(alwaysRun = true)
    public void tearDownDatabase() {
        // Leaves the shared pool itself open — it's process-wide and torn down exactly once,
        // in hooks.Hooks.globalTeardown's @AfterAll, after everything else has finished with it.
        DbClient.update("DROP TABLE IF EXISTS " + TABLE);
    }

    @Test(groups = "smoke")
    public void updateInsertsAndReturnsAffectedRowCount() {
        int affected = DbClient.update("INSERT INTO " + TABLE + " (id, username, active) VALUES (?, ?, ?)", 1, "tomsmith", true);
        Assert.assertEquals(affected, 1);
    }

    @Test(groups = "smoke", dependsOnMethods = "updateInsertsAndReturnsAffectedRowCount")
    public void queryReturnsRowsAsListOfMaps() {
        List<Map<String, Object>> rows = DbClient.query("SELECT id, username, active FROM " + TABLE + " WHERE id = ?", 1);

        Assert.assertEquals(rows.size(), 1);
        Assert.assertEquals(rows.get(0).get("USERNAME"), "tomsmith");
    }

    @Test(groups = "smoke", dependsOnMethods = "updateInsertsAndReturnsAffectedRowCount")
    public void queryMapsRowsToPojosViaRowMapper() {
        List<User> users = DbClient.query("SELECT id, username, active FROM " + TABLE + " WHERE id = ?",
                rs -> new User(rs.getInt("id"), rs.getString("username"), rs.getBoolean("active")),
                1);

        Assert.assertEquals(users.size(), 1);
        Assert.assertEquals(users.get(0).username(), "tomsmith");
        Assert.assertTrue(users.get(0).active());
    }

    @Test(groups = "smoke", dependsOnMethods = "updateInsertsAndReturnsAffectedRowCount")
    public void queryForSingleRowReturnsEmptyWhenNoMatch() {
        Optional<Map<String, Object>> row = DbClient.queryForSingleRow("SELECT id FROM " + TABLE + " WHERE id = ?", 999);

        Assert.assertTrue(row.isEmpty());
    }

    // Depends on every read-only test above, not just the insert, so it can't race them and
    // flip "active" to false while they're still asserting the original inserted value —
    // TestNG runs dependency chains serially even under the suite's parallel="methods" mode.
    @Test(groups = "smoke", dependsOnMethods = {
            "queryReturnsRowsAsListOfMaps",
            "queryMapsRowsToPojosViaRowMapper",
            "queryForSingleRowReturnsEmptyWhenNoMatch"
    })
    public void updateModifiesExistingRow() {
        int affected = DbClient.update("UPDATE " + TABLE + " SET active = ? WHERE id = ?", false, 1);
        Assert.assertEquals(affected, 1);

        Map<String, Object> row = DbClient.queryForSingleRow("SELECT active FROM " + TABLE + " WHERE id = ?", 1).orElseThrow();
        Assert.assertEquals(row.get("ACTIVE"), false);
    }

    private record User(int id, String username, boolean active) {
    }
}
