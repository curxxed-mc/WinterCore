/*package hyp.ilfov.i.icore.Commands;

import hyp.ilfov.i.icore.Main;
import hyp.ilfov.i.icore.utils.DisguiseGUI;
import hyp.ilfov.i.icore.utils.RankManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class DisguiseCommand implements CommandExecutor {

    private final Main plugin;
    private final RankManager rankManager;
    private final DisguiseGUI disguiseGUI;


    public DisguiseCommand(Main plugin, DisguiseGUI disguiseGUI, RankManager rankManager) {
        this.rankManager = rankManager;
        this.plugin = plugin;
        this.disguiseGUI = disguiseGUI;

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args.length != 2) {
                player.sendMessage(ChatColor.RED + "Usage: /disguise <name> <skin>");
                return false;
            }

            String disguiseName = args[0];
            String skin = args[1];

            // Check if the player is allowed to disguise as the given name
            if (Bukkit.getPlayer(disguiseName) != null) {
                player.sendMessage(ChatColor.RED + "That player is currently online!");
                return false;
            }

            // Set the disguise details (you'll add more logic to store these in the RankManager later)
            /*rankManager.setTargetDisguise(player, disguiseName, skin);

            // Open the rank selection GUI
            openRankSelectionGUI(player, disguiseName, skin);
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return false;
        }
    }

    private void openRankSelectionGUI(Player player, String disguiseName, String skin) {
        List<String> availableRanks = rankManager.getSortedRanks();

        Inventory gui = Bukkit.createInventory(null, 9, "Select Rank for Disguise");

        for (int i = 0; i < availableRanks.size(); i++) {
            String rank = availableRanks.get(i);
            ItemStack rankItem = createRankItem(rank);
            gui.setItem(i, rankItem);
        }

        // Open the GUI for the player
        player.openInventory(gui);
    }

    private ItemStack createRankItem(String rank) {
        ItemStack item = new ItemStack(org.bukkit.Material.PAPER);  // Example material
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + rank);
        item.setItemMeta(meta);
        return item;
    }
}*/
