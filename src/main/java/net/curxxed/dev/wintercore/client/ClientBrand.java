package net.curxxed.dev.wintercore.client;

import net.curxxed.dev.wintercore.commands.staff.ClientBrandCommand;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.utils.Utilities;
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
                String coloredName = CC.translate(rankColor) + plugin.getPlayerService().getIdentity(player) + CC.translate("&r");
                String staffMessage = plugin.getMessageConfig().get("client-brand.staff-join",
                        "&9[S] {player}&b joined using: &e{client}&7 ({version})",
                        "{player}", coloredName,
                        "{client}", friendlyName,
                        "{version}", mcVersion);
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
            return Utilities.IS_1_7
                    ? plugin.getMessageConfig().get("client-brand.version-unsupported", "Unknown (ViaVersion not supported on 1.7)")
                    : plugin.getMessageConfig().get("client-brand.version-missing", "Unknown (ViaVersion not installed)");
        }
        return ViaHelper.getVersion(uuid);
    }

    private String getFriendlyClientName(String brand) {
        if (brand == null) return brandName("unknown", "Unknown");
        String clean = brand.replaceAll("[^\\x20-\\x7E]", "").trim().toLowerCase();
        if (clean.contains("lunarclient")) return brandName("lunar", "Lunar Client");
        if (clean.contains("vanilla")) return brandName("vanilla", "Vanilla");
        if (clean.contains("forge") || clean.contains("fml")) return brandName("forge", "Forge");
        if (clean.contains("fabric")) return brandName("fabric", "Fabric");
        if (clean.contains("feather")) return brandName("feather", "Feather Client");
        if (clean.contains("badlion")) return brandName("badlion", "Badlion Client");
        return brandName("unknown", "Unknown");
    }

    private String brandName(String key, String fallback) {
        return plugin.getMessageConfig().get("client-brand.names." + key, fallback);
    }

    private static class ViaHelper {
        static String getVersion(UUID uuid) {
            try {
                int protocol = com.viaversion.viaversion.api.Via.getAPI().getPlayerVersion(uuid);
                com.viaversion.viaversion.api.protocol.version.ProtocolVersion version =
                        com.viaversion.viaversion.api.protocol.version.ProtocolVersion.getProtocol(protocol);
                return version.getName();
            } catch (Exception e) {
                WinterCore plugin = WinterCore.getInstance();
                if (plugin != null && plugin.getMessageConfig() != null) {
                    return plugin.getMessageConfig().get("client-brand.names.unknown", "Unknown");
                }
                return "Unknown";
            }
        }
    }
}
