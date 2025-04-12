package hyp.ilfov.i.icore.Commands.Staff.StaffList;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import hyp.ilfov.i.icore.Database.RedisManager;
import hyp.ilfov.i.icore.Main;
import hyp.ilfov.i.icore.utils.Staff.StaffData;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class StaffRedisHandler {

    private final Main plugin;
    private final RedisManager redisManager;
    private final Map<UUID, StaffData> staffMap;

    public StaffRedisHandler(Main plugin, RedisManager redisManager, Map<UUID, StaffData> staffMap) {
        this.plugin = plugin;
        this.redisManager = redisManager;
        this.staffMap = staffMap;
    }

    public void onMessage(String channel, String message) {
        if (!channel.equals("staff:update")) return;

        JsonObject json = new Gson().fromJson(message, JsonObject.class);
        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        boolean isStaff = json.get("staff").getAsBoolean();

        if (!isStaff) {
            staffMap.remove(uuid);
            return;
        }

        String name = json.get("name").getAsString();
        String server = json.get("server").getAsString();

        staffMap.put(uuid, new StaffData(uuid, name, server));
    }

    public void publishJoin(Player player) {
        publishStaffUpdate(player, true);
    }

    public void publishQuit(Player player) {
        publishStaffUpdate(player, false);
    }

    public void publishServerSwitch(Player player) {
        publishStaffUpdate(player, player.hasPermission("iCore.staff") || player.isOp());
    }

    private void publishStaffUpdate(Player player, boolean isStaff) {
        String serverName = plugin.getConfig().getString("server-name", "hub-restricted");
        JsonObject json = new JsonObject();
        json.addProperty("uuid", player.getUniqueId().toString());
        json.addProperty("name", player.getName());
        json.addProperty("server", serverName); // Use the serverName field directly
        json.addProperty("staff", isStaff);

        redisManager.publishStaffActivity("staff:update", json); // Pass the JsonObject instead of string
    }
}
