package db;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Optional;

/**
 * Proves {@link ContainerDatabaseSuiteListener} actually started a real, throwaway Postgres
 * container and applied schema/seed data to it — only meaningful when the suite is run with
 * {@code -Ddb=container} (e.g. {@code mvn test -Ddb=container -Dgroups=smoke}), so it skips
 * itself otherwise rather than forcing every local/default run to need Docker.
 */
public class ContainerDatabaseTest {

    @Test(groups = "smoke")
    public void seededHealthCheckRowIsPresentInTheContainerDatabase() {
        if (!"container".equalsIgnoreCase(System.getProperty("db", ""))) {
            throw new SkipException("Only meaningful when run with -Ddb=container — see ContainerDatabaseSuiteListener.");
        }

        Optional<Map<String, Object>> row =
                DbClient.queryForSingleRow("SELECT id, note FROM container_health_check WHERE id = ?", "baseline");

        Assert.assertTrue(row.isPresent(), "Expected the container-startup seed row to be present.");
        Assert.assertEquals(row.get().get("NOTE"), "Seeded by db.ContainerDatabaseSuiteListener on container startup");
    }
}
