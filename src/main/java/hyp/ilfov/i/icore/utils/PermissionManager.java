package hyp.ilfov.i.icore.utils;

import hyp.ilfov.i.icore.Main;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PermissionManager {

    private final Main plugin;

    public PermissionManager(Main plugin) {
        this.plugin = plugin;
    }

    // ✅ Apply database permissions to the player (used in PlayerListener)
    public void loadAndApplyPermissions(Player player) {
        plugin.getDatabaseManager().getPlayerPermissions(player.getUniqueId(), permissions -> {
            for (String perm : permissions) {
                player.addAttachment(plugin, perm, true);
            }
        });
    }

    // ✅ Check if player has specific permission
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

    // ✅ Get all player permissions (including subsets)
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
}
