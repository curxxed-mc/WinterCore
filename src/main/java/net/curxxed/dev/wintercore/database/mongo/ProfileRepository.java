package net.curxxed.dev.wintercore.database.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public final class ProfileRepository {

    private final MongoCollection<Document> profiles;

    public ProfileRepository(MongoCollection<Document> profiles) {
        this.profiles = profiles;
    }

    public Document findById(UUID uuid) {
        return profiles.find(Filters.eq("_id", uuid.toString())).first();
    }

    public Document findByNameRegex(String playerName) {
        return profiles.find(Filters.regex("name", "^" + Pattern.quote(playerName) + "$", "i")).first();
    }

    public Iterable<Document> findByIp(String ip) {
        return profiles.find(Filters.eq("ips", ip));
    }

    public Iterable<Document> findByAnyIp(Set<String> ips) {
        return profiles.find(Filters.in("ips", ips));
    }

    public String getRank(UUID uuid) {
        Document doc = findById(uuid);
        return doc != null ? doc.getString("rank") : null;
    }

    public String getTag(UUID uuid) {
        Document doc = findById(uuid);
        return doc != null ? doc.getString("tag") : null;
    }

    public String getChatColor(UUID uuid) {
        Document doc = findById(uuid);
        return doc != null ? doc.getString("chatColor") : null;
    }

    public String getPlayerName(UUID uuid) {
        Document doc = findById(uuid);
        return doc != null ? doc.getString("name") : null;
    }

    public long getLong(UUID uuid, String field, long fallback) {
        Document doc = findById(uuid);
        if (doc == null) {
            return fallback;
        }

        Object value = doc.get(field);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ignored) {
            }
        }

        return fallback;
    }

    public void upsertField(UUID uuid, String field, Object value) {
        profiles.updateOne(
                Filters.eq("_id", uuid.toString()),
                Updates.set(field, value),
                new UpdateOptions().upsert(true)
        );
    }

    public void push(UUID uuid, String field, Object value) {
        profiles.updateOne(
                Filters.eq("_id", uuid.toString()),
                Updates.push(field, value),
                new UpdateOptions().upsert(true)
        );
    }

    public void addToSet(UUID uuid, String field, Object value) {
        profiles.updateOne(
                Filters.eq("_id", uuid.toString()),
                Updates.addToSet(field, value),
                new UpdateOptions().upsert(true)
        );
    }

    public void unset(UUID uuid, String field) {
        profiles.updateOne(Filters.eq("_id", uuid.toString()), Updates.unset(field));
    }

    public void inc(UUID uuid, String field, long amount) {
        profiles.updateOne(
                Filters.eq("_id", uuid.toString()),
                Updates.inc(field, amount),
                new UpdateOptions().upsert(true)
        );
    }

    public boolean decrementIfEnough(UUID uuid, String field, long amount) {
        UpdateResult result = profiles.updateOne(
                Filters.and(Filters.eq("_id", uuid.toString()), Filters.gte(field, amount)),
                Updates.inc(field, -amount)
        );
        return result.getModifiedCount() > 0;
    }

    public void updateMany(Bson filter, Bson update) {
        profiles.updateMany(filter, update);
    }
}
