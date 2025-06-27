package net.curxxed.dev.wintercore.placeholders;

import net.curxxed.dev.wintercore.disguise.DisguiseRegistry;
import net.curxxed.dev.wintercore.disguise.player.DisguiseData;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.rank.RankChangeEvent;
import net.curxxed.dev.wintercore.rank.RankManager;
import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class Placeholder extends PlaceholderExpansion implements Listener {

    private final WinterCore plugin;
    private final DisguiseRegistry disguiseRegistry;

    @Getter
    private static Placeholder instance;

    public Placeholder(WinterCore plugin) {
        this.plugin = plugin;
        this.disguiseRegistry = plugin.getDisguiseRegistry();
        instance = this;
        if (!this.register()) {
            plugin.getLogger().warning("Could not register the WinterCore expansion.");
        }
    }

    @Override
    public @NotNull String getIdentifier() {
        return "wintercore";
    }

    @Override
    public @NotNull String getAuthor() {
        return "curxxed";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "";

        RankManager rankManager = RankManager.getInstance();
        DisguiseData disguiseData = plugin.getDisguiseDataMap().get(player.getUniqueId());
        boolean disguised = disguiseData != null;
        String disguisedRank = disguised ? disguiseData.getRank() : null;
        String disguisedColor = null;
        if (disguised) {
            // Try to get color from disguiseData.info if present
            try {
                if (disguiseData.getInfo() != null && disguiseData.getInfo().has("color") && !disguiseData.getInfo().get("color").isJsonNull()) {
                    disguisedColor = disguiseData.getInfo().get("color").getAsString();
                }
            } catch (Exception ignore) {}
        }

        switch (identifier.toLowerCase()) {
            case "player_rank": {
                if (disguised && disguisedRank != null && !disguisedRank.isEmpty()) {
                    String rankColor = disguisedColor;
                    if (rankColor == null || rankColor.isEmpty()) {
                        try {
                            ConfigurationSection rankSection = rankManager.getRanksSection().getConfigurationSection(disguisedRank);
                            if (rankSection != null) {
                                rankColor = rankSection.getString("name-color", "&f");
                            } else {
                                rankColor = "&f";
                            }
                        } catch (Exception e) {
                            rankColor = "&f";
                        }
                    }
                    return org.bukkit.ChatColor.translateAlternateColorCodes('&', (rankColor != null ? rankColor : "") + disguisedRank);
                } else {
                    String realRank = rankManager.getRankSync(player);
                    String rankColor = rankManager.getColorPreferenceSync(player);
                    return org.bukkit.ChatColor.translateAlternateColorCodes('&', (rankColor != null ? rankColor : "") + (realRank != null ? realRank : ""));
                }
            }
            case "player_color": {
                if (disguised && disguisedRank != null && !disguisedRank.isEmpty()) {
                    String rankColor = disguisedColor;
                    if (rankColor == null || rankColor.isEmpty()) {
                        try {
                            ConfigurationSection rankSection = rankManager.getRanksSection().getConfigurationSection(disguisedRank);
                            if (rankSection != null) {
                                rankColor = rankSection.getString("name-color", "&f");
                            } else {
                                rankColor = "&f";
                            }
                        } catch (Exception e) {
                            rankColor = "&f";
                        }
                    }
                    return org.bukkit.ChatColor.translateAlternateColorCodes('&', rankColor != null ? rankColor : "");
                } else {
                    String color = rankManager.getColorPreferenceSync(player);
                    return org.bukkit.ChatColor.translateAlternateColorCodes('&', color != null ? color : "");
                }
            }
            case "player_rank_name": {
                if (disguised && disguisedRank != null && !disguisedRank.isEmpty()) {
                    return disguisedRank.replaceAll("§.", "");
                } else {
                    String realRank = rankManager.getRankSync(player);
                    return realRank != null ? realRank.replaceAll("§.", "") : "";
                }
            }
            case "player_rank_weight": {
                String usedRank = (disguised && disguisedRank != null && !disguisedRank.isEmpty()) ? disguisedRank : rankManager.getRankSync(player);
                if (usedRank == null || usedRank.isEmpty()) return "0";
                try {
                    return String.valueOf(plugin.getRankManager().getRanksSection()
                            .getConfigurationSection(usedRank)
                            .getInt("weight", 0));
                } catch (Exception e) {
                    return "0";
                }
            }
            default:
                return "";
        }
    }

    @EventHandler
    public void onRankChange(RankChangeEvent event) {
        Player player = event.getPlayer();
        String newRank = event.getNewRank();
        RankManager rm = RankManager.getInstance();

        // Only update if the rank actually changed
        String currentCached = rm.getRankSync(player);
        if (currentCached != null && currentCached.equals(newRank)) {
            // No change, skip update
            return;
        }

        // Update the rank in the cache
        rm.cachePlayerRank(player, newRank);

        // Clear placeholder cache for this player
        // Refresh player display for all (fixes nametag color for everyone)
        rm.refreshPlayerDisplayForAll(player);

        // Only run PlaceholderAPI code if it's enabled
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            Bukkit.getScheduler().runTask(plugin, () -> PlaceholderAPI.setPlaceholders(player, "%wintercore_player_rank% %wintercore_player_color% %wintercore_player_rank_name% %wintercore_player_rank_weight%"));
        }
    }
}
