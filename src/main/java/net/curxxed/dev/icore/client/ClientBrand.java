package net.curxxed.dev.icore.client;

import net.curxxed.dev.icore.plugin.iCore;
import net.curxxed.dev.icore.utils.CC;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;

public class ClientBrand implements PluginMessageListener {

    private final iCore plugin;

    public ClientBrand(iCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equalsIgnoreCase("minecraft:brand") && !channel.equalsIgnoreCase("MC|Brand")) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String clientBrand = new String(message, StandardCharsets.UTF_8);
            String friendlyName = getFriendlyClientName(clientBrand);

            // Get protocol version using ViaVersion
            int protocolVersion = Via.getAPI().getPlayerVersion(player.getUniqueId());
            String mcVersion = getMinecraftVersion(protocolVersion);

            plugin.getRankManager().getRank(player, rank -> plugin.getRankManager().getColorPreference(rank, rankColor -> {
                String coloredName = CC.translate(rankColor) + player.getName() + CC.translate("&r");
                String staffMessage = CC.translate("&9[S] ") + coloredName + CC.translate("&b joined using: &e") + friendlyName
                        + CC.translate("&7 (") + mcVersion + CC.translate("&7)");
                for (Player online : Bukkit.getServer().getOnlinePlayers()) {
                    if ((online.hasPermission("icore.staff") || online.hasPermission("icore.admin") || online.hasPermission("icore.manager") || online.isOp())
                            && !ClientBrandCommand.silenced.contains(online.getUniqueId())) {
                        online.sendMessage(staffMessage);
                    }
                }
            }));
        }, 20L);
    }

    private String getMinecraftVersion(int protocolVersion) {
        ProtocolVersion version = ProtocolVersion.getProtocol(protocolVersion);
        if (version != null) {
            return version.getName();
        }
        return "Unknown";
    }

    private String getFriendlyClientName(String brand) {
        if (brand == null) return "Unknown";
        String clean = brand.replaceAll("[^\\x20-\\x7E]", "").trim().toLowerCase();
        if (clean.contains("lunarclient")) return "Lunar Client";
        if (clean.contains("vanilla")) return "Vanilla";
        if (clean.contains("forge") || clean.contains("fml")) return "Forge";
        if (clean.contains("fabric")) return "Fabric";
        if (clean.contains("feather")) return "Feather Client";
        if (clean.contains("badlion")) return "Badlion Client";
        if (clean.contains("velocity")) return "Velocity";
        return "Unknown";
    }
}