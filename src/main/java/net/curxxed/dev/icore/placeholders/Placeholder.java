package net.curxxed.dev.icore.placeholders;

import net.curxxed.dev.icore.rank.RankChangeEvent;
import net.curxxed.dev.icore.plugin.iCore;
import net.curxxed.dev.icore.rank.RankManager;
import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class Placeholder extends PlaceholderExpansion implements Listener {

    private final iCore plugin;

    @Getter
    private static Placeholder instance;

    public Placeholder(iCore plugin) {
        this.plugin = plugin;
        instance = this;
        if (!this.register()) {
            plugin.getLogger().warning("Could not register the iCore expansion.");
        }
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

        switch (identifier.toLowerCase()) {
            case "player_rank":
                String Rank = rankManager.getRankSync(player);
                String rankColor = rankManager.getColorPreferenceSync(player);
                return rankColor + Rank;
            case "player_color":
                return rankManager.getColorPreferenceSync(player);
            case "player_rank_name":
                return rankManager.getRankSync(player).replaceAll("§.", "");
            case "player_rank_weight":
                String rank = rankManager.getRankSync(player);
                return String.valueOf(plugin.getRankManager().getRanksSection()
                        .getConfigurationSection(rank)
                        .getInt("weight", 0));
            default:
                return null;
        }
    }

    @EventHandler
    public void onRankChange(RankChangeEvent event) {
        Player player = event.getPlayer();
        String newRank = event.getNewRank();
        RankManager rm = RankManager.getInstance();

        // Update the rank in the cache
        rm.cachePlayerRank(player, newRank);

        // Refresh player display
        rm.refreshPlayerDisplay(player);

        // Only run PlaceholderAPI code if it's enabled
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                PlaceholderAPI.setPlaceholders(player, "%icore_player_rank% %icore_player_color% %icore_player_rank_name% %icore_player_rank_weight%");
            });
        }
    }
}
