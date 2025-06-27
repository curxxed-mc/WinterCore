package net.curxxed.dev.wintercore.client;

import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.CommandArgs;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientBrandCommand extends BaseCommand {
    public static final Set<UUID> silenced = ConcurrentHashMap.newKeySet();

    private final WinterCore plugin;

    public ClientBrandCommand(WinterCore plugin) {
        this.plugin = plugin;
    }

    @Command(name = "clientbrand", permission = "wintercore.clientbrand", inGameOnly = true, description = "Toggle client join messages.")
    @Override
    public void onCommand(CommandArgs commandArgs) {
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
