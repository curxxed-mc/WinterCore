package hyp.ilfov.i.icore.utils;

import hyp.ilfov.i.icore.Main;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import lombok.var;

public class PunishmentManager implements Listener {
    private final File punishmentFile;
    private final FileConfiguration punishmentConfig;
    private final Main plugin;

    public PunishmentManager(Main plugin) {
        this.plugin = plugin;
        punishmentFile = new File(plugin.getDataFolder(), "punishmentdata.yml");
        if (!punishmentFile.exists()) {
            try {
                punishmentFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        punishmentConfig = YamlConfiguration.loadConfiguration(punishmentFile);
    }

    // Add punishment data
    public void addPunishment(String playerName, String type, String reason, String issuer, String duration) {
        String path = "punishments." + playerName + "." + type;
        var list = punishmentConfig.getMapList(path);
        if (list == null) {
            list = new ArrayList<>();
        }
        var punishment = new HashMap<String, String>();
        punishment.put("reason", reason);
        punishment.put("date", LocalDate.now().toString());
        punishment.put("issuer", issuer);
        if (duration != null) {
            punishment.put("duration", duration);
            punishment.put("expirationTime", Instant.now().plusMillis(parseDurationToMillis(duration)).toString());
        }
        list.add(punishment);
        punishmentConfig.set(path, list);
        save();
    }

    // Check if a player is muted
    public boolean isPlayerMuted(String playerName) {
        String path = "punishments." + playerName + ".mute";
        return punishmentConfig.contains(path);
    }

    // Get mute data for player
    public Map<String, Object> getMuteData(String playerName) {
        String path = "punishments." + playerName + ".mute";
        if (!punishmentConfig.contains(path)) {
            return null;
        }
        return punishmentConfig.getConfigurationSection(path).getValues(true);
    }

    // Parse duration (in minutes, hours, or days) into milliseconds
    private long parseDurationToMillis(String duration) {
        long multiplier = 1000;
        if (duration.endsWith("m")) multiplier *= 60;
        else if (duration.endsWith("h")) multiplier *= 60 * 60;
        else if (duration.endsWith("d")) multiplier *= 60 * 60 * 24;

        return Long.parseLong(duration.replaceAll("[^0-9]", "")) * multiplier;
    }

    // Save the punishment data to file
    public void save() {
        try {
            punishmentConfig.save(punishmentFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Mute listener for chat events
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        String playerName = e.getPlayer().getName();

        // Access the PunishmentManager and check if the player is muted
        if (isPlayerMuted(playerName)) {
            Map<?, ?> muteData = getMuteData(playerName);
            String reason = (String) muteData.get("reason");
            e.getPlayer().sendMessage(ChatColor.RED + "You are muted. Reason: " + reason);
            e.setCancelled(true); // Prevent the player from chatting
        }
    }

    // Remove punishment
    public void removePunishment(String playerName, String type) {
        String path = "punishments." + playerName + "." + type;
        if (punishmentConfig.contains(path)) {
            punishmentConfig.set(path, null); // Remove the punishment data for the given type
            save();
        }
    }

    // Check if player is banned
    public boolean isPlayerBanned(String playerName) {
        String path = "punishments." + playerName + ".tempban";
        // Check for permanent ban in the BanList
        if (plugin.getServer().getBanList(org.bukkit.BanList.Type.NAME).isBanned(playerName)) {
            return true;
        }
        // Check for temporary ban in the config
        return punishmentConfig.contains(path);
    }

    // Check if ban has expired
    public boolean isBanExpired(String playerName) {
        String path = "punishments." + playerName + ".tempban";
        if (!punishmentConfig.contains(path)) {
            return true; // No ban found, so it's considered expired
        }

        Map<?, ?> banData = punishmentConfig.getConfigurationSection(path).getValues(true);
        String expirationTime = (String) banData.get("expirationTime");

        try {
            Instant banExpiration = Instant.parse(expirationTime);
            return Instant.now().isAfter(banExpiration);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Get ban details for player
    public Map<String, Object> getBanDetails(String playerName) {
        String path = "punishments." + playerName + ".tempban";
        if (!punishmentConfig.contains(path)) {
            return null;
        }
        return punishmentConfig.getConfigurationSection(path).getValues(true);
    }

    // Remove ban
    public void removeBan(String playerName) {
        String path = "punishments." + playerName + ".tempban";
        if (punishmentConfig.contains(path)) {
            punishmentConfig.set(path, null);
            save();
        }
    }

    public Map<String, Object> getPunishmentData(String playerName) {
        // Assuming the punishments are stored in a config file or similar structure
        return plugin.getConfig().getConfigurationSection("punishments." + playerName).getValues(false);
    }
}
