package net.curxxed.dev.wintercore.commands.bungee;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.database.redis.packet.packets.NetworkBroadcastPacket;
import net.curxxed.dev.wintercore.database.redis.packet.packets.PlayerTransferPacket;
import net.curxxed.dev.wintercore.database.redis.packet.packets.RemoteCommandPacket;
import net.curxxed.dev.wintercore.database.redis.service.NetworkRedisService;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.utils.PluginMessageData;
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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@CommandInfo(
        name = "servermanager",
        aliases = {"sm"},
        description = "Manage servers via Redis.",
        usage = "/sm <list|players|info|find|join|send|broadcast|maintenance|runcmd>",
        async = true,
        permission = {
                "wintercore.servermanager",
                "wintercore.network.list",
                "wintercore.network.find",
                "wintercore.network.send",
                "wintercore.network.broadcast",
                "wintercore.network.maintenance"
        }
)
public class ServerManagerCommand extends BaseCommand {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "list", "players", "info", "find", "join", "send", "broadcast", "maintenance", "runcmd");
    private static final List<String> MAINTENANCE_ACTIONS = Arrays.asList("on", "off", "status");
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
                if (!requirePermission(sender, "wintercore.servermanager")) return;
                listServers(sender);
                break;

            case "players":
                if (!requirePermission(sender, "wintercore.network.list")) return;
                listPlayers(sender);
                break;

            case "info":
                if (!requirePermission(sender, "wintercore.servermanager")) return;
                if (args.length() < 2) {
                    reply(sender, "server-manager.info-usage", "&cUsage: /sm info <serverName>");
                    return;
                }
                fetchInfo(sender, args.getOptionalString(1).orElse(""));
                break;

            case "find":
                if (!requirePermission(sender, "wintercore.network.find")) return;
                findPlayer(sender, args);
                break;

            case "join":
                if (!requirePermission(sender, "wintercore.servermanager")) return;
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

            case "send":
                if (!requirePermission(sender, "wintercore.network.send")) return;
                sendPlayer(sender, args);
                break;

            case "broadcast":
                if (!requirePermission(sender, "wintercore.network.broadcast")) return;
                broadcastNetwork(sender, args);
                break;

            case "maintenance":
                if (!requirePermission(sender, "wintercore.network.maintenance")) return;
                maintenance(sender, args);
                break;

            case "runcmd":
            case "execute":
                if (!requirePermission(sender, "wintercore.servermanager")) return;
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
            if ("find".equals(subCommand) || "send".equals(subCommand)) {
                return completeCurrentArg(args, onlinePlayerNames());
            }
            if ("maintenance".equals(subCommand)) {
                return completeCurrentArg(args, MAINTENANCE_ACTIONS);
            }
        }
        if (args.length() == 3 && "send".equals(subCommand)) {
            return completeCurrentArg(args, plugin.getNetworkRedisService().getAliveServers());
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

    private void listPlayers(CommandSender sender) {
        Map<String, List<String>> grouped = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (NetworkRedisService.OnlinePresence presence : plugin.getNetworkRedisService().getOnlinePlayers().values()) {
            grouped.computeIfAbsent(presence.getServerName(), ignored -> new ArrayList<>())
                    .add(presence.getPlayerName());
        }

        if (grouped.isEmpty()) {
            reply(sender, "network-list.empty", "&cNo players are online on the network.");
            return;
        }

        int total = grouped.values().stream().mapToInt(List::size).sum();
        List<String> lines = new ArrayList<>();
        lines.add(message("network-list.header", "&bNetwork Players &7({online}):",
                "{online}", String.valueOf(total),
                "{servers}", String.valueOf(grouped.size())));

        String separator = message("network-list.separator", "&7, ");
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            List<String> players = entry.getValue();
            Collections.sort(players, String.CASE_INSENSITIVE_ORDER);
            lines.add(message("network-list.server", "&7- &b{server}&7: &f{players}",
                    "{server}", entry.getKey(),
                    "{count}", String.valueOf(players.size()),
                    "{players}", String.join(separator, players)));
        }
        replyLines(sender, lines);
    }

    private void findPlayer(CommandSender sender, CommandArguments args) {
        if (args.length() != 2) {
            reply(sender, "general.usage", "&cUsage: {usage}", "{usage}", "/sm find <player>");
            return;
        }

        String target = args.getOptionalString(1).orElse("");
        NetworkRedisService.OnlinePresence presence = plugin.getNetworkRedisService().getOnlinePresenceByName(target);
        if (presence == null) {
            reply(sender, "network-find.not-online", "&c{player} is not online on the network.",
                    "{player}", target);
            return;
        }

        reply(sender, "network-find.found", "&b{player} &7is online on &e{server}&7.",
                "{player}", presence.getPlayerName(),
                "{server}", presence.getServerName());
    }

    private void sendPlayer(CommandSender sender, CommandArguments args) {
        if (args.length() < 3) {
            reply(sender, "general.usage", "&cUsage: {usage}", "{usage}", "/sm send <player> <server>");
            return;
        }

        String targetName = args.getOptionalString(1).orElse("");
        NetworkRedisService.OnlinePresence presence = plugin.getNetworkRedisService().getOnlinePresenceByName(targetName);
        if (presence == null) {
            reply(sender, "network-send.not-online", "&c{player} is not online on the network.",
                    "{player}", targetName);
            return;
        }

        String requestedServer = joinArgs(args.getArgsList(), 2);
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
                sourceServer(), System.currentTimeMillis(), presence.getUuid(),
                presence.getPlayerName(), destination, sender.getName()));
        reply(sender, "network-send.sent", "&aSending {player} to {server}.",
                "{player}", presence.getPlayerName(),
                "{server}", destination);
    }

    private void broadcastNetwork(CommandSender sender, CommandArguments args) {
        if (args.length() < 2) {
            reply(sender, "general.usage", "&cUsage: {usage}", "{usage}", "/sm broadcast <message>");
            return;
        }

        String formatted = message("network-broadcast.format",
                "&8[&bNetwork&8] &f{message}",
                "{message}", joinArgs(args.getArgsList(), 1),
                "{server}", sourceServer(),
                "{sender}", sender.getName());
        plugin.getRedisManager().publishAndHandleLocally(new NetworkBroadcastPacket(
                sourceServer(), System.currentTimeMillis(), sender.getName(), formatted));
        reply(sender, "network-broadcast.sent", "&aNetwork broadcast sent.");
    }

    private void maintenance(CommandSender sender, CommandArguments args) {
        String action = args.length() < 2
                ? "status"
                : args.getOptionalString(1).orElse("").toLowerCase(Locale.ENGLISH);
        if ("status".equals(action)) {
            sendMaintenanceStatus(sender);
            return;
        }
        if ("on".equals(action)) {
            String reason = joinArgs(args.getArgsList(), 2);
            if (reason.isEmpty()) reason = "Maintenance";
            plugin.getNetworkRedisService().setMaintenance(true, reason, sender.getName(), sourceServer());
            broadcastMaintenance("network-maintenance.enabled-broadcast",
                    "&cNetwork maintenance has been enabled. &7Reason: &f{reason}",
                    sender.getName(), reason);
            reply(sender, "network-maintenance.enabled", "&aNetwork maintenance enabled. Reason: &f{reason}",
                    "{reason}", reason);
            return;
        }
        if ("off".equals(action)) {
            plugin.getNetworkRedisService().setMaintenance(false, "Maintenance", sender.getName(), sourceServer());
            broadcastMaintenance("network-maintenance.disabled-broadcast",
                    "&aNetwork maintenance has been disabled.", sender.getName(), "Maintenance");
            reply(sender, "network-maintenance.disabled", "&aNetwork maintenance disabled.");
            return;
        }
        reply(sender, "general.usage", "&cUsage: {usage}",
                "{usage}", "/sm maintenance <on|off|status> [reason]");
    }

    private void sendMaintenanceStatus(CommandSender sender) {
        NetworkRedisService.MaintenanceState state = plugin.getNetworkRedisService().getMaintenanceState();
        String status = state.isEnabled()
                ? message("network-maintenance.status-enabled", "&cEnabled")
                : message("network-maintenance.status-disabled", "&aDisabled");
        replyLines(sender, plugin.getMessageConfig().getList("network-maintenance.status", Arrays.asList(
                "&bNetwork Maintenance: {status}",
                "&7Reason: &f{reason}",
                "&7Updated by: &f{actor} &7from &f{server}"
        ), "{status}", status,
                "{reason}", state.getReason(),
                "{actor}", state.getActor(),
                "{server}", state.getServerName()));
    }

    private void broadcastMaintenance(String path, String fallback, String sender, String reason) {
        String text = message(path, fallback,
                "{sender}", sender,
                "{reason}", reason,
                "{server}", sourceServer());
        plugin.getRedisManager().publishAndHandleLocally(new NetworkBroadcastPacket(
                sourceServer(), System.currentTimeMillis(), sender, text));
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
        runSync(() -> {
            UUID playerId = player.getUniqueId();
            String playerName = player.getName();

            runAsync(() -> {
                try (Jedis jedis = plugin.getRedisPool().getResource()) {
                    jedis.setex("pending_switch:" + playerId, 30, "true");
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to set pending switch for " + playerName + ": " + e.getMessage());
                }
            });

            player.sendMessage(message("server-manager.joining", "&7Sending you to {server}...",
                    "{server}", serverName));

            byte[] payload = PluginMessageData.encode(
                    "Connect",
                    plugin.getConfig().getString("BungeeServers." + serverName, serverName)
            );
            player.sendPluginMessage(plugin, "BungeeCord", payload);
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

    private String sourceServer() {
        return plugin.getConfig().getString("server-name", "unknown");
    }

    private String joinArgs(List<String> args, int startIndex) {
        return startIndex >= args.size()
                ? ""
                : String.join(" ", args.subList(startIndex, args.size())).trim();
    }

    private String resolveAliveServer(String requested) {
        for (String server : plugin.getNetworkRedisService().getAliveServers()) {
            if (server.equalsIgnoreCase(requested.trim())) return server;
        }
        return null;
    }

    private List<String> onlinePlayerNames() {
        return plugin.getNetworkRedisService().getOnlinePlayers().values().stream()
                .map(NetworkRedisService.OnlinePresence::getPlayerName)
                .collect(Collectors.toList());
    }

    private boolean requirePermission(CommandSender sender, String permission) {
        if (hasAnyPermission(sender, "wintercore.servermanager", permission)) return true;
        reply(sender, "general.no-permission", "&cYou do not have permission to execute this command.");
        return false;
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
        plugin.getTasks().sync(() -> {
            for (String message : messages) {
                sender.sendMessage(CC.translate(message));
            }
        });
    }

    private String message(String path, String fallback, String... placeholders) {
        return plugin.getMessageConfig().get(path, fallback, placeholders);
    }
}
