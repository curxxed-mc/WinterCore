package net.curxxed.dev;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private ScoreboardManager scoreboardManager;
    private ConfigHandler configHandler;

    @Override
    public void onEnable() {
        configHandler = new ConfigHandler(this);
        scoreboardManager = new ScoreboardManager(this, configHandler);
        Bukkit.getPluginManager().registerEvents(scoreboardManager, this);

        // Start the periodic scoreboard update task
        scoreboardManager.startScoreboardUpdateTask();

        getLogger().info("Scoreboard Plugin Enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Scoreboard Plugin Disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("reloadscoreboard")) {
            if (sender.hasPermission("scoreboard.reload")) {
                configHandler.reloadConfig();
                scoreboardManager.reload();
                sender.sendMessage("§aScoreboard reloaded!");
            } else {
                sender.sendMessage("§cYou do not have permission!");
            }
            return true;
        }
        return false;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
}
