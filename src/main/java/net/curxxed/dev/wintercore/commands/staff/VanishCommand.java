package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.staff.VanishService;
import net.curxxed.dev.wintercore.utils.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CommandInfo(
        name = "vanish",
        description = "Toggle vanish mode.",
        usage = "/vanish",
        inGameOnly = true,
        async = true,
        permission = {"wintercore.vanish", "WinterCore.vanish"}
)
public class VanishCommand extends BaseCommand {

    private final VanishService vanishService;

    public VanishCommand(WinterCore plugin) {
        super(plugin);
        this.vanishService = plugin.getVanishService();
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        runSync(() -> {
            Player player = commandArgs.getPlayer();

            vanishService.toggle(player, vanished -> {
                ItemStack dye = player.getInventory().getItem(8);
                if (dye != null) {
                    ItemBuilder builder = new ItemBuilder(dye);
                    builder.setName(msg(
                            vanished ? "vanish.item.unvanish" : "vanish.item.vanish",
                            vanished ? "&7Un-Vanish" : "&aVanish"));
                    player.getInventory().setItem(8, builder.toItemStack());
                }
            });
        });
    }

}
