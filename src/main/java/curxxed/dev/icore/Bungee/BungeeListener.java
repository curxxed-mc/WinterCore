package curxxed.dev.icore.Bungee;

import curxxed.dev.icore.iCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class BungeeListener implements PluginMessageListener {
    private final iCore plugin;

    public BungeeListener(iCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) return;

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            String subChannel = in.readUTF();

            if (subChannel.equals("SyncRank")) {
                String playerName = in.readUTF();
                String rank = in.readUTF();

                Player target = Bukkit.getPlayerExact(playerName);
                if (target != null) {
                    plugin.getRankManager().setRank(target, rank, player);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
