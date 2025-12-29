package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

@CommandInfo(
        name = "thru",
            permission = "WinterCore.thru",
            description = "Teleport through the block you are facing.",
            usage = "/thru",
            inGameOnly = true
    
    )
public class ThruCommand extends BaseCommand {

    public ThruCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
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