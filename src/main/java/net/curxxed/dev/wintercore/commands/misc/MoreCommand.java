package net.curxxed.dev.wintercore.commands.misc;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CommandInfo(
        name = "more",
        permission = "WinterCore.more",
        description = "Get a stack of the item in your hand.",
        usage = "/more",
        inGameOnly = true
)
public class MoreCommand extends BaseCommand {

    public MoreCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();
        if (player == null) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "Only players can use this command.");
            return;
        }
        if (!player.hasPermission("WinterCore.more")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return;
        }
        ItemStack itemInHand = player.getItemInHand();
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You are not holding any item.");
            return;
        }
        ItemStack newItemStack = itemInHand.clone();
        newItemStack.setAmount(64);
        player.getInventory().addItem(newItemStack);
        player.sendMessage(ChatColor.GREEN + "You have received a stack of " + itemInHand.getType().name() + "!");
    }
}
