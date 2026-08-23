package net.curxxed.dev.wintercore.database.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bson.Document;

import java.util.concurrent.TimeUnit;

public final class MongoConnection implements AutoCloseable {

    private final MongoClient client;
    @Getter
    private final MongoDatabase database;

    public MongoConnection(WinterCore plugin) {
        String uri = plugin.getConfig().getString("mongodb.uri", "mongodb://localhost:27017");
        String name = plugin.getConfig().getString("mongodb.database", "wintercore");
        long timeoutMillis = Math.max(1_000L,
                plugin.getConfig().getLong("mongodb.server-selection-timeout-ms", 5_000L));

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .applyToClusterSettings(builder ->
                        builder.serverSelectionTimeout(timeoutMillis, TimeUnit.MILLISECONDS))
                .build();
        this.client = MongoClients.create(settings);
        this.database = client.getDatabase(name);
    }

    public MongoCollection<Document> collection(String name) {
        return database.getCollection(name);
    }

    public void ping() {
        database.runCommand(new Document("ping", 1));
    }

    @Override
    public void close() {
        client.close();
    }
}
