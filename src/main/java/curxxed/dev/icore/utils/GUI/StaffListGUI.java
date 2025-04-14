package curxxed.dev.icore.utils.GUI;

import curxxed.dev.icore.Database.RedisManager;
import curxxed.dev.icore.Main;
import curxxed.dev.icore.utils.RankManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class StaffListGUI implements Listener {

    private final Main plugin;
    private final RankManager rankManager;

    public StaffListGUI(Main plugin) {
        this.plugin = plugin;
        this.rankManager = plugin.getRankManager();
    }

    public void openGUI(Player viewer, RedisManager ignoredRedis) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Inventory gui = Bukkit.createInventory(null, 54, ChatColor.AQUA + "Online Staff");

            try (Jedis jedis = plugin.getRedisPool().getResource()) {
                Map<String, String> onlineStaff = jedis.hgetAll("staff:last-server");

                List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (Map.Entry<String, String> entry : onlineStaff.entrySet()) {
                    String uuidStr = entry.getKey();
                    String server = entry.getValue();

                    if (server == null || server.isEmpty()) continue;
                    if (!jedis.exists("server:" + server + ":heartbeat")) continue;

                    UUID uuid;
                    try {
                        uuid = UUID.fromString(uuidStr);
                    } catch (IllegalArgumentException e) {
                        continue;
                    }

                    // Try getting cached username from Redis first
                    String rawName = jedis.get("username:" + uuidStr);
                    String playerName = (rawName != null) ? rawName : Bukkit.getOfflinePlayer(uuid).getName();

                    if (playerName == null) continue; // Skip if the name is still null

                    CompletableFuture<Void> future = new CompletableFuture<>();
                    rankManager.getRank(uuid, rank ->
                            rankManager.getColorPreference(rank, color -> Bukkit.getScheduler().runTask(plugin, () -> {
                                String coloredRank = ChatColor.translateAlternateColorCodes('&', color) + rank;

                                ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
                                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                                if (meta == null) return;

                                meta.setOwner(playerName);
                                meta.setDisplayName(ChatColor.AQUA + playerName);
                                meta.setLore(Arrays.asList(
                                        ChatColor.GRAY + "Server: " + ChatColor.YELLOW + server,
                                        ChatColor.GRAY + "Rank: " + coloredRank
                                ));
                                skull.setItemMeta(meta);

                                gui.addItem(skull);
                                future.complete(null);
                            }))
                    );

                    futures.add(future);
                }

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() ->
                        Bukkit.getScheduler().runTask(plugin, () -> viewer.openInventory(gui))
                );
            }
        });
    }

    @EventHandler
    public void onInventoryEvent(InventoryClickEvent e) {
        if (e.getView().getTitle().equalsIgnoreCase(ChatColor.AQUA + "Online Staff")) {
            e.setCancelled(true);
        }
    }
}
