package net.curxxed.dev.wintercore.commands.network;

import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.database.redis.packet.packets.NetworkBroadcastPacket;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.command.CommandSender;

@CommandInfo(
        name = "networkbroadcast",
        aliases = {"nbc", "gbc", "alert"},
        description = "Broadcast a message to every server.",
        usage = "/networkbroadcast <message>",
        permission = {"wintercore.network.broadcast"}
)
public final class NetworkBroadcastCommand extends NetworkCommand {

    public NetworkBroadcastCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments args) {
        CommandSender sender = args.getSender();
        if (args.length() == 0) {
            sendUsage(sender);
            return;
        }

        String rawMessage = joinArgs(args.getArgsList(), 0);
        String formatted = msg("network-broadcast.format",
                "&8[&bNetwork&8] &f{message}",
                "{message}", rawMessage,
                "{server}", sourceServer(),
                "{sender}", sender.getName());

        plugin.getRedisManager().publishAndHandleLocally(new NetworkBroadcastPacket(
                sourceServer(),
                System.currentTimeMillis(),
                sender.getName(),
                formatted
        ));

        send(sender, "network-broadcast.sent", "&aNetwork broadcast sent.");
    }
}
