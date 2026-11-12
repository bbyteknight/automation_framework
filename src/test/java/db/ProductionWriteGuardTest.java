package db;

import exceptions.FrameworkException;
import org.testng.annotations.Test;

/**
 * Pure logic tests for {@link ProductionWriteGuard} — deliberately don't go through
 * {@link DbClient} or mutate the "env"/"dbUrl" system properties, since those are read
 * concurrently by other config-loading code throughout the suite under parallel execution.
 */
public class ProductionWriteGuardTest {

    @Test(groups = "smoke")
    public void allowsWritesToNonProductionTargets() {
        ProductionWriteGuard.check("dev", "jdbc:h2:mem:automation_dev");
        ProductionWriteGuard.check("qa", "jdbc:postgresql://qa-db.internal:5432/app");
    }

    @Test(groups = "smoke", expectedExceptions = FrameworkException.class)
    public void blocksWritesWhenEnvLooksLikeProduction() {
        ProductionWriteGuard.check("production", "jdbc:postgresql://db.internal:5432/app");
    }

    @Test(groups = "smoke", expectedExceptions = FrameworkException.class)
    public void blocksWritesWhenEnvIsAnUppercaseAbbreviation() {
        ProductionWriteGuard.check("PROD", "jdbc:h2:mem:test");
    }

    @Test(groups = "smoke", expectedExceptions = FrameworkException.class)
    public void blocksWritesWhenDbUrlLooksLikeProductionEvenIfEnvDoesNot() {
        ProductionWriteGuard.check("qa", "jdbc:postgresql://prod-orders.internal:5432/app");
    }
}
