package net.curxxed.dev.wintercore.commands.network;

import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.database.redis.packet.packets.NetworkBroadcastPacket;
import net.curxxed.dev.wintercore.database.redis.service.NetworkRedisService;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@CommandInfo(
        name = "maintenance",
        aliases = {"networkmaintenance"},
        description = "Manage network maintenance mode.",
        usage = "/maintenance <on|off|status> [reason]",
        async = true,
        permission = {"wintercore.network.maintenance"}
)
public final class NetworkMaintenanceCommand extends NetworkCommand {

    private static final List<String> SUBCOMMANDS = Arrays.asList("on", "off", "status");

    public NetworkMaintenanceCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments args) {
        CommandSender sender = args.getSender();
        if (args.length() == 0) {
            sendStatus(sender);
            return;
        }

        String subCommand = args.getOptionalString(0).orElse("").toLowerCase(Locale.ENGLISH);
        if ("status".equals(subCommand)) {
            sendStatus(sender);
            return;
        }

        if ("on".equals(subCommand)) {
            String reason = joinArgs(args.getArgsList(), 1);
            if (reason.isEmpty()) {
                reason = "Maintenance";
            }
            plugin.getNRS().setMaintenance(true, reason, sender.getName(), sourceServer());
            broadcast("network-maintenance.enabled-broadcast",
                    "&cNetwork maintenance has been enabled. &7Reason: &f{reason}",
                    sender.getName(), reason);
            reply(sender, "network-maintenance.enabled", "&aNetwork maintenance enabled. Reason: &f{reason}",
                    "{reason}", reason);
            return;
        }

        if ("off".equals(subCommand)) {
            plugin.getNRS().setMaintenance(false, "Maintenance", sender.getName(), sourceServer());
            broadcast("network-maintenance.disabled-broadcast",
                    "&aNetwork maintenance has been disabled.",
                    sender.getName(), "Maintenance");
            reply(sender, "network-maintenance.disabled", "&aNetwork maintenance disabled.");
            return;
        }

        reply(sender, "general.usage", "&cUsage: {usage}", "{usage}", commandInfo.usage());
    }

    @Override
    public List<String> onTabComplete(CommandArguments args) {
        if (args.length() <= 1) {
            return completeCurrentArg(args, SUBCOMMANDS);
        }
        return Collections.emptyList();
    }

    private void sendStatus(CommandSender sender) {
        NetworkRedisService.MaintenanceState state = plugin.getNRS().getMaintenanceState();
        String status = state.isEnabled()
                ? msg("network-maintenance.status-enabled", "&cEnabled")
                : msg("network-maintenance.status-disabled", "&aDisabled");

        replyList(sender, "network-maintenance.status", Arrays.asList(
                "&bNetwork Maintenance: {status}",
                "&7Reason: &f{reason}",
                "&7Updated by: &f{actor} &7from &f{server}"
        ), "{status}", status,
                "{reason}", state.getReason(),
                "{actor}", state.getActor(),
                "{server}", state.getServerName());
    }

    private void broadcast(String path, String fallback, String sender, String reason) {
        String message = msg(path, fallback,
                "{sender}", sender,
                "{reason}", reason,
                "{server}", sourceServer());

        plugin.getRedisManager().publishAndHandleLocally(new NetworkBroadcastPacket(
                sourceServer(),
                System.currentTimeMillis(),
                sender,
                message
        ));
    }
}
