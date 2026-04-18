package net.curxxed.dev.wintercore.client;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ClientBrand implements PluginMessageListener {

    private final WinterCore plugin;
    private static final boolean VIA_PRESENT = checkVia();

    private static boolean checkVia() {
        try {
            Class.forName("com.viaversion.viaversion.api.Via");
            return Bukkit.getPluginManager().getPlugin("ViaVersion") != null;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public ClientBrand(WinterCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (plugin.channel == null || plugin.channel.isEmpty()) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String clientBrand = new String(message, StandardCharsets.UTF_8);
            String friendlyName = getFriendlyClientName(clientBrand);
            String mcVersion = getMinecraftVersion(player.getUniqueId());

            plugin.getRankManager().getRank(player, rank -> plugin.getRankManager().getColorPreference(rank, rankColor -> {
                String coloredName = CC.translate(rankColor) + plugin.getPlayerListener().getRealName(player) + CC.translate("&r");
                String staffMessage = CC.translate("&9[S] ") + coloredName + CC.translate("&b joined using: &e") + friendlyName
                        + CC.translate("&7 (") + mcVersion + CC.translate("&7)");
                for (Player online : Bukkit.getServer().getOnlinePlayers()) {
                    if ((online.hasPermission("wintercore.staff") || online.hasPermission("wintercore.admin")
                            || online.hasPermission("wintercore.manager") || online.isOp())
                            && !ClientBrandCommand.silenced.contains(online.getUniqueId())) {
                        online.sendMessage(staffMessage);
                    }
                }
            }));
        }, 20L);
    }

    private String getMinecraftVersion(UUID uuid) {
        if (!VIA_PRESENT) {
            return "Unknown";
        }
        return ViaHelper.getVersion(uuid);
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
        return "Unknown";
    }

    private static class ViaHelper {
        static String getVersion(UUID uuid) {
            try {
                int protocol = com.viaversion.viaversion.api.Via.getAPI().getPlayerVersion(uuid);
                com.viaversion.viaversion.api.protocol.version.ProtocolVersion version =
                        com.viaversion.viaversion.api.protocol.version.ProtocolVersion.getProtocol(protocol);
                return version.getName();
            } catch (Exception e) {
                return "Unknown";
            }
        }
    }
}