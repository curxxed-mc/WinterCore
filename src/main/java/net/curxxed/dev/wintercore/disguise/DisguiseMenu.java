package net.curxxed.dev.wintercore.disguise;

import net.curxxed.dev.wintercore.menu.Button;
import net.curxxed.dev.wintercore.menu.Menu;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DisguiseMenu extends Menu {

    private static final Map<UUID, String> pendingTargets = new ConcurrentHashMap<>();

    private final WinterCore plugin;
    private final DisguiseHandler disguiseHandler;
    private final String targetName;

    public DisguiseMenu(WinterCore plugin, DisguiseHandler disguiseHandler, String targetName) {
        this.plugin = plugin;
        this.disguiseHandler = disguiseHandler;
        this.targetName = targetName;
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
        return plugin.getMenuConfig().getString("disguise-menu.title", "&8Select disguise rank",
                "{target}", targetName);
    }

    @Override
    public int getSize() {
        return plugin.getMenuConfig().getSize("disguise-menu");
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        List<String> sortedRanks = plugin.getRankManager().getSortedRanks();
        ConfigurationSection ranksSection = plugin.getRankManager().getRanksSection();

        int slot = 0;
        for (String rank : sortedRanks) {
            if (rank == null || rank.isEmpty()) {
                continue;
            }

            String colorCode = ranksSection.getString(rank + ".name-color", "&f");
            String prefix = ranksSection.getString(rank + ".prefix", "");
            char colorChar = normalizeColorChar(colorCode);
            DyeColor dyeColor = getDyeColorFromColorChar(colorChar);

            Material material = colorChar == '4' ? Material.STAINED_CLAY : Material.WOOL;
            short data = colorChar == '4' ? 14 : dyeColor.getWoolData();
            String section = String.valueOf((char) 167);
            String cleanColorCode = colorCode
                    .replace("&o", "")
                    .replace("&O", "")
                    .replace(section + "o", "")
                    .replace(section + "O", "");

            final String finalRank = rank;
            buttons.put(slot++, new Button(
                    plugin.getMenuConfig().buildItem(
                            "disguise-menu.rank-item",
                            material,
                            data,
                            "{rank}", rank,
                            "{rank_color}", colorCode,
                            "{rank_prefix}", prefix,
                            "{clean_rank_color}", cleanColorCode,
                            "{target}", targetName
                    ),
                    e -> handleRankClick(player, finalRank)
            ));
        }

        Material cancelMaterial = Utilities.IS_1_7 ? Material.STAINED_GLASS_PANE : Material.BARRIER;
        short cancelData = Utilities.IS_1_7 ? (short) 14 : 0;
        Button cancelButton = new Button(
                plugin.getMenuConfig().buildItem("disguise-menu.cancel-button", cancelMaterial, cancelData),
                e -> {
                    clearPendingTarget(player);
                    player.closeInventory();
                }
        );
        buttons.put(plugin.getMenuConfig().getSlot("disguise-menu.cancel-button", getSize() - 1), cancelButton);
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
                    player.sendMessage(menuMessage("disguise.success",
                            "&aDisguise applied as &e{target} &7with rank &b{rank}&a!", rankKey));
                    break;
                case ERROR:
                    player.sendMessage(menuMessage("disguise.disguise-error",
                            "&cFailed to apply disguise. Try again later.", rankKey));
                    break;
                case NO_RANK_FOUND:
                    player.sendMessage(menuMessage("disguise.no-rank-found",
                            "&cSelected rank is invalid or not found.", rankKey));
                    break;
                case GLOBAL_PLAYER_FOUND:
                    player.sendMessage(menuMessage("disguise.global-player-found",
                            "&cA player with that name is already online.", rankKey));
                    break;
                case SAME_NAME:
                    player.sendMessage(menuMessage("disguise.same-name",
                            "&cYou cannot disguise as yourself.", rankKey));
                    break;
                case NOT_ONLINE:
                    player.sendMessage(menuMessage("disguise.not-online",
                            "&cYou must be online to disguise.", rankKey));
                    break;
                default:
                    player.sendMessage(menuMessage("disguise.unknown",
                            "&cUnknown error occurred.", rankKey));
            }
        });
    }

    private String menuMessage(String path, String fallback, String rankKey) {
        return plugin.getMessageConfig().get(path, fallback,
                "{target}", targetName,
                "{rank}", rankKey);
    }

    private char normalizeColorChar(String colorCode) {
        if (colorCode == null || colorCode.isEmpty()) {
            return 'f';
        }
        String normalized = colorCode
                .replace("&", "")
                .replace(String.valueOf((char) 167), "")
                .trim();
        return normalized.isEmpty() ? 'f' : Character.toLowerCase(normalized.charAt(0));
    }

    private DyeColor getDyeColorFromColorChar(char colorChar) {
        switch (colorChar) {
            case '0': return DyeColor.BLACK;
            case '1': return DyeColor.BLUE;
            case '2': return DyeColor.GREEN;
            case '3': return DyeColor.CYAN;
            case '4': return DyeColor.RED;
            case '5': return DyeColor.PURPLE;
            case '6': return DyeColor.ORANGE;
            case '7': return DyeColor.SILVER;
            case '8': return DyeColor.GRAY;
            case '9': return DyeColor.BLUE;
            case 'a': return DyeColor.LIME;
            case 'b': return DyeColor.LIGHT_BLUE;
            case 'c': return DyeColor.RED;
            case 'd': return DyeColor.PINK;
            case 'e': return DyeColor.YELLOW;
            default: return DyeColor.WHITE;
        }
    }
}
