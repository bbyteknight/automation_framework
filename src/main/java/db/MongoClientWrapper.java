package db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import config.ConfigReader;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable MongoDB utility, mirroring {@link DbClient}'s shape for the relational stores: one
 * process-wide {@link MongoClient}, lazily initialized from {@code mongoUri}/{@code
 * mongoDatabase} config, so any step/test can assert against a document store the same way it
 * asserts against a relational one.
 */
public final class MongoClientWrapper {

    private static final Logger logger = LogManager.getLogger(MongoClientWrapper.class);

    private static volatile MongoClient mongoClient;

    private MongoClientWrapper() {
    }

    public static synchronized void init() {
        if (mongoClient != null) {
            return;
        }
        String uri = ConfigReader.get("mongoUri");
        mongoClient = MongoClients.create(uri);
        logger.info("🍃 MongoDB client initialized [{}].", maskUri(uri));
    }

    private static MongoClient getClient() {
        MongoClient current = mongoClient;
        if (current == null) {
            init();
            current = mongoClient;
        }
        return current;
    }

    private static MongoDatabase database() {
        return getClient().getDatabase(ConfigReader.get("mongoDatabase"));
    }

    public static MongoCollection<Document> collection(String name) {
        return database().getCollection(name);
    }

    /** Returns the first document matching the filter, or {@code null} if none match. */
    public static Document findOne(String collectionName, Bson filter) {
        Document result = collection(collectionName).find(filter).first();
        attachEvidence("Mongo findOne", collectionName, filter, result);
        return result;
    }

    public static List<Document> find(String collectionName, Bson filter) {
        List<Document> results = new ArrayList<>();
        collection(collectionName).find(filter).into(results);
        attachEvidence("Mongo find", collectionName, filter, results);
        return results;
    }

    /** Inserts a document, guarded against production, and returns its {@code _id} as a string. */
    public static String insertOne(String collectionName, Document document) {
        ProductionWriteGuard.check(ConfigReader.get("env", ""), ConfigReader.get("mongoUri", ""));
        collection(collectionName).insertOne(document);
        Object id = document.get("_id");
        attachEvidence("Mongo insertOne", collectionName, document, "Inserted _id: " + id);
        return String.valueOf(id);
    }

    /** Deletes every document matching the filter, guarded against production. */
    public static long deleteMany(String collectionName, Bson filter) {
        ProductionWriteGuard.check(ConfigReader.get("env", ""), ConfigReader.get("mongoUri", ""));
        long deleted = collection(collectionName).deleteMany(filter).getDeletedCount();
        attachEvidence("Mongo deleteMany", collectionName, filter, "Deleted: " + deleted);
        return deleted;
    }

    // Guarded the same way as db.DbClient's attachQueryEvidence — a no-op outside an active
    // Allure test/step context (e.g. when called from a plain TestNG test not wrapped by
    // AllureTestNg), to avoid noisy AllureLifecycle errors.
    private static void attachEvidence(String label, String collectionName, Object query, Object result) {
        if (Allure.getLifecycle().getCurrentTestCaseOrStep().isEmpty()) {
            return;
        }
        String content = "Collection: " + collectionName + "\n\nQuery/Document:\n" + query + "\n\nResult:\n" + result;
        Allure.addAttachment(label, "text/plain", content, ".txt");
    }

    // Avoids leaking a "mongodb://user:password@host" credential pair into logs.
    private static String maskUri(String uri) {
        return uri.replaceAll("(?i)://([^:/@]+):([^@]+)@", "://$1:***@");
    }

    /** Closes the client. Safe to call even if {@link #init()} was never reached. */
    public static synchronized void closeAll() {
        if (mongoClient != null) {
            mongoClient.close();
            logger.info("🔌 MongoDB client closed.");
        }
        mongoClient = null;
    }
}
