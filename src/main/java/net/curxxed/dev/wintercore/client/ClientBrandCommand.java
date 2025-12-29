package net.curxxed.dev.wintercore.client;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
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
            commandArgs.getSender().sendMessage(CC.translate("&cThis command can only be used by players."));
            return;
        }
        Player player = commandArgs.getPlayer();
        UUID uuid = player.getUniqueId();
        if (silenced.contains(uuid)) {
            silenced.remove(uuid);
            player.sendMessage(CC.translate("&aClient join messages enabled."));
        } else {
            silenced.add(uuid);
            player.sendMessage(CC.translate("&cClient join messages disabled."));
        }
    }
}
