package hyp.ilfov.i.icore.Commands.misc;

import hyp.ilfov.i.icore.Main;
import hyp.ilfov.i.icore.utils.RankManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ListCommand implements CommandExecutor {
    private final Main plugin;
    private final RankManager rankManager;

    public ListCommand(Main plugin, RankManager rankManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command is only available to players.");
            return false;
        }

        Player player = (Player) sender;

        // Fetch available ranks dynamically
        List<String> ranks = rankManager.getAvailableRanks();

        // Prepare rank priority map (weight) dynamically
        Map<String, Integer> rankWeights = new HashMap<>();
        ConfigurationSection ranksConfig = plugin.getConfig().getConfigurationSection("ranks");
        if (ranksConfig != null) {
            for (String rank : ranksConfig.getKeys(false)) {
                rankWeights.put(rank, ranksConfig.getInt(rank + ".weight", 1)); // Default weight is 1
            }
        }

        // Sort online players asynchronously based on rank weight and name
        List<Player> sortedPlayers = new ArrayList<>(Bukkit.getOnlinePlayers());

        List<CompletableFuture<Void>> rankFutures = new ArrayList<>();
        Map<Player, String> playerRanks = new HashMap<>();
        Map<Player, String> playerColors = new HashMap<>();

        // Fetch ranks and color preferences asynchronously
        for (Player p : sortedPlayers) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            rankManager.getRank(p, rank -> {
                playerRanks.put(p, rank);
                // Fetch rank color based on the player's rank name
                rankManager.getColorPreference(rank, color -> {
                    playerColors.put(p, ChatColor.translateAlternateColorCodes('&', color));
                    future.complete(null);
                });
            });
            rankFutures.add(future);
        }

        // When all data is fetched, continue processing
        CompletableFuture.allOf(rankFutures.toArray(new CompletableFuture[0])).thenRun(() -> {
            // Sort players by rank weight and name
            sortedPlayers.sort((p1, p2) -> {
                String rank1 = playerRanks.getOrDefault(p1, "Default");
                String rank2 = playerRanks.getOrDefault(p2, "Default");
                int weight1 = rankWeights.getOrDefault(rank1, 1);
                int weight2 = rankWeights.getOrDefault(rank2, 1);

                if (weight1 != weight2) {
                    return Integer.compare(weight2, weight1);
                }
                return p1.getName().compareToIgnoreCase(p2.getName());
            });

            // Build the rank list message
            StringBuilder rankList = new StringBuilder();
            List<CompletableFuture<Void>> rankColorFutures = new ArrayList<>();

            for (String rank : ranks) {
                CompletableFuture<Void> future = new CompletableFuture<>();
                rankManager.getColorPreference(rank, color -> {
                    String rankColor = ChatColor.translateAlternateColorCodes('&', color);
                    rankList.append(rankColor).append(rank).append(ChatColor.RESET).append(", ");
                    future.complete(null);
                });
                rankColorFutures.add(future);
            }

            // Wait for rank color preferences to finish
            CompletableFuture.allOf(rankColorFutures.toArray(new CompletableFuture[0])).thenRun(() -> {
                if (rankList.length() > 0) {
                    rankList.setLength(rankList.length() - 2);
                }

                // Build the sorted player list message
                StringBuilder playerList = new StringBuilder();
                for (Player sortedPlayer : sortedPlayers) {
                    String rankColor = playerColors.getOrDefault(sortedPlayer, ChatColor.WHITE.toString());
                    playerList.append(rankColor).append(sortedPlayer.getName()).append(ChatColor.RESET).append(", ");
                }

                if (playerList.length() > 0) {
                    playerList.setLength(playerList.length() - 2);
                }

                // Send messages back to the player on the main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.GRAY + "Ranks: " + rankList);
                    player.sendMessage(ChatColor.GRAY + "(" + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers() + "): " + playerList);
                });
            });
        });

        return true;
    }
}
