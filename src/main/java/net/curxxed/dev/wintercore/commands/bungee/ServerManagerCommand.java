package net.curxxed.dev.wintercore.commands.bungee;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;

import java.util.Set;
import java.util.stream.Collectors;

@CommandInfo(
        name = "servermanager",
        aliases = {"sm"},
        permission = "wintercore.servermanager",
        description = "Manage servers via Redis.",
        usage = "/sm <list|info|join|runcmd>",
        async = true // This command interacts with Redis, so it should be async.
)
public class ServerManagerCommand extends BaseCommand {

    public ServerManagerCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments args) {
        CommandSender sender = args.getSender();

        if (args.length() == 0) {
            sender.sendMessage(CC.translate("&eUsage: " + commandInfo.usage()));
            return;
        }

        String subCommand = args.getOptionalString(0).orElse("").toLowerCase();

        switch (subCommand) {
            case "list":
            case "listservers":
                listServers(sender);
                break;
            case "info":
                if (args.length() < 2) {
                    sender.sendMessage(CC.translate("&cUsage: /sm info <serverName>"));
                    return;
                }
                fetchInfo(sender, args.getOptionalString(1).get());
                break;
            case "join":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(CC.translate("&cThe 'join' subcommand can only be run by a player."));
                    return;
                }
                if (args.length() < 2) {
                    sender.sendMessage(CC.translate("&cUsage: /sm join <serverName>"));
                    return;
                }
                joinServer((Player) sender, args.getOptionalString(1).get());
                break;
            case "runcmd":
            case "execute":
                if (args.length() < 3) {
                    sender.sendMessage(CC.translate("&cUsage: /sm runcmd <serverName> <command...>"));
                    return;
                }
                String server = args.getOptionalString(1).get();
                String command = args.getArgsList().stream().skip(2).collect(Collectors.joining(" "));
                runCommand(sender, server, command);
                break;
            default:
                sender.sendMessage(CC.translate("&eUsage: " + commandInfo.usage()));
        }
    }

    private void listServers(CommandSender sender) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            Set<String> keys = jedis.keys("server:*:heartbeat");
            if (keys.isEmpty()) {
                sender.sendMessage(CC.translate("&cNo servers online."));
                return;
            }
            sender.sendMessage(CC.translate("&bOnline Servers:"));
            for (String key : keys) {
                String name = key.split(":")[1];
                sender.sendMessage(CC.translate("&7 - &a" + name));
            }
        }
    }

    private void fetchInfo(CommandSender sender, String server) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            String key = "server:" + server + ":info";
            if (!jedis.exists(key)) {
                sender.sendMessage(CC.translate("&cNo info found for server '" + server + "'."));
                return;
            }
            sender.sendMessage(CC.translate("&bInfo for " + server + ":"));
            sender.sendMessage(CC.translate(" &7TPS: &a" + jedis.hget(key, "tps")));
            sender.sendMessage(CC.translate(" &7Players: &e" +
                    jedis.hget(key, "players") + "/" + jedis.hget(key, "maxPlayers")));
            sender.sendMessage(CC.translate(" &7Whitelisted: &6" + jedis.hget(key, "whitelisted")));
        }
    }

    private void joinServer(Player player, String serverName) {
        // Mark switch pending so PlayerListener suppresses Quit message
        plugin.getRedisManager().markPendingSwitch(player.getUniqueId());

        player.sendMessage(CC.translate("&7Sending you to " + serverName + "..."));
        // BungeeCord operations must run on the main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        });
    }

    private void runCommand(CommandSender sender, String server, String command) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            // Channel format: "wintercore:<serverName>:commands"
            String channel = "wintercore:" + server + ":commands";
            jedis.publish(channel, command);
            sender.sendMessage(CC.translate("&aSent command to " + server + "."));
        }
    }
}