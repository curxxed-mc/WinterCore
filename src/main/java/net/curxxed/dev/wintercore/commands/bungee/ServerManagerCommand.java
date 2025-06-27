package net.curxxed.dev.wintercore.commands.bungee;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;

import java.util.Arrays;
import java.util.Set;

public class ServerManagerCommand extends BaseCommand {

    private final WinterCore plugin;

    public ServerManagerCommand(WinterCore plugin) {
        this.plugin = plugin;
    }

    @Command(
            name = "servermanager",
            description = "Manage servers via Redis.",
            aliases = {"sm"},
            usage = "/servermanager <runcmd|info|listservers|join>",
            permission = "wintercore.servermanager",
            inGameOnly = false
    )
    public void onCommand(CommandArgs commandArgs) {
        CommandSender sender = commandArgs.getSender();
        String[] args = commandArgs.getArgs();

        if (args.length == 0) {
            sender.sendMessage(CC.translate("&eUsage: /servermanager <runcmd|info|listservers|join>"));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "runcmd":
                if (args.length < 3) {
                    sender.sendMessage(CC.translate("&cUsage: /servermanager runcmd <server|all> <cmd>"));
                    return;
                }
                String serverTarget = args[1];
                String command = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                plugin.getRedisManager().dispatchRemoteCommand(serverTarget, command);
                sender.sendMessage(CC.translate("&aCommand sent to " + serverTarget));
                break;

            case "info":
                if (args.length != 2) {
                    sender.sendMessage(CC.translate("&cUsage: /servermanager info <server>"));
                    return;
                }
                fetchInfo(sender, args[1]);
                break;

            case "listservers":
                listServers(sender);
                break;

            case "join":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(CC.translate("&cOnly players can use this."));
                    return;
                }
                if (args.length != 2) {
                    sender.sendMessage(CC.translate("&cUsage: /servermanager join <server>"));
                    return;
                }
                plugin.getRedisManager().sendPlayerToServer((Player) sender, args[1]);
                break;

            default:
                sender.sendMessage(CC.translate("&cUnknown subcommand."));
        }
    }

    private void fetchInfo(CommandSender sender, String server) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            String key = "server:" + server + ":info";
            if (!jedis.exists(key)) {
                sender.sendMessage(CC.translate("&cNo info found for server " + server));
                return;
            }
            sender.sendMessage(CC.translate("&bInfo for " + server + ":"));
            sender.sendMessage(CC.translate("&7TPS: &a" + jedis.hget(key, "tps")));
            sender.sendMessage(CC.translate("&7Players: &e" +
                    jedis.hget(key, "players") + "/" + jedis.hget(key, "maxPlayers")));
            sender.sendMessage(CC.translate("&7Whitelisted: &6" + jedis.hget(key, "whitelisted")));
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
                sender.sendMessage(CC.translate("&7- &e" + name));
            }
        }
    }
}