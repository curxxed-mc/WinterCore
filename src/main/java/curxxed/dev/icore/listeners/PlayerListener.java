package curxxed.dev.icore.listeners;

import curxxed.dev.icore.Main;
import curxxed.dev.icore.utils.BukkitReflection;
import curxxed.dev.icore.utils.GUI.ColorGUI;
import curxxed.dev.icore.utils.RankManager;
import curxxed.dev.icore.Commands.Staff.FreezeCommand;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {
    private final Main plugin;
    private final RankManager rankManager;
    private final FreezeCommand freezeCommand;
    private final Map<UUID, String> lastServer = new HashMap<>();
    public static final ColorGUI colorGUI = ColorGUI.getInstance();

    public PlayerListener(Main plugin) {
        this.plugin = plugin;


        this.rankManager = new RankManager(plugin);
        this.freezeCommand = plugin.getFreezeCommand();
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        rankManager.getRankAsync(player, rank -> plugin.getLogger().info("Cached rank '" + rank + "' for " + player.getName()));
        rankManager.refreshPlayerDisplay(player);
        rankManager.setRankAboveHead(player);

        rankManager.getRank(player, rank -> {
            rankManager.getColorPreference(rank, rankColor -> {
                BukkitReflection.updatePlayerNameTag(player, rankColor);

                if (player.hasPermission("iCore.staff") || player.hasPermission("iCore.admin") || player.hasPermission("iCore.manager")) {
                    String serverName = plugin.getConfig().getString("server-name", "hub-restricted");
                    String last = plugin.getRedisManager().getLastServer(uuid);
                    plugin.getRedisManager().updateLastServer(uuid, serverName);

                    if (last != null && !last.equals(serverName)) {
                        plugin.getLogger().info("Publishing staff switch: " + player.getName() + " from " + last + " to " + serverName);
                        plugin.getRedisManager().publishStaffActivity("switch", player.getName(), rankColor.toString(), last, serverName);
                    } else {
                        plugin.getLogger().info("Publishing staff join: " + player.getName() + " to " + serverName);
                        plugin.getRedisManager().publishStaffActivity("join", player.getName(), rankColor.toString(), "", serverName);
                    }
                }
            });
        });
    }




    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        plugin.getLogger().info("[RedisDebug] Player quit detected: " + player.getName());

        // Delay check to allow Redis to finish setting the pending key on switch
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            plugin.getLogger().info("[RedisDebug] Checking switch state for: " + player.getName());

            boolean isPending = plugin.getRedisManager().isStillPendingSwitch(uuid);

            if (isPending) {
                plugin.getLogger().info("[RedisDebug] " + player.getName() + " is switching servers. No quit announcement.");
                plugin.getRedisManager().clearPendingSwitch(uuid); // Optional cleanup
                return;
            }

            plugin.getLogger().info("[RedisDebug] " + player.getName() + " is ACTUALLY quitting.");

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

        }, 2L); // Wait 1 second
    }






    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        String displayName = player.getDisplayName();
        String formattedMessage = displayName + ChatColor.WHITE + ": " + message;

        // Check if the message starts with "!"
        if (message.startsWith("!")) {
            event.setCancelled(true);
            String content = message.substring(1).trim();

            if (content.isEmpty()) {
                event.setCancelled(false);
                event.setFormat(formattedMessage);
                return;
            }

            // If the player has the permission for staff chat
            if (player.hasPermission("iCore.Staff") || player.hasPermission("iCore.Admin") || player.hasPermission("iCore.Manager")) {
                getStaffChatMessage(player, content, formatted -> {
                    if (formatted != null) {
                        plugin.getServer().getOnlinePlayers().stream()
                                .filter(p -> p.hasPermission("iCore.Staff") || p.hasPermission("iCore.Admin") || p.hasPermission("iCore.Manager"));

                        plugin.getRedisManager().broadcastStaffMessage(formatted);
                    } else {
                        player.sendMessage(ChatColor.RED + "You do not have permission to use staff chat.");
                    }
                });
            } else {
                player.sendMessage(ChatColor.RED + "You do not have permission to use staff chat.");
            }

            return;
        }

        // Check if the message starts with "@"
        if (message.startsWith("@")) {
            event.setCancelled(true);
            String content = message.substring(1).trim();

            if (content.isEmpty()) {
                event.setCancelled(false);
                event.setFormat(formattedMessage);
                return;
            }

            // If the player has the permission for admin chat
            if (player.hasPermission("iCore.Admin") || player.hasPermission("iCore.Manager")) {
                getAdminChatMessage(player, content, formatted -> {
                    if (formatted != null) {
                        plugin.getServer().getOnlinePlayers().stream()
                                .filter(p -> p.hasPermission("iCore.Admin") || p.hasPermission("iCore.Manager"));


                        plugin.getRedisManager().broadcastAdminMessage(formatted);
                    } else {
                        player.sendMessage(ChatColor.RED + "You do not have permission to use admin chat.");
                    }
                });
            } else {
                player.sendMessage(ChatColor.RED + "You do not have permission to use admin chat.");
            }

            return;
        }

        // Check if the message starts with "#"
        if (message.startsWith("#")) {
            event.setCancelled(true);
            String content = message.substring(1).trim();

            if (content.isEmpty()) {
                event.setCancelled(false);
                event.setFormat(formattedMessage);
                return;
            }

            // If the player has the permission for manager chat
            if (player.hasPermission("iCore.Manager")) {
                getManagerChatMessage(player, content, formatted -> {
                    if (formatted != null) {
                        plugin.getServer().getOnlinePlayers().stream()
                                .filter(p -> p.hasPermission("iCore.Manager"));

                        plugin.getRedisManager().broadcastManagerMessage(formatted);
                    } else {
                        player.sendMessage(ChatColor.RED + "You do not have permission to use manager chat.");
                    }
                });
            } else {
                player.sendMessage(ChatColor.RED + "You do not have permission to use manager chat.");
            }

            return;
        }

        // Default message formatting
        event.setFormat(formattedMessage);
    }





    // Send private message between two players
    public void sendPrivateMessage(Player sender, Player recipient, String message) {
        // Fetch sender and recipient colors asynchronously
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
            String playerName = player.getDisplayName();

            plugin.getRankManager().getRankPrefix(player, rankPrefix -> {
                ChatColor messageColor = ChatColor.valueOf(plugin.getRankManager().getMessageColorPreference(player).name());
                String chatMessage = messageColor + message + ChatColor.RESET;
                String finalMessage = ChatColor.BLUE + "[SC] " + playerName + ": " + chatMessage;

                callback.accept(finalMessage); // Pass the message back to wherever it’s needed
            });
        } else {
            callback.accept(null);
        }
    }


    public void getAdminChatMessage(Player player, String message, java.util.function.Consumer<String> callback) {
        if (player.hasPermission("iCore.Manager") || player.hasPermission("iCore.Admin")) {
            String playerName = player.getDisplayName();

            plugin.getRankManager().getRankPrefix(player, rankPrefix -> {
                ChatColor messageColor = ChatColor.valueOf(plugin.getRankManager().getMessageColorPreference(player).name());
                String chatMessage = messageColor + message + ChatColor.RESET;
                String finalMessage = ChatColor.RED + "[AC] "  + playerName + ": " + chatMessage;

                callback.accept(finalMessage);
            });
        } else {
            callback.accept(null);
        }
    }


    public void getManagerChatMessage(Player player, String message, java.util.function.Consumer<String> callback) {
        if (player.hasPermission("iCore.Manager")) {
            String playerName = player.getDisplayName();

            plugin.getRankManager().getRankPrefix(player, rankPrefix -> {
                ChatColor messageColor = ChatColor.valueOf(plugin.getRankManager().getMessageColorPreference(player).name());
                String chatMessage = messageColor + message + ChatColor.RESET;
                String finalMessage = ChatColor.DARK_RED + "[MC] " + playerName + ": " + chatMessage;

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
