package testdata;

import net.datafaker.Faker;

/**
 * Generates unique test data at run time via Datafaker, so repeated/parallel runs against shared
 * environments (e.g. the public api.restful-api.dev sandbox) never collide on a fixed literal.
 */
public final class TestDataGenerator {

    private static final Faker FAKER = new Faker();

    private TestDataGenerator() {
    }

    /** Appends a short unique suffix to a base name, e.g. "Apple MacBook Pro 16 (a1b2c3)". */
    public static String uniqueName(String baseName) {
        return baseName + " (" + FAKER.random().hex(6).toLowerCase() + ")";
    }

    public static String uniqueEmail() {
        return FAKER.internet().emailAddress();
    }

    public static String uniqueUsername() {
        return FAKER.name().username() + FAKER.number().digits(4);
    }
}
