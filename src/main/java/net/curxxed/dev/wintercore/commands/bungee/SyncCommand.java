package net.curxxed.dev.wintercore.commands.bungee;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.database.redis.packet.packets.ConfigSyncPacket;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

    public SyncCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments args) {
        CommandSender sender = args.getSender();

        if (args.length() == 0) {
            reply(sender, "&eUsage: " + commandInfo.usage());
            return;
        }

        String sub = args.getOptionalString(0).orElse("").toLowerCase();

        switch (sub) {
            case "ranks":
                pushRanks(sender);
                break;
            case "tags":
                pushTags(sender);
                break;
            case "all":
                pushRanks(sender);
                pushTags(sender);
                break;
            default:
                reply(sender, "&eUsage: " + commandInfo.usage());
        }
    }

    private void pushRanks(CommandSender sender) {
        String serverName = plugin.getConfig().getString("server-name", "Unknown");
        File file = new File(plugin.getDataFolder(), "ranks.yml");

        try {
            String yaml = readFile(file);
            if (yaml == null) {
                reply(sender, "&cFailed to read ranks.yml.");
                return;
            }

            plugin.getRedisManager().publish(
                    new ConfigSyncPacket(
                            serverName,
                            System.currentTimeMillis(),
                            ConfigSyncPacket.ConfigType.RANKS,
                            yaml
                    )
            );

            Bukkit.getScheduler().runTask(plugin, () -> applyRanksLocally(serverName));
            reply(sender, "&aPublished &eranks.yml&a as a packet.");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to publish ranks.yml sync: " + e.getMessage());
            reply(sender, "&cRedis error during rank sync: " + e.getMessage());
        }
    }

    private void pushTags(CommandSender sender) {
        String serverName = plugin.getConfig().getString("server-name", "Unknown");
        File file = new File(plugin.getDataFolder(), "tags.yml");

        try {
            String yaml = readFile(file);
            if (yaml == null) {
                reply(sender, "&cFailed to read tags.yml.");
                return;
            }

            plugin.getRedisManager().publish(
                    new ConfigSyncPacket(
                            serverName,
                            System.currentTimeMillis(),
                            ConfigSyncPacket.ConfigType.TAGS,
                            yaml
                    )
            );

            Bukkit.getScheduler().runTask(plugin, () -> applyTagsLocally(serverName));
            reply(sender, "&aPublished &etags.yml&a as a packet.");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to publish tags.yml sync: " + e.getMessage());
            reply(sender, "&cRedis error during tag sync: " + e.getMessage());
        }
    }

    private void applyRanksLocally(String source) {
        plugin.getRankManager().reloadRanksConfig();

        for (Player p : Bukkit.getOnlinePlayers()) {
            plugin.getRankManager().refreshPlayerDisplay(p);
        }

        plugin.getLogger().info("[Sync] ranks.yml applied locally from " + source + ".");
    }

    private void applyTagsLocally(String source) {
        plugin.getTagsManager().loadTags();
        plugin.getTagsGUI().refresh();

        plugin.getLogger().info("[Sync] tags.yml applied locally from " + source + ".");
    }

    private String readFile(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private void reply(CommandSender sender, String message) {
        Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(CC.translate(message)));
    }
}