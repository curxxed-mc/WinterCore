package hyp.ilfov.i.icore.Commands.Bungee;

import hyp.ilfov.i.icore.Main;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;

import java.util.*;

public class ServerManagerCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public ServerManagerCommand(Main plugin) {
        this.plugin = plugin;
        plugin.getCommand("servermanager").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /servermanager <runcmd|info|listservers|join>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "runcmd":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /servermanager runcmd <server|all> <cmd>");
                    return true;
                }
                String serverTarget = args[1];
                String command = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                plugin.getRedisManager().dispatchRemoteCommand(serverTarget, command);
                sender.sendMessage(ChatColor.GREEN + "Command sent to " + serverTarget);
                return true;

            case "info":
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /servermanager info <server>");
                    return true;
                }
                fetchInfo(sender, args[1]);
                return true;

            case "listservers":
                listServers(sender);
                return true;

            case "join":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can use this.");
                    return true;
                }
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /servermanager join <server>");
                    return true;
                }
                plugin.getRedisManager().sendPlayerToServer((Player) sender, args[1]);
                return true;

            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand.");
        }

        return true;
    }

    private void fetchInfo(CommandSender sender, String server) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            String key = "server:" + server + ":info";
            if (!jedis.exists(key)) {
                sender.sendMessage(ChatColor.RED + "No info found for server " + server);
                return;
            }
            sender.sendMessage(ChatColor.AQUA + "Info for " + server + ":");
            sender.sendMessage(ChatColor.GRAY + "TPS: " + ChatColor.GREEN + jedis.hget(key, "tps"));
            sender.sendMessage(ChatColor.GRAY + "Players: " + ChatColor.YELLOW +
                    jedis.hget(key, "players") + "/" + jedis.hget(key, "maxPlayers"));
            sender.sendMessage(ChatColor.GRAY + "Whitelisted: " + ChatColor.GOLD + jedis.hget(key, "whitelisted"));
        }
    }

    private void listServers(CommandSender sender) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            Set<String> keys = jedis.keys("server:*:heartbeat");
            if (keys.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "No servers online.");
                return;
            }
            sender.sendMessage(ChatColor.AQUA + "Online Servers:");
            for (String key : keys) {
                String name = key.split(":")[1];
                sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.YELLOW + name);
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("runcmd", "info", "listservers", "join");
        }
        return Collections.emptyList();
    }
}
