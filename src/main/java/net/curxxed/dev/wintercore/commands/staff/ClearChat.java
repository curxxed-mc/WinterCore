package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "clearchat",
        description = "Clear the chat for all players.",
        usage = "/clearchat",
        inGameOnly = false,
        permission = {"wintercore.clearchat", "WinterCore.clearchat"}
)
public class ClearChat extends BaseCommand {

    public ClearChat(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < 200; i++) {
                player.sendMessage("");
            }
        }
        send(commandArgs.getSender(), "moderation.clear-chat.success",
                "&aChat has been cleared.");
    }
}




