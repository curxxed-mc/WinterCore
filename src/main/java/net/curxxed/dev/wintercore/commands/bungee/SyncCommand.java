package net.curxxed.dev.wintercore.commands.bungee;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.events.network.RankTagSyncEvent;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.command.CommandSender;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@CommandInfo(
        name = "network",
        permission = "wintercore.network",
        description = "Sync ranks.yml or tags.yml across all servers via Redis.",
        usage = "/network <ranks|tags|all>",
        async = true
)
public class SyncCommand extends BaseCommand {

    // Redis channels
    public static final String CHANNEL_RANKS = "config-network:ranks";
    public static final String CHANNEL_TAGS  = "config-network:tags";

    public SyncCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments args) {
        CommandSender sender = args.getSender();

        if (args.length() == 0) {
            sender.sendMessage(CC.translate("&eUsage: " + commandInfo.usage()));
            return;
        }

        String sub = args.getOptionalString(0).orElse("").toLowerCase();

        switch (sub) {
            case "ranks":
                pushSync(sender, RankTagSyncEvent.SyncType.RANKS);
                break;
            case "tags":
                pushSync(sender, RankTagSyncEvent.SyncType.TAGS);
                break;
            case "all":
                pushSync(sender, RankTagSyncEvent.SyncType.ALL);
                break;
            default:
                sender.sendMessage(CC.translate("&eUsage: " + commandInfo.usage()));
        }
    }

    /**
     * Reads the requested file(s), publishes their raw YAML content to Redis,
     * then reloads the local copy and fires {@link RankTagSyncEvent}.
     */
    private void pushSync(CommandSender sender, RankTagSyncEvent.SyncType type) {
        String serverName = plugin.getConfig().getString("server-name", "Unknown");

        try (Jedis jedis = plugin.getRedisPool().getResource()) {

            if (type == RankTagSyncEvent.SyncType.RANKS || type == RankTagSyncEvent.SyncType.ALL) {
                String yaml = readFile(new File(plugin.getDataFolder(), "ranks.yml"));
                if (yaml == null) {
                    sender.sendMessage(CC.translate("&cFailed to read ranks.yml."));
                    return;
                }
                jedis.publish(CHANNEL_RANKS, serverName + "|" + yaml);
                reloadRanks(sender, serverName, true);
                sender.sendMessage(CC.translate("&aPublished &eranks.yml&a to all servers."));
            }

            if (type == RankTagSyncEvent.SyncType.TAGS || type == RankTagSyncEvent.SyncType.ALL) {
                String yaml = readFile(new File(plugin.getDataFolder(), "tags.yml"));
                if (yaml == null) {
                    sender.sendMessage(CC.translate("&cFailed to read tags.yml."));
                    return;
                }
                jedis.publish(CHANNEL_TAGS, serverName + "|" + yaml);
                reloadTags(sender, serverName, true);
                sender.sendMessage(CC.translate("&aPublished &etags.yml&a to all servers."));
            }

        } catch (Exception e) {
            sender.sendMessage(CC.translate("&cRedis error during network: " + e.getMessage()));
            plugin.getLogger().warning("Sync publish failed: " + e.getMessage());
        }
    }

    public static boolean writeFile(File file, String yaml) {
        try {
            Files.write(file.toPath(), yaml.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private String readFile(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    public static void reloadRanks(CommandSender initiator, String source, boolean isLocal) {
        WinterCore plugin = WinterCore.getInstance();
        plugin.getRankManager().reloadRanksConfig();
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                plugin.getRankManager().refreshPlayerDisplay(p);
            }
            org.bukkit.Bukkit.getPluginManager().callEvent(
                    new RankTagSyncEvent(RankTagSyncEvent.SyncType.RANKS, initiator, source));

            if (!isLocal) {
                plugin.getLogger().info("[Sync] ranks.yml received from " + source + " and applied.");
            }
        });
    }

    public static void reloadTags(CommandSender initiator, String source, boolean isLocal) {
        WinterCore plugin = WinterCore.getInstance();
        plugin.getTagsManager().loadTags();
        plugin.getTagsGUI().refresh();

        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            org.bukkit.Bukkit.getPluginManager().callEvent(
                    new RankTagSyncEvent(RankTagSyncEvent.SyncType.TAGS, initiator, source));

            if (!isLocal) {
                plugin.getLogger().info("[Sync] tags.yml received from " + source + " and applied.");
            }
        });
    }
}