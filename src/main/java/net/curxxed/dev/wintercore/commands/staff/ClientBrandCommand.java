package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CommandInfo(name = "clientbrand", permission = "wintercore.clientbrand", inGameOnly = true, description = "Toggle client join messages.")
public class ClientBrandCommand extends BaseCommand {
    public static final Set<UUID> silenced = ConcurrentHashMap.newKeySet();

    private final WinterCore plugin;

    public ClientBrandCommand(WinterCore plugin) {
        super(plugin);
        this.plugin = plugin;
    }


    @Override
    public void execute(CommandArguments commandArgs) {
        if (!commandArgs.isPlayer()) {
            send(commandArgs.getSender(), "client-brand.player-only", "&cThis command can only be used by players.");
            return;
        }
        Player player = commandArgs.getPlayer();
        UUID uuid = player.getUniqueId();
        if (silenced.contains(uuid)) {
            silenced.remove(uuid);
            send(player, "client-brand.enabled", "&aClient join messages enabled.");
        } else {
            silenced.add(uuid);
            send(player, "client-brand.disabled", "&cClient join messages disabled.");
        }
    }
}
