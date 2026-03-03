package net.curxxed.dev.wintercore.auth.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.util.UUID;

public class AuthRepository {

    private static final String FIELD_UUID   = "uuid";
    private static final String FIELD_SECRET = "secret";

    private final MongoCollection<Document> collection;

    public AuthRepository(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    public String getSecret(UUID uuid) {
        Document doc = collection.find(Filters.eq(FIELD_UUID, uuid.toString())).first();
        return doc != null ? doc.getString(FIELD_SECRET) : null;
    }

    public void saveSecret(UUID uuid, String secret) {
        Document doc = new Document(FIELD_UUID, uuid.toString())
                .append(FIELD_SECRET, secret);
        collection.replaceOne(
                Filters.eq(FIELD_UUID, uuid.toString()),
                doc,
                new ReplaceOptions().upsert(true)
        );
    }

    public void deleteSecret(UUID uuid) {
        collection.deleteOne(Filters.eq(FIELD_UUID, uuid.toString()));
    }

    public boolean hasSecret(UUID uuid) {
        return collection.countDocuments(Filters.eq(FIELD_UUID, uuid.toString())) > 0;
    }
}