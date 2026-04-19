package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.chat.StaffChatService;
import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "staffchat",
        aliases = {"sc", "ac", "mc"},
        description = "Send a message to a staff channel.",
        usage = "/sc|ac|mc <message>",
        inGameOnly = true
)
public class StaffChatCommand extends BaseCommand {

    private final StaffChatService staffChatService;

    public StaffChatCommand(WinterCore plugin, StaffChatService staffChatService) {
        super(plugin);
        this.staffChatService = staffChatService;
    }

    @Override
    public void execute(CommandArguments args) {
        Player player = args.getPlayer();

        if (args.length() == 0) {
            player.sendMessage(CC.translate("&cUsage: /" + args.getLabel() + " <message>"));
            return;
        }

        String message = String.join(" ", args.getArgs());

        switch (args.getLabel().toLowerCase()) {
            case "sc": staffChatService.sendStaffMessage(player, message); break;
            case "ac": staffChatService.sendAdminMessage(player, message); break;
            case "mc": staffChatService.sendManagerMessage(player, message); break;
        }
    }
}