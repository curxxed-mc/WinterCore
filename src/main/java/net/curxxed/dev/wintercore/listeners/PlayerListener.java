package net.curxxed.dev.wintercore.listeners;

import net.curxxed.dev.wintercore.client.ClientBrandCommand;
import net.curxxed.dev.wintercore.database.DatabaseManager;
import net.curxxed.dev.wintercore.disguise.player.DisguiseData;
import net.curxxed.dev.wintercore.permissions.WinterCorePermissibleInjector;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.tags.TagsManager;
import net.curxxed.dev.wintercore.menus.ChatColorSelectionMenu;
import net.curxxed.dev.wintercore.rank.RankManager;
import net.curxxed.dev.wintercore.commands.staff.FreezeCommand;
import net.curxxed.dev.wintercore.utils.CC;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {
    private final WinterCore plugin;
    private final RankManager rankManager;
    private final FreezeCommand freezeCommand;
    private final DatabaseManager databaseManager;
    private final Map<UUID, String> lastServer = new HashMap<>();
    public static final ChatColorSelectionMenu CHAT_COLOR_SELECTION_MENU = ChatColorSelectionMenu.getInstance();
    private final TagsManager tagsManager;

    public PlayerListener(WinterCore plugin, TagsManager tagsManager) {
        this.plugin = plugin;
        this.tagsManager = tagsManager;

        this.rankManager = new RankManager(plugin);
        this.freezeCommand = plugin.getFreezeCommand();
        this.databaseManager = plugin.getDatabaseManager();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLoginHandle(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        // Record player IP for alt detection
        String ip = event.getAddress().getHostAddress();
        databaseManager.recordPlayerIP(player.getUniqueId(), ip);
        try {
            WinterCorePermissibleInjector.initPlayer(player);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        ClientBrandCommand.silenced.add(uuid);
        rankManager.refreshPlayerDisplay(player);
        rankManager.refreshPlayerDisplayForAll(player);
        // Also update all online players' nametags for the joining player
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.getUniqueId().equals(player.getUniqueId())) {
                rankManager.refreshPlayerDisplayForAll(other);
            }
        }
        // Set nametag using NameTagHandler only (remove direct NameTagAdapter usage)
        if (plugin.getNameTagHandler() != null) {
            if (plugin.getDisguiseRegistry().isDisguised(player)) {
                plugin.getDisguiseRegistry().getEffectiveColor(player, c -> plugin.getNameTagHandler().getNameTagAdapter().setNameTag(player, c));
            } else {
                rankManager.getColorPreference(rankManager.getRankSync(player), c -> plugin.getNameTagHandler().getNameTagAdapter().setNameTag(player, c));
            }
        }
        if (plugin.getDisguiseRegistry().isDisguised(player)) {
            plugin.getDisguiseRegistry().getEffectivePrefix(player, prefix -> plugin.getDisguiseRegistry().getEffectiveColor(player, color -> {
                if (plugin.getNameTagHandler().getNameTagAdapter() != null) {
                    plugin.getNameTagHandler().getNameTagAdapter().setNameTag(player, color);
                }
                String formattedName = (prefix.isEmpty() ? "" : prefix + " ") + CC.translate(color) + player.getName() + ChatColor.RESET;
                player.setPlayerListName(formattedName);
            }));
        } else {
            rankManager.setRankAboveHead(player);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getDisguiseRegistry().isDisguised(player)) {
                rankManager.getDisguiseRank(player, rank -> rankManager.getColorPreference(rank, rankColor -> {
                    rankManager.updateNameTagColor(player, rankColor);
                    if (player.hasPermission("wintercore.staff") || player.hasPermission("wintercore.admin") || player.hasPermission("wintercore.manager") || player.isOp()) {
                        String serverName = plugin.getConfig().getString("server-name", "hub-restricted");
                        String last = plugin.getRedisManager().getLastServer(uuid);
                        plugin.getRedisManager().updateLastServer(uuid, serverName);
                        String realName = getRealName(player);
                        if (last != null && !last.equals(serverName)) {
                            plugin.getRedisManager().publishStaffActivity("switch", realName, rankColor.toString(), last, serverName);
                        } else {
                            plugin.getRedisManager().publishStaffActivity("join", realName, rankColor.toString(), "", serverName);
                        }
                    }
                }));
            } else {
                rankManager.getRank(player, rank -> rankManager.getColorPreference(rank, rankColor -> {
                    rankManager.updateNameTagColor(player, rankColor);
                    if (player.hasPermission("wintercore.staff") || player.hasPermission("wintercore.admin") || player.hasPermission("wintercore.manager") || player.isOp()) {
                        String serverName = plugin.getConfig().getString("server-name", "hub-restricted");
                        String last = plugin.getRedisManager().getLastServer(uuid);
                        plugin.getRedisManager().updateLastServer(uuid, serverName);
                        String realName = getRealName(player);
                        if (last != null && !last.equals(serverName)) {
                            plugin.getRedisManager().publishStaffActivity("switch", realName, rankColor.toString(), last, serverName);
                        } else {
                            plugin.getRedisManager().publishStaffActivity("join", realName, rankColor.toString(), "", serverName);
                        }
                    }
                }));
            }
            if ((player.hasPermission("wintercore.admin") || player.hasPermission("wintercore.manager")) && !plugin.isPlaceholderAPIEnabled()) {
                player.sendMessage(CC.translate("&cWarning: PlaceholderAPI is not installed on this server!"));
                player.sendMessage(CC.translate("&ePlease install PlaceholderAPI to ensure full functionality."));
                player.sendMessage(CC.translate("&eFor more information, visit:&ahttps://www.spigotmc.org/resources/placeholderapi.6245/"));
            }
        }, 10L);

        databaseManager.getBanDetails(uuid, banDetails -> {
            if (banDetails != null && !banDetails.isEmpty()) {
                Long expiration = (Long) banDetails.get("expiration");
                String reason = (String) banDetails.get("reason");

                if (expiration != null) {
                    long timeLeft = expiration - System.currentTimeMillis();
                    String timeMessage;
                    if (timeLeft < 60000) {
                        timeMessage = (timeLeft / 1000) + " seconds";
                    } else if (timeLeft < 3600000) {
                        timeMessage = (timeLeft / 60000) + " minutes";
                    } else if (timeLeft < 86400000) {
                        timeMessage = (timeLeft / 3600000) + " hours";
                    } else {
                        timeMessage = (timeLeft / 86400000) + " days";
                    }

                    String banMessage = CC.translate("&cYou are temporarily banned from the server for: " + timeMessage + "\n"
                            + "&cReason: &b" + (reason != null ? reason : "No reason provided"));
                    player.sendMessage(banMessage);
                } else {
                    String banMessage = CC.translate("&cYou are permanently banned from the server.\n"
                            + "&cReason: &b" + (reason != null ? reason : "No reason provided"));
                    player.sendMessage(banMessage);
                }
            }
        });
    }

    public void AreConditionsMet(Player player, java.util.function.Consumer<Boolean> callback) {
        databaseManager.isPlayerMuted(player.getUniqueId(), isMuted -> {
            if (isMuted) {
                player.sendMessage(CC.translate("&cYou are muted and cannot send messages."));
            }
            callback.accept(isMuted);
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        // Reset nametag using NameTagHandler only
        if (plugin.getNameTagHandler() != null) {
            plugin.getNameTagHandler().getNameTagAdapter().resetNameTag(player);
        }
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {

            boolean isPending = plugin.getRedisManager().isStillPendingSwitch(uuid);

            if (isPending) {
                plugin.getRedisManager().clearPendingSwitch(uuid);
                return;
            }


            if (player.hasPermission("WinterCore.staff") || player.hasPermission("WinterCore.admin") || player.hasPermission("WinterCore.manager")) {
                rankManager.getRank(player, rank -> rankManager.getColorPreference(rank, rankColor -> {
                    String lastServer = plugin.getRedisManager().getLastServer(uuid);
                    if (lastServer == null) lastServer = "unknown";

                    plugin.getRedisManager().publishStaffActivity(
                            "quit",
                            player.getName(),
                            rankColor.toString(),
                            plugin.getConfig().getString("server-name"),
                            lastServer
                    );
                    plugin.getRedisManager().removeLastServer(player.getUniqueId());
                }));
            }
        }, 2L);
    }


    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        String message = event.getMessage();
        // Always cancel the event to prevent default chat
        event.setCancelled(true);
        AreConditionsMet(player, isMuted -> {
            if (isMuted) {
                return;
            }
            if (message.startsWith("!")) {
                handleStaffChat(player, message.substring(1).trim());
                return;
            }

            if (message.startsWith("@")) {
                handleAdminChat(player, message.substring(1).trim());
                return;
            }

            if (message.startsWith("#")) {
                handleManagerChat(player, message.substring(1).trim());
                return;
            }

            tagsManager.getPlayerTag(player.getUniqueId(), tag -> {
                final String tagPrefix;
                if (tag != null) {
                    tagPrefix = " " + CC.translate(TagsManager.colorNameToCode(tag.getColor())) + tag.getPrefix() + org.bukkit.ChatColor.RESET;
                } else {
                    tagPrefix = "";
                }
                // Check if disguised
                if (plugin.getDisguiseRegistry().isDisguised(player)) {
                    plugin.getDisguiseRegistry().getEffectivePrefix(player, prefix -> plugin.getDisguiseRegistry().getEffectiveColor(player, color -> rankManager.getDisguiseRank(player, disguiseRank -> rankManager.getColorPreference(disguiseRank, nameColor -> {
                        final String formattedPrefix = prefix.isEmpty() ? "" : prefix + " ";
                        final String formattedName = formattedPrefix + CC.translate(nameColor) + player.getName() + ChatColor.RESET + tagPrefix;
                        final String colorCode = rankManager.getMessageColorPreference(player).toString();
                        final ChatColor messageColor = ChatColor.getByChar(colorCode.replace("&", "").charAt(0));
                        final String formattedMessage = formattedName + ChatColor.WHITE + ": " + messageColor + message;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                p.sendMessage(formattedMessage);
                            }
                        });
                    }))));
                } else {
                    plugin.getDisguiseRegistry().getEffectivePrefix(player, prefix -> plugin.getDisguiseRegistry().getEffectiveColor(player, color -> {
                        final String formattedPrefix = prefix.isEmpty() ? "" : prefix + " ";
                        final String formattedName = formattedPrefix + CC.translate(color) + player.getName() + ChatColor.RESET + tagPrefix;
                        final String colorCode = rankManager.getMessageColorPreference(player).toString();
                        final ChatColor messageColor = ChatColor.getByChar(colorCode.replace("&", "").charAt(0));
                        final String formattedMessage = formattedName + ChatColor.WHITE + ": " + messageColor + message;
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                p.sendMessage(formattedMessage);
                            }
                        });
                    }));
                }
            });
        });
    }

    private void handleStaffChat(Player player, String content) {
        if (content.isEmpty()) return;

        if (player.hasPermission("WinterCore.staff") || player.hasPermission("WinterCore.Admin") || player.hasPermission("WinterCore.Manager")) {
            getStaffChatMessage(player, content, formatted -> {
                if (formatted != null) {
                    plugin.getRedisManager().broadcastStaffMessage(formatted);
                }
            });
        } else {
            player.sendMessage(CC.translate("&cYou do not have permission to use staff chat."));
        }
    }

    private void handleAdminChat(Player player, String content) {
        if (content.isEmpty()) return;

        if (player.hasPermission("WinterCore.Admin") || player.hasPermission("WinterCore.Manager")) {
            getAdminChatMessage(player, content, formatted -> {
                if (formatted != null) {
                    plugin.getRedisManager().broadcastAdminMessage(formatted);
                }
            });
        } else {
            player.sendMessage(CC.translate("&cYou do not have permission to use admin chat."));
        }
    }

    private void handleManagerChat(Player player, String content) {
        if (content.isEmpty()) return;

        if (player.hasPermission("WinterCore.Manager")) {
            getManagerChatMessage(player, content, formatted -> {
                if (formatted != null) {
                    plugin.getRedisManager().broadcastManagerMessage(formatted);
                }
            });
        } else {
            player.sendMessage(CC.translate("&cYou do not have permission to use manager chat."));
        }
    }

    public void sendPrivateMessage(Player sender, Player recipient, String message) {
        rankManager.getRank(sender, senderRank -> rankManager.getColorPreference(senderRank, senderColor -> rankManager.getRank(recipient, recipientRank -> rankManager.getColorPreference(recipientRank, recipientColor -> {
            ChatColor senderMessageColor = ChatColor.getByChar(senderColor.charAt(1));
            ChatColor recipientMessageColor = ChatColor.getByChar(recipientColor.charAt(1));
            TextComponent senderMessage = new TextComponent(ChatColor.YELLOW + "(To " + recipientMessageColor + recipient.getDisplayName() + ChatColor.YELLOW + ") " + message);
            TextComponent recipientMessage = new TextComponent( ChatColor.YELLOW + "(From " + senderMessageColor + sender.getDisplayName() + ChatColor.YELLOW + ") " + message);
            sender.spigot().sendMessage(senderMessage);
            recipient.spigot().sendMessage(recipientMessage);
        }))));
    }

    public void notifyStaff(Player reporter, Player target, String reason) {
        if (reporter == null || target == null || reason == null || reason.isEmpty()) {
            plugin.getLogger().warning("Invalid report data: reporter, target, or reason is null/empty.");
            return;
        }
        String serverName = plugin.getConfig().getString("server-name", "Unknown");
        plugin.getRedisManager().publishReport(
                reporter.getName(),
                target.getName(),
                reason,
                serverName
        );
    }

    public void getStaffChatMessage(Player player, String message, java.util.function.Consumer<String> callback) {
        if (player.hasPermission("WinterCore.staff") || player.hasPermission("WinterCore.Admin") || player.hasPermission("WinterCore.Manager")) {
            plugin.getDisguiseRegistry().getEffectivePrefix(player, rankPrefix -> {
                String playerName = player.getDisplayName();
                String chatMessage = CC.translate("&9") + message + org.bukkit.ChatColor.RESET;
                String finalMessage = CC.translate("&9[SC] ") + rankPrefix + playerName + ": " + chatMessage;
                callback.accept(finalMessage);
            });
        } else {
            callback.accept(null);
        }
    }

    public void getAdminChatMessage(Player player, String message, java.util.function.Consumer<String> callback) {
        if (player.hasPermission("WinterCore.Manager") || player.hasPermission("WinterCore.Admin")) {
            plugin.getDisguiseRegistry().getEffectivePrefix(player, rankPrefix -> {
                String playerName = player.getDisplayName();
                String chatMessage = CC.translate("&c") + message + org.bukkit.ChatColor.RESET;
                String finalMessage = CC.translate("&c[AC] ") + rankPrefix + playerName + ": " + chatMessage;
                callback.accept(finalMessage);
            });
        } else {
            callback.accept(null);
        }
    }

    public void getManagerChatMessage(Player player, String message, java.util.function.Consumer<String> callback) {
        if (player.hasPermission("WinterCore.Manager")) {
            plugin.getDisguiseRegistry().getEffectivePrefix(player, rankPrefix -> {
                String playerName = player.getDisplayName();
                String chatMessage = CC.translate("&4") + message + org.bukkit.ChatColor.RESET;
                String finalMessage = CC.translate("&4[MC] ") + rankPrefix + playerName + ": " + chatMessage;
                callback.accept(finalMessage);
            });
        } else {
            callback.accept(null);
        }
    }

    public void sendFreezeNotification(Player target, Player staff, boolean isFrozen) {
        rankManager.getRank(target, targetRank -> rankManager.getColorPreference(targetRank, targetColor -> rankManager.getRank(staff, staffRank -> rankManager.getColorPreference(staffRank, staffColor -> {
            String targetName = target.getDisplayName();
            String staffName = staff.getDisplayName();
            String message = isFrozen
                    ? CC.translate("&9[S] ") + targetName + CC.translate("&c has been frozen by ") + staffName + "."
                    : CC.translate("&9[S] ") + targetName + CC.translate("&a has been unfrozen by ") + staffName + ".";

            Bukkit.getOnlinePlayers().stream()
                    .filter(player -> player.hasPermission("WinterCore.staff") || player.hasPermission("WinterCore.admin") || player.hasPermission("WinterCore.manager"))
                    .forEach(staffMember -> staffMember.sendMessage(message));
        }))));
    }

    public String getRealName(Player player) {
        if (plugin.getDisguiseDataMap().containsKey(player.getUniqueId())) {
            DisguiseData data = plugin.getDisguiseDataMap().get(player.getUniqueId());
            if (data != null && data.getInfo() != null && data.getInfo().has("name")) {
                return data.getInfo().get("name").getAsString();
            }
        }
        return player.getName();
    }
}
