package net.curxxed.dev.wintercore.placeholders;

import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.disguise.DisguiseRegistry;
import net.curxxed.dev.wintercore.disguise.player.DisguiseData;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.rank.RankManager;
import lombok.Getter;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public class Placeholder extends PlaceholderExpansion {

    private final WinterCore plugin;
    private final DisguiseRegistry disguiseRegistry;

    @Getter
    private static Placeholder instance;

    public Placeholder(WinterCore plugin) {
        this.plugin = plugin;
        this.disguiseRegistry = plugin.getDisguiseRegistry();
        instance = this;
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

        RankManager rankManager = RankManager.getInstance();
        DisguiseData disguiseData = plugin.getDisguiseDataMap().get(player.getUniqueId());
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
                    return CC.translateAlternateColorCodes('&', (rankColor != null ? rankColor : "") + disguisedRank);
                } else {
                    String realRank = rankManager.getRankSync(player);
                    String rankColor = rankManager.getColorPreferenceSync(player);
                    return CC.translateAlternateColorCodes('&', (rankColor != null ? rankColor : "") + (realRank != null ? realRank : ""));
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
                    return CC.translateAlternateColorCodes('&', rankColor != null ? rankColor : "");
                } else {
                    String color = rankManager.getColorPreferenceSync(player);
                    return CC.translateAlternateColorCodes('&', color != null ? color : "");
                }
            }
            case "player_rank_name": {
                if (disguised && disguisedRank != null && !disguisedRank.isEmpty()) {
                    return CC.stripColor(CC.translate(disguisedRank));
                } else {
                    String realRank = rankManager.getRankSync(player);
                    return realRank != null ? CC.stripColor(CC.translate(realRank)) : "";
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
}





