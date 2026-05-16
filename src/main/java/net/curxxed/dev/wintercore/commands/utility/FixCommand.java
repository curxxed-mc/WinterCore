package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
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

    public FixCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();
        String[] args = commandArgs.getArgs();
        if (player == null) {
            send(commandArgs.getSender(), "general.in-game-only-command", "&cOnly players can use this command.");
            return;
        }
        PlayerInventory inventory = player.getInventory();
        if (args.length == 0) {
            ItemStack itemInHand = player.getInventory().getItemInHand();
            if (itemInHand != null && itemInHand.getType() != Material.AIR) {
                itemInHand.setDurability((short) 0);
                send(player, "fix.item-success", "&aYour item has been repaired!");
            } else {
                send(player, "fix.no-repairable-item", "&cYou are not holding an item that can be repaired.");
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
            send(player, "fix.all-success", "&aAll items in your inventory and armor have been repaired!");
        } else {
            sendUsage(player);
        }
    }
}
