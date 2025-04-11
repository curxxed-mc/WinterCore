package hyp.ilfov.i.icore.Commands.Utility;

import hyp.ilfov.i.icore.Main;
import hyp.ilfov.i.icore.listeners.PlayerListener;
import hyp.ilfov.i.icore.utils.RankManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReportCommand implements CommandExecutor {
    private final Main plugin;
    private final RankManager rankManager;

    public ReportCommand(Main plugin) {
        this.plugin = plugin;
        this.rankManager = new RankManager(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player reporter = (Player) sender;

        if (args.length < 2) {
            reporter.sendMessage(ChatColor.RED + "Usage: /report <player> <reason>");
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null || !target.isOnline()) {
            reporter.sendMessage(ChatColor.RED + "Player not found or not online.");
            return true;
        }

        if (target.equals(reporter)) {
            reporter.sendMessage(ChatColor.RED + "You cannot report yourself!");
            return true;
        }

        String reason = String.join(" ", args).substring(args[0].length()).trim();

        // Message to the reporter
        reporter.sendMessage(ChatColor.GREEN + "You have reported " + ChatColor.WHITE + target.getName() +
                ChatColor.GREEN + " for: " + ChatColor.WHITE + reason);

        // Notify staff using the ChatListener method
        PlayerListener playerListener = new PlayerListener(plugin);
        playerListener.notifyStaff(reporter, target, reason); // Call the method to notify staff

        return true;
    }
}
