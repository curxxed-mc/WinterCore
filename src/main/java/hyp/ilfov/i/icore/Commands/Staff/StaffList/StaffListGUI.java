package hyp.ilfov.i.icore.Commands.Staff.StaffList;

import hyp.ilfov.i.icore.Main;
import hyp.ilfov.i.icore.utils.Staff.StaffData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;

public class StaffListGUI implements Listener {

    private final Main plugin;

    public StaffListGUI(Main plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer, List<StaffData> staffList) {
        Inventory gui = Bukkit.createInventory(null, 54, "§eNetwork Staff Online");

        for (StaffData data : staffList) {
            Player staffPlayer = data.getPlayer();

            if (staffPlayer == null || !staffPlayer.isOnline()) {
                continue; // Skip offline staff since RankManager works with Player objects
            }

            ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();

            meta.setOwner(data.getName());

            String rankName = plugin.getRankManager().getRankSync(staffPlayer); // You'll need to add this if not present
            String prefix = plugin.getRankManager().getRankPrefixSync(staffPlayer);

            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', prefix + data.getName()));
            meta.setLore(Arrays.asList(
                    ChatColor.RESET + "Server: " + ChatColor.GREEN + data.getServer(),
                    ChatColor.RESET + "Rank: " + ChatColor.AQUA + rankName
            ));

            skull.setItemMeta(meta);
            gui.addItem(skull);
        }

        viewer.openInventory(gui);
    }
}
