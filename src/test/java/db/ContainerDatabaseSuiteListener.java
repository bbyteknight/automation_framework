package db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testng.ISuite;
import org.testng.ISuiteListener;

/**
 * Registered in testng.xml's {@code <listeners>} (not a plain {@code @BeforeSuite} method) so it
 * reliably runs once, before EVERY {@code <test>} block in the suite — including
 * {@code db.DbClientTest} and {@code runners.CucumberTestRunner} — regardless of the suite's
 * {@code parallel="methods"} mode. Only acts when run with {@code -Ddb=container} (pipeline/CI
 * use, needs a Docker daemon reachable from the agent); a bare {@code mvn test} leaves
 * {@link DbClient} on its H2 in-memory default (see {@code dev.properties}) and this listener
 * does nothing.
 */
public class ContainerDatabaseSuiteListener implements ISuiteListener {

    private static final Logger logger = LogManager.getLogger(ContainerDatabaseSuiteListener.class);
    private static final String POSTGRES_IMAGE = "postgres:16-alpine";

    private static volatile PostgreSQLContainer<?> container;

    @Override
    public void onStart(ISuite suite) {
        if (!"container".equalsIgnoreCase(System.getProperty("db", ""))) {
            return;
        }

        container = new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE));
        container.start();

        System.setProperty("dbUrl", container.getJdbcUrl());
        System.setProperty("dbUsername", container.getUsername());
        System.setProperty("dbPassword", container.getPassword());
        logger.info("🐳 Started a throwaway Postgres container for this run — dbUrl now points at it [{}].",
                container.getJdbcUrl());

        applySchemaAndSeedData();
    }

    @Override
    public void onFinish(ISuite suite) {
        if (container != null) {
            container.stop();
            logger.info("🐳 Stopped the throwaway Postgres container.");
            container = null;
        }
    }

    private void applySchemaAndSeedData() {
        DbClient.update(QueryLoader.load("create_orders_table"));
        DbClient.update(QueryLoader.load("create_synced_objects_table"));
        DbClient.update(QueryLoader.load("create_container_health_check_table"));
        DbClient.update(QueryLoader.load("seed_container_health_check"), "baseline",
                "Seeded by db.ContainerDatabaseSuiteListener on container startup");
        logger.info("🐳 Applied schema and seed data to the container database.");
    }
}
