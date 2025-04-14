package curxxed.dev.icore.utils.Staff;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class StaffData {
    private final UUID uuid;
    private final String name;
    private final String server;

    public StaffData(UUID uuid, String name, String server) {
        this.uuid = uuid;
        this.name = name;
        this.server = server;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getServer() {
        return server;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public boolean isOnline() {
        return getPlayer() != null && getPlayer().isOnline();
    }
}
