package net.curxxed.dev.wintercore.commands.misc;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import lombok.Getter;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.rank.RankManager;
import net.curxxed.dev.wintercore.disguise.DisguiseRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ListCommand extends BaseCommand {

    private final WinterCore plugin;
    @Getter
    private final RankManager rankManager;
    private final DisguiseRegistry disguiseRegistry;

    public ListCommand(WinterCore plugin, RankManager rankManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
        this.disguiseRegistry = plugin.getDisguiseRegistry();
    }

    @Command(
            name = "list",
            permission = "WinterCore.list",
            description = "List online players by rank.",
            usage = "/list",
            inGameOnly = false
    )
    public void onCommand(CommandArgs commandArgs) {
        if (!(commandArgs.getSender() instanceof Player)) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "This command is only available to players.");
            return;
        }
        Player player = (Player) commandArgs.getSender();
        List<String> sortedRanks = rankManager.getSortedRanks();
        Map<String, Integer> rankWeights = new HashMap<>();
        ConfigurationSection ranksSection = rankManager.getRanksSection();
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
            if (disguiseRegistry.isDisguised(p)) {
                disguiseRegistry.getEffectiveRank(p, effectiveRank -> {
                    playerRankMap.put(p.getUniqueId(), effectiveRank);
                    disguiseRegistry.getEffectiveColor(p, effectiveColor -> {
                        String rawColor = (effectiveColor == null || effectiveColor.isEmpty()) ? "&f" : effectiveColor;
                        String parsedColor = ChatColor.translateAlternateColorCodes('&', rawColor);
                        playerColorMap.put(p.getUniqueId(), parsedColor);
                        future.complete(null);
                    });
                });
            } else {
                rankManager.getRank(p, rank -> {
                    playerRankMap.put(p.getUniqueId(), rank);
                    rankManager.getColorPreference(rank, color -> {
                        String rawColor = (color == null || color.isEmpty()) ? "&f" : color;
                        String parsedColor = ChatColor.translateAlternateColorCodes('&', rawColor);
                        playerColorMap.put(p.getUniqueId(), parsedColor);
                        future.complete(null);
                    });
                });
            }
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
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
                assert ranksSection != null;
                String rawColor = ranksSection.getString(rank + ".name-color", "&f");
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
    }
}