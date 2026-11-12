package db;

import config.ConfigReader;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Reusable Redis utility, mirroring {@link DbClient}'s shape for cache checks: one process-wide
 * {@link JedisPool}, lazily initialized from {@code redisHost}/{@code redisPort}/{@code
 * redisPassword} config, so any step/test can assert cache state the same way it asserts
 * database state.
 */
public final class RedisClient {

    private static final Logger logger = LogManager.getLogger(RedisClient.class);
    private static final int CONNECTION_TIMEOUT_MS = 2000;

    private static volatile JedisPool jedisPool;

    private RedisClient() {
    }

    public static synchronized void init() {
        if (jedisPool != null) {
            return;
        }
        String host = ConfigReader.get("redisHost", "localhost");
        int port = Integer.parseInt(ConfigReader.get("redisPort", "6379"));
        String password = ConfigReader.get("redisPassword", "");

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(Integer.parseInt(ConfigReader.get("redisMaxPoolSize", "10")));

        jedisPool = password.isBlank()
                ? new JedisPool(poolConfig, host, port)
                : new JedisPool(poolConfig, host, port, CONNECTION_TIMEOUT_MS, password);

        logger.info("🧰 Redis client initialized [{}:{}].", host, port);
    }

    private static JedisPool getPool() {
        JedisPool current = jedisPool;
        if (current == null) {
            init();
            current = jedisPool;
        }
        return current;
    }

    public static String get(String key) {
        try (Jedis jedis = getPool().getResource()) {
            String value = jedis.get(key);
            attachEvidence("Redis GET", key, value);
            return value;
        }
    }

    public static boolean exists(String key) {
        try (Jedis jedis = getPool().getResource()) {
            boolean result = jedis.exists(key);
            attachEvidence("Redis EXISTS", key, result);
            return result;
        }
    }

    public static Long ttl(String key) {
        try (Jedis jedis = getPool().getResource()) {
            Long result = jedis.ttl(key);
            attachEvidence("Redis TTL", key, result);
            return result;
        }
    }

    /** Sets a key's value, guarded against production. */
    public static String set(String key, String value) {
        ProductionWriteGuard.check(ConfigReader.get("env", ""), ConfigReader.get("redisHost", ""));
        try (Jedis jedis = getPool().getResource()) {
            String result = jedis.set(key, value);
            attachEvidence("Redis SET", key, value);
            return result;
        }
    }

    /** Deletes a key, guarded against production. Returns the number of keys removed (0 or 1). */
    public static long delete(String key) {
        ProductionWriteGuard.check(ConfigReader.get("env", ""), ConfigReader.get("redisHost", ""));
        try (Jedis jedis = getPool().getResource()) {
            long result = jedis.del(key);
            attachEvidence("Redis DEL", key, result);
            return result;
        }
    }

    // Guarded the same way as db.DbClient's attachQueryEvidence — a no-op outside an active
    // Allure test/step context, to avoid noisy AllureLifecycle errors.
    private static void attachEvidence(String label, String key, Object result) {
        if (Allure.getLifecycle().getCurrentTestCaseOrStep().isEmpty()) {
            return;
        }
        Allure.addAttachment(label, "text/plain", "Key: " + key + "\n\nResult:\n" + result, ".txt");
    }

    /** Closes the pool. Safe to call even if {@link #init()} was never reached. */
    public static synchronized void closeAll() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            logger.info("🔌 Redis client closed.");
        }
        jedisPool = null;
    }
}
