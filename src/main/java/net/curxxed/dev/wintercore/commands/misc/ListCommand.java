package net.curxxed.dev.wintercore.commands.misc;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.rank.RankManager;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@CommandInfo(
        name = "list",
        aliases = {"who", "online", "players"},
        permission = "wintercore.command.list",
        description = "Displays the list of online players, sorted by rank.",
        usage = "/list",
        async = true // The command logic is asynchronous
)
public class ListCommand extends BaseCommand {

    private final RankManager rankManager;

    public ListCommand(WinterCore plugin, RankManager rankManager) {
        super(plugin);
        this.rankManager = rankManager;
    }

    @Override
    public void execute(CommandArguments args) {
        CommandSender sender = args.getSender();

        // Step 1: Get the rank configuration section from the RankManager.
        ConfigurationSection ranksSection = rankManager.getRanksSection();
        if (ranksSection == null) {
            sender.sendMessage(CC.translate("&cThe 'ranks' section is missing from ranks.yml."));
            return;
        }

        // Step 2: Load rank properties into maps for efficient lookups.
        Map<String, Integer> rankWeights = new HashMap<>();
        Map<String, String> rankColors = new HashMap<>();
        for (String rankName : ranksSection.getKeys(false)) {
            rankWeights.put(rankName.toLowerCase(), ranksSection.getInt(rankName + ".weight", 0));
            rankColors.put(rankName.toLowerCase(), ranksSection.getString(rankName + ".name-color", "&f"));
        }

        // Step 3: Asynchronously fetch all player rank names.
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        // This map will store the results of our async lookups (UUID -> Rank Name)
        Map<UUID, String> playerRankNames = new ConcurrentHashMap<>();

        // Step 4: When ALL asynchronous rank lookups are complete, proceed.
        CompletableFuture.allOf(onlinePlayers.stream()
                .map(player -> {
                    CompletableFuture<Void> future = new CompletableFuture<>();
                    // Use the correct async method from RankManager
                    rankManager.getRank(player, rankName -> {
                        playerRankNames.put(player.getUniqueId(), rankName);
                        future.complete(null); // Mark this player's lookup as complete
                    });
                    return future;
                }).toArray(CompletableFuture[]::new)).thenRun(() -> {

            // Step 5: Sort players based on their now-retrieved rank weights.
            onlinePlayers.sort((p1, p2) -> {
                String rankName1 = playerRankNames.getOrDefault(p1.getUniqueId(), "Default").toLowerCase();
                String rankName2 = playerRankNames.getOrDefault(p2.getUniqueId(), "Default").toLowerCase();

                int weight1 = rankWeights.getOrDefault(rankName1, 0);
                int weight2 = rankWeights.getOrDefault(rankName2, 0);

                if (weight1 != weight2) {
                    return Integer.compare(weight2, weight1); // Sort by weight descending
                }
                return p1.getName().compareToIgnoreCase(p2.getName()); // Then alphabetically
            });

            // Step 6: Build the final display strings.
            // Use the RankManager's own sorting logic for consistency.
            String rankDisplay = rankManager.getSortedRanks().stream()
                    .map(rankName -> CC.translate(rankColors.getOrDefault(rankName.toLowerCase(), "&f") + rankName))
                    .collect(Collectors.joining(CC.translate("&7, ")));

            String playerDisplay = onlinePlayers.stream()
                    .map(player -> {
                        String rankName = playerRankNames.getOrDefault(player.getUniqueId(), "Default").toLowerCase();
                        String color = rankColors.getOrDefault(rankName, "&f");
                        return CC.translate(color + player.getName());
                    })
                    .collect(Collectors.joining(CC.translate("&7, ")));

            String finalPlayerCount = CC.translate("&7(" + onlinePlayers.size() + "/" + Bukkit.getMaxPlayers() + ") ");

            // Step 7: Send the final messages back on the main server thread.
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(rankDisplay);
                sender.sendMessage(finalPlayerCount + playerDisplay);
            });
        });
    }
}

