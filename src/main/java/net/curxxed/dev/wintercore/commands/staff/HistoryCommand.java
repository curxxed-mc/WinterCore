package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.wintercore.database.DatabaseManager;
import net.curxxed.dev.wintercore.menus.HistroyMenuContext;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;

public class HistoryCommand extends BaseCommand implements Listener {
    private final WinterCore plugin;
    private final DatabaseManager databaseManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public HistoryCommand(WinterCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @Command(
            name = "history",
            permission = "WinterCore.history",
            description = "View a player's punishment and grant history.",
            usage = "/history <player>",
            inGameOnly = true
    )
    public void onCommand(CommandArgs commandArgs) {
        Player player = commandArgs.getPlayer();
        String[] args = commandArgs.getArgs();

        if (args.length != 1) {
            player.sendMessage(CC.Red + "Usage: /history <player>");
            return;
        }

        String playerName = args[0];
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (offlinePlayer == null || offlinePlayer.getUniqueId() == null) {
            player.sendMessage(CC.Red + "Player not found.");
            return;
        }
        fetchAndOpenHistory(player, playerName, offlinePlayer.getUniqueId());
    }

    private void fetchAndOpenHistory(Player viewer, String playerName, UUID uuid) {
        Inventory menu = Bukkit.createInventory(null, 27, CC.Aqua + playerName + "'s History");
        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 3);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(CC.Aqua + "❄");
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 27; i++) menu.setItem(i, glass);
        menu.setItem(10, createMenuButton(
                Material.BOOK,
                CC.Aqua + CC.Bold + "- Warnings -",
                Arrays.asList(
                        CC.Gray + "❄ " + CC.White + "View all warnings for this player.",
                        CC.Aqua + "----------------------",
                        CC.Yellow + CC.Bold + "Click to view warnings history."
                )));
        menu.setItem(12, createMenuButton(
                Material.PAPER,
                CC.Blue + CC.Bold + "- Mutes -",
                Arrays.asList(
                        CC.Gray + "❄ " + CC.White + "View all mutes for this player.",
                        CC.Aqua + "----------------------",
                        CC.Aqua + CC.Bold + "Click to view mutes history."
                )));
        menu.setItem(14, createMenuButton(
                Material.REDSTONE_BLOCK,
                CC.DarkRed + CC.Bold + "- Bans -",
                Arrays.asList(
                        CC.Gray + "❄ " + CC.White + "View all bans for this player.",
                        CC.Aqua + "----------------------",
                        CC.Red + CC.Bold + "Click to view bans history."
                )));
        menu.setItem(16, createMenuButton(
                Material.NAME_TAG,
                CC.Gold + CC.Bold + "- Grants -",
                Arrays.asList(
                        CC.Gray + "❄ " + CC.White + "View all grants for this player.",
                        CC.Aqua + "----------------------",
                        CC.Gold + CC.Bold + "Click to view grants history."
                )));
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short)3);
        ItemMeta skullMeta = skull.getItemMeta();
        skullMeta.setDisplayName(CC.Aqua + CC.Bold + "❄ " + CC.White + playerName + CC.Aqua + CC.Bold + " ❄");
        skullMeta.setLore(Arrays.asList(
                CC.Aqua + "----------------------",
                CC.White + CC.Bold + "Player History Menu",
                CC.Aqua + "----------------------"
        ));
        if (skullMeta instanceof org.bukkit.inventory.meta.SkullMeta) {
            ((org.bukkit.inventory.meta.SkullMeta) skullMeta).setOwner(playerName);
        }
        skull.setItemMeta(skullMeta);
        menu.setItem(4, skull);
        viewer.openInventory(menu);
        menuContext.put(viewer.getUniqueId(), new HistroyMenuContext(playerName, uuid));
    }

    private ItemStack createMenuButton(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private final Map<UUID, HistroyMenuContext> menuContext = new HashMap<>();

    private void openCategoryHistory(Player viewer, String category, String playerName, UUID uuid) {
        switch (category) {
            case "Warnings":
                databaseManager.getWarnings(playerName, warnings -> {
                    List<ItemStack> items = new ArrayList<>();
                    for (Map<String, String> warning : warnings) {
                        ItemStack item = new ItemStack(Material.BOOK);
                        ItemMeta meta = item.getItemMeta();
                        meta.setDisplayName(CC.Yellow + "Warning");
                        List<String> lore = new ArrayList<>();
                        lore.add(CC.Gray + "Reason: " + warning.get("reason"));
                        lore.add(CC.Gray + "Issuer: " + warning.get("issuer"));
                        lore.add(CC.Gray + "Date: " + warning.get("date"));
                        lore.add(CC.Red + "Status: Expired");
                        lore.add(CC.Red + "Click to remove this warning.");
                        if (warning.containsKey("id")) {
                            lore.add(CC.DarkGray + "ID: " + warning.get("id"));
                        }
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                        items.add(item);
                    }
                    openPaginatedHistory(viewer, items, CC.Yellow + playerName + "'s Warnings");
                });
                break;
            case "Mutes":
                databaseManager.getMutes(uuid, mutes -> {
                    List<ItemStack> items = new ArrayList<>();
                    for (Map<String, String> mute : mutes) {
                        boolean active = isActive(mute.get("expiration"));
                        ItemStack item = new ItemStack(Material.PAPER);
                        ItemMeta meta = item.getItemMeta();
                        meta.setDisplayName(CC.Aqua + "Mute");
                        List<String> lore = new ArrayList<>();
                        lore.add(CC.Gray + "Reason: " + mute.get("reason"));
                        lore.add(CC.Gray + "Issuer: " + mute.get("issuer"));
                        lore.add(CC.Gray + "Expires: " + formatDate(mute.get("expiration")));
                        lore.add((active ? CC.Green + "Status: Active" : CC.Red + "Status: Expired"));
                        lore.add(CC.Red + "Click to remove this mute.");
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                        items.add(item);
                    }
                    openPaginatedHistory(viewer, items, CC.Aqua + playerName + "'s Mutes");
                });
                break;
            case "Bans":
                databaseManager.getBans(uuid.toString(), bans -> {
                    List<ItemStack> items = new ArrayList<>();
                    for (Map<String, String> ban : bans) {
                        boolean active = isActive(ban.get("expiration"));
                        ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta meta = item.getItemMeta();
                        meta.setDisplayName(CC.Red + "Ban");
                        List<String> lore = new ArrayList<>();
                        lore.add(CC.Gray + "Reason: " + ban.get("reason"));
                        lore.add(CC.Gray + "Date: " + ban.get("date"));
                        lore.add(CC.Gray + "Expires: " + formatDate(ban.get("expiration")));
                        lore.add((active ? CC.Green + "Status: Active" : CC.Red + "Status: Expired"));
                        lore.add(CC.Red + "Click to remove this ban.");
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                        items.add(item);
                    }
                    openPaginatedHistory(viewer, items, CC.Red + playerName + "'s Bans");
                });
                break;
            case "Grants":
                fetchGrants(uuid, grantItems -> openPaginatedHistory(viewer, grantItems, CC.Gold + playerName + "'s Grants"));
                break;
        }
    }

    private void openPaginatedHistory(Player player, List<ItemStack> items, String title) {
        int size = Math.min(54, ((items.size() / 9) + 1) * 9);
        Inventory inv = Bukkit.createInventory(null, size, title);
        for (int i = 0; i < items.size() && i < size; i++) {
            inv.setItem(i, items.get(i));
        }
        player.openInventory(inv);
    }

    private void fetchGrants(UUID uuid, Consumer<List<ItemStack>> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<ItemStack> grantItems = new ArrayList<>();
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT id, rank, granted_by, granted_at, expires_at, reason FROM player_rank_grants WHERE uuid = ? ORDER BY granted_at DESC")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    int slot = 0;
                    while (rs.next()) {
                        String rank = rs.getString("rank");
                        String grantedBy = rs.getString("granted_by");
                        long grantedAt = rs.getLong("granted_at");
                        long expiresAt = rs.getLong("expires_at");
                        String reason = rs.getString("reason");
                        int id = rs.getInt("id");
                        boolean active = expiresAt == 0 || expiresAt > System.currentTimeMillis();
                        ItemStack item = new ItemStack(active ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK);
                        ItemMeta meta = item.getItemMeta();
                        meta.setDisplayName((active ? CC.Green : CC.Red) + "Grant: " + CC.Gold + rank);
                        List<String> lore = new ArrayList<>();
                        lore.add(CC.Gray + "#" + (slot + 1) + " | " + (active ? CC.Green + "Active" : CC.Red + "Expired"));
                        lore.add(CC.Gray + "Rank: " + CC.Gold + rank);
                        lore.add(CC.Gray + "Granted By: " + CC.Aqua + grantedBy);
                        lore.add(CC.Gray + "Granted At: " + CC.White + formatDate(String.valueOf(grantedAt)));
                        lore.add(CC.Gray + "Expires: " + CC.White + formatDate(String.valueOf(expiresAt)));
                        lore.add(CC.Gray + "Reason: " + CC.Yellow + (reason == null ? "None" : reason));
                        lore.add(CC.Red + "Click to remove this grant.");
                        lore.add(CC.DarkGray + "ID: " + id);
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                        grantItems.add(item);
                        slot++;
                    }
                }
            } catch (Exception e) {
                // ignore
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(grantItems));
        });
    }

    private boolean isActive(String expiration) {
        if (expiration == null || expiration.equals("0") || expiration.isEmpty()) return true;
        try {
            long exp = Long.parseLong(expiration);
            return exp > System.currentTimeMillis();
        } catch (Exception e) {
            return true;
        }
    }

    private String formatDate(String millis) {
        if (millis == null || millis.equals("0") || millis.isEmpty()) return "Never";
        try {
            return dateFormat.format(new Date(Long.parseLong(millis)));
        } catch (Exception e) {
            return "Unknown";
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        String title = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();
        HistroyMenuContext context = menuContext.get(player.getUniqueId());
        if (context == null) return;
        // Handle Grants GUI
        if (title.endsWith("'s Grants")) {
            event.setCancelled(true);
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta == null || !meta.hasLore()) return;
            List<String> lore = meta.getLore();
            int grantId = -1;
            String grantRank = null;
            for (String line : lore) {
                String stripped = org.bukkit.ChatColor.stripColor(line);
                if (stripped.startsWith("ID: ")) {
                    try {
                        grantId = Integer.parseInt(stripped.replace("ID: ", "").trim());
                    } catch (NumberFormatException ignored) {}
                }
                if (stripped.startsWith("Rank: ")) {
                    grantRank = stripped.replace("Rank: ", "").trim();
                }
            }
            if (grantId == -1) return;
            final String grantRankFinal = grantRank;
            databaseManager.removeRankGrant(grantId);
            databaseManager.getRank(context.getUuid(), currentRank -> {
                if (grantRankFinal != null && grantRankFinal.equalsIgnoreCase(currentRank)) {
                    Player target = Bukkit.getPlayer(context.getUuid());
                    if (target != null) {
                        Bukkit.getPluginManager().callEvent(new net.curxxed.dev.wintercore.rank.RankChangeEvent(target, "Default"));
                    }
                }
                fetchGrants(context.getUuid(), grantItems -> openPaginatedHistory(player, grantItems, CC.Gold + context.getPlayerName() + "'s Grants"));
            });
            return;
        }
        // Handle main history menu (category selection)
        if (title.endsWith("'s History")) {
            event.setCancelled(true);
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) return;
            String displayName = meta.getDisplayName();
            String stripped = org.bukkit.ChatColor.stripColor(displayName).toLowerCase();
            if (stripped.contains("warnings")) {
                context.setCurrentCategory("Warnings");
                openCategoryHistory(player, "Warnings", context.getPlayerName(), context.getUuid());
            } else if (stripped.contains("mutes")) {
                context.setCurrentCategory("Mutes");
                openCategoryHistory(player, "Mutes", context.getPlayerName(), context.getUuid());
            } else if (stripped.contains("bans")) {
                context.setCurrentCategory("Bans");
                openCategoryHistory(player, "Bans", context.getPlayerName(), context.getUuid());
            } else if (stripped.contains("grants")) {
                context.setCurrentCategory("Grants");
                openCategoryHistory(player, "Grants", context.getPlayerName(), context.getUuid());
            }
            return;
        }
        event.setCancelled(true);
    }
}
