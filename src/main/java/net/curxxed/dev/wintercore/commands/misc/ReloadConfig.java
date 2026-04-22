package net.curxxed.dev.wintercore.commands.misc;


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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@CommandInfo(
        name = "reloadconfig",
        permission = "wintercore.reloadconfig",
        description = "Reload local config files, optionally syncing to the network.",
        usage = "/reloadconfig [all|config|ranks|tags|menus|permissions] [sync]",
        inGameOnly = false
)
public class ReloadConfig extends BaseCommand {

    private final WinterCore plugin;

    public ReloadConfig(WinterCore pl) {
       super(pl);
       this.plugin = pl;
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        ConfigTarget target = ConfigTarget.ALL;
        boolean sync = false;

        for (String rawArg : commandArgs.getArgs()) {
            String arg = rawArg.toLowerCase(Locale.ENGLISH);
            if ("sync".equals(arg) || "--sync".equals(arg) || "network".equals(arg)) {
                sync = true;
                continue;
            }

            ConfigTarget parsed = ConfigTarget.fromToken(arg);
            if (parsed == null) {
                sendUsage(commandArgs.getSender());
                return;
            }
            target = parsed;
        }

        List<String> localReloaded = reloadLocal(target);
        commandArgs.getSender().sendMessage(CC.translate(
                "&aReloaded locally: &f" + String.join("&7, &f", localReloaded)
        ));

        if (!sync) {
            commandArgs.getSender().sendMessage(CC.translate("&7Tip: add &esync&7 to broadcast the changes over Redis."));
            return;
        }

        List<String> pushed = pushToNetwork(target, commandArgs.getSender());
        if (!pushed.isEmpty()) {
            commandArgs.getSender().sendMessage(CC.translate(
                    "&bBroadcasted over Redis: &f" + String.join("&7, &f", pushed)
            ));
        }
    }

    private List<String> reloadLocal(ConfigTarget target) {
        List<String> reloaded = new ArrayList<>();
        for (ConfigTarget part : expandTarget(target)) {
            switch (part) {
                case CONFIG:
                    plugin.reloadConfig();
                    reloaded.add("config.yml");
                    break;
                case RANKS:
                    plugin.getRankManager().reloadRanksConfig();
                    refreshPlayerDisplays();
                    reloaded.add("ranks.yml");
                    break;
                case TAGS:
                    plugin.getTagsManager().loadTags();
                    plugin.getTagsGUI().refresh();
                    reloaded.add("tags.yml");
                    break;
                case MENUS:
                    plugin.getMenuConfig().load();
                    reloaded.add("menus.yml");
                    break;
                case PERMISSIONS:
                    plugin.getPermissionConfigManager().load();
                    refreshPlayerDisplays();
                    reloaded.add("permissions.yml");
                    break;
                default:
                    break;
            }
        }
        return reloaded;
    }

    private List<String> pushToNetwork(ConfigTarget target, CommandSender sender) {
        List<String> pushed = new ArrayList<>();
        String sourceServer = plugin.getConfig().getString("server-name", "Unknown");

        for (ConfigTarget part : expandTarget(target)) {
            File file = new File(plugin.getDataFolder(), part.fileName);
            String yaml = readYaml(file);
            if (yaml == null) {
                sender.sendMessage(CC.translate("&cFailed to read &f" + part.fileName + "&c for network sync."));
                continue;
            }

            plugin.getRedisManager().publish(new ConfigSyncPacket(
                    sourceServer,
                    System.currentTimeMillis(),
                    part.packetType,
                    yaml
            ));
            pushed.add(part.fileName);
        }

        return pushed;
    }

    private List<ConfigTarget> expandTarget(ConfigTarget target) {
        List<ConfigTarget> targets = new ArrayList<>();
        if (target == ConfigTarget.ALL) {
            targets.add(ConfigTarget.CONFIG);
            targets.add(ConfigTarget.RANKS);
            targets.add(ConfigTarget.TAGS);
            targets.add(ConfigTarget.MENUS);
            targets.add(ConfigTarget.PERMISSIONS);
            return targets;
        }
        targets.add(target);
        return targets;
    }

    private void refreshPlayerDisplays() {
        if (plugin.getRankManager() == null) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getRankManager().refreshPlayerDisplay(player);
            if (plugin.getPlayerService() != null) {
                plugin.getPlayerService().loadPlayerData(player.getUniqueId(), player.getName());
            }
        }
    }

    private String readYaml(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(CC.translate("&eUsage: " + commandInfo.usage()));
        sender.sendMessage(CC.translate("&7Examples:"));
        sender.sendMessage(CC.translate("&7- /reloadconfig"));
        sender.sendMessage(CC.translate("&7- /reloadconfig permissions"));
        sender.sendMessage(CC.translate("&7- /reloadconfig all sync"));
    }

    private enum ConfigTarget {
        ALL(null, null),
        CONFIG("config.yml", ConfigSyncPacket.ConfigType.CONFIG),
        RANKS("ranks.yml", ConfigSyncPacket.ConfigType.RANKS),
        TAGS("tags.yml", ConfigSyncPacket.ConfigType.TAGS),
        MENUS("menus.yml", ConfigSyncPacket.ConfigType.MENUS),
        PERMISSIONS("permissions.yml", ConfigSyncPacket.ConfigType.PERMISSIONS);

        private final String fileName;
        private final ConfigSyncPacket.ConfigType packetType;

        ConfigTarget(String fileName, ConfigSyncPacket.ConfigType packetType) {
            this.fileName = fileName;
            this.packetType = packetType;
        }

        private static ConfigTarget fromToken(String token) {
            if ("all".equals(token)) return ALL;
            if ("config".equals(token) || "cfg".equals(token)) return CONFIG;
            if ("ranks".equals(token)) return RANKS;
            if ("tags".equals(token)) return TAGS;
            if ("menus".equals(token) || "menu".equals(token)) return MENUS;
            if ("permissions".equals(token) || "perms".equals(token)) return PERMISSIONS;
            return null;
        }
    }
}
