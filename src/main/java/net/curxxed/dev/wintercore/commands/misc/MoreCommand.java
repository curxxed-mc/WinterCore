package net.curxxed.dev.wintercore.commands.misc;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MoreCommand extends BaseCommand {

    private final WinterCore plugin;

    public MoreCommand(WinterCore plugin) {
        this.plugin = plugin;
    }

    @Command(
        name = "more",
        permission = "WinterCore.more",
        description = "Get a stack of the item in your hand.",
        usage = "/more",
        inGameOnly = true
    )
    public void onCommand(CommandArgs commandArgs) {
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
