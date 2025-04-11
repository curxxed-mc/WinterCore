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

    public void playerHasPermission(Player player, String permission, Consumer<Boolean> callback) {
        // Fetch the rank asynchronously
        plugin.getRankManager().getRank(player, rank -> {
            // Get the list of permissions for this rank
            List<String> rankPermissions = plugin.getConfig().getStringList("permissions." + rank);

            // First, check if the player has the direct permission
            if (rankPermissions.contains(permission)) {
                callback.accept(true);
                return;
            }

            // Now check if the player has a parent permission that includes subsets
            for (String perm : rankPermissions) {
                // If the permission is a parent permission, check if it has a subset
                List<String> subsetPermissions = plugin.getConfig().getStringList("permissions-subsets." + perm);
                if (subsetPermissions != null && subsetPermissions.contains(permission)) {
                    callback.accept(true);
                    return;
                }
            }

            callback.accept(false);  // If no permission found
        });
    }

    public void getAllPermissions(Player player, Consumer<List<String>> callback) {
        // Fetch the rank asynchronously
        plugin.getRankManager().getRank(player, rank -> {
            // Get the list of permissions for this rank
            List<String> rankPermissions = plugin.getConfig().getStringList("permissions." + rank);

            // Create a list to store all permissions (including subsets)
            List<String> allPermissions = new ArrayList<>(rankPermissions);

            // Add subset permissions for each parent permission
            for (String perm : rankPermissions) {
                List<String> subsetPermissions = plugin.getConfig().getStringList("permissions-subsets." + perm);
                if (subsetPermissions != null) {
                    allPermissions.addAll(subsetPermissions);  // Add all subset permissions
                }
            }

            callback.accept(allPermissions);  // Return the result asynchronously
        });
    }
}
