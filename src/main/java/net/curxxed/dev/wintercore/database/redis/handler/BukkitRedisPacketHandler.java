package net.curxxed.dev.wintercore.database.redis.handler;

import net.curxxed.dev.wintercore.database.redis.packet.packets.*;
import net.curxxed.dev.wintercore.disguise.DisguiseEventListener;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.utils.ModerationMessages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class BukkitRedisPacketHandler implements RedisPacketHandler {

    private final WinterCore plugin;
    private final DisguiseEventListener disguiseEventListener;

    public BukkitRedisPacketHandler(WinterCore plugin, DisguiseEventListener disguiseEventListener) {
        this.plugin = plugin;
        this.disguiseEventListener = disguiseEventListener;
    }

    @Override
    public void handle(RankTagSyncPacket packet) {
        Player player = Bukkit.getPlayer(packet.getTargetUuid());
        if (player == null || !player.isOnline()) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            String rank = packet.getRank();
            if (rank != null && !rank.trim().isEmpty()) {
                plugin.getRankManager().cachePlayerRank(player, rank);
            } else {
                plugin.getRankManager().refreshCache(player);
            }

            plugin.getRankManager().refreshPlayerDisplay(player);
            plugin.getRankManager().refreshPlayerDisplayForAll(player);
            plugin.getPlayerService().loadPlayerData(player.getUniqueId(), player.getName());
        });
    }

    @Override
    public void handle(ServerSwitchPacket packet) {
        Player player = Bukkit.getPlayer(packet.getUuid());
        if (player == null) return;

        Bukkit.getScheduler().runTask(plugin, () ->
                disguiseEventListener.onServerSwitch(player)
        );
    }

    @Override
    public void handle(ServerStatusPacket packet) {
        String statusMessage = packet.isOnline()
                ? CC.translate("&7Server &b" + packet.getSourceServer() + "&7 has just came &aonline&7 and will be &b&ljoinable in 5 seconds!")
                : CC.translate("&7Server &b" + packet.getSourceServer() + "&7 has just went &4offline&7 and is no longer &4&ljoinable!");

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("wintercore.servermanager") || p.isOp()) {
                    p.sendMessage(statusMessage);
                }
            }
            plugin.getLogger().info(statusMessage.replaceAll("§.", ""));
        });
    }

    @Override
    public void handle(StaffActivityPacket packet) {
        String template = plugin.getConfig().getString(packet.getActivityType() + "-message");
        if (template == null) return;

        String formatted = CC.translate(
                template.replace("%player%", packet.getColor() + packet.getPlayerName() + "&r")
                        .replace("%previous-server%", packet.getFromServer())
                        .replace("%server-name%", packet.getToServer() == null || packet.getToServer().isEmpty()
                                ? packet.getFromServer()
                                : packet.getToServer())
        );

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.hasPermission("wintercore.staff")
                        || online.hasPermission("wintercore.admin")
                        || online.hasPermission("wintercore.manager")
                        || online.isOp()) {
                    online.sendMessage(formatted);
                }
            }
        });
    }

    @Override
    public void handle(RemoteCommandPacket packet) {
        if (!packet.getTargetServer().equalsIgnoreCase("all")
                && !packet.getTargetServer().equalsIgnoreCase(
                plugin.getConfig().getString("server-name", "Unknown"))) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), packet.getCommand())
        );
    }

    @Override
    public void handle(ModerationActionPacket packet) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (plugin.getBanList() != null) {
                if (packet.getActionType() == ModerationActionPacket.ActionType.BAN_APPLIED) {
                    plugin.getBanList().applyBan(packet.getTargetUuid(), packet.getReason(), packet.getExpiresAt(), true);
                } else {
                    plugin.getBanList().removeBan(packet.getTargetUuid());
                }
            }

            if (packet.getActionType() != ModerationActionPacket.ActionType.BAN_APPLIED) {
                return;
            }

            String formatted = ModerationMessages.formatBanAnnouncement(
                    packet.getTargetName(),
                    packet.getIssuer(),
                    packet.getReason(),
                    packet.getExpiresAt(),
                    packet.isSilent()
            );

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!packet.isSilent() || isStaffAudience(online)) {
                    online.sendMessage(formatted);
                }
            }
        });
    }

    @Override
    public void handle(PlayerReportPacket packet) {
        String formattedMessage = ModerationMessages.formatReportMessage(
                packet.getReporter(),
                packet.getReported(),
                packet.getReason(),
                packet.getServer()
        );

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (isStaffAudience(online)) {
                    online.sendMessage(formattedMessage);
                }
            }
        });
    }

    @Override
    public void handle(DisguiseStatePacket packet) {
        Player player = Bukkit.getPlayer(packet.getUuid());
        if (plugin.getNameTagColorManager() == null) return;

        if (packet.isDisguised()) {
            String name = null;
            String color = "&f";

            if (packet.getDisguiseJson() != null && !packet.getDisguiseJson().isEmpty()) {
                try {
                    com.google.gson.JsonObject obj =
                            com.google.gson.JsonParser.parseString(packet.getDisguiseJson()).getAsJsonObject();

                    if (obj.has("name")) {
                        name = obj.get("name").getAsString();
                    }
                    if (obj.has("color")) {
                        color = obj.get("color").getAsString();
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("[Disguise] Failed to parse disguise JSON: " + e.getMessage());
                }
            }

            if (player != null) {
                plugin.getNameTagColorManager().applyDisguise(
                        player,
                        name != null ? name : player.getName(),
                        color
                );
            }
        } else {
            if (player != null) {
                plugin.getNameTagColorManager().clearDisguise(player);
                plugin.getNameTagColorManager().applyColor(
                        player,
                        plugin.getRankManager().getColorPreferenceSync(player)
                );
            }
        }
    }

    @Override
    public void handle(ConfigSyncPacket packet) {
        final ConfigSyncPacket.ConfigType type = packet.getConfigType();
        final File target;
        switch (type) {
            case CONFIG:
                target = new File(plugin.getDataFolder(), "config.yml");
                break;
            case RANKS:
                target = new File(plugin.getDataFolder(), "ranks.yml");
                break;
            case TAGS:
                target = new File(plugin.getDataFolder(), "tags.yml");
                break;
            case MENUS:
                target = new File(plugin.getDataFolder(), "menus.yml");
                break;
            case PERMISSIONS:
                target = new File(plugin.getDataFolder(), "permissions.yml");
                break;
            case MESSAGES:
                target = new File(plugin.getDataFolder(), "messages.yml");
                break;
            case CHAT_FILTER:
                target = new File(plugin.getDataFolder(), "chat-filter.yml");
                break;
            default:
                plugin.getLogger().warning("[Sync] Unsupported config type received: " + type);
                return;
        }

        writeFile(target, packet.getYaml());

        Bukkit.getScheduler().runTask(plugin, () -> {
            switch (type) {
                case CONFIG:
                    plugin.reloadConfig();
                    break;
                case RANKS:
                    plugin.getRankManager().reloadRanksConfig();
                    refreshDisplaysForOnlinePlayers();
                    break;
                case TAGS:
                    plugin.getTagsManager().loadTags();
                    plugin.getTagsGUI().refresh();
                    break;
                case MENUS:
                    plugin.getMenuConfig().load();
                    break;
                case PERMISSIONS:
                    plugin.getPermissionConfigManager().load();
                    refreshDisplaysForOnlinePlayers();
                    break;
                case MESSAGES:
                    plugin.getMessageConfig().load();
                    break;
                case CHAT_FILTER:
                    plugin.getChatFilterService().load();
                    break;
                default:
                    break;
            }

            plugin.getLogger().info("[Sync] " + target.getName() + " received from "
                    + packet.getSourceServer() + " and applied.");
        });
    }

    @Override
    public void handle(ChatBroadcastPacket packet) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (canReceiveChatBroadcast(online, packet.getChatType())) {
                    online.sendMessage(packet.getMessage());
                }
            }
            plugin.getLogger().info(packet.getMessage().replaceAll("§.", ""));
        });
    }

    private boolean canReceiveChatBroadcast(Player player, ChatBroadcastPacket.ChatType chatType) {
        if (player.isOp()) {
            return true;
        }
        if (chatType == ChatBroadcastPacket.ChatType.MANAGER) {
            return player.hasPermission("wintercore.manager");
        }
        if (chatType == ChatBroadcastPacket.ChatType.ADMIN) {
            return player.hasPermission("wintercore.admin")
                    || player.hasPermission("wintercore.manager");
        }
        return player.hasPermission("wintercore.staff")
                || player.hasPermission("wintercore.admin")
                || player.hasPermission("wintercore.manager");
    }

    private boolean isStaffAudience(Player player) {
        return player.isOp()
                || player.hasPermission("wintercore.staff")
                || player.hasPermission("wintercore.admin")
                || player.hasPermission("wintercore.manager");
    }

    @Override
    public void handle(VanishPacket packet) {
        Player player = Bukkit.getPlayer(packet.getPlayerUuid());
        if (player == null) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (packet.isVanished()) {
                Bukkit.getOnlinePlayers().stream()
                        .filter(p -> !(p.hasPermission("wintercore.staff") || p.hasPermission("wintercore.admin") || p.hasPermission("wintercore.manager")))
                        .forEach(p -> p.hidePlayer(player));
            } else {
                Bukkit.getOnlinePlayers().forEach(p -> p.showPlayer(player));
            }
        });
    }

    @Override
    public void handle(PlayerUpdatePacket packet) {
        Player player = Bukkit.getPlayer(packet.getTargetUuid());
        if (player != null && player.isOnline()) {
            plugin.getPlayerService().loadPlayerData(player.getUniqueId(), player.getName());
            plugin.getRankManager().refreshCache(player);
            plugin.getRankManager().refreshPlayerDisplayForAll(player);
        }
    }

    private void writeFile(File file, String yaml) {
        try {
            Files.write(file.toPath(), yaml.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write " + file.getName() + ": " + e.getMessage());
        }
    }

    private void refreshDisplaysForOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getRankManager().refreshPlayerDisplay(player);
            if (plugin.getPlayerService() != null) {
                plugin.getPlayerService().loadPlayerData(player.getUniqueId(), player.getName());
            }
        }
    }
}

