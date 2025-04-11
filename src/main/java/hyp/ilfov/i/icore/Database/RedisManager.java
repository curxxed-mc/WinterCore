package hyp.ilfov.i.icore.Database;

import hyp.ilfov.i.icore.Commands.Staff.VanishCommand;
import hyp.ilfov.i.icore.Main;
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

    public RedisManager(Main plugin) {
        this.plugin = plugin;
        this.serverName = plugin.getConfig().getString("server-name", "Unknown");
        startListening();
    }

    // Sync vanish state across servers
    public void syncVanishState(Player player, boolean vanished) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            jedis.publish("vanishSync", player.getUniqueId() + ":" + vanished);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to publish vanish state: " + e.getMessage());
        }
    }

    // Announce server online/offline with custom ChatColor formatting
    public void publishServerStatus(boolean isOnline) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            jedis.publish("server-status", serverName + "|" + (isOnline ? "online" : "offline"));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to publish server status: " + e.getMessage());
        }
    }

    // Redis listener
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
                        }
                    }
                }, "vanishSync", "server-status", "server-command", "staff-activity", "staff:message", "admin:message", "manager:message");
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

        // Don't announce our own server status
        if (sourceServer.equals(serverName)) return;

        //* Format the prefix as per your request
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
                    if (diff > 10000) { // 10s grace period
                        jedis.del(key); // Optional: clean up dead key
                        jedis.publish("server-status", otherServer + "|offline");
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Heartbeat monitor failed: " + e.getMessage());
            }
        }, 0L, 100L); // Run every 5 seconds
    }

    public void startHeartbeatSender() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try (Jedis jedis = plugin.getRedisPool().getResource()) {
                String key = "server:" + serverName + ":heartbeat";
                jedis.set(key, String.valueOf(System.currentTimeMillis()));
                jedis.expire(key, 15); // Optional: Add TTL to auto-cleanup if not refreshed
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send heartbeat: " + e.getMessage());
            }
        }, 0L, 40L); // Every 2 seconds
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

    public void sendPlayerToServer(Player player, String server) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            out.writeUTF("Connect");
            out.writeUTF(server);
            player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to send player to server: " + e.getMessage());
        }
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
                jedis.expire(key, 10);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to update server info: " + e.getMessage());
            }
        }, 0L, 100L);
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
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            String payload = type + "|" + playerName + "|" + color + "|" + fromServer + "|" + toServer;
            jedis.publish("staff-activity", payload);
        }
    }


    private void handleStaffActivityMessage(String message) {
        String[] parts = message.split("\\|");
        if (parts.length < 5) return;

        String type = parts[0];
        String playerName = parts[1];
        String color = parts[2];
        String fromServer = parts[3];
        String toServer = parts[4];

        String template = plugin.getConfig().getString(type + "-message");
        if (template == null) return;

        // Safely replace server placeholders with fallback
        String formattedMessage = ChatColor.translateAlternateColorCodes('&',
                template.replace("%player%", color + playerName + ChatColor.RESET)
                        .replace("%previous-server%", fromServer)
                        .replace("%server-name%", toServer)
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


}
