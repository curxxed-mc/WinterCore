package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

@CommandInfo(
        name = "invsee",
        description = "View another player's inventory.",
        usage = "/invsee <player>",
        inGameOnly = true,
        permission = {"wintercore.invsee"}
)
public class InvSeeCommand extends BaseCommand {

    public InvSeeCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();

        if (commandArgs.length() != 1) {
            player.sendMessage(CC.RED + "Usage: /invsee <player>");
            return;
        }

        Player target = Bukkit.getPlayer(commandArgs.getArgs()[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(CC.RED + "Player not found or offline.");
            return;
        }

        openInventory(player, target);
    }

    private void openInventory(Player viewer, Player target) {
        Inventory targetInv = target.getInventory();
        viewer.openInventory(targetInv);
        viewer.sendMessage(CC.GREEN + "You are now viewing " + CC.YELLOW + target.getName() + "'s inventory.");
    }
}




