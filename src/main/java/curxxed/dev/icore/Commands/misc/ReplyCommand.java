/*package hyp.ilfov.i.icore.Commands;

import curxxed.dev.icore.Main;
import listeners.curxxed.dev.icore.PlayerListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReplyCommand implements CommandExecutor {

    private final Main plugin;
    private final PlayerListener playerListener;

    public ReplyCommand(Main plugin) {
        this.plugin = plugin;
        this.playerListener = plugin.getPlayerListener();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can send private messages.");
            return false;
        }

        Player player = (Player) sender;

        // Check if the player has a last sender (last private message)
        if (!plugin.getPlayerDataManager().hasLastSender(player)) {
            player.sendMessage("You have no one to reply to.");
            return false;
        }

        Player lastSender = plugin.getPlayerDataManager().getLastSender(player);

        if (args.length < 1) {
            player.sendMessage("Usage: /r <message>");
            return false;
        }

        String message = String.join(" ", args);

        // Send the reply
        playerListener.sendPrivateMessage(player, lastSender, message);

        return true;
    }
}*/
