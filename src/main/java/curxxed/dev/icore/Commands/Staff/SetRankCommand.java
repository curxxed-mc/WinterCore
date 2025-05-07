package curxxed.dev.icore.Commands.Staff;

import curxxed.dev.icore.iCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class SetRankCommand implements CommandExecutor {

    private final iCore plugin;

    public SetRankCommand(iCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("iCore.setrank")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /setrank <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        Player player = (Player) sender;
        plugin.getRankManager().setTargetPlayer(player, target); // Store the target player for later use in the listener

        openRankGUI(player, target); // Open the rank GUI for the sender
        return true;
    }

    private void openRankGUI(Player player, Player target) {
        Inventory rankGUI = Bukkit.createInventory(null, 27, "Set Rank for " + target.getName());

        ConfigurationSection ranksSection = plugin.getRankManager().getRanksSection();
        if (ranksSection == null) {
            player.sendMessage(ChatColor.RED + "No ranks found in the configuration.");
            return;
        }

        List<String> sortedRanks = plugin.getRankManager().getSortedRanks();

        int slot = 0;
        for (String rank : sortedRanks) {
            if (rank == null || rank.isEmpty()) continue;

            ItemStack rankItem = createRankItem(rank, ranksSection);
            rankGUI.setItem(slot++, rankItem);
        }

        ItemStack cancelItem = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "Cancel");
        cancelItem.setItemMeta(cancelMeta);
        rankGUI.setItem(26, cancelItem);

        player.openInventory(rankGUI);
    }

    private ItemStack createRankItem(String rank, ConfigurationSection ranksSection) {
        String colorCode = ranksSection.getString(rank + ".name-color", "&f");
        String translatedColorCode = ChatColor.translateAlternateColorCodes('&', colorCode);

        ItemStack rankItem = new ItemStack(Material.WOOL);
        rankItem.setDurability(getDyeColorDataFromChatColor(ChatColor.getByChar(translatedColorCode.charAt(1))));

        ItemMeta meta = rankItem.getItemMeta();
        meta.setDisplayName(translatedColorCode + rank);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Click to select this rank.");
        meta.setLore(lore);
        rankItem.setItemMeta(meta);

        return rankItem;
    }

    private byte getDyeColorDataFromChatColor(ChatColor chatColor) {
        switch (chatColor) {
            case RED: return 14;
            case BLUE: return 11;
            case GREEN: return 13;
            case YELLOW: return 4;
            case AQUA: return 3;
            case LIGHT_PURPLE: return 2;
            case WHITE: return 0;
            case BLACK: return 15;
            case DARK_GRAY: return 7;
            case GRAY: return 8;
            case DARK_RED: return 1;
            case DARK_GREEN: return 5;
            case DARK_BLUE: return 6;
            case DARK_AQUA: return 9;
            case GOLD: return 4;
            default: return 0;
        }
    }
}