package hyp.ilfov.i.icore.Commands.misc;

import hyp.ilfov.i.icore.Main;
import hyp.ilfov.i.icore.listeners.PlayerListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.*;

public class MessageCommand implements CommandExecutor {

    private final Main plugin;
    private final PlayerListener playerListener;

    public MessageCommand(Main plugin) {
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

        if (args.length < 2) {
            player.sendMessage("Usage: /msg <player> <message>");
            return false;
        }

        Player recipient = plugin.getServer().getPlayer(args[0]);
        if (recipient == null) {
            player.sendMessage("Player not found.");
            return false;
        }

        // Create the message by joining the args array starting from index 1
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        // Send the private message
        playerListener.sendPrivateMessage(player, recipient, message);

        return true;
    }
}
