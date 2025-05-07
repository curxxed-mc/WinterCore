package curxxed.dev.icore.Commands.Staff;

import curxxed.dev.icore.iCore;
import curxxed.dev.icore.utils.RankManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RankCommand implements CommandExecutor {

    private final RankManager rankManager;
    private final iCore plugin;

    public RankCommand(iCore plugin, RankManager rankManager) {
        this.rankManager = rankManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String subcommand = args.length > 0 ? args[0].toLowerCase() : "";

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
                sender.sendMessage(ChatColor.YELLOW + "Rank Help:");
                sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.AQUA + "/rank create <name>");
                sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.AQUA + "/rank delete <name>");
                sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.AQUA + "/rank weight");
                sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.AQUA + "/rank color");
                sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.AQUA + "/rank add <rank> <permission>");
                sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.AQUA + "/rank remove <rank> <permission> [inherit]");
                sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.AQUA + "/rank prefix <rank> <prefix>");
                sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.AQUA + "/rank permissions <rank>");
                sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.AQUA + "/setrank <player>");
        }
        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /rank create <name>");
            return;
        }
        String rankName = args[1];
        if (rankManager.isValidRank(rankName)) {
            sender.sendMessage(ChatColor.RED + "Rank already exists!");
            return;
        }
        rankManager.createRank(rankName);
        sender.sendMessage(ChatColor.GREEN + "Rank " + rankName + " created successfully.");
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /rank delete <name>");
            return;
        }
        String rankName = args[1];
        if (!rankManager.isValidRank(rankName)) {
            sender.sendMessage(ChatColor.RED + "Rank does not exist!");
            return;
        }
        rankManager.deleteRank(rankName);
        sender.sendMessage(ChatColor.GREEN + "Rank " + rankName + " deleted successfully.");
    }

    private void handleWeight(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /rank weight <rank> <weight>");
            return;
        }
        String rankName = args[1];
        int weight;
        try {
            weight = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Weight must be a valid number.");
            return;
        }
        if (!rankManager.isValidRank(rankName)) {
            sender.sendMessage(ChatColor.RED + "Rank does not exist!");
            return;
        }
        rankManager.setWeight(rankName, weight);
        sender.sendMessage(ChatColor.GREEN + "Weight for rank " + rankName + " set to " + weight + ".");
    }

    private void handleColor(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /rank color <rank> <color>");
            return;
        }
        String rankName = args[1];
        String color = args[2];
        if (!rankManager.isValidRank(rankName)) {
            sender.sendMessage(ChatColor.RED + "Rank does not exist!");
            return;
        }
        rankManager.setColor(rankName, color);
        sender.sendMessage(ChatColor.GREEN + "Color for rank " + rankName + " set to " + color + ".");
    }

    private void handleAddPermission(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /rank add <rank> <permission>");
            return;
        }
        String rankName = args[1];
        String permission = args[2];
        if (!rankManager.isValidRank(rankName)) {
            sender.sendMessage(ChatColor.RED + "Rank does not exist!");
            return;
        }
        rankManager.addPermission(rankName, permission);

        // Reapply permissions for all players with this rank
        for (Player player : Bukkit.getOnlinePlayers()) {
            rankManager.getRank(player, rank -> {
                if (rank.equals(rankName)) {
                    plugin.getPermissionManager().reapplyPermissions(player);
                }
            });
        }

        sender.sendMessage(ChatColor.GREEN + "Permission " + permission + " added to rank " + rankName + ".");
    }

    private void handleRemovePermission(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /rank remove <rank> <permission> [inherit]");
            return;
        }
        String rankName = args[1];
        String permission = args[2];
        boolean inherit = args.length > 3 && Boolean.parseBoolean(args[3]);
        if (!rankManager.isValidRank(rankName)) {
            sender.sendMessage(ChatColor.RED + "Rank does not exist!");
            return;
        }
        rankManager.removePermission(rankName, permission, inherit);

        // Reapply permissions for all players with this rank
        for (Player player : Bukkit.getOnlinePlayers()) {
            rankManager.getRank(player, rank -> {
                if (rank.equals(rankName)) {
                    plugin.getPermissionManager().reapplyPermissions(player);
                }
            });
        }

        sender.sendMessage(ChatColor.GREEN + "Permission " + permission + " removed from rank " + rankName + ".");
    }

    private void handlePrefix(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /rank prefix <rank> <prefix>");
            return;
        }
        String rankName = args[1];
        String prefix = args[2];
        if (!rankManager.isValidRank(rankName)) {
            sender.sendMessage(ChatColor.RED + "Rank does not exist!");
            return;
        }
        rankManager.setPrefix(rankName, prefix);
        sender.sendMessage(ChatColor.GREEN + "Prefix for rank " + rankName + " set to " + prefix + ".");
    }

    private void handlePermissions(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /rank permissions <rank>");
            return;
        }
        String rankName = args[1];
        if (!rankManager.isValidRank(rankName)) {
            sender.sendMessage(ChatColor.RED + "Rank does not exist!");
            return;
        }
        sender.sendMessage(ChatColor.GREEN + "Permissions for rank " + rankName + ": " + rankManager.getPermissions(rankName));
    }
}