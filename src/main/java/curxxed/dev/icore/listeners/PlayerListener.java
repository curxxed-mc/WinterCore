package curxxed.dev.icore.listeners;

import curxxed.dev.icore.Database.DatabaseManager;
import curxxed.dev.icore.Placeholders.Placeholder;
import curxxed.dev.icore.iCore;
import curxxed.dev.icore.utils.GUI.ColorGUI;
import curxxed.dev.icore.utils.RankManager;
import curxxed.dev.icore.Commands.Staff.FreezeCommand;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
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

    public PlayerListener(iCore plugin) {
        this.plugin = plugin;


        this.rankManager = new RankManager(plugin);
        this.freezeCommand = plugin.getFreezeCommand();
        this.databaseManager = plugin.getDatabaseManager();
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        plugin.getPermissionManager().loadAndApplyPermissions(player);
        rankManager.refreshPlayerDisplay(player);
        rankManager.setRankAboveHead(player);

        rankManager.getRank(player, rank -> {
            rankManager.getColorPreference(rank, rankColor -> {
                org.bukkit.ChatColor color = org.bukkit.ChatColor.getByChar(rankColor.replace("&", "").charAt(0));
                rankManager.updateNameTagColor(player, color);

                if (player.hasPermission("iCore.staff") || player.hasPermission("iCore.admin") || player.hasPermission("iCore.manager")) {
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

                    String banMessage = ChatColor.RED + "You are temporarily banned from the server " + "for: " + timeMessage + "\n" +
                            ChatColor.RED + "Reason: " + ChatColor.AQUA + (reason != null ? reason : "No reason provided");
                    player.sendMessage(banMessage);
                } else {
                    String banMessage = ChatColor.RED + "You are permanently banned from the server.\n" +
                            ChatColor.RED + "Reason: " + ChatColor.AQUA + (reason != null ? reason : "No reason provided");
                    player.sendMessage(banMessage);
                }
            }
        });
    }
    public void isPlayerMuted(Player player, java.util.function.Consumer<Boolean> callback) {
        databaseManager.isPlayerMuted(player.getUniqueId(), isMuted -> {
            if (isMuted) {
                player.sendMessage(ChatColor.RED + "You are muted and cannot send messages.");
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

            plugin.getRankManager().getRankPrefix(player, prefix -> {
                String color = plugin.getRankManager().getColorPreferenceSync(player);
                String formattedName = prefix + ChatColor.translateAlternateColorCodes('&', color) + player.getName();
                ChatColor messageColor = plugin.getRankManager().getMessageColorPreference(player).asBungee();
                String formattedMessage = formattedName + ChatColor.WHITE + ": " + messageColor + message;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendMessage(formattedMessage);
                    }
                });
            });
        });
    }

    private void handleStaffChat(Player player, String content) {
        if (content.isEmpty()) return;

        if (player.hasPermission("iCore.Staff") || player.hasPermission("iCore.Admin") || player.hasPermission("iCore.Manager")) {
            getStaffChatMessage(player, content, formatted -> {
                if (formatted != null) {
                    // Broadcast only via Redis
                    plugin.getRedisManager().broadcastStaffMessage(formatted);
                }
            });
        } else {
            player.sendMessage(ChatColor.RED + "You do not have permission to use staff chat.");
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
            player.sendMessage(ChatColor.RED + "You do not have permission to use admin chat.");
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
            player.sendMessage(ChatColor.RED + "You do not have permission to use manager chat.");
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

                        TextComponent senderMessage = new TextComponent("(To " + recipientMessageColor + recipient.getDisplayName() + ChatColor.YELLOW + ") " + message);
                        senderMessage.setColor(ChatColor.YELLOW);

                        TextComponent recipientMessage = new TextComponent("(From " + senderMessageColor + sender.getDisplayName() + ChatColor.YELLOW + ") " + message);
                        recipientMessage.setColor(ChatColor.YELLOW);

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
        if (player.hasPermission("iCore.Staff") || player.hasPermission("iCore.Admin") || player.hasPermission("iCore.Manager")) {
            rankManager.getRankPrefix(player, rankPrefix -> {
                String playerName = player.getDisplayName();
                String chatMessage = ChatColor.BLUE + message + ChatColor.RESET;
                String finalMessage = ChatColor.BLUE + "[SC] " + rankPrefix + playerName + ": " + chatMessage;

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
                String chatMessage = ChatColor.RED + message + ChatColor.RESET;
                String finalMessage = ChatColor.RED + "[AC] " + rankPrefix + playerName + ": " + chatMessage;

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
                String chatMessage = ChatColor.DARK_RED + message + ChatColor.RESET;
                String finalMessage = ChatColor.DARK_RED + "[MC] " + rankPrefix + playerName + ": " + chatMessage;

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
                                ? ChatColor.BLUE + "[S] " + targetName + ChatColor.RED + " has been frozen by " +  staffName + "."
                                : ChatColor.BLUE + "[S] " + targetName + ChatColor.GREEN + " has been unfrozen by " +  staffName + ".";

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
