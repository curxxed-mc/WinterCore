package net.curxxed.dev.wintercore.listeners;

import net.curxxed.dev.wintercore.database.DatabaseManager;
import net.curxxed.dev.wintercore.disguise.player.DisguiseData;
import net.curxxed.dev.wintercore.menus.ChatColorSelectionMenu;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.rank.RankManager;
import net.curxxed.dev.wintercore.utils.CC;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.function.Consumer;

public class PlayerListener implements Listener {

    private final WinterCore plugin;
    private final RankManager rankManager;
    private final DatabaseManager databaseManager;

    public static final ChatColorSelectionMenu CHAT_COLOR_SELECTION_MENU = ChatColorSelectionMenu.getInstance();

    public PlayerListener(WinterCore plugin) {
        this.plugin = plugin;
        this.rankManager = RankManager.getInstance();
        this.databaseManager = plugin.getDatabaseManager();
    }

    public void AreConditionsMet(Player player, Consumer<Boolean> callback) {
        databaseManager.isPlayerMuted(player.getUniqueId(), isMuted -> {
            if (isMuted) player.sendMessage(CC.translate("&cYou are muted and cannot send messages."));
            callback.accept(isMuted);
        });
    }

    public void sendPrivateMessage(Player sender, Player recipient, String message) {
        rankManager.getRank(sender, senderRank ->
                rankManager.getColorPreference(senderRank, senderColor ->
                        rankManager.getRank(recipient, recipientRank ->
                                rankManager.getColorPreference(recipientRank, recipientColor -> {
                                    ChatColor senderColor2 = ChatColor.getByChar(senderColor.charAt(1));
                                    ChatColor recipientColor2 = ChatColor.getByChar(recipientColor.charAt(1));
                                    sender.spigot().sendMessage(new TextComponent(
                                            ChatColor.YELLOW + "(To " + recipientColor2 + recipient.getDisplayName() + ChatColor.YELLOW + ") " + message));
                                    recipient.spigot().sendMessage(new TextComponent(
                                            ChatColor.YELLOW + "(From " + senderColor2 + sender.getDisplayName() + ChatColor.YELLOW + ") " + message));
                                })
                        )
                )
        );
    }

    public void notifyStaff(Player reporter, Player target, String reason) {
        if (reporter == null || target == null || reason == null || reason.isEmpty()) {
            plugin.getLogger().warning("Invalid report data: reporter, target, or reason is null/empty.");
            return;
        }
        plugin.getRedisManager().publishReport(
                reporter.getName(),
                target.getName(),
                reason,
                plugin.getConfig().getString("server-name", "Unknown")
        );
    }

    public void sendFreezeNotification(Player target, Player staff, boolean isFrozen) {
        String targetName = target.getDisplayName();
        String staffName = staff.getDisplayName();
        String message = isFrozen
                ? CC.translate("&9[S] ") + targetName + CC.translate("&c has been frozen by ") + staffName + "."
                : CC.translate("&9[S] ") + targetName + CC.translate("&a has been unfrozen by ") + staffName + ".";

        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("wintercore.staff") || p.hasPermission("wintercore.admin") || p.hasPermission("wintercore.manager"))
                .forEach(p -> p.sendMessage(message));
    }

    public String getRealName(Player player) {
        DisguiseData data = plugin.getDisguiseDataMap().get(player.getUniqueId());
        if (data != null && data.getInfo() != null && data.getInfo().has("name")) {
            return data.getInfo().get("name").getAsString();
        }
        return player.getName();
    }
}