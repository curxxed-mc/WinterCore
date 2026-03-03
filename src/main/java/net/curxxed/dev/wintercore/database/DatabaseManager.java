package net.curxxed.dev.wintercore.database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import lombok.Getter;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bson.Document;
import org.bukkit.Bukkit;

import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;

public class DatabaseManager {

    @Getter
    private static DatabaseManager instance;

    private final WinterCore plugin;
    private MongoClient mongoClient;
    @Getter
    private MongoDatabase database;
    private MongoCollection<Document> profiles;

    public static RankCache rankCache = new RankCache();
    public static TagCache tagCache = new TagCache();

    public DatabaseManager(WinterCore plugin) {
        this.plugin = plugin;
        setupDataSource();
    }

    public static DatabaseManager init(WinterCore plugin) {
        instance = new DatabaseManager(plugin);
        return instance;
    }

    private void setupDataSource() {
        try {
            String uri = plugin.getConfig().getString("mongodb.uri", "mongodb://localhost:27017");
            String dbName = plugin.getConfig().getString("mongodb.database", "wintercore");

            this.mongoClient = MongoClients.create(uri);
            this.database = mongoClient.getDatabase(dbName);
            this.profiles = database.getCollection("profiles");

            plugin.getLogger().info("MongoDB connection initialized successfully.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize MongoDB connection!", e);
        }
    }

    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            plugin.getLogger().info("MongoDB connection closed.");
        }
    }

    // --- Helper for Commands ---

    /**
     * Synchronously fetches the player name from the database.
     * Useful for async commands where Bukkit cache might be empty.
     */
    public String getPlayerName(UUID uuid) {
        try {
            Document doc = profiles.find(Filters.eq("_id", uuid.toString())).first();
            if (doc != null && doc.containsKey("name")) {
                return doc.getString("name");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not fetch player name for " + uuid, e);
        }
        return null;
    }

    // --- Ranks ---

    public void setRank(UUID uuid, String rank) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                profiles.updateOne(
                        Filters.eq("_id", uuid.toString()),
                        Updates.set("rank", rank),
                        new UpdateOptions().upsert(true)
                );
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not update rank for " + uuid, e);
            }
        });
    }

    public void setRankWithMeta(UUID uuid, String rank, UUID grantedBy, long grantedAt, Long expiresAt, String reason) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Update active rank and set metadata
                profiles.updateOne(
                        Filters.eq("_id", uuid.toString()),
                        Updates.combine(
                                Updates.set("rank", rank),
                                Updates.set("rank_meta", new Document()
                                        .append("grantedBy", grantedBy.toString())
                                        .append("grantedAt", grantedAt)
                                        .append("expiresAt", expiresAt)
                                        .append("reason", reason))
                        ),
                        new UpdateOptions().upsert(true)
                );
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not update rank with meta for " + uuid, e);
            }
        });
    }

    public void getRank(UUID uuid, Consumer<String> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Document doc = profiles.find(Filters.eq("_id", uuid.toString())).first();
                String rank = (doc != null && doc.containsKey("rank")) ? doc.getString("rank") : "Default";

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    rankCache.set(uuid, rank);
                    callback.accept(rank);
                });
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not fetch rank for " + uuid, e);
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept("Default"));
            }
        });
    }

    // --- Tags ---

    public void setPlayerTag(UUID uuid, String tag) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                profiles.updateOne(
                        Filters.eq("_id", uuid.toString()),
                        Updates.set("tag", tag),
                        new UpdateOptions().upsert(true)
                );
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not set tag for " + uuid, e);
            }
        });
    }

    public void getPlayerTag(UUID uuid, Consumer<String> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Document doc = profiles.find(Filters.eq("_id", uuid.toString())).first();
                String tag = (doc != null) ? doc.getString("tag") : null;

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    tagCache.set(uuid, tag);
                    callback.accept(tag);
                });
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not fetch tag for " + uuid, e);
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }

    // --- Bans ---

    public void banPlayer(UUID uuid, String reason, Instant expiration) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Document banDoc = new Document()
                        .append("reason", reason)
                        .append("bannedAt", System.currentTimeMillis())
                        .append("expiration", expiration != null ? expiration.toEpochMilli() : null);

                profiles.updateOne(
                        Filters.eq("_id", uuid.toString()),
                        Updates.combine(
                                Updates.set("activeBan", banDoc),
                                Updates.push("banHistory", banDoc)
                        ),
                        new UpdateOptions().upsert(true)
                );
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not ban player " + uuid, e);
            }
        });
    }

    public void unbanPlayer(UUID uuid) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                profiles.updateOne(
                        Filters.eq("_id", uuid.toString()),
                        Updates.unset("activeBan")
                );
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not unban player " + uuid, e);
            }
        });
    }

    public void isPlayerBanned(UUID uuid, Consumer<Boolean> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Document doc = profiles.find(Filters.eq("_id", uuid.toString())).first();
                boolean isBanned = false;

                if (doc != null && doc.containsKey("activeBan")) {
                    Document banDoc = (Document) doc.get("activeBan");
                    Long expiration = banDoc.getLong("expiration");

                    if (expiration == null || expiration > System.currentTimeMillis()) {
                        isBanned = true;
                    } else {
                        // Auto-clean expired ban
                        unbanPlayer(uuid);
                    }
                }

                boolean finalStatus = isBanned;
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(finalStatus));
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not check ban status for " + uuid, e);
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }

    public void getBanReason(UUID uuid, Consumer<String> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Document doc = profiles.find(Filters.eq("_id", uuid.toString())).first();
                String reason = null;
                if (doc != null && doc.containsKey("activeBan")) {
                    reason = ((Document) doc.get("activeBan")).getString("reason");
                }
                String finalReason = reason;
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(finalReason));
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not fetch ban reason for " + uuid, e);
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }

    public void getBanDetails(UUID uuid, Consumer<Map<String, Object>> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Document doc = profiles.find(Filters.eq("_id", uuid.toString())).first();
                Map<String, Object> details = new HashMap<>();
                if (doc != null && doc.containsKey("activeBan")) {
                    Document banDoc = (Document) doc.get("activeBan");
                    details.put("expiration", banDoc.get("expiration"));
                    details.put("reason", banDoc.getString("reason"));
                }
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(details));
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not fetch ban details for " + uuid, e);
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }

    public void removeExpiredBans() {
        // Mongo operation to remove expired bans in bulk
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> profiles.updateMany(
                Filters.and(
                        Filters.exists("activeBan"),
                        Filters.lte("activeBan.expiration", System.currentTimeMillis()),
                        Filters.ne("activeBan.expiration", null)
                ),
                Updates.unset("activeBan")
        ));
    }

    // --- Mutes ---

    public void mutePlayer(UUID targetUUID, String reason, String issuer, Instant expirationTime) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Document muteDoc = new Document()
                        .append("reason", reason)
                        .append("issuer", issuer)
                        .append("expiration", expirationTime != null ? expirationTime.toEpochMilli() : null)
                        .append("date", System.currentTimeMillis());

                profiles.updateOne(
                        Filters.eq("_id", targetUUID.toString()),
                        Updates.combine(
                                Updates.set("activeMute", muteDoc),
                                Updates.push("muteHistory", muteDoc)
                        ),
                        new UpdateOptions().upsert(true)
                );
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not mute player " + targetUUID, e);
            }
        });
    }

    public void unmutePlayer(UUID uuid) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                profiles.updateOne(Filters.eq("_id", uuid.toString()), Updates.unset("activeMute"));
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not unmute player " + uuid, e);
            }
        });
    }

    public void isPlayerMuted(UUID uuid, Consumer<Boolean> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Document doc = profiles.find(Filters.eq("_id", uuid.toString())).first();
                boolean isMuted = false;
                if (doc != null && doc.containsKey("activeMute")) {
                    Document muteDoc = (Document) doc.get("activeMute");
                    Long expiration = muteDoc.getLong("expiration");
                    if (expiration == null || expiration > System.currentTimeMillis()) {
                        isMuted = true;
                    } else {
                        unmutePlayer(uuid);
                    }
                }
                boolean finalStatus = isMuted;
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(finalStatus));
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not check mute status for " + uuid, e);
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }

    public void getMuteDetails(UUID uuid, Consumer<Map<String, Object>> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Document doc = profiles.find(Filters.eq("_id", uuid.toString())).first();
                Map<String, Object> details = new HashMap<>();
                if (doc != null && doc.containsKey("activeMute")) {
                    Document muteDoc = (Document) doc.get("activeMute");
                    Long expiration = muteDoc.getLong("expiration");

                    if (expiration == null || expiration > System.currentTimeMillis()) {
                        details.put("reason", muteDoc.getString("reason"));
                        details.put("expiration", expiration);
                    }
                }
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(details));
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not fetch mute details for " + uuid, e);
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }

    // --- Warnings & Kicks ---

    public void addWarning(String playerName, String reason, String issuer) {
        getUUIDByName(playerName, uuid -> {
            if (uuid == null) {
                plugin.getLogger().warning("Could not add warning for " + playerName + " (UUID not found)");
                return;
            }
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                Document warning = new Document()
                        .append("id", UUID.randomUUID().toString())
                        .append("reason", reason)
                        .append("issuer", issuer)
                        .append("warnedAt", System.currentTimeMillis());

                profiles.updateOne(
                        Filters.eq("_id", uuid.toString()),
                        Updates.push("warnings", warning),
                        new UpdateOptions().upsert(true)
                );
            });
        });
    }

    public void incrementKickCount(UUID uuid) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> profiles.updateOne(
                Filters.eq("_id", uuid.toString()),
                Updates.inc("kickCount", 1),
                new UpdateOptions().upsert(true)
        ));
    }

    // --- History Getters ---

    public void getWarnings(String playerName, Consumer<List<Map<String, String>>> callback) {
        getUUIDByName(playerName, uuid -> {
            if (uuid == null) {
                callback.accept(Collections.emptyList());
                return;
            }
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                List<Map<String, String>> result = new ArrayList<>();
                try {
                    Document doc = profiles.find(Filters.eq("_id", uuid.toString())).first();
                    if (doc != null && doc.containsKey("warnings")) {
                        List<Document> list = doc.getList("warnings", Document.class);
                        for (Document w : list) {
                            Map<String, String> map = new HashMap<>();
                            map.put("reason", w.getString("reason"));
                            map.put("issuer", w.getString("issuer"));
                            map.put("date", new java.util.Date(w.getLong("warnedAt")).toString());
                            result.add(map);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(result));
            });
        });
    }

    public void getMutes(UUID uuid, Consumer<List<Map<String, String>>> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Map<String, String>> result = new ArrayList<>();
            try {
                Document doc = profiles.find(Filters.eq("_id", uuid.toString())).first();
                if (doc != null && doc.containsKey("muteHistory")) {
                    List<Document> list = doc.getList("muteHistory", Document.class);
                    for (Document m : list) {
                        Map<String, String> map = new HashMap<>();
                        map.put("reason", m.getString("reason"));
                        map.put("issuer", m.getString("issuer"));
                        map.put("expiration", String.valueOf(m.get("expiration")));
                        result.add(map);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(result));
        });
    }

    public void getKicks(String playerName, Consumer<List<Map<String, String>>> callback) {
        getUUIDByName(playerName, uuid -> {
            if (uuid == null) {
                callback.accept(Collections.emptyList());
                return;
            }
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                List<Map<String, String>> result = new ArrayList<>();
                try {
                    Document doc = profiles.find(Filters.eq("_id", uuid.toString())).first();
                    if (doc != null) {
                        Map<String, String> map = new HashMap<>();
                        map.put("count", String.valueOf(doc.getInteger("kickCount", 0)));
                        result.add(map);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(result));
            });
        });
    }

    public void getBans(String playerName, Consumer<List<Map<String, String>>> callback) {
        getUUIDByName(playerName, uuid -> {
            if (uuid == null) {
                callback.accept(Collections.emptyList());
                return;
            }
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                List<Map<String, String>> result = new ArrayList<>();
                try {
                    Document doc = profiles.find(Filters.eq("_id", uuid.toString())).first();
                    if (doc != null && doc.containsKey("banHistory")) {
                        List<Document> list = doc.getList("banHistory", Document.class);
                        for (Document b : list) {
                            Map<String, String> map = new HashMap<>();
                            map.put("reason", b.getString("reason"));
                            map.put("date", new java.util.Date(b.getLong("bannedAt")).toString());
                            map.put("expiration", String.valueOf(b.get("expiration")));
                            result.add(map);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(result));
            });
        });
    }

    // --- IPS & Alts ---

    public void recordPlayerIP(UUID uuid, String ip) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> profiles.updateOne(
                Filters.eq("_id", uuid.toString()),
                Updates.combine(
                        Updates.addToSet("ips", ip),
                        Updates.set("lastIp", ip),
                        Updates.set("lastSeen", System.currentTimeMillis()),
                        Updates.set("name", Bukkit.getOfflinePlayer(uuid).getName()) // Cache name for lookups
                ),
                new UpdateOptions().upsert(true)
        ));
    }

    public void getIPsForUUID(UUID uuid, Consumer<Set<String>> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Set<String> ips = new HashSet<>();
            try {
                Document doc = profiles.find(Filters.eq("_id", uuid.toString())).first();
                if (doc != null && doc.containsKey("ips")) {
                    ips.addAll(doc.getList("ips", String.class));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(ips));
        });
    }

    public void getUUIDsForIP(String ip, Consumer<Set<UUID>> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Set<UUID> uuids = new HashSet<>();
            try {
                // Find any profile where 'ips' array contains this IP
                for (Document doc : profiles.find(Filters.eq("ips", ip))) {
                    try {
                        uuids.add(UUID.fromString(doc.getString("_id")));
                    } catch (IllegalArgumentException ignored) {}
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(uuids));
        });
    }

    public void getAlts(UUID uuid, Consumer<Set<UUID>> callback) {
        getIPsForUUID(uuid, ips -> {
            if (ips.isEmpty()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(Collections.singleton(uuid)));
                return;
            }
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                Set<UUID> alts = new HashSet<>();
                try {
                    // Find all profiles that share any of these IPs
                    for (Document doc : profiles.find(Filters.in("ips", ips))) {
                        try {
                            alts.add(UUID.fromString(doc.getString("_id")));
                        } catch (IllegalArgumentException ignored) {}
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(alts));
            });
        });
    }

    // --- Rank Grants ---

    public void addRankGrant(UUID targetUUID, String rank, UUID grantedBy, long grantedAt, Long expiresAt, String reason) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Document grant = new Document()
                    .append("id", UUID.randomUUID().toString())
                    .append("rank", rank)
                    .append("grantedBy", grantedBy.toString())
                    .append("grantedAt", grantedAt)
                    .append("expiresAt", expiresAt)
                    .append("reason", reason);

            profiles.updateOne(
                    Filters.eq("_id", targetUUID.toString()),
                    Updates.push("rankGrants", grant),
                    new UpdateOptions().upsert(true)
            );
        });
    }

    // --- Utility & Cleanup ---

    public void getUUIDByName(String playerName, Consumer<UUID> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            UUID uuid = null;
            try {
                // Try Mongo regex search first (case-insensitive)
                Document doc = profiles.find(Filters.regex("name", "^" + playerName + "$", "i")).first();
                if (doc != null) {
                    uuid = UUID.fromString(doc.getString("_id"));
                } else {
                    // Fallback to Bukkit
                    uuid = Bukkit.getOfflinePlayer(playerName).getUniqueId();
                }
            } catch (Exception ignored) {}

            final UUID result = uuid;
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(result));
        });
    }

    // New methods for String IDs (UUIDs)
    public void removeWarning(String warningId) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> profiles.updateOne(
                Filters.exists("warnings"),
                Updates.pull("warnings", new Document("id", warningId))
        ));
    }

    public void removeRankGrant(String grantId) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> profiles.updateOne(
                Filters.exists("rankGrants"),
                Updates.pull("rankGrants", new Document("id", grantId))
        ));
    }


    // --- Caches ---

    public static class RankCache {
        private final HashMap<UUID, String> cache = new HashMap<>();
        public void set(UUID uuid, String rank) { cache.put(uuid, rank); }
        public String get(UUID uuid) { return cache.getOrDefault(uuid, "Default"); }
        public boolean has(UUID uuid) { return cache.containsKey(uuid); }
    }

    public static class TagCache {
        private final HashMap<UUID, String> cache = new HashMap<>();
        public void set(UUID uuid, String tag) { cache.put(uuid, tag); }
        public String get(UUID uuid) { return cache.getOrDefault(uuid, null); }
        public boolean has(UUID uuid) { return cache.containsKey(uuid); }
    }
}