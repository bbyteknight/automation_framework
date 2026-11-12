package db;

import exceptions.FrameworkException;

/**
 * Blocks {@link DbClient#update} from ever executing against what looks like a production
 * database — checked on every write (not just the seed/cleanup hooks), since a bug in any step
 * could otherwise run arbitrary SQL against a live environment. Reads via {@link DbClient#query}
 * are intentionally not guarded, since read-only validation against production is a legitimate
 * use case this framework doesn't need to block.
 */
final class ProductionWriteGuard {

    private ProductionWriteGuard() {
    }

    static void check(String env, String dbUrl) {
        if (looksLikeProduction(env) || looksLikeProduction(dbUrl)) {
            throw new FrameworkException("❌ Refusing to run a database write — env=\"" + env
                    + "\" and/or dbUrl appear to target production. DbClient.update() never runs "
                    + "there; point dbUrl at a dev/qa/staging/test database instead.");
        }
    }

    // Deliberately broad (a plain "contains", not an exact "prod"/"production" match) so a
    // dbUrl like "jdbc:postgresql://prod-orders.internal:5432/app" is also caught, not just an
    // exact env=production — a false positive here is far cheaper than a false negative.
    private static boolean looksLikeProduction(String value) {
        return value != null && value.toLowerCase().contains("prod");
    }
}
