package net.curxxed.dev.wintercore.commands.network;

import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.database.redis.service.NetworkRedisService;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@CommandInfo(
        name = "networklist",
        aliases = {"glist", "nlist"},
        description = "List online players across the network.",
        usage = "/networklist",
        async = true,
        permission = {"wintercore.network.list"}
)
public final class NetworkListCommand extends NetworkCommand {

    public NetworkListCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments args) {
        CommandSender sender = args.getSender();
        Map<String, List<String>> grouped = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (NetworkRedisService.OnlinePresence presence : plugin.getNRS().getOnlinePlayers().values()) {
            grouped.computeIfAbsent(presence.getServerName(), ignored -> new ArrayList<>())
                    .add(presence.getPlayerName());
        }

        if (grouped.isEmpty()) {
            reply(sender, "network-list.empty", "&cNo players are online on the network.");
            return;
        }

        int total = grouped.values().stream().mapToInt(List::size).sum();
        List<String> lines = new ArrayList<>();
        lines.add(msg("network-list.header", "&bNetwork Players &7({online}):",
                "{online}", String.valueOf(total),
                "{servers}", String.valueOf(grouped.size())));

        String separator = msg("network-list.separator", "&7, ");
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            List<String> players = entry.getValue();
            Collections.sort(players, String.CASE_INSENSITIVE_ORDER);
            lines.add(msg("network-list.server", "&7- &b{server}&7: &f{players}",
                    "{server}", entry.getKey(),
                    "{count}", String.valueOf(players.size()),
                    "{players}", String.join(separator, players)));
        }

        runSync(() -> {
            for (String line : lines) {
                sender.sendMessage(line);
            }
        });
    }
}
