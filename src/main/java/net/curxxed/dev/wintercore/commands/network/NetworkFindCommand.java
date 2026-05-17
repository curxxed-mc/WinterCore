package net.curxxed.dev.wintercore.commands.network;

import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.database.redis.service.NetworkRedisService;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@CommandInfo(
        name = "find",
        aliases = {"whereis"},
        description = "Find which server a player is on.",
        usage = "/find <player>",
        async = true,
        permission = {"wintercore.network.find"}
)
public final class NetworkFindCommand extends NetworkCommand {

    public NetworkFindCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments args) {
        CommandSender sender = args.getSender();
        if (args.length() != 1) {
            reply(sender, "general.usage", "&cUsage: {usage}", "{usage}", commandInfo.usage());
            return;
        }

        String target = args.getOptionalString(0).orElse("");
        NetworkRedisService.OnlinePresence presence = plugin.getNRS().getOnlinePresenceByName(target);
        if (presence == null) {
            reply(sender, "network-find.not-online", "&c{player} is not online on the network.",
                    "{player}", target);
            return;
        }

        reply(sender, "network-find.found", "&b{player} &7is online on &e{server}&7.",
                "{player}", presence.getPlayerName(),
                "{server}", presence.getServerName());
    }

    @Override
    public List<String> onTabComplete(CommandArguments args) {
        if (args.length() > 1) {
            return Collections.emptyList();
        }

        return plugin.getNRS().getOnlinePlayers().values().stream()
                .map(NetworkRedisService.OnlinePresence::getPlayerName)
                .collect(Collectors.toList());
    }
}
