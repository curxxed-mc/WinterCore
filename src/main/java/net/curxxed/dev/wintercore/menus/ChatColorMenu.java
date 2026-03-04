package net.curxxed.dev.wintercore.menus;

import net.curxxed.dev.wintercore.menu.Button;
import net.curxxed.dev.wintercore.menu.ButtonBuilder;
import net.curxxed.dev.wintercore.menu.Menu;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ChatColorMenu extends Menu {


    private static final Map<String, String> COLOR_CODE_TO_NAME = new LinkedHashMap<>();
    private static final Map<String, Byte>   COLOR_CODE_TO_DYE  = new LinkedHashMap<>();

    static {
        COLOR_CODE_TO_NAME.put("&1", "Dark Blue");
        COLOR_CODE_TO_NAME.put("&2", "Dark Green");
        COLOR_CODE_TO_NAME.put("&3", "Dark Aqua");
        COLOR_CODE_TO_NAME.put("&4", "Dark Red");
        COLOR_CODE_TO_NAME.put("&5", "Purple");
        COLOR_CODE_TO_NAME.put("&6", "Gold");
        COLOR_CODE_TO_NAME.put("&7", "Gray");
        COLOR_CODE_TO_NAME.put("&9", "Blue");
        COLOR_CODE_TO_NAME.put("&a", "Green");
        COLOR_CODE_TO_NAME.put("&b", "Aqua");
        COLOR_CODE_TO_NAME.put("&c", "Red");
        COLOR_CODE_TO_NAME.put("&d", "Pink");
        COLOR_CODE_TO_NAME.put("&e", "Yellow");
    }

    static {
        COLOR_CODE_TO_DYE.put("&1", (byte)  4);
        COLOR_CODE_TO_DYE.put("&2", (byte)  2);
        COLOR_CODE_TO_DYE.put("&3", (byte)  6);
        COLOR_CODE_TO_DYE.put("&4", (byte)  1);
        COLOR_CODE_TO_DYE.put("&5", (byte)  5);
        COLOR_CODE_TO_DYE.put("&6", (byte) 14);
        COLOR_CODE_TO_DYE.put("&7", (byte)  8);
        COLOR_CODE_TO_DYE.put("&9", (byte) 11);
        COLOR_CODE_TO_DYE.put("&a", (byte) 10);
        COLOR_CODE_TO_DYE.put("&b", (byte)  6);
        COLOR_CODE_TO_DYE.put("&c", (byte)  1);
        COLOR_CODE_TO_DYE.put("&d", (byte)  9);
        COLOR_CODE_TO_DYE.put("&e", (byte) 11);
    }

    private final WinterCore plugin;
    private final Player player;

    public ChatColorMenu(WinterCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    @Override
    public String getTitle() {
        return "Select Chat Colour";
    }

    @Override
    public int getSize() {

        return ((COLOR_CODE_TO_NAME.size() - 1) / 9 + 1) * 9;
    }

    @Override
    public Map<Integer, Button> getButtons(Player viewer) {
        Map<Integer, Button> buttons = new HashMap<>();
        int slot = 0;

        for (Map.Entry<String, String> entry : COLOR_CODE_TO_NAME.entrySet()) {
            String colorCode = entry.getKey();
            String colorName = entry.getValue();
            byte dyeData     = COLOR_CODE_TO_DYE.getOrDefault(colorCode, (byte) 0);

            buttons.put(slot++, ButtonBuilder.of(Material.INK_SACK)
                    .data(dyeData)
                    .name(colorCode + colorName)
                    .lore(
                            "&7Example:",
                            "&f" + viewer.getName() + "&r: " + colorCode + "Hi! :)"
                    )
                    .action(e -> {
                        plugin.getRankManager().setMessageColorPreference(player, colorCode);
                        player.sendMessage(CC.translate(
                                "&aChat message colour set to: " + colorName));
                        player.closeInventory();
                    })
                    .build());
        }

        return buttons;
    }
}