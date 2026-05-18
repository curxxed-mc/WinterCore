package net.curxxed.dev.wintercore.client;

import net.curxxed.dev.wintercore.commands.staff.ClientBrandCommand;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
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
        if (plugin.channel == null || plugin.channel.isEmpty()) {
            return;
        }

        plugin.getTasks().later(() -> {
            String clientBrand = new String(message, StandardCharsets.UTF_8);
            String friendlyName = getFriendlyClientName(clientBrand);
            String mcVersion = getMinecraftVersion(player);

            plugin.getRankManager().getRank(player, rank -> plugin.getRankManager().getColorPreference(rank, rankColor -> {
                String coloredName = CC.translate(rankColor) + plugin.getPlayerService().getIdentity(player) + CC.translate("&r");
                String staffMessage = plugin.getMessageConfig().get("client-brand.staff-join",
                        "&9[S] {player}&b joined using: &e{client}&7 ({version})",
                        "{player}", coloredName,
                        "{client}", friendlyName,
                        "{version}", mcVersion);
                for (Player online : net.curxxed.dev.wintercore.utils.Utilities.getOnlinePlayers()) {
                    if ((online.hasPermission("wintercore.staff") || online.hasPermission("wintercore.admin")
                            || online.hasPermission("wintercore.manager") || online.isOp())
                            && !ClientBrandCommand.silenced.contains(online.getUniqueId())) {
                        online.sendMessage(staffMessage);
                    }
                }
            }));
        }, 20L);
    }

    private String getMinecraftVersion(Player player) {
        return plugin.getProtocolResolver().resolveName(player);
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
}
