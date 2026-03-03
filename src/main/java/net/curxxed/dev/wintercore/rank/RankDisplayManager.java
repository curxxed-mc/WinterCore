package net.curxxed.dev.wintercore.rank;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.curxxed.dev.wintercore.nametags.NameTag;
import net.curxxed.dev.wintercore.nametags.NameTagAdapter;
import net.curxxed.dev.wintercore.permissions.WinterCorePermissible;
import net.curxxed.dev.wintercore.permissions.WinterCorePermissibleInjector;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collection;

public class RankDisplayManager {

    private final WinterCore plugin;
    private final RankConfigManager config;
    private final NameTagAdapter nameTagAdapter;

    public RankDisplayManager(WinterCore plugin, RankConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.nameTagAdapter = new NameTag();
    }

    public void applyRank(Player player, String rank, String color) {
        applyPermissions(player, rank);
        applyVisuals(player, color);
        applyNameTag(player, color);
    }

    public void refreshDisplay(Player player, String rank, String color) {
        applyRank(player, rank, color);
    }

    public void refreshDisplayForAll(Player target, String rank, String color) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            applyRank(target, rank, color);
            if (plugin.getNameTagHandler() != null) {
                plugin.getNameTagHandler().getNameTagAdapter().setNameTag(target, color);
            }
        });
    }

    public void applyNameTag(Player player, String color) {
        if (config.isShowRankAboveHead()) {
            nameTagAdapter.setNameTag(player, color);
        } else {
            nameTagAdapter.resetNameTag(player);
        }
    }

    public void resetNameTag(Player player) {
        nameTagAdapter.resetNameTag(player);
    }

    public void sendRankUpdateToBungee(String playerName, String rank) {
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        if (online.isEmpty()) return;

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF("SyncRank");
        out.writeUTF(playerName);
        out.writeUTF(rank);

        online.iterator().next().sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    private void applyPermissions(Player player, String rank) {
        try {
            WinterCorePermissible permissible = (WinterCorePermissible)
                    WinterCorePermissibleInjector.HUMAN_ENTITY_PERMISSIBLE_FIELD.get(player);
            permissible.clearRawPermissions();
            for (String permission : config.getPermissionsForRank(rank)) {
                permissible.addRawPermission(permission, true);
            }
            permissible.recalculatePermissions();
        } catch (IllegalAccessException e) {
            plugin.getLogger().severe("Failed to update permissions for " + player.getName() + ": " + e.getMessage());
        }
    }

    private void applyVisuals(Player player, String color) {
        String coloredName = CC.translate(color) + player.getName() + ChatColor.RESET;
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.setDisplayName(coloredName);
            safeSetPlayerListName(player, coloredName);
            player.setCustomName(coloredName);
            player.setCustomNameVisible(true);
        });
    }

    private static void safeSetPlayerListName(Player player, String name) {
        if (name != null && name.length() <= 16) {
            player.setPlayerListName(name);
        } else {
            player.setPlayerListName(player.getName());
        }
    }
}