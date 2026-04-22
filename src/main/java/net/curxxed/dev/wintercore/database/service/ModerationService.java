package net.curxxed.dev.wintercore.database.service;

import net.curxxed.dev.wintercore.database.mongo.ProfileRepository;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bson.Document;
import org.bukkit.Bukkit;

import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class ModerationService {

    private final WinterCore plugin;
    private final ProfileRepository profiles;
    private final IdentityService identityService;

    public static final class ActiveBan {
        private final String reason;
        private final Long expiration;

        public ActiveBan(String reason, Long expiration) {
            this.reason = reason;
            this.expiration = expiration;
        }

        public String getReason() {
            return reason;
        }

        public Long getExpiration() {
            return expiration;
        }

        public boolean isPermanent() {
            return expiration == null;
        }
    }

    public ModerationService(WinterCore plugin, ProfileRepository profiles, IdentityService identityService) {
        this.plugin = plugin;
        this.profiles = profiles;
        this.identityService = identityService;
    }

    public void banPlayer(UUID uuid, String reason, Instant expiration) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Document banDoc = new Document()
                        .append("reason", reason)
                        .append("bannedAt", System.currentTimeMillis())
                        .append("expiration", expiration != null ? expiration.toEpochMilli() : null);

                profiles.upsertField(uuid, "activeBan", banDoc);
                profiles.push(uuid, "banHistory", banDoc);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not ban player " + uuid, e);
            }
        });
    }

    public void unbanPlayer(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                profiles.unset(uuid, "activeBan");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not unban player " + uuid, e);
            }
        });
    }

    public ActiveBan getActiveBan(UUID uuid) {
        try {
            Document doc = profiles.findById(uuid);
            if (doc == null || !doc.containsKey("activeBan")) {
                return null;
            }

            Document banDoc = (Document) doc.get("activeBan");
            if (banDoc == null) {
                return null;
            }

            Long expiration = banDoc.getLong("expiration");
            if (expiration != null && expiration <= System.currentTimeMillis()) {
                profiles.unset(uuid, "activeBan");
                return null;
            }

            String reason = banDoc.getString("reason");
            return new ActiveBan(reason, expiration);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not fetch active ban for " + uuid, e);
            return null;
        }
    }

    public void getActiveBan(UUID uuid, Consumer<ActiveBan> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ActiveBan activeBan = getActiveBan(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(activeBan));
        });
    }

    public void isPlayerBanned(UUID uuid, Consumer<Boolean> callback) {
        getActiveBan(uuid, activeBan -> callback.accept(activeBan != null));
    }

    public void getBanReason(UUID uuid, Consumer<String> callback) {
        getActiveBan(uuid, activeBan -> callback.accept(activeBan != null ? activeBan.getReason() : null));
    }

    public void getBanDetails(UUID uuid, Consumer<Map<String, Object>> callback) {
        getActiveBan(uuid, activeBan -> {
            Map<String, Object> details = new HashMap<>();
            if (activeBan != null) {
                details.put("expiration", activeBan.getExpiration());
                details.put("reason", activeBan.getReason());
            }
            callback.accept(details);
        });
    }

    public void removeExpiredBans() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                profiles.updateMany(
                        new Document("$and", Arrays.asList(
                                new Document("activeBan", new Document("$exists", true)),
                                new Document("activeBan.expiration", new Document("$lte", System.currentTimeMillis())),
                                new Document("activeBan.expiration", new Document("$ne", null))
                        )),
                        new Document("$unset", new Document("activeBan", ""))
                );
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not remove expired bans", e);
            }
        });
    }

    public void mutePlayer(UUID targetUUID, String reason, String issuer, Instant expirationTime) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Document muteDoc = new Document()
                        .append("reason", reason)
                        .append("issuer", issuer)
                        .append("expiration", expirationTime != null ? expirationTime.toEpochMilli() : null)
                        .append("date", System.currentTimeMillis());

                profiles.upsertField(targetUUID, "activeMute", muteDoc);
                profiles.push(targetUUID, "muteHistory", muteDoc);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not mute player " + targetUUID, e);
            }
        });
    }

    public void unmutePlayer(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                profiles.unset(uuid, "activeMute");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not unmute player " + uuid, e);
            }
        });
    }

    public void isPlayerMuted(UUID uuid, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean muted = false;
            try {
                Document doc = profiles.findById(uuid);
                if (doc != null && doc.containsKey("activeMute")) {
                    Document muteDoc = (Document) doc.get("activeMute");
                    Long expiration = muteDoc.getLong("expiration");

                    if (expiration == null || expiration > System.currentTimeMillis()) {
                        muted = true;
                    } else {
                        profiles.unset(uuid, "activeMute");
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not check mute status for " + uuid, e);
            }

            boolean finalMuted = muted;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalMuted));
        });
    }

    public void getMuteDetails(UUID uuid, Consumer<Map<String, Object>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, Object> details = new HashMap<>();
            try {
                Document doc = profiles.findById(uuid);
                if (doc != null && doc.containsKey("activeMute")) {
                    Document muteDoc = (Document) doc.get("activeMute");
                    Long expiration = muteDoc.getLong("expiration");
                    if (expiration == null || expiration > System.currentTimeMillis()) {
                        details.put("reason", muteDoc.getString("reason"));
                        details.put("expiration", expiration);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not fetch mute details for " + uuid, e);
            }

            Map<String, Object> finalDetails = details;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalDetails));
        });
    }

    public void addWarning(String playerName, String reason, String issuer) {
        identityService.getUUIDByName(playerName, uuid -> {
            if (uuid == null) {
                plugin.getLogger().warning("Could not add warning for " + playerName + " (UUID not found)");
                return;
            }

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    Document warning = new Document()
                            .append("id", UUID.randomUUID().toString())
                            .append("reason", reason)
                            .append("issuer", issuer)
                            .append("warnedAt", System.currentTimeMillis());

                    profiles.push(uuid, "warnings", warning);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not add warning for " + playerName, e);
                }
            });
        });
    }

    public void incrementKickCount(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                profiles.inc(uuid, "kickCount", 1);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not increment kick count for " + uuid, e);
            }
        });
    }

    public void addRankGrant(UUID targetUUID, String rank, UUID grantedBy, long grantedAt, Long expiresAt, String reason) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Document grant = new Document()
                        .append("id", UUID.randomUUID().toString())
                        .append("rank", rank)
                        .append("grantedBy", grantedBy.toString())
                        .append("grantedAt", grantedAt)
                        .append("expiresAt", expiresAt)
                        .append("reason", reason);

                profiles.push(targetUUID, "rankGrants", grant);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not add rank grant for " + targetUUID, e);
            }
        });
    }

    public void getWarnings(String playerName, Consumer<List<Map<String, String>>> callback) {
        identityService.getUUIDByName(playerName, uuid -> {
            if (uuid == null) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(Collections.emptyList()));
                return;
            }

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                List<Map<String, String>> result = new ArrayList<>();
                try {
                    Document doc = profiles.findById(uuid);
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
                    plugin.getLogger().log(Level.SEVERE, "Could not fetch warnings for " + playerName, e);
                }

                List<Map<String, String>> finalResult = result;
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalResult));
            });
        });
    }

    public void getMutes(UUID uuid, Consumer<List<Map<String, String>>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Map<String, String>> result = new ArrayList<>();
            try {
                Document doc = profiles.findById(uuid);
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
                plugin.getLogger().log(Level.SEVERE, "Could not fetch mutes for " + uuid, e);
            }

            List<Map<String, String>> finalResult = result;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalResult));
        });
    }

    public void getKicks(String playerName, Consumer<List<Map<String, String>>> callback) {
        identityService.getUUIDByName(playerName, uuid -> {
            if (uuid == null) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(Collections.emptyList()));
                return;
            }

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                List<Map<String, String>> result = new ArrayList<>();
                try {
                    Document doc = profiles.findById(uuid);
                    if (doc != null) {
                        Map<String, String> map = new HashMap<>();
                        map.put("count", String.valueOf(doc.getInteger("kickCount", 0)));
                        result.add(map);
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not fetch kicks for " + playerName, e);
                }

                List<Map<String, String>> finalResult = result;
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalResult));
            });
        });
    }

    public void getBans(String playerName, Consumer<List<Map<String, String>>> callback) {
        identityService.getUUIDByName(playerName, uuid -> {
            if (uuid == null) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(Collections.emptyList()));
                return;
            }

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                List<Map<String, String>> result = new ArrayList<>();
                try {
                    Document doc = profiles.findById(uuid);
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
                    plugin.getLogger().log(Level.SEVERE, "Could not fetch bans for " + playerName, e);
                }

                List<Map<String, String>> finalResult = result;
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalResult));
            });
        });
    }

    public void getRankGrants(UUID uuid, Consumer<List<Map<String, String>>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Map<String, String>> result = new ArrayList<>();
            try {
                Document doc = profiles.findById(uuid);
                if (doc != null && doc.containsKey("rankGrants")) {
                    List<Document> list = doc.getList("rankGrants", Document.class);
                    for (Document g : list) {
                        Map<String, String> map = new HashMap<>();
                        map.put("rank", g.getString("rank"));
                        map.put("issuer", g.getString("grantedBy"));
                        map.put("date", new java.util.Date(g.getLong("grantedAt")).toString());
                        map.put("expiration", g.get("expiresAt") != null
                                ? new java.util.Date((Long) g.get("expiresAt")).toString()
                                : "Permanent");
                        map.put("reason", g.getString("reason"));
                        result.add(map);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not fetch rank grants for " + uuid, e);
            }

            List<Map<String, String>> finalResult = result;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalResult));
        });
    }

    public void removeWarning(String warningId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                profiles.updateMany(
                        new Document("warnings", new Document("$exists", true)),
                        new Document("$pull", new Document("warnings", new Document("id", warningId)))
                );
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not remove warning " + warningId, e);
            }
        });
    }

    public void removeRankGrant(String grantId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                profiles.updateMany(
                        new Document("rankGrants", new Document("$exists", true)),
                        new Document("$pull", new Document("rankGrants", new Document("id", grantId)))
                );
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not remove rank grant " + grantId, e);
            }
        });
    }
}
