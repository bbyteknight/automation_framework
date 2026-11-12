package db;

import exceptions.FrameworkException;

import java.util.regex.Pattern;

/**
 * Validates that a table/column name is a safe SQL identifier before it's substituted into a
 * query template — needed wherever a table/column name comes from something other than a fixed
 * string literal (Gherkin text, hook parameters), since JDBC can only bind actual values, never
 * identifiers, as "?" parameters.
 */
public final class SqlIdentifiers {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    private SqlIdentifiers() {
    }

    public static void validate(String identifier) {
        if (identifier == null || !SAFE_IDENTIFIER.matcher(identifier).matches()) {
            throw new FrameworkException("❗ Not a safe table/column identifier: " + identifier);
        }
    }
}
