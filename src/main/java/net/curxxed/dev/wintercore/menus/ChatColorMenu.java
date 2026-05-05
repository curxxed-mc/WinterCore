package net.curxxed.dev.wintercore.menus;

import net.curxxed.dev.wintercore.menu.Button;
import net.curxxed.dev.wintercore.menu.ButtonBuilder;
import net.curxxed.dev.wintercore.menu.Menu;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
        return plugin.getMenuConfig().getString("chat-color-menu.title", "&bSelect Chat Color",
                "{player}", player.getName());
    }

    @Override
    public int getSize() {
        return plugin.getMenuConfig().getSize("chat-color-menu");
    }

    @Override
    public Map<Integer, Button> getButtons(Player viewer) {
        Map<Integer, Button> buttons = new HashMap<>();

        if (plugin.getMenuConfig().getBoolean("chat-color-menu.filler.enabled", false)) {
            Button filler = new Button(plugin.getMenuConfig().buildItem(
                    "chat-color-menu.filler",
                    Material.STAINED_GLASS_PANE,
                    "{player}", viewer.getName()
            ));
            for (int i = 0; i < getSize(); i++) {
                buttons.put(i, filler);
            }
        }

        for (ColorOption option : loadColorOptions()) {
            buttons.put(option.slot, new Button(
                    buildColorItem(option, viewer),
                    event -> handleColorClick(option)
            ));
        }

        return buttons;
    }

    private void handleColorClick(ColorOption option) {
        if (!option.permission.isEmpty() && !player.hasPermission(option.permission)) {
            player.sendMessage(plugin.getMessageConfig().get("chat-color.no-permission",
                    "&cYou do not have permission to use {color_name}.",
                    "{color_name}", option.displayName));
            return;
        }

        plugin.getPlayerService().setChatColorPreference(player, option.colorCode);
        player.sendMessage(plugin.getMessageConfig().get("chat-color.selected",
                "&aChat message color set to: {color_name}",
                "{color_name}", CC.translate(option.colorCode + option.displayName)));
        player.closeInventory();
    }

    private List<ColorOption> loadColorOptions() {
        ConfigurationSection colors = plugin.getMenuConfig().getSection("chat-color-menu.colors");
        if (colors == null) {
            return defaultColorOptions();
        }

        List<ColorOption> options = new ArrayList<>();
        int fallbackSlot = 0;
        for (String key : colors.getKeys(false)) {
            String path = "chat-color-menu.colors." + key;
            ConfigurationSection section = colors.getConfigurationSection(key);
            if (section == null || !section.getBoolean("enabled", true)) {
                continue;
            }

            String colorCode = section.getString("color-code", "&f");
            String displayName = section.getString("display-name", COLOR_CODE_TO_NAME.getOrDefault(colorCode, key));
            String permission = section.getString("permission", "");
            int slot = plugin.getMenuConfig().getSlot(path, fallbackSlot++);
            byte dyeData = (byte) section.getInt("data", COLOR_CODE_TO_DYE.getOrDefault(colorCode, (byte) 15));
            options.add(new ColorOption(path, slot, colorCode, displayName, permission, dyeData));
        }

        Collections.sort(options, Comparator.comparingInt(option -> option.slot));
        return options;
    }

    private List<ColorOption> defaultColorOptions() {
        List<ColorOption> options = new ArrayList<>();
        int slot = 0;
        for (Map.Entry<String, String> entry : COLOR_CODE_TO_NAME.entrySet()) {
            byte dyeData = COLOR_CODE_TO_DYE.getOrDefault(entry.getKey(), (byte) 15);
            options.add(new ColorOption(null, slot++, entry.getKey(), entry.getValue(), "", dyeData));
        }
        return options;
    }

    private org.bukkit.inventory.ItemStack buildColorItem(ColorOption option, Player viewer) {
        if (option.configPath != null && plugin.getMenuConfig().getSection(option.configPath) != null) {
            return plugin.getMenuConfig().buildItem(
                    option.configPath,
                    Material.INK_SACK,
                    option.dyeData,
                    "{player}", viewer.getName(),
                    "{color}", CC.translate(option.colorCode),
                    "{color_code}", option.colorCode,
                    "{color_name}", option.displayName
            );
        }

        return ButtonBuilder.of(Material.INK_SACK)
                .data(option.dyeData)
                .name(option.colorCode + option.displayName)
                .lore(
                        "&7Preview: &f" + viewer.getName() + "&7: " + option.colorCode + "Hello there.",
                        "&eClick to use this chat color."
                )
                .build()
                .getItem();
    }

    private static final class ColorOption {
        private final String configPath;
        private final int slot;
        private final String colorCode;
        private final String displayName;
        private final String permission;
        private final byte dyeData;

        private ColorOption(String configPath, int slot, String colorCode, String displayName, String permission, byte dyeData) {
            this.configPath = configPath;
            this.slot = slot;
            this.colorCode = colorCode;
            this.displayName = displayName;
            this.permission = permission == null ? "" : permission;
            this.dyeData = dyeData;
        }
    }
}
