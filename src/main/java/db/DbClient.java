package db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import config.ConfigReader;
import exceptions.FrameworkException;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Reusable database utility backed by a single HikariCP connection pool, shared across
 * whichever database the active environment points at (Postgres/MySQL/SQL Server/Oracle —
 * see pom.xml for the JDBC drivers on the classpath). Lets any step/test query the database
 * in any environment via {@code dbUrl}/{@code dbUsername}/{@code dbPassword} config, without
 * each caller managing its own Connection lifecycle.
 */
public final class DbClient {

    private static final Logger logger = LogManager.getLogger(DbClient.class);

    private static volatile HikariDataSource dataSource;

    private DbClient() {
    }

    /**
     * Builds the pool from config (idempotent — a second call is a no-op if already initialized).
     * Called lazily by the first query/update, so most tests never need to call this directly.
     */
    public static synchronized void init() {
        if (dataSource != null) {
            return;
        }

        String url = ConfigReader.get("dbUrl");
        String username = ConfigReader.get("dbUsername", "");
        String password = ConfigReader.get("dbPassword", "");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        if (!username.isBlank()) {
            config.setUsername(username);
        }
        if (!password.isBlank()) {
            config.setPassword(password);
        }
        config.setPoolName("automation-db-pool");
        config.setMaximumPoolSize(Integer.parseInt(ConfigReader.get("dbMaxPoolSize", "10")));
        config.setMinimumIdle(Integer.parseInt(ConfigReader.get("dbMinIdle", "2")));
        config.setConnectionTimeout(Long.parseLong(ConfigReader.get("dbConnectionTimeoutMs", "30000")));
        config.setIdleTimeout(Long.parseLong(ConfigReader.get("dbIdleTimeoutMs", "600000")));
        config.setMaxLifetime(Long.parseLong(ConfigReader.get("dbMaxLifetimeMs", "1800000")));

        dataSource = new HikariDataSource(config);
        logger.info("🗄️ Database connection pool initialized [{}].", maskUrl(url));
    }

    private static HikariDataSource getDataSource() {
        HikariDataSource current = dataSource;
        if (current == null) {
            init();
            current = dataSource;
        }
        return current;
    }

    /** Borrows a pooled connection. Caller is responsible for closing it (try-with-resources). */
    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    /**
     * Runs a SELECT, mapping every row to a {@code Map<columnLabel, value>}. The map's keys are
     * case-insensitive, since databases differ on how they fold unquoted identifiers (H2/Oracle
     * default to uppercase, Postgres to lowercase, MySQL depends on the OS) — callers can look
     * columns up using whatever case they wrote in the SQL.
     */
    public static List<Map<String, Object>> query(String sql, Object... params) {
        return query(sql, DbClient::mapRowToMap, params);
    }

    /** Runs a SELECT, mapping every row to a POJO (or any type) via the given {@link RowMapper}. */
    public static <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParams(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(mapper.map(resultSet));
                }
                attachQueryEvidence("DB Query", sql, params, results);
                return results;
            }
        } catch (SQLException e) {
            throw new FrameworkException("❌ Query failed: " + sql, e);
        }
    }

    /** Convenience for a query expected to return at most one row. */
    public static Optional<Map<String, Object>> queryForSingleRow(String sql, Object... params) {
        List<Map<String, Object>> rows = query(sql, params);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        if (rows.size() > 1) {
            throw new FrameworkException("❌ Expected at most one row but got " + rows.size() + " for query: " + sql);
        }
        return Optional.of(rows.get(0));
    }

    /** Runs an INSERT/UPDATE/DELETE (or DDL), returning the affected row count. */
    public static int update(String sql, Object... params) {
        ProductionWriteGuard.check(ConfigReader.get("env", ""), ConfigReader.get("dbUrl", ""));
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParams(statement, params);
            int affected = statement.executeUpdate();
            attachQueryEvidence("DB Update", sql, params, "Rows affected: " + affected);
            return affected;
        } catch (SQLException e) {
            throw new FrameworkException("❌ Update failed: " + sql, e);
        }
    }

    private static void bindParams(PreparedStatement statement, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }

    private static Map<String, Object> mapRowToMap(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        Map<String, Object> row = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            row.put(metaData.getColumnLabel(i), resultSet.getObject(i));
        }
        return row;
    }

    // Avoids leaking credentials embedded in a JDBC URL (e.g. "...?user=x&password=y") into logs.
    private static String maskUrl(String url) {
        return url.replaceAll("(?i)(password|pwd)=[^&;]*", "$1=***");
    }

    // Attaches every executed query/update plus its result to the active Allure test (same
    // Allure.addAttachment pattern as api.filters.ApiLoggingFilter — thread-bound, so no
    // Scenario/test reference needs to be threaded through step/client code). Guarded on an
    // active test/step actually existing: unlike API steps (always Cucumber-driven), DbClient
    // is also exercised directly from plain TestNG tests not wrapped by AllureTestNg (e.g.
    // DbClientTest) — attaching with no active context logs noisy AllureLifecycle errors and
    // was observed to disturb fixture tracking on unrelated concurrently-running threads.
    private static void attachQueryEvidence(String label, String sql, Object[] params, Object result) {
        if (Allure.getLifecycle().getCurrentTestCaseOrStep().isEmpty()) {
            return;
        }
        String content = "SQL:\n" + sql
                + "\n\nParameters: " + Arrays.toString(params)
                + "\n\nResult:\n" + result;
        Allure.addAttachment(label, "text/plain", content, ".txt");
    }

    /** Closes the pool. Safe to call even if {@link #init()} was never reached. */
    public static synchronized void closeAll() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("🔌 Database connection pool closed.");
        }
        dataSource = null;
    }
}
