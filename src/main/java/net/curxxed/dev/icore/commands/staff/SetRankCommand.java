package net.curxxed.dev.icore.commands.staff;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.icore.plugin.iCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SetRankCommand extends BaseCommand {

    private final iCore plugin;

    public SetRankCommand(iCore plugin) {
        this.plugin = plugin;
    }

    @Command(
            name = "setrank",
            permission = "iCore.setrank",
            description = "Open the rank selection GUI for a player.",
            usage = "/setrank <player>",
            inGameOnly = true
    )
    public void onCommand(CommandArgs commandArgs) {
        Player sender = commandArgs.getPlayer();

        if (commandArgs.length() != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /setrank <player>");
            return;
        }

        Player target = Bukkit.getPlayer(commandArgs.getArgs(0));
        if (target == null || !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return;
        }

        plugin.getRankManager().setTargetPlayer(sender, target);
        openRankGUI(sender, target);
    }

    private void openRankGUI(Player player, Player target) {
        Inventory rankGUI = Bukkit.createInventory(null, 27, "Set rank for " + target.getName());

        ConfigurationSection ranksSection = plugin.getRankManager().getRanksSection();
        if (ranksSection == null) {
            player.sendMessage(ChatColor.RED + "No ranks found in the configuration.");
            return;
        }

        List<String> sortedRanks = plugin.getRankManager().getSortedRanks();

        int slot = 0;
        for (String rank : sortedRanks) {
            if (rank == null || rank.isEmpty()) continue;

            ItemStack rankItem = createRankItem(rank, ranksSection, player);
            rankGUI.setItem(slot++, rankItem);
        }

        ItemStack cancelItem = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "Cancel");
        cancelItem.setItemMeta(cancelMeta);
        rankGUI.setItem(26, cancelItem);

        player.openInventory(rankGUI);
    }

    private ItemStack createRankItem(String rank, ConfigurationSection ranksSection, Player player) {
        String colorCode = ranksSection.getString(rank + ".name-color", "&f");
        ChatColor chatColor = ChatColor.getByChar(colorCode.replace("&", "").charAt(0));
        String translatedColorCode = ChatColor.translateAlternateColorCodes('&', colorCode);
        String prefix = ranksSection.getString(rank + ".prefix", "");

        ItemStack rankItem = new ItemStack(Material.WOOL);
        rankItem.setDurability(getDyeColorDataFromChatColor(chatColor));

        ItemMeta meta = rankItem.getItemMeta();
        meta.setDisplayName(translatedColorCode + rank);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Click to select this rank.");
        lore.add(ChatColor.GRAY + "Preview:");
        String translatedPrefix = ChatColor.translateAlternateColorCodes('&', prefix);
        lore.add(translatedPrefix + translatedColorCode + " " + player.getName() + ChatColor.WHITE + ": Hi! This is what your message would look like with this rank.");

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