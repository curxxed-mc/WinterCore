package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

@CommandInfo(
        name = "thru",
        description = "Teleport through the block you are facing.",
        usage = "/thru",
        inGameOnly = true,
        permission = {"wintercore.thru", "WinterCore.thru"}
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
                    send(player, "thru.success", "&aThere you go.");
                } else {
                    send(player, "thru.blocked", "&cThe other side is blocked!");
                }
                return;
            }
        }

        send(player, "thru.no-block", "&cNo block found in front of you.");
    }
    private boolean isSafeLocation(Vector position, Player player) {
        Block block = position.toLocation(player.getWorld()).getBlock();
        Block aboveBlock = position.toLocation(player.getWorld()).add(0, 1, 0).getBlock();
        return !block.getType().isSolid() && !aboveBlock.getType().isSolid();
    }
}
