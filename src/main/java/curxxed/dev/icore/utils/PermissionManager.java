package curxxed.dev.icore.utils;

import curxxed.dev.icore.iCore;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.*;
import java.util.function.Consumer;

public class PermissionManager {

    private final iCore plugin;
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public PermissionManager(iCore plugin) {
        this.plugin = plugin;
    }

    // ✅ Load and apply permissions for a player from the database
    public void loadAndApplyPermissions(Player player) {
        // Clear existing permissions
        clearPermissions(player);

        // Load individual permissions from the database
        plugin.getDatabaseManager().getPlayerPermissions(player.getUniqueId(), permissions -> {
            PermissionAttachment attachment = getOrCreateAttachment(player);
            for (String perm : permissions) {
                attachment.setPermission(perm, true);
            }
        });

        // Load rank-based permissions
        plugin.getRankManager().getRank(player, rank -> {
            List<String> rankPermissions = plugin.getConfig().getStringList("permissions." + rank);
            PermissionAttachment attachment = getOrCreateAttachment(player);
            for (String perm : rankPermissions) {
                attachment.setPermission(perm, true);
            }
        });
    }

    // ✅ Add a permission to a player and persist it in the database
    public void addPermission(Player player, String permission) {
        PermissionAttachment attachment = getOrCreateAttachment(player);
        attachment.setPermission(permission, true);
        plugin.getDatabaseManager().addPermission(player.getUniqueId(), permission);
    }

    // ✅ Remove a permission from a player and update the database
    public void removePermission(Player player, String permission) {
        PermissionAttachment attachment = getOrCreateAttachment(player);
        attachment.unsetPermission(permission);
        plugin.getDatabaseManager().removePermission(player.getUniqueId(), permission);
    }

    // ✅ Check if a player has a specific permission
    public void playerHasPermission(Player player, String permission, Consumer<Boolean> callback) {
        plugin.getRankManager().getRank(player, rank -> {
            List<String> rankPermissions = plugin.getConfig().getStringList("permissions." + rank);

            if (rankPermissions.contains(permission)) {
                callback.accept(true);
                return;
            }

            for (String perm : rankPermissions) {
                List<String> subsetPermissions = plugin.getConfig().getStringList("permissions-subsets." + perm);
                if (subsetPermissions != null && subsetPermissions.contains(permission)) {
                    callback.accept(true);
                    return;
                }
            }

            callback.accept(false);
        });
    }

    // ✅ Get all permissions for a player, including subsets
    public void getAllPermissions(Player player, Consumer<List<String>> callback) {
        plugin.getRankManager().getRank(player, rank -> {
            List<String> rankPermissions = plugin.getConfig().getStringList("permissions." + rank);
            List<String> allPermissions = new ArrayList<>(rankPermissions);

            for (String perm : rankPermissions) {
                List<String> subsetPermissions = plugin.getConfig().getStringList("permissions-subsets." + perm);
                if (subsetPermissions != null) {
                    allPermissions.addAll(subsetPermissions);
                }
            }

            callback.accept(allPermissions);
        });
    }

    // ✅ Reapply permissions for a player (e.g., after rank change)
    public void reapplyPermissions(Player player) {
        clearPermissions(player);
        loadAndApplyPermissions(player);
    }

    // ✅ Clear all permissions for a player
    public void clearPermissions(Player player) {
        PermissionAttachment attachment = attachments.remove(player.getUniqueId());
        if (attachment != null) {
            attachment.remove();
        }
    }

    // ✅ Get or create a PermissionAttachment for a player
    PermissionAttachment getOrCreateAttachment(Player player) {
        return attachments.computeIfAbsent(player.getUniqueId(), uuid -> player.addAttachment(plugin));
    }

    // ✅ Handle player logout to clean up attachments
    public void handlePlayerLogout(Player player) {
        clearPermissions(player);
    }
}