package curxxed.dev.icore.Commands.Utility;

import curxxed.dev.icore.utils.NMSUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public class TPSCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        double[] tps = NMSUtils.getTPS(); // Use NMSUtils to get TPS
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long allocatedMemory = runtime.totalMemory() / (1024 * 1024);

        sender.sendMessage(ChatColor.AQUA + "Server Performance:");
        sender.sendMessage(ChatColor.GOLD + "TPS: "
                + getTPSColor(tps[0]) + formatTPS(tps[0]) + ChatColor.GRAY + " (1m) "
                + getTPSColor(tps[1]) + formatTPS(tps[1]) + ChatColor.GRAY + " (5m) "
                + getTPSColor(tps[2]) + formatTPS(tps[2]) + ChatColor.GRAY + " (15m)");
        sender.sendMessage(ChatColor.YELLOW + "Memory Usage: " + ChatColor.GREEN + usedMemory + "MB " + ChatColor.GRAY + "/ " + ChatColor.GREEN + allocatedMemory + "MB");
        sender.sendMessage(ChatColor.YELLOW + "CPU Usage: " + ChatColor.GREEN + String.format("%.2f", getCPUUsage()) + "%");

        return true;
    }

    private double getCPUUsage() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osBean).getSystemCpuLoad() * 100;
        }
        return 0.0;
    }

    private String formatTPS(double tps) {
        return String.format("%.2f", Math.min(tps, 20.0));
    }

    private ChatColor getTPSColor(double tps) {
        if (tps < 5) {
            return ChatColor.DARK_RED;
        } else if (tps < 10) {
            return ChatColor.RED;
        } else if (tps < 15) {
            return ChatColor.GOLD;
        } else if (tps < 18) {
            return ChatColor.YELLOW;
        } else {
            return ChatColor.GREEN;
        }
    }
}