package curxxed.dev.icore.Placeholders;

import curxxed.dev.icore.Main;
import curxxed.dev.icore.utils.RankManager;
import lombok.Getter;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Placeholder extends PlaceholderExpansion {

    private final Main plugin;
    @Getter
    private static Placeholder instance;

    public Placeholder(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "icore";
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

        switch (identifier) {
            case "player_rank":
                String rank = rankManager.getRankSync(player); 
                String rankColor = rankManager.getColorPreferenceSync(player);  
                return rankColor + rank; 
            case "player_color":
                return rankManager.getColorPreferenceSync(player);
            case "player_prefix":
                return rankManager.getRankPrefixSync(player);
            case "player_rank_name":
                return rankManager.getRankSync(player).replaceAll("§.", "");
            case "player_rank_weight":
                return String.valueOf(plugin.getConfig().getConfigurationSection("ranks")
                        .getConfigurationSection(rankManager.getRankSync(player)).getInt("weight"));
            case "player_rank_noformat":
                String rankNoFormat = rankManager.getRankSync(player);
                String rankColorNoFormat = rankManager.getColorPreferenceSync(player);
                return rankColorNoFormat + rankNoFormat.replaceAll("(?i)§[k-or]", ""); 
            default:
                return null;
        }
    }

}
