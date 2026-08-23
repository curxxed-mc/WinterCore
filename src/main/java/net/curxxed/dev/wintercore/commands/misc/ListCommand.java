package net.curxxed.dev.wintercore.commands.misc;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
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
        description = "Displays the list of online players, sorted by rank.",
        usage = "/list",
        async = true,
        permission = {"wintercore.command.list"}
)
public class ListCommand extends BaseCommand {

    private final RankManager rankManager;

    public ListCommand(WinterCore plugin, RankManager rankManager) {
        super(plugin);
        this.rankManager = rankManager;
    }

    @Override
    public void execute(CommandArguments args) {
        runSync(() -> executeOnMainThread(args));
    }

    private void executeOnMainThread(CommandArguments args) {
        CommandSender sender = args.getSender();
        ConfigurationSection ranksSection = rankManager.getRanksSection();
        if (ranksSection == null) {
            send(sender, "list.ranks-missing", "&cThe 'ranks' section is missing from ranks.yml.");
            return;
        }
        Map<String, Integer> rankWeights = new HashMap<>();
        Map<String, String> rankColors = new HashMap<>();
        for (String rankName : ranksSection.getKeys(false)) {
            rankWeights.put(rankName.toLowerCase(), ranksSection.getInt(rankName + ".weight", 0));
            rankColors.put(rankName.toLowerCase(), ranksSection.getString(rankName + ".name-color", "&f"));
        }

        List<Player> onlinePlayers = new ArrayList<>(net.curxxed.dev.wintercore.utils.Utilities.getOnlinePlayers());
        Map<UUID, String> playerRankNames = new ConcurrentHashMap<>();
        CompletableFuture.allOf(onlinePlayers.stream()
                .map(player -> {
                    CompletableFuture<Void> future = new CompletableFuture<>();
                    rankManager.getRank(player, rankName -> {
                        playerRankNames.put(player.getUniqueId(), rankName);
                        future.complete(null);
                    });
                    return future;
                }).toArray(CompletableFuture[]::new)).thenRun(() -> runSync(() -> {

            onlinePlayers.sort((p1, p2) -> {
                String rankName1 = playerRankNames.getOrDefault(p1.getUniqueId(), "Default").toLowerCase();
                String rankName2 = playerRankNames.getOrDefault(p2.getUniqueId(), "Default").toLowerCase();

                int weight1 = rankWeights.getOrDefault(rankName1, 0);
                int weight2 = rankWeights.getOrDefault(rankName2, 0);

                if (weight1 != weight2) {
                    return Integer.compare(weight2, weight1);
                }
                return p1.getName().compareToIgnoreCase(p2.getName());
            });

            String rankDisplay = rankManager.getSortedRanks().stream()
                    .map(rankName -> CC.translate(rankColors.getOrDefault(rankName.toLowerCase(), "&f") + rankName))
                    .collect(Collectors.joining(msg("list.rank-separator", "&7, ")));

            String playerDisplay = onlinePlayers.stream()
                    .map(player -> {
                        String rankName = playerRankNames.getOrDefault(player.getUniqueId(), "Default").toLowerCase();
                        String color = rankColors.getOrDefault(rankName, "&f");
                        return CC.translate(color + player.getName());
                    })
                    .collect(Collectors.joining(msg("list.player-separator", "&7, ")));

            String finalPlayerCount = msg("list.player-count", "&7({online}/{max}) ",
                    "{online}", String.valueOf(onlinePlayers.size()),
                    "{max}", String.valueOf(Bukkit.getMaxPlayers()));

            send(sender, "list.ranks-line", "{ranks}", "{ranks}", rankDisplay);
            send(sender, "list.players-line", "{count}{players}",
                    "{count}", finalPlayerCount,
                    "{players}", playerDisplay);
        }));
    }
}

