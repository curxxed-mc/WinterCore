package net.curxxed.dev.wintercore.commands.staff;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@CommandInfo(
        name = "whosisdisguised",
        aliases = {"disguises", "rn", "listdisguises", "realname", "whois"},
        description = "Shows the real names and ranks of disguised players on the network",
        usage = "/whosisdisguised",
        async = true,
        inGameOnly = true,
        permission = {"wintercore.staff"}
)
public class WhoIsDisguisedCommand extends BaseCommand {

    public WhoIsDisguisedCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments args) {
        if (!args.isPlayer()) {
            runSync(() -> send(args.getSender(), "general.in-game-only-command", "&cThis command is for players only."));
            return;
        }

        UUID senderId = args.getPlayer().getUniqueId();
        List<TextComponent> lines = new ArrayList<>();

        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            Set<String> aliveServers = plugin.getNRS().getAliveServers();
            List<String> disguiseKeys = scanDisguiseKeys(jedis);
            if (!disguiseKeys.isEmpty()) {
                Pipeline pipeline = jedis.pipelined();
                Map<String, Response<String>> responses = new HashMap<>();
                for (String key : disguiseKeys) {
                    responses.put(key, pipeline.get(key));
                }
                pipeline.sync();

                for (Map.Entry<String, Response<String>> entry : responses.entrySet()) {
                    DisguiseSnapshot snapshot = parseSnapshot(entry.getKey(), entry.getValue().get());
                    if (snapshot == null) {
                        continue;
                    }

                    if (snapshot.serverName != null
                            && !snapshot.serverName.isEmpty()
                            && !"unknown".equalsIgnoreCase(snapshot.serverName)
                            && !aliveServers.contains(snapshot.serverName)) {
                        continue;
                    }

                    String realName = resolveRealName(snapshot.uuid, snapshot.uuidString);
                    String realRank = resolveRealRank(snapshot.uuid);
                    String realRankColor = plugin.getRankManager()
                            .getRanksSection()
                            .getString(realRank + ".name-color", "&f");
                    String shownServer = (snapshot.serverName == null || snapshot.serverName.trim().isEmpty())
                            ? "unknown"
                            : snapshot.serverName;

                    TextComponent message = new TextComponent(msg("whoisdisguised.line",
                            " &7* &f{disguise} &7-> &c{real} &7[{server}]",
                            "{disguise}", snapshot.disguiseName,
                            "{real}", realName,
                            "{server}", shownServer));
                    String hoverMessage = String.join("\n", msgList("whoisdisguised.hover", Arrays.asList(
                            "&eInformation:",
                            "&7Real Name: &f{real}",
                            "&7Real Rank: {real_rank_color}{real_rank}",
                            "&7Disguised Name: &f{disguise}",
                            "&7Disguised Rank: {disguise_rank_color}{disguise_rank}",
                            "&7Server: &b{server}"
                    ), "{real}", realName,
                            "{real_rank}", realRank,
                            "{real_rank_color}", realRankColor,
                            "{disguise}", snapshot.disguiseName,
                            "{disguise_rank}", snapshot.disguiseRank,
                            "{disguise_rank_color}", snapshot.disguiseColor,
                            "{server}", shownServer));
                    ComponentBuilder hoverText = new ComponentBuilder(hoverMessage);
                    message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText.create()));
                    lines.add(message);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to retrieve disguised players: " + e.getMessage());
            runSync(() -> {
                Player sender = Bukkit.getPlayer(senderId);
                if (sender != null && sender.isOnline()) {
                    send(sender, "whoisdisguised.redis-failed", "&cFailed to retrieve data from Redis.");
                }
            });
            return;
        }

        runSync(() -> sendResult(senderId, lines));
    }

    private void sendResult(UUID senderId, List<TextComponent> lines) {
        Player sender = Bukkit.getPlayer(senderId);
        if (sender == null || !sender.isOnline()) {
            return;
        }

        sendList(sender, "whoisdisguised.header", Arrays.asList(
                "&6&m----------------------------------------",
                "&e&lNetwork Disguised Players",
                ""
        ));

        if (lines.isEmpty()) {
            send(sender, "whoisdisguised.none", "&cThere are no disguised players online.");
        } else {
            for (TextComponent line : lines) {
                sender.spigot().sendMessage(line);
            }
        }

        send(sender, "whoisdisguised.footer", "&6&m----------------------------------------");
    }

    private DisguiseSnapshot parseSnapshot(String key, String disguiseJson) {
        if (key == null || !key.startsWith("player:disguise:")) {
            return null;
        }
        if (disguiseJson == null || disguiseJson.isEmpty()) {
            return null;
        }

        String uuidString = key.substring("player:disguise:".length());
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidString);
        } catch (IllegalArgumentException ex) {
            return null;
        }

        String disguiseName = "Unknown";
        String disguiseRank = "Default";
        String disguiseColor = "&f";
        String serverName = "unknown";

        try {
            JsonObject object = new JsonParser().parse(disguiseJson).getAsJsonObject();
            if (object.has("name")) {
                disguiseName = object.get("name").getAsString();
            }
            if (object.has("rank")) {
                disguiseRank = object.get("rank").getAsString();
            }
            if (object.has("color")) {
                disguiseColor = object.get("color").getAsString();
            }
            if (object.has("server")) {
                serverName = object.get("server").getAsString();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error parsing disguise JSON for " + uuidString);
        }

        return new DisguiseSnapshot(uuid, uuidString, disguiseName, disguiseRank, disguiseColor, serverName);
    }

    private String resolveRealName(UUID uuid, String uuidString) {
        try {
            CompletableFuture<String> nameFuture = new CompletableFuture<>();
            plugin.getDatabaseManager().getIdentityService().getPlayerName(uuid, nameFuture::complete);
            String realName = nameFuture.get(2, TimeUnit.SECONDS);
            if (realName != null && !realName.trim().isEmpty()) {
                return realName;
            }
        } catch (Exception ignored) {
        }

        String offlineName = resolveOfflineNameOnMainThread(uuid);
        if (offlineName != null && !offlineName.trim().isEmpty()) {
            return offlineName;
        }

        return "Unknown (" + uuidString.substring(0, 8) + ")";
    }

    private String resolveOfflineNameOnMainThread(UUID uuid) {
        CompletableFuture<String> future = new CompletableFuture<>();
        runSync(() -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            future.complete(offlinePlayer != null ? offlinePlayer.getName() : null);
        });

        try {
            return future.get(2, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveRealRank(UUID uuid) {
        try {
            CompletableFuture<String> rankFuture = new CompletableFuture<>();
            plugin.getRankManager().getRank(uuid, rankFuture::complete);
            String rank = rankFuture.get(2, TimeUnit.SECONDS);
            if (rank != null && !rank.trim().isEmpty()) {
                return rank;
            }
        } catch (Exception ignored) {
        }
        return "Default";
    }

    private List<String> scanDisguiseKeys(Jedis jedis) {
        List<String> keys = new ArrayList<>();
        String cursor = "0";
        ScanParams params = new ScanParams().match("player:disguise:*").count(200);
        do {
            ScanResult<String> result = jedis.scan(cursor, params);
            keys.addAll(result.getResult());
            cursor = result.getCursor();
        } while (!"0".equals(cursor));
        return keys;
    }

    private static final class DisguiseSnapshot {
        private final UUID uuid;
        private final String uuidString;
        private final String disguiseName;
        private final String disguiseRank;
        private final String disguiseColor;
        private final String serverName;

        private DisguiseSnapshot(UUID uuid, String uuidString, String disguiseName,
                                 String disguiseRank, String disguiseColor, String serverName) {
            this.uuid = uuid;
            this.uuidString = uuidString;
            this.disguiseName = disguiseName;
            this.disguiseRank = disguiseRank;
            this.disguiseColor = disguiseColor;
            this.serverName = serverName;
        }
    }
}
