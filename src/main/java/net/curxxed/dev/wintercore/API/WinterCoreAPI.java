package net.curxxed.dev.wintercore.API;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.rank.RankManager;
import net.curxxed.dev.wintercore.rank.Rank;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public class WinterCoreAPI {
    private final RankManager rankManager;
    private final WinterCore plugin;

    public WinterCoreAPI(WinterCore plugin) {
        this.plugin = plugin;
        this.rankManager = plugin.getRankManager();
    }

    /**
     * Asynchronously get the rank name of a player.
     * Returns a CompletableFuture that completes with the rank id (e.g. "Default").
     */
    public CompletableFuture<String> getRankAsync(Player player) {
        CompletableFuture<String> future = new CompletableFuture<>();
        try {
            rankManager.getRank(player, future::complete);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * Synchronous helper to get the rank name. Delegates to RankManager.getRankSync.
     * Use only when you know you're on a safe thread.
     */
    public String getRankSync(Player player) {
        return rankManager.getRankSync(player);
    }

    /**
     * Asynchronously get the name-color preference for a player (resolves rank then color).
     * Completes with a color code string (e.g. "&f").
     */
    public CompletableFuture<String> getNameColorAsync(Player player) {
        CompletableFuture<String> future = new CompletableFuture<>();
        try {
            // first get the rank asynchronously, then fetch color for that rank
            getRankAsync(player).thenAccept(rank -> {
                try {
                    rankManager.getColorPreference(rank, future::complete);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }).exceptionally(ex -> {
                future.completeExceptionally(ex);
                return null;
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * Synchronous helper to get the name-color preference. Use on main thread only.
     */
    public String getNameColorSync(Player player) {
        return rankManager.getColorPreferenceSync(player);
    }

    /**
     * Update the player's name tag color using the configured NameTagAdapter. This method
     * returns a CompletableFuture that completes when the name tag has been updated.
     */
    public CompletableFuture<Void> updateNameTagColorAsync(Player player) {
        // Ensure the actual name-tag change runs on the main server thread
        return getNameColorAsync(player).thenAccept(color -> plugin.getServer().getScheduler().runTask(plugin,
                () -> plugin.getNameTagHandler().getNameTagAdapter().setNameTag(player, color)));
    }

    /**
     * Convenience: get a Rank object for the player's current rank asynchronously.
     * Completes with a Rank instance or completes exceptionally if the rank does not exist.
     */
    public CompletableFuture<Rank> getRankObjectAsync(Player player) {
        return getRankAsync(player).thenApply(rankName -> new Rank(rankName, plugin));
    }

}
