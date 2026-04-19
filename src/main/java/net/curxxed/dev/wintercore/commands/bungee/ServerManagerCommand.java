package net.curxxed.dev.wintercore.commands.bungee;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.database.redis.packet.packets.RemoteCommandPacket;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CommandInfo(
        name = "servermanager",
        aliases = {"sm"},
        permission = "wintercore.servermanager",
        description = "Manage servers via Redis.",
        usage = "/sm <list|info|join|runcmd>",
        async = true
)
public class ServerManagerCommand extends BaseCommand {

    public ServerManagerCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments args) {
        CommandSender sender = args.getSender();

        if (args.length() == 0) {
            reply(sender, "&eUsage: " + commandInfo.usage());
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
                    reply(sender, "&cUsage: /sm info <serverName>");
                    return;
                }
                fetchInfo(sender, args.getOptionalString(1).orElse(""));
                break;

            case "join":
                if (!(sender instanceof Player)) {
                    reply(sender, "&cThe 'join' subcommand can only be run by a player.");
                    return;
                }
                if (args.length() < 2) {
                    reply(sender, "&cUsage: /sm join <serverName>");
                    return;
                }
                joinServer((Player) sender, args.getOptionalString(1).orElse(""));
                break;

            case "runcmd":
            case "execute":
                if (args.length() < 3) {
                    reply(sender, "&cUsage: /sm runcmd <serverName> <command...>");
                    return;
                }
                String server = args.getOptionalString(1).orElse("");
                String command = args.getArgsList().stream().skip(2).collect(Collectors.joining(" "));
                runCommand(sender, server, command);
                break;

            default:
                reply(sender, "&eUsage: " + commandInfo.usage());
        }
    }

    private void listServers(CommandSender sender) {
        List<String> servers = new ArrayList<>();

        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            String cursor = ScanParams.SCAN_POINTER_START;
            ScanParams params = new ScanParams()
                    .match("server:*:heartbeat")
                    .count(100);

            do {
                ScanResult<String> result = jedis.scan(cursor, params);
                for (String key : result.getResult()) {
                    String[] parts = key.split(":");
                    if (parts.length >= 3) {
                        servers.add(parts[1]);
                    }
                }
                cursor = result.getCursor();
            } while (!ScanParams.SCAN_POINTER_START.equals(cursor));

            if (servers.isEmpty()) {
                reply(sender, "&cNo servers online.");
                return;
            }

            Set<String> uniqueServers = new LinkedHashSet<>(servers);
            List<String> lines = new ArrayList<>();
            lines.add("&bOnline Servers:");
            for (String name : uniqueServers) {
                lines.add("&7 - &a" + name);
            }
            replyLines(sender, lines);
        } catch (Exception e) {
            reply(sender, "&cFailed to list servers.");
            plugin.getLogger().warning("Failed to list servers: " + e.getMessage());
        }
    }

    private void fetchInfo(CommandSender sender, String server) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            String key = "server:" + server + ":info";
            if (!jedis.exists(key)) {
                reply(sender, "&cNo info found for server '" + server + "'.");
                return;
            }

            List<String> lines = new ArrayList<>();
            lines.add("&bInfo for " + server + ":");
            lines.add(" &7TPS: &a" + jedis.hget(key, "tps"));
            lines.add(" &7Players: &e" + jedis.hget(key, "players") + "/" + jedis.hget(key, "maxPlayers"));
            lines.add(" &7Whitelisted: &6" + jedis.hget(key, "whitelisted"));
            replyLines(sender, lines);
        } catch (Exception e) {
            reply(sender, "&cFailed to fetch server info.");
            plugin.getLogger().warning("Failed to fetch info for " + server + ": " + e.getMessage());
        }
    }

    private void joinServer(Player player, String serverName) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            jedis.setex("pending_switch:" + player.getUniqueId(), 30, "true");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to set pending switch for " + player.getName() + ": " + e.getMessage());
        }

        reply(player, "&7Sending you to " + serverName + "...");

        Bukkit.getScheduler().runTask(plugin, () -> {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        });
    }

    private void runCommand(CommandSender sender, String server, String command) {
        String sourceServer = plugin.getConfig().getString("server-name", "Unknown");
        RemoteCommandPacket packet = new RemoteCommandPacket(sourceServer, System.currentTimeMillis(), server, command);

        plugin.getRedisManager().publish(packet);
        reply(sender, "&aSent command to " + server + ".");
    }

    private void reply(CommandSender sender, String message) {
        replyLines(sender, java.util.Collections.singletonList(message));
    }

    private void replyLines(CommandSender sender, List<String> messages) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (String message : messages) {
                sender.sendMessage(CC.translate(message));
            }
        });
    }
}