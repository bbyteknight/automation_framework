package db;

import exceptions.FrameworkException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads named SQL templates from {@code resources/queries/<name>.sql} instead of having them
 * hardcoded as Java string literals, so queries can be reviewed/edited like any other SQL file
 * and reused across step definitions. Templates may contain {@code ${placeholder}} tokens for
 * structural pieces (table/column names) that can't be bound as JDBC "?" parameters — callers
 * are responsible for substituting those with values already validated as safe identifiers.
 */
public final class QueryLoader {

    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();
    private static final String QUERIES_DIR = "queries/";

    private QueryLoader() {
    }

    public static String load(String name) {
        return CACHE.computeIfAbsent(name, QueryLoader::readFromClasspath);
    }

    private static String readFromClasspath(String name) {
        String path = QUERIES_DIR + name + ".sql";
        try (InputStream inputStream = QueryLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new FrameworkException("❗ SQL query file not found on classpath: " + path);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new FrameworkException("❌ Failed to read SQL query file: " + path, e);
        }
    }
}
