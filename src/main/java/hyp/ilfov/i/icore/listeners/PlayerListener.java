package hyp.ilfov.i.icore.listeners;

import hyp.ilfov.i.icore.Main;
import hyp.ilfov.i.icore.utils.BukkitReflection;
import hyp.ilfov.i.icore.utils.RankManager;
import hyp.ilfov.i.icore.Commands.Staff.FreezeCommand;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
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

        rankManager.getRankAsync(player, rank -> plugin.getLogger().info("Cached rank '" + rank + "' for " + player.getName()));
        rankManager.refreshPlayerDisplay(player);
        rankManager.setRankAboveHead(player);

        rankManager.getRank(player, rank -> {
            rankManager.getColorPreference(rank, rankColor -> {
                BukkitReflection.updatePlayerNameTag(player, rankColor);

                String serverName = plugin.getConfig().getString("server-name", "hub-restricted");
                String playerName = rankColor + player.getName() + ChatColor.RESET;

                String last = plugin.getRedisManager().getLastServer(player.getUniqueId());
                plugin.getRedisManager().updateLastServer(player.getUniqueId(), serverName);

                if (last != null && !last.equals(serverName)) {
                    plugin.getRedisManager().publishStaffActivity("switch", player.getName(), rankColor.toString(), last, serverName);
                } else {
                    plugin.getRedisManager().publishStaffActivity("join", player.getName(), rankColor.toString(), "", serverName);
                }
            });
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        Player player = event.getPlayer();
        String serverName = plugin.getConfig().getString("server-name", "Unknown");

        rankManager.getRank(player, rank -> {
            rankManager.getColorPreference(rank, rankColor -> {
                plugin.getRedisManager().publishStaffActivity("quit", player.getName(), rankColor.toString(), serverName, "");
                plugin.getRedisManager().removeLastServer(player.getUniqueId());
            });
        });
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
        String staffMessage = getStaffNotificationMessage(reporter, target, reason);
        TextComponent tpButton = new TextComponent(" [TP]");
        tpButton.setColor(ChatColor.GREEN);
        tpButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + target.getName()));
        tpButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[] { (BaseComponent)new TextComponent("Teleport to " + target
                .getName()) }));
        this.plugin.getServer().getOnlinePlayers().stream()
                .filter(player -> player.hasPermission("iCore.staff"))
                .forEach(staff -> staff.spigot().sendMessage(new BaseComponent[] { (BaseComponent)new TextComponent(staffMessage), (BaseComponent)tpButton }));
    }

    public String getStaffNotificationMessage(Player reporter, Player target, String reason) {
        String reporterName = reporter.getDisplayName();
        String targetName = target.getDisplayName();
        return ChatColor.BLUE + "[SC] " + reporterName + ChatColor.WHITE + " has reported " + targetName + " for: " + ChatColor.GRAY + reason;
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
