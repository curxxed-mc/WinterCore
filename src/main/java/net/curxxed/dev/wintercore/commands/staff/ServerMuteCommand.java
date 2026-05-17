package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.chat.ChatListener;
import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;

@CommandInfo(
        name = "servermute",
        description = "Mute a player.",
        usage = "/servermute",
        inGameOnly = true,
        permission = {"wintercore.servermute", "WinterCore.servermute"}
)
public class ServerMuteCommand extends BaseCommand {

    public ServerMuteCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments args) {
        final ChatListener chatListener = plugin.getChatListener();
        if (chatListener == null) {
            send(args.getPlayer(), "errors.chat-listener-unavailable",
                    "&cChat listener is not available. Please try again later.");
            return;
        }
        boolean isMuted = chatListener.isChatMuted();
        chatListener.setChatMuted(!isMuted);
        String stateText = !isMuted ? "enabled" : "disabled";
        send(args.getPlayer(), "set-server-chat-mute",
                "&aSuccessfully set server chat mute state to: {state}",
                "{state}", stateText);
    }
}
