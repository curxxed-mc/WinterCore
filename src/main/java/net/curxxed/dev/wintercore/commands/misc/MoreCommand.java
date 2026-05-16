package net.curxxed.dev.wintercore.commands.misc;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
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
            send(commandArgs.getSender(), "general.in-game-only-command", "&cOnly players can use this command.");
            return;
        }
        ItemStack itemInHand = player.getItemInHand();
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            send(player, "more.no-item", "&cYou are not holding any item.");
            return;
        }
        ItemStack newItemStack = itemInHand.clone();
        newItemStack.setAmount(64);
        player.getInventory().addItem(newItemStack);
        send(player, "more.success", "&aYou have received a stack of {item}!",
                "{item}", itemInHand.getType().name());
    }
}
