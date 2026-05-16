package net.curxxed.dev.wintercore.rank;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

@CommandInfo(
        name = "rank",
        permission = "WinterCore.rank",
        description = "Manage ranks.",
        usage = "/rank",
        inGameOnly = false
)
public class RankCommand extends BaseCommand {

    private final RankManager rankManager;
    private final WinterCore plugin;

    public RankCommand(WinterCore plugin, RankManager rankManager) {
        super(plugin);
        this.rankManager = rankManager;
        this.plugin = plugin;
    }


    public void execute(CommandArguments commandArgs) {
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
                sendList(sender, "rank.help", Arrays.asList(
                        "&eRank Help:",
                        "&7- &b/rank create <name>",
                        "&7- &b/rank delete <name>",
                        "&7- &b/rank weight <rank> <weight>",
                        "&7- &b/rank color <rank> <color>",
                        "&7- &b/rank add <rank> <permission>",
                        "&7- &b/rank remove <rank> <permission> [inherit]",
                        "&7- &b/rank prefix <rank> <prefix>",
                        "&7- &b/rank permissions <rank>",
                        "&7- &b/grant <player>"
                ));
        }
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            send(sender, "rank.usage.create", "&cUsage: /rank create <name>");
            return;
        }
        String rankName = args[1];
        try {
            rankManager.createRank(rankName);
            send(sender, "rank.created", "&aRank {rank} created successfully.", "{rank}", rankName);
        } catch (IllegalArgumentException e) {
            sendRankError(sender, e);
        }
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            send(sender, "rank.usage.delete", "&cUsage: /rank delete <name>");
            return;
        }
        String rankName = args[1];
        try {
            new Rank(rankName, plugin).delete();
            send(sender, "rank.deleted", "&aRank {rank} deleted successfully.", "{rank}", rankName);
        } catch (IllegalArgumentException e) {
            sendRankError(sender, e);
        }
    }

    private void handleWeight(CommandSender sender, String[] args) {
        if (args.length < 3) {
            send(sender, "rank.usage.weight", "&cUsage: /rank weight <rank> <weight>");
            return;
        }
        String rankName = args[1];
        int weight;
        try {
            weight = Integer.parseInt(args[2]);
            Rank rank = new Rank(rankName, plugin);
            rank.setWeight(weight);
            send(sender, "rank.weight-set", "&aWeight for rank {rank} set to {weight}.",
                    "{rank}", rankName,
                    "{weight}", String.valueOf(weight));
        } catch (NumberFormatException e) {
            send(sender, "rank.invalid-weight", "&cWeight must be a valid number.");
        } catch (IllegalArgumentException e) {
            sendRankError(sender, e);
        }
    }

    private void handleColor(CommandSender sender, String[] args) {
        if (args.length < 3) {
            send(sender, "rank.usage.color", "&cUsage: /rank color <rank> <color>");
            return;
        }
        String rankName = args[1];
        String color = args[2];
        try {
            Rank rank = new Rank(rankName, plugin);
            rank.setColor(color);
            send(sender, "rank.color-set", "&aColor for rank {rank} set to {color}.",
                    "{rank}", rankName,
                    "{color}", color);
        } catch (IllegalArgumentException e) {
            sendRankError(sender, e);
        }
    }

    private void handleAddPermission(CommandSender sender, String[] args) {
        if (args.length < 3) {
            send(sender, "rank.usage.add", "&cUsage: /rank add <rank> <permission>");
            return;
        }
        String rankName = args[1];
        String permission = args[2];
        try {
            Rank rank = new Rank(rankName, plugin);
            rank.addPermission(permission);
            send(sender, "rank.permission-added", "&aPermission {permission} added to rank {rank}.",
                    "{permission}", permission,
                    "{rank}", rankName);
        } catch (IllegalArgumentException e) {
            sendRankError(sender, e);
        }
    }

    private void handleRemovePermission(CommandSender sender, String[] args) {
        if (args.length < 3) {
            send(sender, "rank.usage.remove", "&cUsage: /rank remove <rank> <permission>");
            return;
        }
        String rankName = args[1];
        String permission = args[2];
        try {
            Rank rank = new Rank(rankName, plugin);
            rank.removePermission(permission);
            send(sender, "rank.permission-removed", "&aPermission {permission} removed from rank {rank}.",
                    "{permission}", permission,
                    "{rank}", rankName);
        } catch (IllegalArgumentException e) {
            sendRankError(sender, e);
        }
    }

    private void handlePrefix(CommandSender sender, String[] args) {
        if (args.length < 3) {
            send(sender, "rank.usage.prefix", "&cUsage: /rank prefix <rank> <prefix>");
            return;
        }
        String rankName = args[1];
        String prefix = args[2];
        try {
            Rank rank = new Rank(rankName, plugin);
            rank.setPrefix(prefix);
            send(sender, "rank.prefix-set", "&aPrefix for rank {rank} set to {prefix}.",
                    "{rank}", rankName,
                    "{prefix}", prefix);
        } catch (IllegalArgumentException e) {
            sendRankError(sender, e);
        }
    }

    private void handlePermissions(CommandSender sender, String[] args) {
        if (args.length < 2) {
            send(sender, "rank.usage.permissions", "&cUsage: /rank permissions <rank>");
            return;
        }
        String rankName = args[1];
        try {
            Rank rank = new Rank(rankName, plugin);
            send(sender, "rank.permissions-list", "&aPermissions for rank {rank}: {permissions}",
                    "{rank}", rankName,
                    "{permissions}", String.valueOf(rank.getPermissions()));
        } catch (IllegalArgumentException e) {
            sendRankError(sender, e);
        }
    }

    private void sendRankError(CommandSender sender, IllegalArgumentException e) {
        send(sender, "rank.error", "&c{error}", "{error}", e.getMessage());
    }
}
