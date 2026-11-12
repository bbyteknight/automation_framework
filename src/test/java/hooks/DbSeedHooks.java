package hooks;

import context.ScenarioContext;
import db.DbClient;
import db.DbTestDataManager;
import db.QueryLoader;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

/**
 * Tag-driven DB seed/cleanup hooks. Each {@code @seed-*} tag inserts the fixture data a feature
 * needs directly into the database (fast, no UI/API round-trip) before the scenario runs; every
 * seeded row is tracked via {@link DbTestDataManager} so the shared cleanup sweep below removes
 * it afterward, pass or fail — no scenario tagged {@code @seed-*} is allowed to leave data behind.
 * Adding a new {@code @seed-<thing>} tag means adding its own {@code @Before} here and extending
 * the {@code @After} tag expression to include it.
 */
public class DbSeedHooks {

    private static final Logger logger = LogManager.getLogger(DbSeedHooks.class);

    private final ScenarioContext scenarioContext;
    private final DbTestDataManager dbTestDataManager = new DbTestDataManager();

    public DbSeedHooks(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Before("@seed-orders")
    public void seedOrders() {
        DbClient.update(QueryLoader.load("create_orders_table"));

        String orderId = "seed-" + UUID.randomUUID();
        DbClient.update(QueryLoader.load("seed_order"), orderId, "Seeded Customer", "PENDING");
        dbTestDataManager.track(scenarioContext, "orders", "id", orderId);
        scenarioContext.set("seededOrderId", orderId);

        logger.info("🌱 Seeded order {} for this scenario.", orderId);
    }

    @After("@seed-orders")
    public void cleanupSeededData() {
        dbTestDataManager.cleanup(scenarioContext);
    }
}
