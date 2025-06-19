package net.curxxed.dev.icore.commands.staff;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.icore.database.DatabaseManager;
import net.curxxed.dev.icore.plugin.iCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class HistoryCommand extends BaseCommand implements Listener {
    private final iCore plugin;
    private final DatabaseManager databaseManager;

    public HistoryCommand(iCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @Command(
            name = "history",
            permission = "iCore.history",
            description = "View a player's punishment history.",
            usage = "/history <player>",
            inGameOnly = true
    )
    public void onCommand(CommandArgs commandArgs) {
        Player player = commandArgs.getPlayer();
        String[] args = commandArgs.getArgs();

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /history <player>");
            return;
        }

        String playerName = args[0];
        openHistoryCategoriesGui(player, playerName);
    }

    private void openHistoryCategoriesGui(Player player, String playerName) {
        Inventory inv = Bukkit.createInventory(null, 9, "Punishment History");

        inv.addItem(createGuiItem("Warnings", Material.BOOK, "Click to view warnings.", playerName, "warnings"));
        inv.addItem(createGuiItem("Mutes", Material.PAPER, "Click to view mutes.", playerName, "mutes"));
        inv.addItem(createGuiItem("Kicks", Material.BARRIER, "Click to view kicks.", playerName, "kicks"));
        inv.addItem(createGuiItem("Bans", Material.REDSTONE_BLOCK, "Click to view bans.", playerName, "bans"));

        player.openInventory(inv);
    }

    private ItemStack createGuiItem(String name, Material material, String lore, String playerName, String category) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + name);
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + lore,
                ChatColor.GRAY + "Player: " + playerName,
                ChatColor.GRAY + "Category: " + category
        ));
        item.setItemMeta(meta);
        return item;
    }

    private void openPunishmentListGui(Player player, String playerName, String category) {
        Inventory inv = Bukkit.createInventory(null, 36, category + " History");

        databaseManager.getUUIDByName(playerName, uuid -> {
            if (uuid == null) {
                player.sendMessage(ChatColor.RED + "Player not found.");
                return;
            }

            switch (category.toLowerCase()) {
                case "warnings":
                    databaseManager.getWarnings(playerName, warnings -> populatePunishmentGui(inv, warnings));
                    break;
                case "mutes":
                    databaseManager.getMutes(uuid, mutes -> populatePunishmentGui(inv, mutes));
                    break;
                case "kicks":
                    databaseManager.getKicks(playerName, kicks -> populatePunishmentGui(inv, kicks));
                    break;
                case "bans":
                    databaseManager.getBans(playerName, bans -> populatePunishmentGui(inv, bans));
                    break;
            }

            player.openInventory(inv);
        });
    }

    private void populatePunishmentGui(Inventory inv, List<Map<String, String>> punishments) {
        if (punishments.isEmpty()) {
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "No punishments found.");
            item.setItemMeta(meta);
            inv.addItem(item);
        } else {
            for (Map<String, String> punishment : punishments) {
                ItemStack item = new ItemStack(Material.PAPER);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.YELLOW + punishment.get("reason"));
                meta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Date: " + punishment.get("date"),
                        ChatColor.GRAY + "Issuer: " + punishment.get("issuer")
                ));
                item.setItemMeta(meta);
                inv.addItem(item);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        String title = event.getView().getTitle();
        if (title.equals("Punishment History") || title.endsWith("History")) {
            event.setCancelled(true);

            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasLore()) {
                List<String> lore = meta.getLore();
                String category = null;
                String playerName = null;

                for (String line : lore) {
                    if (line.contains("Category: ")) {
                        category = line.replace(ChatColor.GRAY + "Category: ", "").toLowerCase();
                    }
                    if (line.contains("Player: ")) {
                        playerName = line.replace(ChatColor.GRAY + "Player: ", "");
                    }
                }

                if (category != null && playerName != null) {
                    openPunishmentListGui((Player) event.getWhoClicked(), playerName, category);
                }
            }
        }
    }
}