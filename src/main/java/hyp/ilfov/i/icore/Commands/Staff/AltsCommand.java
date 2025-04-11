/*package hyp.ilfov.i.icore.Commands;

import hyp.ilfov.i.icore.utils.AltManager;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AltsCommand implements CommandExecutor {
    private final AltManager altManager;

    public AltsCommand(AltManager altManager) {
        this.altManager = altManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("iCore.alts")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /alts <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || target.getName() == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        List<UUID> alts = altManager.getAlts(target.getUniqueId());

        if (alts.isEmpty()) {
            sender.sendMessage(ChatColor.GREEN + target.getName() + " has no detected alts.");
            return true;
        }

        String altList = alts.stream()
                .map(uuid -> formatAltName(uuid))
                .collect(Collectors.joining(", "));

        sender.sendMessage(ChatColor.YELLOW + "Alts of " + target.getName() + ": " + altList);
        return true;
    }

    private String formatAltName(UUID uuid) {
        OfflinePlayer altPlayer = Bukkit.getOfflinePlayer(uuid);
        ChatColor color;

        if (altPlayer.isOnline()) {
            color = ChatColor.GREEN; // Online
        } else if (Bukkit.getBanList(BanList.Type.NAME).isBanned(altPlayer.getName())) {
            color = ChatColor.RED; // Banned
        } else {
            color = ChatColor.GRAY; // Offline
        }

        return color + altPlayer.getName();
    }
}*/
