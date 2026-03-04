package net.curxxed.dev.wintercore.disguise;


import net.curxxed.dev.wintercore.menu.Button;
import net.curxxed.dev.wintercore.menu.ButtonBuilder;
import net.curxxed.dev.wintercore.menu.Menu;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DisguiseMenu extends Menu {



    private static final Map<UUID, String> pendingTargets = new ConcurrentHashMap<>();

    private final WinterCore      plugin;
    private final DisguiseHandler disguiseHandler;
    private final String          targetName;

    public DisguiseMenu(WinterCore plugin, DisguiseHandler disguiseHandler, String targetName) {
        this.plugin          = plugin;
        this.disguiseHandler = disguiseHandler;
        this.targetName      = targetName;
    }

    public static void setPendingTarget(Player player, String targetName) {
        pendingTargets.put(player.getUniqueId(), targetName);
    }

    public static void clearPendingTarget(Player player) {
        pendingTargets.remove(player.getUniqueId());
    }

    public static String getPendingTarget(Player player) {
        return pendingTargets.get(player.getUniqueId());
    }





    @Override
    public String getTitle() {
        return "Select disguise rank";
    }

    @Override
    public int getSize() {
        return 36;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        List<String>         sortedRanks  = plugin.getRankManager().getSortedRanks();
        ConfigurationSection ranksSection = plugin.getRankManager().getRanksSection();

        int slot = 0;
        for (String rank : sortedRanks) {
            if (rank == null || rank.isEmpty()) continue;

            String    colorCode  = ranksSection.getString(rank + ".name-color", "&f");
            String    prefix     = ranksSection.getString(rank + ".prefix", "");
            ChatColor chatColor  = ChatColor.getByChar(
                    colorCode.replace("&", "").charAt(0));
            DyeColor  dyeColor   = getDyeColorFromChatColor(chatColor);


            Material mat  = chatColor == ChatColor.DARK_RED ? Material.STAINED_CLAY : Material.WOOL;
            short    data = chatColor == ChatColor.DARK_RED ? 14 : dyeColor.getWoolData();


            String cleanColorCode = colorCode.replaceAll("(?i)&o|§o", "");

            List<String> lore = new ArrayList<>();
            lore.add(CC.translate("&7") + "§m------------------------");
            lore.add(CC.translate("&6") + "Prefix: "
                    + CC.translate("&f") + CC.translate(cleanColorCode) + CC.translate(prefix));
            lore.add(CC.translate("&7") + "§m------------------------");
            lore.add(CC.translate("&a") + "Click to disguise as "
                    + CC.translate("&b") + targetName + CC.translate("&a."));

            final String finalRank = rank;
            buttons.put(slot++, ButtonBuilder.of(mat)
                    .data(data)
                    .name(colorCode + rank)
                    .lore(lore)
                    .action(e -> handleRankClick(player, finalRank))
                    .build());
        }


        Button cancelBtn = Utilities.IS_1_7
                ? ButtonBuilder.of(Material.STAINED_GLASS_PANE).data((short) 14)
                .name("&cCancel")
                .action(e -> {
                    clearPendingTarget(player);
                    player.closeInventory();
                }).build()
                : ButtonBuilder.of(Material.BARRIER)
                .name("&cCancel")
                .action(e -> {
                    clearPendingTarget(player);
                    player.closeInventory();
                }).build();

        buttons.put(35, cancelBtn);
        return buttons;
    }

    @Override
    public void onClose(Player player) {

        clearPendingTarget(player);
    }





    private void handleRankClick(Player player, String rankKey) {
        clearPendingTarget(player);
        player.closeInventory();
        disguiseHandler.disguise(player, rankKey, targetName, targetName, result -> {
            switch (result) {
                case SUCCESS:
                    player.sendMessage(CC.translate("&aDisguise applied as &e" + targetName + " &7with rank &b" + rankKey + "!"));
                    break;
                case ERROR:
                    player.sendMessage(CC.translate("&cFailed to apply disguise. Try again later."));
                    break;
                case NO_RANK_FOUND:
                    player.sendMessage(CC.translate("&cSelected rank is invalid or not found."));
                    break;
                case GLOBAL_PLAYER_FOUND:
                    player.sendMessage(CC.translate("&cA player with that name is already online."));
                    break;
                case SAME_NAME:
                    player.sendMessage(CC.translate("&cYou cannot disguise as yourself."));
                    break;
                case NOT_ONLINE:
                    player.sendMessage(CC.translate("&cYou must be online to disguise."));
                    break;
                default:
                    player.sendMessage(CC.translate("&cUnknown error occurred."));
            }
        });
    }





    private DyeColor getDyeColorFromChatColor(ChatColor chatColor) {
        if (chatColor == null) return DyeColor.WHITE;
        switch (chatColor) {
            case BLACK:        return DyeColor.BLACK;
            case DARK_BLUE:    return DyeColor.BLUE;
            case DARK_GREEN:   return DyeColor.GREEN;
            case DARK_AQUA:    return DyeColor.CYAN;
            case DARK_RED:     return DyeColor.RED;
            case DARK_PURPLE:  return DyeColor.PURPLE;
            case GOLD:         return DyeColor.ORANGE;
            case GRAY:         return DyeColor.SILVER;
            case DARK_GRAY:    return DyeColor.GRAY;
            case BLUE:         return DyeColor.LIGHT_BLUE;
            case GREEN:        return DyeColor.LIME;
            case AQUA:         return DyeColor.LIGHT_BLUE;
            case RED:          return DyeColor.RED;
            case LIGHT_PURPLE: return DyeColor.PINK;
            case YELLOW:       return DyeColor.YELLOW;
            case WHITE:        return DyeColor.WHITE;
            default:           return DyeColor.WHITE;
        }
    }
}