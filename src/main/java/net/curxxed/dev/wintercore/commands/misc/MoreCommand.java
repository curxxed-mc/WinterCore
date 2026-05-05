package net.curxxed.dev.wintercore.commands.misc;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CommandInfo(
        name = "more",
        description = "Get a stack of the item in your hand.",
        usage = "/more",
        inGameOnly = true,
        permission = {"wintercore.more", "WinterCore.more"}
)
public class MoreCommand extends BaseCommand {

    public MoreCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();
        if (player == null) {
            commandArgs.getSender().sendMessage(CC.RED + "Only players can use this command.");
            return;
        }
        ItemStack itemInHand = player.getItemInHand();
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            player.sendMessage(CC.RED + "You are not holding any item.");
            return;
        }
        ItemStack newItemStack = itemInHand.clone();
        newItemStack.setAmount(64);
        player.getInventory().addItem(newItemStack);
        player.sendMessage(CC.GREEN + "You have received a stack of " + itemInHand.getType().name() + "!");
    }
}



