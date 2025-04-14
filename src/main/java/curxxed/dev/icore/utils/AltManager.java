/*package hyp.ilfov.i.icore.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.*;

public class AltManager implements Listener {
    private final PlayerDataManager playerDataManager;

    public AltManager(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String ip = player.getAddress().getAddress().getHostAddress();
        UUID playerUUID = player.getUniqueId();

        // Load stored alts
        List<String> storedAlts = playerDataManager.getAltsByIP(ip);
        if (!storedAlts.contains(playerUUID.toString())) {
            storedAlts.add(playerUUID.toString());
            playerDataManager.setAltsByIP(ip, storedAlts);
        }

        // Log the player's IP address (optional)
        logPlayerIP(player);
    }

    public List<UUID> getAlts(UUID uuid) {
        FileConfiguration config = playerDataManager.getConfig();
        List<UUID> alts = new ArrayList<>();

        for (String ip : config.getConfigurationSection("alts").getKeys(false)) {
            List<String> storedAlts = config.getStringList("alts." + ip);
            if (storedAlts.contains(uuid.toString())) {
                for (String altUUID : storedAlts) {
                    if (!altUUID.equals(uuid.toString())) {
                        alts.add(UUID.fromString(altUUID));
                    }
                }
            }
        }
        return alts;
    }

    // Method to log player IP address
    public void logPlayerIP(Player player) {
        String ip = player.getAddress().getAddress().getHostAddress();
        // Log the player's IP (this is just an example)
        System.out.println("Player " + player.getName() + " has connected with IP: " + ip);
    }
}*/
