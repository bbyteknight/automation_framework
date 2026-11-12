package hooks;

import com.github.fppt.jedismock.RedisServer;
import config.ConfigReader;
import db.MongoClientWrapper;
import db.RedisClient;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Self-contained NoSQL test infrastructure for {@code @nosql} scenarios: starts an in-process,
 * pure-Java fake MongoDB (mongo-java-server) and fake Redis (jedis-mock) — no native binary, no
 * docker — and points {@code db.MongoClientWrapper}/{@code db.RedisClient} at them via system
 * properties, the same "zero setup on dev" idea as {@code dbUrl}'s H2 default (see
 * dev.properties), just resolved dynamically since these fakes bind to an ephemeral port.
 * A real environment overrides {@code mongoUri}/{@code redisHost}/{@code redisPort} via config
 * instead, in which case this class does nothing — see CONTRIBUTING.md "Validating NoSQL stores".
 */
public class NoSqlTestInfraHooks {

    private static final Logger logger = LogManager.getLogger(NoSqlTestInfraHooks.class);

    private static volatile MongoServer mongoServer;
    private static volatile RedisServer redisServer;

    @Before("@nosql")
    public synchronized void startEmbeddedServersIfNeeded() {
        if (mongoServer == null && ConfigReader.get("mongoUri", "").isBlank()) {
            mongoServer = new MongoServer(new MemoryBackend());
            String connectionString = mongoServer.bindAndGetConnectionString();
            System.setProperty("mongoUri", connectionString);
            System.setProperty("mongoDatabase", "automation_test");
            logger.info("🍃 Embedded fake MongoDB started [{}].", connectionString);
        }

        if (redisServer == null && ConfigReader.get("redisHost", "").isBlank()) {
            redisServer = RedisServer.newRedisServer();
            try {
                redisServer.start();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to start the embedded fake Redis server", e);
            }
            System.setProperty("redisHost", redisServer.getHost());
            System.setProperty("redisPort", String.valueOf(redisServer.getBindPort()));
            logger.info("🧰 Embedded fake Redis started [{}:{}].", redisServer.getHost(), redisServer.getBindPort());
        }
    }

    @AfterAll
    public static void stopEmbeddedServers() {
        MongoClientWrapper.closeAll();
        RedisClient.closeAll();

        if (mongoServer != null) {
            mongoServer.shutdown();
            mongoServer = null;
            logger.info("🔌 Embedded fake MongoDB stopped.");
        }
        if (redisServer != null) {
            try {
                redisServer.stop();
            } catch (IOException e) {
                logger.warn("⚠️ Failed to stop the embedded fake Redis server: {}", e.getMessage());
            }
            redisServer = null;
            logger.info("🔌 Embedded fake Redis stopped.");
        }
    }
}
