package net.curxxed.dev.wintercore.database.service;

import net.curxxed.dev.wintercore.database.mongo.ProfileRepository;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class IdentityService {

    private final WinterCore plugin;
    private final ProfileRepository profiles;

    public IdentityService(WinterCore plugin, ProfileRepository profiles) {
        this.plugin = plugin;
        this.profiles = profiles;
    }

    public void recordPlayerIP(UUID uuid, String ip) {
        plugin.getTasks().sync(() -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            String name = offlinePlayer.getName();

            plugin.getTasks().async(() -> {
                try {
                    profiles.addToSet(uuid, "ips", ip);
                    profiles.upsertField(uuid, "lastIp", ip);
                    profiles.upsertField(uuid, "lastSeen", System.currentTimeMillis());
                    profiles.upsertField(uuid, "name", name);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not record IP for " + uuid, e);
                }
            });
        });
    }

    public void getPlayerName(UUID uuid, Consumer<String> callback) {
        plugin.getTasks().async(() -> {
            String name = null;
            try {
                name = profiles.getPlayerName(uuid);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not fetch player name for " + uuid, e);
            }

            String finalName = name;
            plugin.getTasks().sync(() -> callback.accept(finalName));
        });
    }

    public void getUUIDByName(String playerName, Consumer<UUID> callback) {
        plugin.getTasks().async(() -> {
            UUID uuid = null;

            try {
                Document doc = profiles.findByNameRegex(playerName);
                if (doc != null) {
                    uuid = UUID.fromString(doc.getString("_id"));
                }
            } catch (Exception ignored) {
            }

            if (uuid == null) {
                plugin.getTasks().sync(() -> {
                    try {
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
                        callback.accept(offlinePlayer.getUniqueId());
                    } catch (Exception e) {
                        callback.accept(null);
                    }
                });
                return;
            }

            UUID finalUuid = uuid;
            plugin.getTasks().sync(() -> callback.accept(finalUuid));
        });
    }

    public void getIPsForUUID(UUID uuid, Consumer<Set<String>> callback) {
        plugin.getTasks().async(() -> {
            Set<String> ips = new HashSet<>();
            try {
                Document doc = profiles.findById(uuid);
                if (doc != null && doc.containsKey("ips")) {
                    ips.addAll(doc.getList("ips", String.class));
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not fetch IPs for " + uuid, e);
            }

            Set<String> finalIps = ips;
            plugin.getTasks().sync(() -> callback.accept(finalIps));
        });
    }

    public void getUUIDsForIP(String ip, Consumer<Set<UUID>> callback) {
        plugin.getTasks().async(() -> {
            Set<UUID> uuids = new HashSet<>();
            try {
                for (Document doc : profiles.findByIp(ip)) {
                    try {
                        uuids.add(UUID.fromString(doc.getString("_id")));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not fetch UUIDs for IP " + ip, e);
            }

            Set<UUID> finalUuids = uuids;
            plugin.getTasks().sync(() -> callback.accept(finalUuids));
        });
    }

    public void getAlts(UUID uuid, Consumer<Set<UUID>> callback) {
        getIPsForUUID(uuid, ips -> {
            if (ips.isEmpty()) {
                plugin.getTasks().sync(() -> callback.accept(Collections.singleton(uuid)));
                return;
            }

            plugin.getTasks().async(() -> {
                Set<UUID> alts = new HashSet<>();
                try {
                    for (Document doc : profiles.findByAnyIp(ips)) {
                        try {
                            alts.add(UUID.fromString(doc.getString("_id")));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not fetch alts for " + uuid, e);
                }

                Set<UUID> finalAlts = alts;
                plugin.getTasks().sync(() -> callback.accept(finalAlts));
            });
        });
    }
}