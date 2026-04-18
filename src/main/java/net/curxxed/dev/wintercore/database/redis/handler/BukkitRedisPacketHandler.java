package net.curxxed.dev.wintercore.database.redis.handler;

import net.curxxed.dev.wintercore.commands.bungee.SyncCommand;
import net.curxxed.dev.wintercore.database.redis.event.RedisPacketReceivedEvent;
import net.curxxed.dev.wintercore.database.redis.packet.packets.*;
import net.curxxed.dev.wintercore.events.network.RankTagSyncEvent;
import net.curxxed.dev.wintercore.events.network.ServerSwitchEvent;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;

public final class BukkitRedisPacketHandler implements RedisPacketHandler {

    private final WinterCore plugin;

    public BukkitRedisPacketHandler(WinterCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(RankTagSyncPacket packet) {
        if (packet.getSyncType() == RankTagSyncPacket.SyncType.RANKS) {
            File target = new File(plugin.getDataFolder(), "ranks.yml");
            SyncCommand.writeFile(target, packet.getYaml());
            plugin.getRankManager().reloadRanksConfig();

            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        plugin.getRankManager().refreshPlayerDisplay(p);
                    }
                    Bukkit.getPluginManager().callEvent(
                            new RankTagSyncEvent(RankTagSyncEvent.SyncType.RANKS, null, packet.getSourceServer())
                    );
                }
            });
        } else if (packet.getSyncType() == RankTagSyncPacket.SyncType.TAGS) {
            File target = new File(plugin.getDataFolder(), "tags.yml");
            SyncCommand.writeFile(target, packet.getYaml());
            plugin.getTagsManager().loadTags();
            plugin.getTagsGUI().refresh();

            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    Bukkit.getPluginManager().callEvent(
                            new RankTagSyncEvent(RankTagSyncEvent.SyncType.TAGS, null, packet.getSourceServer())
                    );
                }
            });
        } else {
            File ranks = new File(plugin.getDataFolder(), "ranks.yml");
            File tags = new File(plugin.getDataFolder(), "tags.yml");
            SyncCommand.writeFile(ranks, packet.getYaml());
            SyncCommand.writeFile(tags, packet.getYaml());
            plugin.getRankManager().reloadRanksConfig();
            plugin.getTagsManager().loadTags();
            plugin.getTagsGUI().refresh();
        }
    }

    @Override
    public void handle(ServerSwitchPacket packet) {
        Player player = Bukkit.getPlayer(packet.getUuid());
        if (player == null) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                Bukkit.getPluginManager().callEvent(
                        new ServerSwitchEvent(player, packet.getPreviousServer(), packet.getCurrentServer())
                );
            }
        });
    }

    @Override
    public void handle(ServerStatusPacket packet) {
        String statusMessage = packet.isOnline()
                ? CC.translate("&7Server &b" + packet.getSourceServer() + "&7 has just came &aonline&7 and will be &b&ljoinable in 5 seconds!")
                : CC.translate("&7Server &b" + packet.getSourceServer() + "&7 has just went &4offline&7 and is no longer &4&ljoinable!");

        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("wintercore.servermanager") || p.isOp()) {
                        p.sendMessage(statusMessage);
                    }
                }
                plugin.getLogger().info(statusMessage.replaceAll("§.", ""));
            }
        });
    }

    @Override
    public void handle(StaffActivityPacket packet) {
        String template = plugin.getConfig().getString(packet.getActivityType() + "-message");
        if (template == null) {
            return;
        }

        String formatted = CC.translate(
                template.replace("%player%", packet.getColor() + packet.getPlayerName() + "&r")
                        .replace("%previous-server%", packet.getFromServer())
                        .replace("%server-name%", packet.getToServer() == null || packet.getToServer().isEmpty() ? packet.getFromServer() : packet.getToServer())
        );

        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.hasPermission("wintercore.staff")
                            || online.hasPermission("wintercore.admin")
                            || online.hasPermission("wintercore.manager")
                            || online.isOp()) {
                        online.sendMessage(formatted);
                    }
                }
            }
        });
    }

    @Override
    public void handle(RemoteCommandPacket packet) {
        if (!packet.getTargetServer().equalsIgnoreCase("all")
                && !packet.getTargetServer().equalsIgnoreCase(plugin.getConfig().getString("server-name", "Unknown"))) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), packet.getCommand());
            }
        });
    }

    @Override
    public void handle(PlayerReportPacket packet) {
        String formattedMessage = CC.translate("&7[&9S&7] "
                + packet.getReporter() + "&b reported "
                + packet.getReported() + "&b for: "
                + "&e" + packet.getReason() + "&7 (Server: " + packet.getServer() + ")");

        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.hasPermission("wintercore.staff")
                            || online.hasPermission("wintercore.admin")
                            || online.hasPermission("wintercore.manager")
                            || online.isOp()) {
                        online.sendMessage(formattedMessage);
                    }
                }
            }
        });
    }

    @Override
    public void handle(DisguiseStatePacket packet) {
        Player player = Bukkit.getPlayer(packet.getUuid());
        if (player == null || plugin.getNameTagColorManager() == null) {
            return;
        }

        if (packet.isDisguised()) {
            String json = packet.getDisguiseJson();
            String color = "&f";

            if (json != null) {
                try {
                    com.google.gson.JsonObject obj = new com.google.gson.JsonParser().parse(json).getAsJsonObject();
                    if (obj.has("color")) {
                        color = obj.get("color").getAsString();
                    }
                } catch (Exception ignored) {
                }
            }

            plugin.getNameTagColorManager().applyColor(player, color);
        } else {
            plugin.getNameTagColorManager().applyColor(player, plugin.getRankManager().getColorPreferenceSync(player));
        }
    }

    @Override
    public void handle(ConfigSyncPacket packet) {
        if (packet.getConfigType() == ConfigSyncPacket.ConfigType.RANKS) {
            File target = new File(plugin.getDataFolder(), "ranks.yml");
            SyncCommand.writeFile(target, packet.getYaml());
            plugin.getRankManager().reloadRanksConfig();

            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        plugin.getRankManager().refreshPlayerDisplay(p);
                    }
                    Bukkit.getPluginManager().callEvent(
                            new RankTagSyncEvent(RankTagSyncEvent.SyncType.RANKS, null, packet.getSourceServer())
                    );
                }
            });
        } else {
            File target = new File(plugin.getDataFolder(), "tags.yml");
            SyncCommand.writeFile(target, packet.getYaml());
            plugin.getTagsManager().loadTags();
            plugin.getTagsGUI().refresh();

            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    Bukkit.getPluginManager().callEvent(
                            new RankTagSyncEvent(RankTagSyncEvent.SyncType.TAGS, null, packet.getSourceServer())
                    );
                }
            });
        }
    }
}