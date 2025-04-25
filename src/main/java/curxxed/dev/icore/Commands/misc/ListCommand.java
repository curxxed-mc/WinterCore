package curxxed.dev.icore.Commands.misc;

import curxxed.dev.icore.Main;
import curxxed.dev.icore.utils.RankManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ListCommand implements CommandExecutor {

    private final Main plugin;
    @Getter
    private final RankManager rankManager;

    public ListCommand(Main plugin, RankManager rankManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command is only available to players.");
            return true;
        }

        Player player = (Player) sender;

        List<String> sortedRanks = rankManager.getSortedRanks();

        Map<String, Integer> rankWeights = new HashMap<>();
        ConfigurationSection ranksSection = plugin.getConfig().getConfigurationSection("ranks");
        if (ranksSection != null) {
            for (String rank : ranksSection.getKeys(false)) {
                int weight = ranksSection.getInt(rank + ".weight", 1);
                rankWeights.put(rank, weight);
            }
        }

        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        Map<UUID, String> playerRankMap = new HashMap<>();
        Map<UUID, String> playerColorMap = new HashMap<>();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Player p : onlinePlayers) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            rankManager.getRank(p, rank -> {
                playerRankMap.put(p.getUniqueId(), rank);
                rankManager.getColorPreference(rank, color -> {
                    String rawColor = (color == null || color.isEmpty()) ? "&f" : color;
                    String parsedColor = ChatColor.translateAlternateColorCodes('&', rawColor);
                    playerColorMap.put(p.getUniqueId(), parsedColor);
                    future.complete(null);
                });
            });
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
            // Sortăm playerii
            onlinePlayers.sort((p1, p2) -> {
                String r1 = playerRankMap.getOrDefault(p1.getUniqueId(), "Default");
                String r2 = playerRankMap.getOrDefault(p2.getUniqueId(), "Default");

                int w1 = rankWeights.getOrDefault(r1, 1);
                int w2 = rankWeights.getOrDefault(r2, 1);

                if (w1 != w2) return Integer.compare(w2, w1);
                return p1.getName().compareToIgnoreCase(p2.getName());
            });

            StringBuilder rankDisplay = new StringBuilder(ChatColor.GRAY + "Ranks: ");
            for (String rank : sortedRanks) {
                String rawColor = plugin.getConfig().getString("ranks." + rank + ".name-color", "&f");
                String rankColor = ChatColor.translateAlternateColorCodes('&', rawColor);
                rankDisplay.append(rankColor).append(rank).append(ChatColor.GRAY).append(", ");
            }
            if (rankDisplay.toString().endsWith(", ")) {
                rankDisplay.setLength(rankDisplay.length() - 2);
            }

            StringBuilder playerDisplay = new StringBuilder(ChatColor.GRAY + "(" + onlinePlayers.size() + "/" + Bukkit.getMaxPlayers() + "): ");
            for (Player p : onlinePlayers) {
                String color = playerColorMap.getOrDefault(p.getUniqueId(), ChatColor.WHITE.toString());
                playerDisplay.append(color).append(p.getName()).append(ChatColor.GRAY).append(", ");
            }
            if (playerDisplay.toString().endsWith(", ")) {
                playerDisplay.setLength(playerDisplay.length() - 2);
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(rankDisplay.toString());
                player.sendMessage(playerDisplay.toString());
            });
        });

        return true;
    }
}
