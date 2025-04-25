package curxxed.dev.icore.Commands.Social;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DiscordCommand implements CommandExecutor {
    private final String discordLink = ChatColor.DARK_PURPLE + "discord.gg/icore";
    @Override
    public boolean onCommand(CommandSender sender, Command command, String string, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
            return true;
        }

        Player player = (Player) sender;

        player.sendMessage(ChatColor.AQUA + "Join our discord: " + discordLink);

        return false;
    }
}
