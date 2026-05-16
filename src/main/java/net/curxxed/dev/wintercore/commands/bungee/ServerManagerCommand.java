package net.curxxed.dev.wintercore.commands.bungee;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.database.redis.packet.packets.RemoteCommandPacket;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@CommandInfo(
        name = "servermanager",
        aliases = {"sm"},
        description = "Manage servers via Redis.",
        usage = "/sm <list|info|join|runcmd>",
        async = true,
        permission = {"wintercore.servermanager"}
)
public class ServerManagerCommand extends BaseCommand {

    private static final List<String> SUBCOMMANDS = Arrays.asList("list", "listservers", "info", "join", "runcmd", "execute");
    private static final long SERVER_CACHE_TTL_MILLIS = 5_000L;
    private final AtomicBoolean serverCacheRefreshing = new AtomicBoolean(false);
    private volatile List<String> cachedServers = Collections.emptyList();
    private volatile long serverCacheExpiresAt = 0L;

    public ServerManagerCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments args) {
        CommandSender sender = args.getSender();

        if (args.length() == 0) {
            reply(sender, "server-manager.usage", "&eUsage: {usage}", "{usage}", commandInfo.usage());
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
                    reply(sender, "server-manager.info-usage", "&cUsage: /sm info <serverName>");
                    return;
                }
                fetchInfo(sender, args.getOptionalString(1).orElse(""));
                break;

            case "join":
                if (!(sender instanceof Player)) {
                    reply(sender, "server-manager.join-player-only", "&cThe 'join' subcommand can only be run by a player.");
                    return;
                }
                if (args.length() < 2) {
                    reply(sender, "server-manager.join-usage", "&cUsage: /sm join <serverName>");
                    return;
                }
                joinServer((Player) sender, args.getOptionalString(1).orElse(""));
                break;

            case "runcmd":
            case "execute":
                if (args.length() < 3) {
                    reply(sender, "server-manager.runcmd-usage", "&cUsage: /sm runcmd <serverName> <command...>");
                    return;
                }
                String server = args.getOptionalString(1).orElse("");
                String command = args.getArgsList().stream().skip(2).collect(Collectors.joining(" "));
                runCommand(sender, server, command);
                break;

            default:
                reply(sender, "server-manager.usage", "&eUsage: {usage}", "{usage}", commandInfo.usage());
        }
    }

    @Override
    public List<String> onTabComplete(CommandArguments args) {
        if (args.length() <= 1) {
            return completeCurrentArg(args, SUBCOMMANDS);
        }

        String subCommand = args.getOptionalString(0).orElse("").toLowerCase(Locale.ENGLISH);
        if (args.length() == 2) {
            if ("info".equals(subCommand) || "join".equals(subCommand)
                    || "runcmd".equals(subCommand) || "execute".equals(subCommand)) {
                return completeCurrentArg(args, getCachedServerSuggestions());
            }
        }

        return Collections.emptyList();
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
                reply(sender, "server-manager.no-servers", "&cNo servers online.");
                return;
            }

            Set<String> uniqueServers = new LinkedHashSet<>(servers);
            List<String> lines = new ArrayList<>();
            lines.add(message("server-manager.list-header", "&bOnline Servers:"));
            for (String name : uniqueServers) {
                lines.add(message("server-manager.list-entry", "&7 - &a{server}", "{server}", name));
            }
            replyLines(sender, lines);
        } catch (Exception e) {
            reply(sender, "server-manager.list-failed", "&cFailed to list servers.");
            plugin.getLogger().warning("Failed to list servers: " + e.getMessage());
        }
    }

    private void fetchInfo(CommandSender sender, String server) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            String resolvedServer = resolveServerName(jedis, server);
            if (resolvedServer == null) {
                reply(sender, "server-manager.no-match", "&cNo online server matched '{server}'.",
                        "{server}", server);
                return;
            }

            String key = "server:" + resolvedServer + ":info";
            if (!jedis.exists(key)) {
                if (!jedis.exists("server:" + resolvedServer + ":heartbeat")) {
                    reply(sender, "server-manager.no-info", "&cNo info found for server '{server}'.",
                            "{server}", resolvedServer);
                    return;
                }

                List<String> pending = new ArrayList<>();
                pending.addAll(plugin.getMessageConfig().getList("server-manager.info-pending", Arrays.asList(
                        "&bInfo for {server}:",
                        " &7Status: &aOnline",
                        " &7Details: &ePending heartbeat data..."
                ), "{server}", resolvedServer));
                replyLines(sender, pending);
                return;
            }

            List<String> lines = new ArrayList<>();
            lines.addAll(plugin.getMessageConfig().getList("server-manager.info", Arrays.asList(
                    "&bInfo for {server}:",
                    " &7TPS: &a{tps}",
                    " &7Players: &e{players}/{max_players}",
                    " &7Whitelisted: &6{whitelisted}"
            ), "{server}", resolvedServer,
                    "{tps}", String.valueOf(jedis.hget(key, "tps")),
                    "{players}", String.valueOf(jedis.hget(key, "players")),
                    "{max_players}", String.valueOf(jedis.hget(key, "maxPlayers")),
                    "{whitelisted}", String.valueOf(jedis.hget(key, "whitelisted"))));
            replyLines(sender, lines);
        } catch (Exception e) {
            reply(sender, "server-manager.info-failed", "&cFailed to fetch server info.");
            plugin.getLogger().warning("Failed to fetch info for " + server + ": " + e.getMessage());
        }
    }

    private void joinServer(Player player, String serverName) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            jedis.setex("pending_switch:" + player.getUniqueId(), 30, "true");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to set pending switch for " + player.getName() + ": " + e.getMessage());
        }

        reply(player, "server-manager.joining", "&7Sending you to {server}...",
                "{server}", serverName);

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
        reply(sender, "server-manager.command-sent", "&aSent command to {server}.",
                "{server}", server);
    }

    private String resolveServerName(Jedis jedis, String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        String requested = input.trim();
        String exactHeartbeatKey = "server:" + requested + ":heartbeat";
        if (jedis.exists(exactHeartbeatKey)) {
            return requested;
        }

        String cursor = ScanParams.SCAN_POINTER_START;
        ScanParams params = new ScanParams().match("server:*:heartbeat").count(100);
        do {
            ScanResult<String> result = jedis.scan(cursor, params);
            for (String key : result.getResult()) {
                String[] parts = key.split(":");
                if (parts.length >= 3 && parts[1].equalsIgnoreCase(requested)) {
                    return parts[1];
                }
            }
            cursor = result.getCursor();
        } while (!ScanParams.SCAN_POINTER_START.equals(cursor));

        return null;
    }

    private List<String> getCachedServerSuggestions() {
        if (System.currentTimeMillis() > serverCacheExpiresAt) {
            refreshServerCacheAsync();
        }
        return cachedServers;
    }

    private void refreshServerCacheAsync() {
        if (!serverCacheRefreshing.compareAndSet(false, true)) {
            return;
        }

        runAsync(() -> {
            try (Jedis jedis = plugin.getRedisPool().getResource()) {
                List<String> discovered = new ArrayList<>();
                String cursor = ScanParams.SCAN_POINTER_START;
                ScanParams params = new ScanParams().match("server:*:heartbeat").count(100);
                do {
                    ScanResult<String> result = jedis.scan(cursor, params);
                    for (String key : result.getResult()) {
                        String[] parts = key.split(":");
                        if (parts.length >= 3) {
                            discovered.add(parts[1]);
                        }
                    }
                    cursor = result.getCursor();
                } while (!ScanParams.SCAN_POINTER_START.equals(cursor));

                Set<String> unique = new LinkedHashSet<>(discovered);
                cachedServers = new ArrayList<>(unique);
                serverCacheExpiresAt = System.currentTimeMillis() + SERVER_CACHE_TTL_MILLIS;
            } catch (Exception ignored) {
            } finally {
                serverCacheRefreshing.set(false);
            }
        });
    }

    private void reply(CommandSender sender, String path, String fallback, String... placeholders) {
        replyLines(sender, java.util.Collections.singletonList(message(path, fallback, placeholders)));
    }

    private void replyLines(CommandSender sender, List<String> messages) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (String message : messages) {
                sender.sendMessage(CC.translate(message));
            }
        });
    }

    private String message(String path, String fallback, String... placeholders) {
        return plugin.getMessageConfig().get(path, fallback, placeholders);
    }
}
