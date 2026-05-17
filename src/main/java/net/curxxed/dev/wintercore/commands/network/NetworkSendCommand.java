package net.curxxed.dev.wintercore.commands.network;

import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.database.redis.packet.packets.PlayerTransferPacket;
import net.curxxed.dev.wintercore.database.redis.service.NetworkRedisService;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

@CommandInfo(
        name = "networksend",
        aliases = {"sendto", "sendserver"},
        description = "Send an online player to another server.",
        usage = "/networksend <player> <server>",
        async = true,
        permission = {"wintercore.network.send"}
)
public final class NetworkSendCommand extends NetworkCommand {

    public NetworkSendCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments args) {
        CommandSender sender = args.getSender();
        if (args.length() < 2) {
            reply(sender, "general.usage", "&cUsage: {usage}", "{usage}", commandInfo.usage());
            return;
        }

        String targetName = args.getOptionalString(0).orElse("");
        String requestedServer = joinArgs(args.getArgsList(), 1);
        NetworkRedisService.OnlinePresence presence = plugin.getNRS().getOnlinePresenceByName(targetName);
        if (presence == null) {
            reply(sender, "network-send.not-online", "&c{player} is not online on the network.",
                    "{player}", targetName);
            return;
        }

        String destination = resolveAliveServer(requestedServer);
        if (destination == null) {
            reply(sender, "network-send.server-not-online", "&cServer {server} is not online.",
                    "{server}", requestedServer);
            return;
        }

        if (destination.equalsIgnoreCase(presence.getServerName())) {
            reply(sender, "network-send.already-there", "&c{player} is already on {server}.",
                    "{player}", presence.getPlayerName(),
                    "{server}", destination);
            return;
        }

        plugin.getRedisManager().publishAndHandleLocally(new PlayerTransferPacket(
                sourceServer(),
                System.currentTimeMillis(),
                presence.getUuid(),
                presence.getPlayerName(),
                destination,
                sender.getName()
        ));

        reply(sender, "network-send.sent", "&aSending {player} to {server}.",
                "{player}", presence.getPlayerName(),
                "{server}", destination);
    }

    @Override
    public List<String> onTabComplete(CommandArguments args) {
        if (args.length() == 1) {
            return plugin.getNRS().getOnlinePlayers().values().stream()
                    .map(NetworkRedisService.OnlinePresence::getPlayerName)
                    .collect(java.util.stream.Collectors.toList());
        }
        if (args.length() == 2) {
            return aliveServers();
        }
        return Collections.emptyList();
    }
}
