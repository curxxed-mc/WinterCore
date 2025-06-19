package net.curxxed.dev.icore.rank;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.icore.plugin.iCore;
import net.curxxed.dev.icore.utils.CC;
import org.bukkit.command.CommandSender;

public class RankCommand extends BaseCommand {

    private final RankManager rankManager;
    private final iCore plugin;

    public RankCommand(iCore plugin, RankManager rankManager) {
        this.rankManager = rankManager;
        this.plugin = plugin;
    }

    @Command(
        name = "rank",
        permission = "iCore.rank",
        description = "Manage ranks.",
        usage = "/rank",
        inGameOnly = false
    )
    public void onCommand(CommandArgs commandArgs) {
        String[] args = commandArgs.getArgs();
        String subcommand = args.length > 0 ? args[0].toLowerCase() : "";
        org.bukkit.command.CommandSender sender = commandArgs.getSender();

        switch (subcommand) {
            case "create":
                handleCreate(sender, args);
                break;
            case "delete":
                handleDelete(sender, args);
                break;
            case "weight":
                handleWeight(sender, args);
                break;
            case "color":
                handleColor(sender, args);
                break;
            case "add":
                handleAddPermission(sender, args);
                break;
            case "remove":
                handleRemovePermission(sender, args);
                break;
            case "prefix":
                handlePrefix(sender, args);
                break;
            case "permissions":
                handlePermissions(sender, args);
                break;
            default:
                sender.sendMessage(CC.translate("&eRank Help:"));
                sender.sendMessage(CC.translate("&7- &b/rank create <name>"));
                sender.sendMessage(CC.translate("&7- &b/rank delete <name>"));
                sender.sendMessage(CC.translate("&7- &b/rank weight"));
                sender.sendMessage(CC.translate("&7- &b/rank color"));
                sender.sendMessage(CC.translate("&7- &b/rank add <rank> <permission>"));
                sender.sendMessage(CC.translate("&7- &b/rank remove <rank> <permission> [inherit]"));
                sender.sendMessage(CC.translate("&7- &b/rank prefix <rank> <prefix>"));
                sender.sendMessage(CC.translate("&7- &b/rank permissions <rank>"));
                sender.sendMessage(CC.translate("&7- &b/setrank <player>"));
        }
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(CC.translate("&cUsage: /rank create <name>"));
            return;
        }
        String rankName = args[1];
        try {
            rankManager.createRank(rankName);
            sender.sendMessage(CC.translate("&aRank " + rankName + " created successfully."));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(CC.translate("&c" + e.getMessage()));
        }
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(CC.translate("&cUsage: /rank delete <name>"));
            return;
        }
        String rankName = args[1];
        try {
            new Rank(rankName, plugin).delete();
            sender.sendMessage(CC.translate("&aRank " + rankName + " deleted successfully."));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(CC.translate("&c" + e.getMessage()));
        }
    }

    private void handleWeight(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(CC.translate("&cUsage: /rank weight <rank> <weight>"));
            return;
        }
        String rankName = args[1];
        int weight;
        try {
            weight = Integer.parseInt(args[2]);
            Rank rank = new Rank(rankName, plugin);
            rank.setWeight(weight);
            sender.sendMessage(CC.translate("&aWeight for rank " + rankName + " set to " + weight + "."));
        } catch (NumberFormatException e) {
            sender.sendMessage(CC.translate("&cWeight must be a valid number."));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(CC.translate("&c" + e.getMessage()));
        }
    }

    private void handleColor(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(CC.translate("&cUsage: /rank color <rank> <color>"));
            return;
        }
        String rankName = args[1];
        String color = args[2];
        try {
            Rank rank = new Rank(rankName, plugin);
            rank.setColor(color);
            sender.sendMessage(CC.translate("&aColor for rank " + rankName + " set to " + color + "."));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(CC.translate("&c" + e.getMessage()));
        }
    }

    private void handleAddPermission(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(CC.translate("&cUsage: /rank add <rank> <permission>"));
            return;
        }
        String rankName = args[1];
        String permission = args[2];
        try {
            Rank rank = new Rank(rankName, plugin);
            rank.addPermission(permission);
            sender.sendMessage(CC.translate("&aPermission " + permission + " added to rank " + rankName + "."));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(CC.translate("&c" + e.getMessage()));
        }
    }

    private void handleRemovePermission(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(CC.translate("&cUsage: /rank remove <rank> <permission>"));
            return;
        }
        String rankName = args[1];
        String permission = args[2];
        try {
            Rank rank = new Rank(rankName, plugin);
            rank.removePermission(permission);
            sender.sendMessage(CC.translate("&aPermission " + permission + " removed from rank " + rankName + "."));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(CC.translate("&c" + e.getMessage()));
        }
    }

    private void handlePrefix(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(CC.translate("&cUsage: /rank prefix <rank> <prefix>"));
            return;
        }
        String rankName = args[1];
        String prefix = args[2];
        try {
            Rank rank = new Rank(rankName, plugin);
            rank.setPrefix(prefix);
            sender.sendMessage(CC.translate("&aPrefix for rank " + rankName + " set to " + prefix + "."));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(CC.translate("&c" + e.getMessage()));
        }
    }

    private void handlePermissions(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(CC.translate("&cUsage: /rank permissions <rank>"));
            return;
        }
        String rankName = args[1];
        try {
            Rank rank = new Rank(rankName, plugin);
            sender.sendMessage(CC.translate("&aPermissions for rank " + rankName + ": " + rank.getPermissions()));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(CC.translate("&c" + e.getMessage()));
        }
    }
}
