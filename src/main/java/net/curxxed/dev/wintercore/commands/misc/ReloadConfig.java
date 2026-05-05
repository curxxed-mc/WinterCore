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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@CommandInfo(
        name = "reloadconfig",
        description = "Reloadconfig files, optionally syncing to the network.",
        usage = "/reloadconfig [all|ranks|tags|menus|permissions|messages|chatfilter] [sync]",
        inGameOnly = false,
        permission = {"wintercore.reloadconfig"}
)
public class ReloadConfig extends BaseCommand {

    private final WinterCore plugin;
    private static final List<String> TARGET_TOKENS = Arrays.asList("all", "ranks", "tags", "menus", "permissions", "messages", "chatfilter", "chat-filter");
    private static final List<String> SYNC_TOKENS = Arrays.asList("sync", "--sync", "network");

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
                sendReloadUsage(commandArgs.getSender());
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

    @Override
    public List<String> onTabComplete(CommandArguments args) {
        if (args.length() == 0) {
            return completeCurrentArg(args, TARGET_TOKENS);
        }

        if (args.length() == 1) {
            List<String> firstArgOptions = new ArrayList<>(TARGET_TOKENS);
            firstArgOptions.addAll(SYNC_TOKENS);
            return completeCurrentArg(args, firstArgOptions);
        }

        if (args.length() == 2) {
            String first = args.getOptionalString(0).orElse("").toLowerCase(Locale.ENGLISH);
            if (isSyncToken(first)) {
                return completeCurrentArg(args, TARGET_TOKENS);
            }
            if (ConfigTarget.fromToken(first) != null) {
                return completeCurrentArg(args, SYNC_TOKENS);
            }
        }

        return Collections.emptyList();
    }

    private List<String> reloadLocal(ConfigTarget target) {
        List<String> reloaded = new ArrayList<>();
        for (ConfigTarget part : expandTarget(target)) {
            switch (part) {
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
                case MESSAGES:
                    plugin.getMessageConfig().load();
                    reloaded.add("messages.yml");
                    break;
                case CHAT_FILTER:
                    plugin.getChatFilterService().load();
                    reloaded.add("chat-filter.yml");
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
            targets.add(ConfigTarget.RANKS);
            targets.add(ConfigTarget.TAGS);
            targets.add(ConfigTarget.MENUS);
            targets.add(ConfigTarget.PERMISSIONS);
            targets.add(ConfigTarget.MESSAGES);
            targets.add(ConfigTarget.CHAT_FILTER);
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

    private void sendReloadUsage(CommandSender sender) {
        sender.sendMessage(CC.translate("&eUsage: " + commandInfo.usage()));
        sender.sendMessage(CC.translate("&7Examples:"));
        sender.sendMessage(CC.translate("&7- /reloadconfig"));
        sender.sendMessage(CC.translate("&7- /reloadconfig permissions"));
        sender.sendMessage(CC.translate("&7- /reloadconfig all sync"));
    }

    private boolean isSyncToken(String token) {
        return "sync".equals(token) || "--sync".equals(token) || "network".equals(token);
    }

    private enum ConfigTarget {
        ALL(null, null),
        RANKS("ranks.yml", ConfigSyncPacket.ConfigType.RANKS),
        TAGS("tags.yml", ConfigSyncPacket.ConfigType.TAGS),
        MENUS("menus.yml", ConfigSyncPacket.ConfigType.MENUS),
        PERMISSIONS("permissions.yml", ConfigSyncPacket.ConfigType.PERMISSIONS),
        MESSAGES("messages.yml", ConfigSyncPacket.ConfigType.MESSAGES),
        CHAT_FILTER("chat-filter.yml", ConfigSyncPacket.ConfigType.CHAT_FILTER);

        private final String fileName;
        private final ConfigSyncPacket.ConfigType packetType;

        ConfigTarget(String fileName, ConfigSyncPacket.ConfigType packetType) {
            this.fileName = fileName;
            this.packetType = packetType;
        }

        private static ConfigTarget fromToken(String token) {
            if ("all".equals(token)) return ALL;
            if ("ranks".equals(token)) return RANKS;
            if ("tags".equals(token)) return TAGS;
            if ("menus".equals(token) || "menu".equals(token)) return MENUS;
            if ("permissions".equals(token) || "perms".equals(token)) return PERMISSIONS;
            if ("messages".equals(token) || "message".equals(token)) return MESSAGES;
            if ("chatfilter".equals(token) || "chat-filter".equals(token) || "filter".equals(token)) return CHAT_FILTER;
            return null;
        }
    }
}
