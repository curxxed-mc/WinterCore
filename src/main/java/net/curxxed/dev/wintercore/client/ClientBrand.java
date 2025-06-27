package net.curxxed.dev.wintercore.client;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;

public class ClientBrand implements PluginMessageListener {

    private final WinterCore plugin;

    public ClientBrand(WinterCore plugin) {
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

            int protocolVersion = Via.getAPI().getPlayerVersion(player.getUniqueId());
            String mcVersion = getMinecraftVersion(protocolVersion);

            plugin.getRankManager().getRank(player, rank -> plugin.getRankManager().getColorPreference(rank, rankColor -> {
                String coloredName = CC.translate(rankColor) + plugin.getPlayerListener().getRealName(player) + CC.translate("&r");
                String staffMessage = CC.translate("&9[S] ") + coloredName + CC.translate("&b joined using: &e") + friendlyName
                        + CC.translate("&7 (") + mcVersion + CC.translate("&7)");
                for (Player online : Bukkit.getServer().getOnlinePlayers()) {
                    if ((online.hasPermission("wintercore.staff") || online.hasPermission("wintercore.admin") || online.hasPermission("wintercore.manager") || online.isOp())
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