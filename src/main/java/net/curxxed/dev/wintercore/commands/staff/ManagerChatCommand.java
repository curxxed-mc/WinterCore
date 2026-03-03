package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.listeners.ChatListener;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "mc",
        permission = "wintercore.manager",
        description = "Send a message to manager chat.",
        usage = "/mc <message>",
        inGameOnly = true
)
public class ManagerChatCommand extends BaseCommand {

    private final WinterCore plugin;
    private final ChatListener chatListener;

    public ManagerChatCommand(WinterCore plugin, ChatListener chatListener) {
        super(plugin);
        this.plugin = plugin;
        this.chatListener = chatListener;
    }

    @Override
    public void execute(CommandArguments args) {
        Player player = args.getPlayer();
        if (args.length() == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /mc <message>");
            return;
        }
        String message = String.join(" ", args.getArgs());
        chatListener.buildManagerChatMessage(player, message, plugin.getRedisManager()::broadcastManagerMessage);
    }
}