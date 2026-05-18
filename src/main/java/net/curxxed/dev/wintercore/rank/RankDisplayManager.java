package net.curxxed.dev.wintercore.rank;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.curxxed.dev.wintercore.config.RankConfigManager;
import net.curxxed.dev.wintercore.permissions.WinterCorePermissible;
import net.curxxed.dev.wintercore.permissions.WinterCorePermissibleInjector;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.entity.Player;

import java.util.Collection;

public class RankDisplayManager {

    private final WinterCore plugin;
    private final RankConfigManager config;

    public RankDisplayManager(WinterCore plugin, RankConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void applyRank(Player player, String rankName, String color) {
        applyPermissions(player, rankName);
        applyVisuals(player, color);
        applyNameTag(player, rankName);
    }

    public void refreshDisplay(Player player, String rankName, String color) {
        applyRank(player, rankName, color);
    }

    public void refreshDisplayForAll(Player target, String rankName, String color) {
        plugin.getTasks().sync(() -> applyRank(target, rankName, color));
    }

    public void applyNameTag(Player player, String rankName) {
        if (plugin.getNameTagColorManager() == null) return;

        if (config.isShowRankAboveHead()) {
            plugin.getNameTagColorManager().applyRank(player, rankName);
        } else {
            plugin.getNameTagColorManager().refresh(player);
        }
    }

    public void applyNameTagColor(Player player, String color) {
        if (plugin.getNameTagColorManager() == null) return;
        plugin.getNameTagColorManager().applyColor(player, color);
    }

    public void refreshNameTag(Player player) {
        if (plugin.getNameTagColorManager() != null) {
            plugin.getNameTagColorManager().refresh(player);
        }
    }

    public void sendRankUpdateToBungee(String playerName, String rankName) {
        Collection<? extends Player> online = net.curxxed.dev.wintercore.utils.Utilities.getOnlinePlayers();
        if (online.isEmpty()) return;

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF("SyncRank");
        out.writeUTF(playerName);
        out.writeUTF(rankName);

        online.iterator().next().sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    private void applyPermissions(Player player, String rankName) {
        try {
            Object current = WinterCorePermissibleInjector.HUMAN_ENTITY_PERMISSIBLE_FIELD.get(player);
            if (!(current instanceof WinterCorePermissible)) {
                WinterCorePermissibleInjector.initPlayer(player);
                current = WinterCorePermissibleInjector.HUMAN_ENTITY_PERMISSIBLE_FIELD.get(player);
            }
            if (!(current instanceof WinterCorePermissible)) {
                plugin.getLogger().warning("Failed to resolve custom permissible for " + player.getName());
                return;
            }

            WinterCorePermissible permissible = (WinterCorePermissible) current;
            permissible.clearRawPermissions();
            for (String permission : config.getPermissionsForRank(rankName)) {
                permissible.addRawPermission(permission, true);
            }
            if (plugin.getPermissionConfigManager() != null) {
                plugin.getPermissionConfigManager().applyOverrides(player.getUniqueId(), permissible);
            }
            permissible.recalculatePermissions();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to update permissions for " + player.getName() + ": " + e.getMessage());
        }
    }
    private void applyVisuals(Player player, String color) {
        String visibleName = player.getName();
        if (plugin.getNameTagColorManager() != null) {
            visibleName = plugin.getNameTagColorManager().getVisibleName(player);
        }

        String coloredName = CC.translate(color) + visibleName + CC.translate("&r");

        plugin.getTasks().sync(() -> player.setDisplayName(coloredName));
    }
}
