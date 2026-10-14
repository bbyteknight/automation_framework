package context;

/**
 * Process-wide state set up once per test run (e.g. @BeforeAll/@AfterAll),
 * as opposed to ScenarioContext which is scoped to a single scenario.
 */
public class GlobalContext {

    private static String authToken;

    private GlobalContext() {
    }

    public static String getAuthToken() {
        return authToken;
    }

    public static void setAuthToken(String token) {
        authToken = token;
    }

    public static void clear() {
        authToken = null;
    }
}
