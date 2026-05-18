package net.curxxed.dev.wintercore.database.redis.handler;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.curxxed.dev.wintercore.database.redis.packet.packets.*;
import net.curxxed.dev.wintercore.disguise.DisguiseEventListener;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.config.ModerationMessages;
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

        plugin.getTasks().sync(() -> {
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

        plugin.getTasks().sync(() ->
                disguiseEventListener.onServerSwitch(player)
        );
    }

    @Override
    public void handle(ServerStatusPacket packet) {
        String statusMessage = packet.isOnline()
                ? plugin.getMessageConfig().get("network.server-online",
                        "&7Server &b{server}&7 has just came &aonline&7 and will be &b&ljoinable in 5 seconds!",
                        "{server}", packet.getSourceServer())
                : plugin.getMessageConfig().get("network.server-offline",
                        "&7Server &b{server}&7 has just went &4offline&7 and is no longer &4&ljoinable!",
                        "{server}", packet.getSourceServer());

        plugin.getTasks().sync(() -> {
            for (Player p : net.curxxed.dev.wintercore.utils.Utilities.getOnlinePlayers()) {
                if (p.hasPermission("wintercore.servermanager") || p.isOp()) {
                    p.sendMessage(statusMessage);
                }
            }
            plugin.getLogger().info(CC.stripColor(statusMessage));
        });
    }

    @Override
    public void handle(StaffActivityPacket packet) {
        String legacyTemplate = plugin.getConfig().getString(packet.getActivityType() + "-message");
        String fallback = legacyTemplate != null
                ? legacyTemplate
                .replace("%player%", "{player}")
                .replace("%previous-server%", "{previous_server}")
                .replace("%server-name%", "{server}")
                : staffActivityFallback(packet.getActivityType());
        if (fallback == null) return;

        String formatted = plugin.getMessageConfig().get("staff-activity." + packet.getActivityType(),
                fallback,
                "{player}", packet.getColor() + packet.getPlayerName() + "&r",
                "{previous_server}", packet.getFromServer(),
                "{server}", packet.getToServer() == null || packet.getToServer().isEmpty()
                        ? packet.getFromServer()
                        : packet.getToServer());

        plugin.getTasks().sync(() -> {
            for (Player online : net.curxxed.dev.wintercore.utils.Utilities.getOnlinePlayers()) {
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

        plugin.getTasks().sync(() ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), packet.getCommand())
        );
    }

    @Override
    public void handle(ModerationActionPacket packet) {
        plugin.getTasks().sync(() -> {
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

            for (Player online : net.curxxed.dev.wintercore.utils.Utilities.getOnlinePlayers()) {
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

        plugin.getTasks().sync(() -> {
            for (Player online : net.curxxed.dev.wintercore.utils.Utilities.getOnlinePlayers()) {
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

        plugin.getTasks().sync(() -> {
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
                    plugin.getTagsMenu().refresh();
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
        plugin.getTasks().sync(() -> {
            for (Player online : net.curxxed.dev.wintercore.utils.Utilities.getOnlinePlayers()) {
                if (canReceiveChatBroadcast(online, packet.getChatType())) {
                    online.sendMessage(packet.getMessage());
                }
            }
            plugin.getLogger().info(CC.stripColor(packet.getMessage()));
        });
    }

    private String staffActivityFallback(String activityType) {
        if ("join".equalsIgnoreCase(activityType)) {
            return "&9[S] {player} &bconnected to &e{server}";
        }
        if ("switch".equalsIgnoreCase(activityType)) {
            return "&9[S] {player} &bconnected to &e{server} &bfrom &e{previous_server}";
        }
        if ("quit".equalsIgnoreCase(activityType)) {
            return "&9[S] {player} &bdisconnected from &e{server}";
        }
        return null;
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

        plugin.getTasks().sync(() -> {
            if (packet.isVanished()) {
                net.curxxed.dev.wintercore.utils.Utilities.getOnlinePlayers().stream()
                        .filter(p -> !(p.hasPermission("wintercore.staff") || p.hasPermission("wintercore.admin") || p.hasPermission("wintercore.manager")))
                        .forEach(p -> p.hidePlayer(player));
            } else {
                net.curxxed.dev.wintercore.utils.Utilities.getOnlinePlayers().forEach(p -> p.showPlayer(player));
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

    @Override
    public void handle(NetworkBroadcastPacket packet) {
        plugin.getTasks().sync(() -> {
            for (Player online : net.curxxed.dev.wintercore.utils.Utilities.getOnlinePlayers()) {
                online.sendMessage(packet.getMessage());
            }
            plugin.getLogger().info(CC.stripColor(packet.getMessage()));
        });
    }

    @Override
    public void handle(PlayerTransferPacket packet) {
        Player target = Bukkit.getPlayer(packet.getTargetUuid());
        if (target == null || !target.isOnline()) {
            return;
        }

        plugin.getTasks().sync(() -> {
            target.sendMessage(plugin.getMessageConfig().get("network-send.target",
                    "&7Sending you to &b{server}&7...",
                    "{server}", packet.getDestinationServer(),
                    "{issuer}", packet.getIssuer()));

            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(packet.getDestinationServer());
            target.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        });
    }

    private void writeFile(File file, String yaml) {
        try {
            Files.write(file.toPath(), yaml.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write " + file.getName() + ": " + e.getMessage());
        }
    }

    private void refreshDisplaysForOnlinePlayers() {
        for (Player player : net.curxxed.dev.wintercore.utils.Utilities.getOnlinePlayers()) {
            plugin.getRankManager().refreshPlayerDisplay(player);
            if (plugin.getPlayerService() != null) {
                plugin.getPlayerService().loadPlayerData(player.getUniqueId(), player.getName());
            }
        }
    }
}
