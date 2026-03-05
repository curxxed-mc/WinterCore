package net.curxxed.dev.wintercore.menus;

import net.curxxed.dev.wintercore.menu.Button;
import net.curxxed.dev.wintercore.menu.Menu;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.rank.RankManager;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class StaffListMenu extends Menu {

    private final WinterCore plugin;
    private final RankManager rankManager;

    private final List<ItemStack> skulls = new CopyOnWriteArrayList<>();

    public StaffListMenu(WinterCore plugin) {
        this.plugin = plugin;
        this.rankManager = plugin.getRankManager();
    }

    @Override
    public String getTitle() {
        return CC.translate("&bOnline Staff");
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        for (int i = 0; i < skulls.size() && i < getSize(); i++) {
            buttons.put(i, new Button(skulls.get(i)));
        }
        return buttons;
    }

    @Override
    public void onOpen(Player viewer) {
        loadStaffAsync(viewer);
    }

    private void loadStaffAsync(Player viewer) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<StaffEntry> entries = new ArrayList<>();

            try (Jedis jedis = plugin.getRedisPool().getResource()) {
                Map<String, String> lastServers = jedis.hgetAll("staff:last-server");
                if (lastServers == null || lastServers.isEmpty()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        skulls.clear();
                        if (viewer.isOnline()) refresh(viewer);
                    });
                    return;
                }

                for (Map.Entry<String, String> entry : lastServers.entrySet()) {
                    String uuidStr = entry.getKey();
                    String server  = entry.getValue();

                    if (server == null || server.isEmpty()) continue;
                    if (!jedis.exists("server:" + server + ":heartbeat")) continue;

                    UUID uuid;
                    try {
                        uuid = UUID.fromString(uuidStr);
                    } catch (IllegalArgumentException e) {
                        continue;
                    }

                    String cachedName = jedis.get("username:" + uuidStr);
                    String playerName = cachedName != null
                            ? cachedName
                            : Bukkit.getOfflinePlayer(uuid).getName();
                    if (playerName == null) continue;

                    entries.add(new StaffEntry(uuid, playerName, server));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("StaffListMenu: Redis error — " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    skulls.clear();
                    if (viewer.isOnline()) refresh(viewer);
                });
                return;
            }

            if (entries.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    skulls.clear();
                    if (viewer.isOnline()) refresh(viewer);
                });
                return;
            }

            List<ItemStack> loaded = Collections.synchronizedList(new ArrayList<>());
            AtomicInteger remaining = new AtomicInteger(entries.size());

            for (StaffEntry se : entries) {
                rankManager.getRank(se.uuid, rank -> {
                    if (rank == null) rank = "Default";
                    final String resolvedRank = rank;
                    rankManager.getColorPreference(resolvedRank, color -> {
                        if (color == null) color = "&f";
                        String coloredRank = CC.translate(color) + resolvedRank;

                        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
                        SkullMeta meta  = (SkullMeta) skull.getItemMeta();
                        if (meta != null) {
                            meta.setOwner(se.playerName);
                            meta.setDisplayName(CC.translate("&b" + se.playerName));
                            meta.setLore(Arrays.asList(
                                    CC.translate("&7Server: &e" + se.server),
                                    CC.translate("&7Rank: ") + coloredRank
                            ));
                            skull.setItemMeta(meta);
                        }
                        loaded.add(skull);

                        if (remaining.decrementAndGet() == 0) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                skulls.clear();
                                skulls.addAll(loaded);
                                if (viewer.isOnline()) refresh(viewer);
                            });
                        }
                    });
                });
            }
        });
    }

    private static class StaffEntry {
        final UUID uuid;
        final String playerName;
        final String server;

        StaffEntry(UUID uuid, String playerName, String server) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.server = server;
        }
    }
}