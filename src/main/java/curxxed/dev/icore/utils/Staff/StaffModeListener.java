package curxxed.dev.icore.utils.Staff;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import curxxed.dev.icore.iCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class StaffModeListener implements Listener {

    private final StaffModeManager staffModeManager;
    private final iCore plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_TIME = 950;

    public StaffModeListener(iCore plugin, StaffModeManager manager) {
        this.plugin = plugin;
        this.staffModeManager = manager;
        setupProtocolLib();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        switch (event.getAction()) {
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                break;
            default:
                return;
        }

        ItemStack item = player.getItemInHand();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String itemName = item.getItemMeta().getDisplayName();
        Player target = staffModeManager.getTargetPlayer(player);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid) && (now - cooldowns.get(uuid)) < COOLDOWN_TIME) return;
        cooldowns.put(uuid, now);

        // delegăm la manager
        staffModeManager.handleItemUse(player, itemName, target, item);
    }

    private void setupProtocolLib() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.PLAYER_INFO) {
            @Override
            public void onPacketSending(PacketEvent event) {
                staffModeManager.modifyPlayerInfoPacket(event);
            }
        });
    }
}
