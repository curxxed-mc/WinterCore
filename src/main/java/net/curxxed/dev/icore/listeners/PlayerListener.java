package net.curxxed.dev.icore.listeners;

import net.curxxed.dev.icore.database.DatabaseManager;
import net.curxxed.dev.icore.permissions.iCorePermissibleInjector;
import net.curxxed.dev.icore.tags.TagsManager;
import net.curxxed.dev.icore.plugin.iCore;
import net.curxxed.dev.icore.gui.ColorGUI;
import net.curxxed.dev.icore.rank.RankManager;
import net.curxxed.dev.icore.commands.staff.FreezeCommand;
import net.curxxed.dev.icore.utils.CC;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
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
    private final iCore plugin;
    private final RankManager rankManager;
    private final FreezeCommand freezeCommand;
    private final DatabaseManager databaseManager;
    private final Map<UUID, String> lastServer = new HashMap<>();
    public static final ColorGUI colorGUI = ColorGUI.getInstance();
    private final TagsManager tagsManager;

    public PlayerListener(iCore plugin, TagsManager tagsManager) {
        this.plugin = plugin;
        this.tagsManager = tagsManager;

        this.rankManager = new RankManager(plugin);
        this.freezeCommand = plugin.getFreezeCommand();
        this.databaseManager = plugin.getDatabaseManager();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLoginHandle(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        try {
            iCorePermissibleInjector.initPlayer(player);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        rankManager.refreshPlayerDisplay(player);
        rankManager.setRankAboveHead(player);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            rankManager.getRank(player, rank -> {
                rankManager.getColorPreference(rank, rankColor -> {
                    org.bukkit.ChatColor color = org.bukkit.ChatColor.getByChar(rankColor.replace("&", "").charAt(0));
                    rankManager.updateNameTagColor(player, color);

                    if (player.hasPermission("icore.staff") || player.hasPermission("icore.admin") || player.hasPermission("icore.manager") || player.isOp()) {
                        String serverName = plugin.getConfig().getString("server-name", "hub-restricted");
                        String last = plugin.getRedisManager().getLastServer(uuid);
                        plugin.getRedisManager().updateLastServer(uuid, serverName);

                        if (last != null && !last.equals(serverName)) {
                            plugin.getRedisManager().publishStaffActivity("switch", player.getName(), rankColor.toString(), last, serverName);
                        } else {
                            plugin.getRedisManager().publishStaffActivity("join", player.getName(), rankColor.toString(), "", serverName);
                        }
                    }
                });
            });
            if ((player.hasPermission("icore.admin") || player.hasPermission("icore.manager")) && !plugin.isPlaceholderAPIEnabled()) {
                player.sendMessage(CC.translate("&cWarning: PlaceholderAPI is not installed on this server!"));
                player.sendMessage(CC.translate("&ePlease install PlaceholderAPI to ensure full functionality."));
                player.sendMessage(CC.translate("&eFor more information, visit:&ahttps://www.spigotmc.org/resources/placeholderapi.6245/"));
            }
            if ((player.hasPermission("icore.admin") || player.hasPermission("icore.manager")) && plugin.isWinterSpigotDetected()) {
                player.sendMessage(CC.translate("&bWinterSpigot is installed on this server. This is not needed for iCore to function properly but it is recommended by the iCore team."));
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

    public void isPlayerMuted(Player player, java.util.function.Consumer<Boolean> callback) {
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

        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {

            boolean isPending = plugin.getRedisManager().isStillPendingSwitch(uuid);

            if (isPending) {
                plugin.getRedisManager().clearPendingSwitch(uuid); // Optional cleanup
                return;
            }


            // Only process quit messages for staff members
            if (player.hasPermission("iCore.staff") || player.hasPermission("iCore.admin") || player.hasPermission("iCore.manager")) {
                rankManager.getRank(player, rank -> {
                    rankManager.getColorPreference(rank, rankColor -> {
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
                    });
                });
            }
        }, 2L); // Wait 1 second
    }


    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // Cancel vanilla formatting
        event.setCancelled(true);

        isPlayerMuted(player, isMuted -> {
            if (isMuted) return;

            // Handle channel prefixes (!, @, #)
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
                // Check if player is disguised (sync)
                if (plugin.getDisguiseRegistry().isDisguised(player)) {
                    plugin.getDisguiseRegistry().getEffectivePrefix(player, prefix -> {
                        plugin.getDisguiseRegistry().getEffectiveColor(player, color -> {
                            final String formattedPrefix = prefix.isEmpty() ? "" : prefix + " ";
                            final String formattedName = formattedPrefix + CC.translate(color) + player.getName() + org.bukkit.ChatColor.RESET + tagPrefix;
                            final String colorCode = plugin.getRankManager().getMessageColorPreference(player).toString();
                            final org.bukkit.ChatColor messageColor = org.bukkit.ChatColor.getByChar(colorCode.replace("&", "").charAt(0));
                            final String formattedMessage = formattedName + org.bukkit.ChatColor.WHITE + ": " + messageColor + message;
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                for (Player p : Bukkit.getOnlinePlayers()) {
                                    p.sendMessage(formattedMessage);
                                }
                            });
                        });
                    });
                } else {
                    plugin.getRankManager().getRankPrefix(player, prefix -> {
                        final String color = plugin.getRankManager().getColorPreferenceSync(player);
                        final String formattedPrefix = prefix.isEmpty() ? "" : prefix + " ";
                        final String formattedName = formattedPrefix + CC.translate(color) + player.getName() + org.bukkit.ChatColor.RESET + tagPrefix;
                        final String colorCode = plugin.getRankManager().getMessageColorPreference(player).toString();
                        final org.bukkit.ChatColor messageColor = org.bukkit.ChatColor.getByChar(colorCode.replace("&", "").charAt(0));
                        final String formattedMessage = formattedName + org.bukkit.ChatColor.WHITE + ": " + messageColor + message;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                p.sendMessage(formattedMessage);
                            }
                        });
                    });
                }
            });
        });
    }

    private void handleStaffChat(Player player, String content) {
        if (content.isEmpty()) return;

        if (player.hasPermission("iCore.staff") || player.hasPermission("iCore.Admin") || player.hasPermission("iCore.Manager")) {
            getStaffChatMessage(player, content, formatted -> {
                if (formatted != null) {
                    // Broadcast only via Redis
                    plugin.getRedisManager().broadcastStaffMessage(formatted);
                }
            });
        } else {
            player.sendMessage(CC.translate("&cYou do not have permission to use staff chat."));
        }
    }

    private void handleAdminChat(Player player, String content) {
        if (content.isEmpty()) return;

        if (player.hasPermission("iCore.Admin") || player.hasPermission("iCore.Manager")) {
            getAdminChatMessage(player, content, formatted -> {
                if (formatted != null) {
                    // Broadcast only via Redis
                    plugin.getRedisManager().broadcastAdminMessage(formatted);
                }
            });
        } else {
            player.sendMessage(CC.translate("&cYou do not have permission to use admin chat."));
        }
    }

    private void handleManagerChat(Player player, String content) {
        if (content.isEmpty()) return;

        if (player.hasPermission("iCore.Manager")) {
            getManagerChatMessage(player, content, formatted -> {
                if (formatted != null) {
                    // Broadcast only via Redis
                    plugin.getRedisManager().broadcastManagerMessage(formatted);
                }
            });
        } else {
            player.sendMessage(CC.translate("&cYou do not have permission to use manager chat."));
        }
    }

    public void sendPrivateMessage(Player sender, Player recipient, String message) {
        rankManager.getRank(sender, senderRank -> {
            rankManager.getColorPreference(senderRank, senderColor -> {
                rankManager.getRank(recipient, recipientRank -> {
                    rankManager.getColorPreference(recipientRank, recipientColor -> {
                        // Convert to Bukkit's ChatColor
                        org.bukkit.ChatColor senderMessageColor = org.bukkit.ChatColor.getByChar(senderColor.charAt(1));
                        org.bukkit.ChatColor recipientMessageColor = org.bukkit.ChatColor.getByChar(recipientColor.charAt(1));

                        TextComponent senderMessage = new TextComponent(org.bukkit.ChatColor.YELLOW + "(To " + recipientMessageColor + recipient.getDisplayName() + org.bukkit.ChatColor.YELLOW + ") " + message);
                        // No need to setColor, handled by color codes in string

                        TextComponent recipientMessage = new TextComponent( org.bukkit.ChatColor.YELLOW + "(From " + senderMessageColor + sender.getDisplayName() + org.bukkit.ChatColor.YELLOW + ") " + message);
                        // No need to setColor, handled by color codes in string

                        // Send the formatted messages
                        sender.spigot().sendMessage(senderMessage);
                        recipient.spigot().sendMessage(recipientMessage);
                    });
                });
            });
        });
    }

    public void notifyStaff(Player reporter, Player target, String reason) {
        // Ensure inputs are valid
        if (reporter == null || target == null || reason == null || reason.isEmpty()) {
            plugin.getLogger().warning("Invalid report data: reporter, target, or reason is null/empty.");
            return;
        }

        // Construct the plain text message
        String serverName = plugin.getConfig().getString("server-name", "Unknown");

        // Broadcast the message to Redis only
        plugin.getRedisManager().publishReport(
                reporter.getName(),
                target.getName(),
                reason,
                serverName
        );
    }

    public void getStaffChatMessage(Player player, String message, java.util.function.Consumer<String> callback) {
        if (player.hasPermission("iCore.staff") || player.hasPermission("iCore.Admin") || player.hasPermission("iCore.Manager")) {
            rankManager.getRankPrefix(player, rankPrefix -> {
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
        if (player.hasPermission("iCore.Manager") || player.hasPermission("iCore.Admin")) {
            rankManager.getRankPrefix(player, rankPrefix -> {
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
        if (player.hasPermission("iCore.Manager")) {
            rankManager.getRankPrefix(player, rankPrefix -> {
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
        rankManager.getRank(target, targetRank -> {
            rankManager.getColorPreference(targetRank, targetColor -> {
                rankManager.getRank(staff, staffRank -> {
                    rankManager.getColorPreference(staffRank, staffColor -> {
                        String targetName = target.getDisplayName();
                        String staffName = staff.getDisplayName();
                        String message = isFrozen
                                ? CC.translate("&9[S] ") + targetName + CC.translate("&c has been frozen by ") + staffName + "."
                                : CC.translate("&9[S] ") + targetName + CC.translate("&a has been unfrozen by ") + staffName + ".";

                        // Send the notification to all staff members
                        Bukkit.getOnlinePlayers().stream()
                                .filter(player -> player.hasPermission("iCore.staff") || player.hasPermission("iCore.admin") || player.hasPermission("iCore.manager"))
                                .forEach(staffMember -> staffMember.sendMessage(message));
                    });
                });
            });
        });
    }
}
