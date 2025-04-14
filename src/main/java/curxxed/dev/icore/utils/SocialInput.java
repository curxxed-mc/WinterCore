package curxxed.dev.icore.utils;

import curxxed.dev.icore.Database.RedisManager;
import curxxed.dev.icore.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.*;

public class SocialInput implements Listener {

    private final Main plugin;
    private static final Map<UUID, SocialPrompt> awaiting = new HashMap<>();

    // Constructor registers the listener
    public SocialInput(Main plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Called when player clicks a social head in the menu
     */
    public static boolean waitForInput(Player player, String platform, RedisManager redis) {
        UUID uuid = player.getUniqueId();

        // Prevent multiple inputs
        if (awaiting.containsKey(uuid)) {
            player.sendMessage(ChatColor.RED + "You already have an active input pending. Please type your " + awaiting.get(uuid).platform + " info first.");
            return false;
        }

        // Set the player and platform they're waiting for
        awaiting.put(uuid, new SocialPrompt(platform, redis));

        // Inform the player to type their info
        player.sendMessage(ChatColor.YELLOW + "Type your " + ChatColor.AQUA + platform + ChatColor.YELLOW + " info in chat:");
        return true;
    }

    // Handle player chat events for social media input
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Check if the player is waiting for input
        if (!awaiting.containsKey(uuid)) return;

        SocialPrompt prompt = awaiting.remove(uuid); // Get the current prompt for the player
        String input = event.getMessage(); // Player's chat input
        event.setCancelled(true); // Prevent the input from showing in chat

        // Handle the input asynchronously
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!prompt.validate(input)) {
                // If the input is invalid, inform the player
                player.sendMessage(ChatColor.RED + "Invalid " + prompt.platform + " link.");
            } else {
                // If valid, save the link to Redis
                prompt.redis.setSocialLink(player.getUniqueId(), prompt.platform, input);
                player.sendMessage(ChatColor.GREEN + "Updated your " + prompt.platform + " link to: " + input);
            }
        });
    }

    // This class holds the details for the social media input
    private static class SocialPrompt {
        final String platform;
        final RedisManager redis;

        SocialPrompt(String platform, RedisManager redis) {
            this.platform = platform.toLowerCase(); // Standardize the platform name (e.g., "discord", "youtube")
            this.redis = redis;
        }

        // Validate the input based on the platform
        boolean validate(String input) {
            switch (platform) {
                case "discord":
                    // Discord validation: Should be like "discord.gg/" or "username#1234"
                    return input.length() <= 32 && (input.contains("#") || input.contains("discord.gg/"));
                case "youtube":
                    // YouTube validation: Should start with "youtube.com/@"
                    return input.contains("youtube.com/@");
                case "twitter":
                    // Twitter validation: Should start with "http://twitter.com" or "https://twitter.com"
                    return input.startsWith("http://twitter.com") ||
                            input.startsWith("https://twitter.com") ||
                            input.startsWith("http://x.com") ||
                            input.startsWith("https://x.com") ||
                            input.startsWith("twitter.com") ||
                            input.startsWith("x.com");
                default:
                    return false; // Invalid if no known platform
            }
        }
    }
}
