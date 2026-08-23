package net.curxxed.dev.wintercore.placeholders;

import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.disguise.player.DisguiseData;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.rank.RankManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public class WinterCoreExpansion extends PlaceholderExpansion {

    private final WinterCore plugin;

    public WinterCoreExpansion(WinterCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NonNull String getIdentifier() {
        return "wintercore";
    }

    @Override
    public @NonNull String getAuthor() {
        return "curxxed";
    }

    @Override
    public @NonNull String getVersion() {
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
    public String onPlaceholderRequest(Player player, @NonNull String identifier) {
        if (player == null) return "";

        RankManager rankManager = plugin.getRankManager();
        DisguiseData disguiseData = plugin.getDisguiseRegistry().getDisguiseData(player.getUniqueId());
        boolean disguised = disguiseData != null;
        String disguisedRank = disguised ? disguiseData.getRank() : null;
        String disguisedColor = null;
        if (disguised) {
            try {
                if (disguiseData.getInfo() != null && disguiseData.getInfo().has("color") && !disguiseData.getInfo().get("color").isJsonNull()) {
                    disguisedColor = disguiseData.getInfo().get("color").getAsString();
                }
            } catch (Exception ignore) {}
        }

        boolean useDisguise = disguised && disguisedRank != null && !disguisedRank.isEmpty();

        switch (identifier.toLowerCase()) {
            case "player_rank": {
                if (useDisguise) {
                    return CC.translate(resolveDisguisedColor(rankManager, disguisedRank, disguisedColor) + disguisedRank);
                } else {
                    String realRank = rankManager.getRankSync(player);
                    String rankColor = rankManager.getColorPreferenceSync(player);
                    return CC.translate((rankColor != null ? rankColor : "") + (realRank != null ? realRank : ""));
                }
            }
            case "player_color": {
                if (useDisguise) {
                    return CC.translate(resolveDisguisedColor(rankManager, disguisedRank, disguisedColor));
                } else {
                    String color = rankManager.getColorPreferenceSync(player);
                    return CC.translate(color != null ? color : "");
                }
            }
            case "player_rank_name": {
                if (useDisguise) {
                    return ChatColor.stripColor(CC.translate(disguisedRank));
                } else {
                    String realRank = rankManager.getRankSync(player);
                    return realRank != null ? ChatColor.stripColor(CC.translate(realRank)) : "";
                }
            }
            case "player_rank_weight": {
                String usedRank = useDisguise ? disguisedRank : rankManager.getRankSync(player);
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

    private String resolveDisguisedColor(RankManager rankManager, String disguisedRank, String disguisedColor) {
        if (disguisedColor != null && !disguisedColor.isEmpty()) {
            return disguisedColor;
        }
        try {
            ConfigurationSection rankSection = rankManager.getRanksSection().getConfigurationSection(disguisedRank);
            return rankSection != null ? rankSection.getString("name-color", "&f") : "&f";
        } catch (Exception e) {
            return "&f";
        }
    }
}





