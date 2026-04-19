package net.curxxed.dev.wintercore.commands.staff;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@CommandInfo(
        name = "whosisdisguised",
        aliases = {"disguises", "rn", "listdisguises", "realname", "whois"},
        permission = "wintercore.staff",
        description = "Shows the real names and ranks of disguised players on the network",
        usage = "/whosisdisguised",
        async = true,
        inGameOnly = true
)
public class WhoIsDisguisedCommand extends BaseCommand {

    public WhoIsDisguisedCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments args) {
        if (!args.isPlayer()) {
            args.getSender().sendMessage(CC.translate("&cThis command is for players only."));
            return;
        }
        Player sender = args.getPlayer();

        sender.sendMessage(CC.translate("&6&m----------------------------------------"));
        sender.sendMessage(CC.translate("&e&lNetwork Disguised Players"));
        sender.sendMessage("");

        int count = 0;

        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            // Get all keys representing servers' player lists
            Set<String> serverKeys = jedis.keys("server:*:players");

            for (String serverKey : serverKeys) {
                // Key format: server:{serverName}:players
                String[] parts = serverKey.split(":");
                if (parts.length < 3) continue;
                String serverName = parts[1];

                Set<String> uuids = jedis.smembers(serverKey);
                if (uuids == null || uuids.isEmpty()) continue;

                // Use a pipeline to fetch disguise data for all players on this server efficiently
                Pipeline p = jedis.pipelined();
                Map<String, Response<String>> responses = new HashMap<>();
                for (String uuidStr : uuids) {
                    responses.put(uuidStr, p.get("disguise:" + uuidStr));
                }
                p.sync();

                for (Map.Entry<String, Response<String>> entry : responses.entrySet()) {
                    String disguiseJson = entry.getValue().get();

                    // If JSON is present, the player is disguised
                    if (disguiseJson != null) {
                        count++;
                        String uuidStr = entry.getKey();
                        UUID uuid;
                        try {
                            uuid = UUID.fromString(uuidStr);
                        } catch (IllegalArgumentException e) {
                            continue;
                        }

                        // Parse Disguise Info from Redis JSON
                        String disguiseName = "Unknown";
                        String disguiseRank = "Default";
                        String disguiseColor = "&f";

                        try {
                            JsonObject obj = new JsonParser().parse(disguiseJson).getAsJsonObject();
                            if (obj.has("name")) disguiseName = obj.get("name").getAsString();
                            if (obj.has("rank")) disguiseRank = obj.get("rank").getAsString();
                            if (obj.has("color")) disguiseColor = obj.get("color").getAsString();
                        } catch (Exception e) {
                            plugin.getLogger().warning("Error parsing disguise JSON for " + uuidStr);
                        }

                        CompletableFuture<String> nameFuture = new CompletableFuture<>();
                        plugin.getDatabaseManager().getIdentityService().getPlayerName(uuid, nameFuture::complete);
                        String realName = nameFuture.get(2, TimeUnit.SECONDS);

                        if (realName == null) {
                            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                            if (offlinePlayer != null) {
                                realName = offlinePlayer.getName();
                            }
                        }

                        if (realName == null) {
                            realName = "Unknown (" + uuidStr.substring(0, 8) + ")";
                        }

                        String realRank = "Default";
                        try {
                            CompletableFuture<String> rankFuture = new CompletableFuture<>();
                            plugin.getRankManager().getRank(uuid, rankFuture::complete);
                            realRank = rankFuture.get(2, TimeUnit.SECONDS);
                        } catch (Exception ignored) {
                        }

                        // Resolve Real Rank Color
                        String realRankColor = plugin.getRankManager().getRanksSection().getString(realRank + ".name-color", "&f");

                        // Construct the message
                        TextComponent message = new TextComponent(CC.translate(" &7* &f" + disguiseName + " &7-> &c" + realName + " &7[" + serverName + "]"));

                        ComponentBuilder hoverText = new ComponentBuilder(CC.translate("&eInformation:\n"))
                                .append(CC.translate("&7Real Name: &f" + realName + "\n"))
                                .append(CC.translate("&7Real Rank: " + realRankColor + realRank + "\n"))
                                .append(CC.translate("&7Disguised Name: &f" + disguiseName + "\n"))
                                .append(CC.translate("&7Disguised Rank: " + disguiseColor + disguiseRank + "\n"))
                                .append(CC.translate("&7Server: &b" + serverName));

                        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText.create()));

                        sender.spigot().sendMessage(message);
                    }
                }
            }
        } catch (Exception e) {
            sender.sendMessage(CC.translate("&cFailed to retrieve data from Redis."));
            e.printStackTrace();
        }

        if (count == 0) {
            sender.sendMessage(CC.translate("&cThere are no disguised players online."));
        }

        sender.sendMessage(CC.translate("&6&m----------------------------------------"));
    }
}