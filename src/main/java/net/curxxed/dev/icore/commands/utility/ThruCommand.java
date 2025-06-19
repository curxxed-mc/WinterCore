package net.curxxed.dev.icore.commands.utility;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.icore.utils.CC;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

public class ThruCommand extends BaseCommand {

    @Command(
            name = "thru",
            permission = "iCore.thru",
            description = "Teleport through the block you are facing.",
            usage = "/thru",
            inGameOnly = true
    )
    public void onCommand(CommandArgs commandArgs) {
        Player player = commandArgs.getPlayer();
        BlockIterator iterator = new BlockIterator(player, 5);
        while (iterator.hasNext()) {
            Block block = iterator.next();

            if (block.getType().isSolid()) {
                Vector direction = player.getLocation().getDirection().normalize();
                Vector newPosition = block.getLocation().toVector().add(direction).add(direction);

                if (isSafeLocation(newPosition, player)) {
                    player.teleport(newPosition.toLocation(player.getWorld()));
                    player.sendMessage(CC.translate("&aThere you go.") );
                } else {
                    player.sendMessage(CC.translate("&cThe other side is blocked!"));
                }
                return;
            }
        }

        player.sendMessage(CC.translate("&cNo block found in front of you."));
    }
    private boolean isSafeLocation(Vector position, Player player) {
        Block block = position.toLocation(player.getWorld()).getBlock();
        Block aboveBlock = position.toLocation(player.getWorld()).add(0, 1, 0).getBlock();
        return !block.getType().isSolid() && !aboveBlock.getType().isSolid();
    }
}