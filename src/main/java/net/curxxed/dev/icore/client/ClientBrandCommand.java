package net.curxxed.dev.icore.client;

import net.curxxed.dev.icore.plugin.iCore;
import net.curxxed.dev.icore.utils.CC;
import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.CommandArgs;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientBrandCommand extends BaseCommand {
    public static final Set<UUID> silenced = ConcurrentHashMap.newKeySet();

    private final iCore plugin;

    public ClientBrandCommand(iCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onCommand(CommandArgs commandArgs) {

        Player player = (Player) commandArgs;
        UUID uuid = player.getUniqueId();
        if (!commandArgs.isPlayer()) {
            commandArgs.getSender().sendMessage(CC.translate("&cThis command can only be used by players."));
           return;
        }
        if (silenced.contains(uuid)) {
            silenced.remove(uuid);
            player.sendMessage(CC.translate("&aClient join messages enabled."));
        } else {
            silenced.add(uuid);
            player.sendMessage(CC.translate("&cClient join messages silenced."));
        }
    }
}

