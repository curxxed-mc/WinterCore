package curxxed.dev.icore.Database;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import curxxed.dev.icore.Commands.Staff.VanishCommand;
import curxxed.dev.icore.Main;
import curxxed.dev.icore.utils.RankManager;
import net.kyori.adventure.platform.facet.Facet;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class RedisManager {

    private final Main plugin;
    private final String serverName;
    private final Gson gson;

    public RedisManager(Main plugin) {
        this.plugin = plugin;
        this.serverName = plugin.getConfig().getString("server-name", "Unknown");
        this.gson = new Gson();
        startListening();
    }

    public void syncVanishState(Player player, boolean vanished) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            jedis.publish("vanishSync", player.getUniqueId() + ":" + vanished);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to publish vanish state: " + e.getMessage());
        }
    }

    private void handlePlayerReportMessage(String message) {
        try {
            JsonObject report = gson.fromJson(message, JsonObject.class);
            String reporterName = report.get("reporter").getAsString();
            String reportedName = report.get("reported").getAsString();
            String reason = report.get("reason").getAsString();
            String server = report.get("server").getAsString();
            String reporterColor = plugin.getRankManager().getColorPreferenceSync(Bukkit.getPlayer(reporterName));
            String reportedColor = plugin.getRankManager().getColorPreferenceSync(Bukkit.getPlayer(reportedName));
            String formattedReporter = reporterColor + reporterName;
            String formattedReported = reportedColor + reportedName;
            String formattedMessage = ChatColor.GRAY + "[" + ChatColor.BLUE + "S" + ChatColor.GRAY + "] " +
                    formattedReporter + ChatColor.AQUA + " reported " +
                    formattedReported + ChatColor.AQUA + " for: " +
                    ChatColor.YELLOW + reason + ChatColor.GRAY + " (Server: " + server + ")";

            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.hasPermission("iCore.staff") || online.hasPermission("iCore.admin") || online.hasPermission("iCore.manager") || online.isOp()) {
                        online.sendMessage(formattedMessage);
                    }
                }
            });
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to handle player report message: " + e.getMessage());
        }
    }
    public void publishServerStatus(boolean isOnline) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            jedis.publish("server-status", serverName + "|" + (isOnline ? "online" : "offline"));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to publish server status: " + e.getMessage());
        }
    }

    private void startListening() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Jedis jedis = plugin.getRedisPool().getResource()) {
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        switch (channel) {
                            case "vanishSync":
                                handleVanishMessage(message);
                                break;
                            case "server-status":
                                handleServerStatusMessage(message);
                                break;
                            case "server-command":
                                handleRemoteCommand(message);
                                break;
                            case "staff-activity":
                                handleStaffActivityMessage(message);
                                break;
                            case "staff:message":
                                handleStaffChatMessage(message);
                                break;
                            case "admin:message":
                                handleAdminChatMessage(message);
                                break;
                            case "manager:message":
                                handleManagerChatMessage(message);
                                break;
                            case "player-report":
                                handlePlayerReportMessage(message);
                                break;
                        }
                    }
                }, "vanishSync", "server-status", "server-command", "staff-activity", "staff:message", "admin:message", "manager:message", "player-report");
            } catch (Exception e) {
                plugin.getLogger().warning("Redis subscription failed: " + e.getMessage());
            }
        });
    }


    private void handleStaffChatMessage(String message) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("iCore.Staff") || player.hasPermission("iCore.Admin") || player.hasPermission("iCore.Manager")) {
                    player.sendMessage(message);
                }
            }
        });
    }
    private void handleAdminChatMessage(String message) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("iCore.Admin") || player.hasPermission("iCore.Manager")) {
                    player.sendMessage(message);
                }
            }
        });
    }
    private void handleManagerChatMessage(String message) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("iCore.Manager")) {
                    player.sendMessage(message);
                }
            }
        });
    }

    private void handleVanishMessage(String message) {
        String[] parts = message.split(":");
        if (parts.length != 2) return;

        UUID uuid = UUID.fromString(parts[0]);
        final boolean vanished = Boolean.parseBoolean(parts[1]);

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                if (vanished) {
                    VanishCommand.vanishedPlayers.add(uuid);
                } else {
                    VanishCommand.vanishedPlayers.remove(uuid);
                }
            }
        });
    }

    private void handleServerStatusMessage(String message) {
        String[] parts = message.split("\\|");
        if (parts.length != 2) return;

        final String sourceServer = parts[0];
        final boolean isOnline = parts[1].equalsIgnoreCase("online");
        if (sourceServer.equals(serverName)) return;
        final String statusPrefix = ChatColor.GRAY + "[" + ChatColor.DARK_GRAY + ChatColor.RESET + "i" + ChatColor.AQUA + "Core" + ChatColor.GRAY + "] ";
        final String statusMessage = statusPrefix + (isOnline
                ? ChatColor.GRAY + "Server " + ChatColor.AQUA + sourceServer + ChatColor.GRAY + " has just came " + ChatColor.GREEN + "online" + ChatColor.GRAY + " and will be " + ChatColor.AQUA + ChatColor.BOLD + "joinable in 5 seconds!"
                : ChatColor.GRAY + "Server " + ChatColor.AQUA + sourceServer + ChatColor.GRAY + " has just went " + ChatColor.DARK_RED + "offline" + ChatColor.GRAY + " and is no longer " + ChatColor.DARK_RED + ChatColor.BOLD + "joinable!");

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("icore.servermanager") || p.isOp()) {
                    p.sendMessage(statusMessage);
                }
            }
            plugin.getLogger().info(statusMessage.replaceAll("§.", ""));
        });
    }

    public void startHeartbeatMonitor() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try (Jedis jedis = plugin.getRedisPool().getResource()) {
                Set<String> keys = jedis.keys("server:*:heartbeat");
                long now = System.currentTimeMillis();

                for (String key : keys) {
                    String[] parts = key.split(":");
                    if (parts.length < 3) continue;

                    String otherServer = parts[1];
                    if (otherServer.equalsIgnoreCase(serverName)) continue;

                    String lastHeartbeat = jedis.get(key);
                    if (lastHeartbeat == null) continue;

                    long diff = now - Long.parseLong(lastHeartbeat);
                    if (diff > 10000) {
                        jedis.del(key);
                        jedis.publish("server-status", otherServer + "|offline");
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Heartbeat monitor failed: " + e.getMessage());
            }
        }, 0L, 100L);
    }

    public void startHeartbeatSender() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try (Jedis jedis = plugin.getRedisPool().getResource()) {
                String key = "server:" + serverName + ":heartbeat";
                jedis.set(key, String.valueOf(System.currentTimeMillis()));
                jedis.expire(key, 15);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send heartbeat: " + e.getMessage());
            }
        }, 0L, 40L);
    }



    public void setSocialLink(UUID playerId, String platform, String link) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            jedis.hset("social:" + playerId.toString(), platform.toLowerCase(), link);
        }
    }

    public String getSocialLink(UUID playerId, String platform) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            return jedis.hget("social:" + playerId.toString(), platform.toLowerCase());
        }
    }

    public void dispatchRemoteCommand(String targetServer, String command) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            String payload = targetServer + "|" + command;
            jedis.publish("server-command", payload);
        }
    }

    public void publishReport(String reporter, String reported, String reason, String server) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", "report");
        obj.addProperty("reporter", reporter);
        obj.addProperty("reported", reported);
        obj.addProperty("reason", reason);
        obj.addProperty("server", server);

        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            jedis.publish("player-report", obj.toString());
        }
    }


    public void sendPlayerToServer(Player player, String server) {
        UUID uuid = player.getUniqueId();
        markPendingSwitch(uuid);


        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            try {
                out.writeUTF("Connect");
                out.writeUTF(server);
                player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to send player to server: " + e.getMessage());
            }
        }, 2L);
    }


    private void handleRemoteCommand(String message) {
        String[] parts = message.split("\\|", 2);
        if (parts.length != 2) return;

        String target = parts[0];
        String cmd = parts[1];

        if (target.equalsIgnoreCase("all") || target.equalsIgnoreCase(serverName)) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
            );
        }
    }

    public void startInfoUpdater() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try (Jedis jedis = plugin.getRedisPool().getResource()) {
                String key = "server:" + serverName + ":info";
                double[] tpsArray = getTPS();
                double tps = tpsArray.length > 0 ? tpsArray[0] : 0.0;

                jedis.hset(key, "tps", String.format("%.2f", tps));
                jedis.hset(key, "players", String.valueOf(Bukkit.getOnlinePlayers().size()));
                jedis.hset(key, "maxPlayers", String.valueOf(Bukkit.getMaxPlayers()));
                jedis.hset(key, "whitelisted", String.valueOf(Bukkit.hasWhitelist()));

                jedis.del("server:" + serverName + ":players");
                for (Player player : Bukkit.getOnlinePlayers()) {
                    jedis.sadd("server:" + serverName + ":players", player.getUniqueId().toString());
                }

                jedis.expire(key, 10);
                jedis.expire("server:" + serverName + ":players", 10);

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to update server info: " + e.getMessage());
            }
        }, 0L, 100L);
    }

    public Set<String> getAllOnlinePlayers() {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            Set<String> keys = jedis.keys("server:*:players");
            Set<String> allPlayers = new java.util.HashSet<>();

            for (String key : keys) {
                allPlayers.addAll(jedis.smembers(key));
            }
            return allPlayers;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get online players: " + e.getMessage());
            return java.util.Collections.emptySet();
        }
    }


    private double[] getTPS() {
        try {
            Object minecraftServer = Class.forName("org.bukkit.craftbukkit.v1_8_R3.CraftServer")
                    .getMethod("getServer").invoke(org.bukkit.Bukkit.getServer());
            return (double[]) minecraftServer.getClass().getField("recentTps").get(minecraftServer);
        } catch (Exception e) {
            e.printStackTrace();
            return new double[]{0.0, 0.0, 0.0};
        }
    }

    public void publishStaffActivity(String type, String playerName, String color, String fromServer, String toServer) {
        plugin.getLogger().info("[RedisDebug] Trying to publish staff activity:");
        plugin.getLogger().info("[RedisDebug] Type: " + type);
        plugin.getLogger().info("[RedisDebug] Payload: " + playerName + ", " + color + ", " + fromServer + ", " + toServer);

        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            String payload = type + "|" + playerName + "|" + color + "|" + fromServer + "|" + toServer;
            jedis.publish("staff-activity", payload);
            plugin.getLogger().info("[RedisDebug] Redis publish successful: " + payload);
        } catch (Exception e) {
            plugin.getLogger().warning("[RedisDebug] Failed to publish staff activity: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void publishStaffActivity(String type, JsonObject json) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("iCore.staff") || online.hasPermission("iCore.admin") || online.hasPermission("iCore.manager") || online.isOp()) continue;
        }
        plugin.getLogger().info("[RedisDebug] Trying to publish staff activity:");
        plugin.getLogger().info("[RedisDebug] Type: " + type);
        plugin.getLogger().info("[RedisDebug] Payload: " + json.toString());

        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            jedis.publish("staff-activity", json.toString());
            plugin.getLogger().info("[RedisDebug] Redis publish successful: " + json.toString());
        } catch (Exception e) {
            plugin.getLogger().warning("[RedisDebug] Failed to publish staff activity: " + e.getMessage());
            e.printStackTrace();
        }
    }



    private void handleStaffActivityMessage(String message) {
        plugin.getLogger().info("[RedisDebug] Received staff-activity message: " + message);
        String[] parts = message.split("\\|");
        if (parts.length < 5) return;

        String type = parts[0];
        String playerName = parts[1];
        String color = parts[2];
        String fromServer = parts[3];
        String toServer = parts[4];

        String template = plugin.getConfig().getString(type + "-message");
        if (template == null) return;

        String formattedMessage = ChatColor.translateAlternateColorCodes('&',
                template.replace("%player%", color + playerName + ChatColor.RESET)
                        .replace("%previous-server%", fromServer)
                        .replace("%server-name%", toServer.isEmpty() ? fromServer : toServer)

        );

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("iCore.staff") || online.isOp()) {
                online.sendMessage(formattedMessage);
            }
        }
    }


    public void broadcastStaffMessage(String message) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            jedis.publish("staff:message", message);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to broadcast staff message: " + e.getMessage());
        }
    }

    public void broadcastAdminMessage(String message) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            jedis.publish("admin:message", message);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to broadcast admin message: " + e.getMessage());
        }
    }

    public void broadcastManagerMessage(String message) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            jedis.publish("manager:message", message);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to broadcast manager message: " + e.getMessage());
        }
    }

    public void updateLastServer(UUID uuid, String server) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            jedis.hset("staff:last-server", uuid.toString(), server);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update last server: " + e.getMessage());
        }
    }

    public String getLastServer(UUID uuid) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            return jedis.hget("staff:last-server", uuid.toString());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get last server: " + e.getMessage());
            return null;
        }
    }

    public void removeLastServer(UUID uuid) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            jedis.hdel("staff:last-server", uuid.toString());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to remove last server: " + e.getMessage());
        }
    }

    public void markPendingSwitch(UUID uuid) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            String key = "pendingSwitch:" + uuid.toString();
            jedis.del(key);
            jedis.setex(key, 5, "1");
            long ttl = jedis.ttl(key);
            plugin.getLogger().info("[RedisDebug] markPendingSwitch -> Set " + key + " with TTL " + ttl + "s");
        }
    }


    public boolean isStillPendingSwitch(UUID uuid) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            String key = "pendingSwitch:" + uuid.toString();
            boolean exists = jedis.exists(key);
            long ttl = jedis.ttl(key);
            plugin.getLogger().info("[RedisDebug] isStillPendingSwitch -> " + key + " exists: " + exists + ", TTL: " + ttl + "s");
            return exists && ttl > 0;
        }
    }



    public void clearPendingSwitch(UUID uuid) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            String key = "pendingSwitch:" + uuid.toString();
            jedis.del(key);
            plugin.getLogger().info("[RedisDebug] clearPendingSwitch -> Removed " + key);
        }
    }
}
