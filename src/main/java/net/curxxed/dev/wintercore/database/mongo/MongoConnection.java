package net.curxxed.dev.wintercore.database.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bson.Document;

public final class MongoConnection implements AutoCloseable {

    private final MongoClient client;
    @Getter
    private final MongoDatabase database;

    public MongoConnection(WinterCore plugin) {
        String uri = plugin.getConfig().getString("mongodb.uri", "mongodb://localhost:27017");
        String name = plugin.getConfig().getString("mongodb.database", "wintercore");

        this.client = MongoClients.create(uri);
        this.database = client.getDatabase(name);
    }

    public MongoCollection<Document> collection(String name) {
        return database.getCollection(name);
    }

    @Override
    public void close() {
        client.close();
    }
}