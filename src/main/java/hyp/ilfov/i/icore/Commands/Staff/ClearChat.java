package hyp.ilfov.i.icore.Commands.Staff;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClearChat implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("clearchat")) {

            if (!sender.hasPermission("iCore.clearchat")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }

            // Clear the chat for all players
            for (Player player : sender.getServer().getOnlinePlayers()) {
                for (int i = 0; i < 200; i++) {
                    player.sendMessage(""); // Send empty messages to clear the chat
                }
            }

            sender.sendMessage(ChatColor.GREEN + "Chat has been cleared.");
            return true;
        }
        return false;
    }
}