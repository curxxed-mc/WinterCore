package net.curxxed.dev.wintercore.commands.network;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

abstract class NetworkCommand extends BaseCommand {

    protected NetworkCommand(WinterCore plugin) {
        super(plugin);
    }

    protected String sourceServer() {
        return plugin.getConfig().getString("server-name", "unknown");
    }

    protected String joinArgs(List<String> args, int startIndex) {
        if (args == null || startIndex >= args.size()) {
            return "";
        }
        return String.join(" ", args.subList(startIndex, args.size())).trim();
    }

    protected void reply(CommandSender sender, String path, String fallback, String... placeholders) {
        runSync(() -> send(sender, path, fallback, placeholders));
    }

    protected void replyList(CommandSender sender, String path, List<String> fallback, String... placeholders) {
        runSync(() -> sendList(sender, path, fallback, placeholders));
    }

    protected String resolveAliveServer(String requested) {
        if (requested == null || requested.trim().isEmpty()) {
            return null;
        }

        String normalized = requested.trim();
        Set<String> alive = plugin.getNRS().getAliveServers();
        for (String server : alive) {
            if (server.equalsIgnoreCase(normalized)) {
                return server;
            }
        }

        return null;
    }

    protected List<String> aliveServers() {
        return new ArrayList<>(plugin.getNRS().getAliveServers());
    }
}
