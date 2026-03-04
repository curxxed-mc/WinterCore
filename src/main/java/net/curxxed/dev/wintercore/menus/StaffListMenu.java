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

public class StaffListMenu extends Menu {

    private final WinterCore  plugin;
    private final RankManager rankManager;


    private final List<ItemStack> skulls = new CopyOnWriteArrayList<>();

    public StaffListMenu(WinterCore plugin) {
        this.plugin      = plugin;
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

            List<ItemStack> loaded = new ArrayList<>();

            try (Jedis jedis = plugin.getRedisPool().getResource()) {
                Map<String, String> onlineStaff = jedis.hgetAll("staff:last-server");

                for (Map.Entry<String, String> entry : onlineStaff.entrySet()) {
                    String uuidStr = entry.getKey();
                    String server  = entry.getValue();

                    if (server == null || server.isEmpty()
                            || !jedis.exists("server:" + server + ":heartbeat")) continue;

                    UUID uuid;
                    try {
                        uuid = UUID.fromString(uuidStr);
                    } catch (IllegalArgumentException e) {
                        continue;
                    }

                    String rawName    = jedis.get("username:" + uuidStr);
                    String playerName = (rawName != null)
                            ? rawName
                            : Bukkit.getOfflinePlayer(uuid).getName();
                    if (playerName == null) continue;


                    final String[] rankHolder  = {null};
                    final String[] colorHolder = {null};
                    final Object   lock        = new Object();

                    rankManager.getRank(uuid, rank -> {
                        rankHolder[0] = rank;
                        rankManager.getColorPreference(rank, color -> {
                            colorHolder[0] = color;
                            synchronized (lock) { lock.notifyAll(); }
                        });
                    });


                    synchronized (lock) {
                        try { lock.wait(2_000); } catch (InterruptedException ignored) {}
                    }

                    if (rankHolder[0] == null || colorHolder[0] == null) continue;

                    String coloredRank = CC.translate(colorHolder[0]) + rankHolder[0];

                    ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
                    SkullMeta meta  = (SkullMeta) skull.getItemMeta();
                    if (meta == null) continue;

                    meta.setOwner(playerName);
                    meta.setDisplayName(CC.translate("&b") + playerName);
                    meta.setLore(Arrays.asList(
                            CC.translate("&7Server: &e" + server),
                            CC.translate("&7Rank: ")     + coloredRank
                    ));
                    skull.setItemMeta(meta);
                    loaded.add(skull);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("StaffListMenu: Redis error — " + e.getMessage());
            }


            Bukkit.getScheduler().runTask(plugin, () -> {
                skulls.clear();
                skulls.addAll(loaded);
                if (viewer.isOnline()) refresh(viewer);
            });
        });
    }
}