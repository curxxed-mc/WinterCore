package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

@CommandInfo(
        name = "fix",
        description = "Repair the item in your hand or all items in your inventory.",
        usage = "/fix [all]",
        inGameOnly = true,
        permission = {"wintercore.fix", "WinterCore.fix"}
)
public class FixCommand extends BaseCommand {

    private final WinterCore plugin;

    public FixCommand(WinterCore plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();
        String[] args = commandArgs.getArgs();
        if (player == null) {
            commandArgs.getSender().sendMessage(CC.RED + "Only players can use this command.");
            return;
        }
        PlayerInventory inventory = player.getInventory();
        if (args.length == 0) {
            ItemStack itemInHand = player.getInventory().getItemInHand();
            if (itemInHand != null && itemInHand.getType() != Material.AIR) {
                itemInHand.setDurability((short) 0);
                player.sendMessage(CC.GREEN + "Your item has been repaired!");
            } else {
                player.sendMessage(CC.RED + "You are not holding an item that can be repaired.");
            }
        } else if (args.length == 1 && args[0].equalsIgnoreCase("all")) {
            for (ItemStack item : inventory.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    item.setDurability((short) 0);
                }
            }
            for (ItemStack item : inventory.getArmorContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    item.setDurability((short) 0);
                }
            }
            player.sendMessage(CC.GREEN + "All items in your inventory and armor have been repaired!");
        } else {
            player.sendMessage(CC.RED + "Usage: /fix [all]");
        }
    }
}





