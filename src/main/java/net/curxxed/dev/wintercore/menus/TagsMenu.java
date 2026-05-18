package net.curxxed.dev.wintercore.menus;

import net.curxxed.dev.wintercore.menu.Button;
import net.curxxed.dev.wintercore.menu.Menu;
import net.curxxed.dev.wintercore.menu.MenuManager;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.tags.Tag;
import net.curxxed.dev.wintercore.tags.TagsManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TagsMenu extends Menu {

    private static final int SIZE = 54;
    private static final int PAGE_SIZE = 45;

    private static final int SLOT_PREVIOUS = 45;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_CLEAR = 50;
    private static final int SLOT_NEXT = 53;

    private final WinterCore plugin;
    private final TagsManager tagsManager;
    private final Map<UUID, Integer> pageByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, String> selectedTagByPlayer = new ConcurrentHashMap<>();

    public TagsMenu(WinterCore plugin, TagsManager tagsManager) {
        this.plugin = plugin;
        this.tagsManager = tagsManager;
    }

    @Override
    public String getTitle() {
        return plugin.getMenuConfig().getString("tags-menu.title", "&8Select a Tag");
    }

    @Override
    public int getSize() {
        return plugin.getMenuConfig().getSize("tags-menu");
    }

    public void openTagsGUI(Player player) {
        openTagsGUI(player, 0);
    }

    public void openTagsGUI(Player player, int page) {
        UUID uuid = player.getUniqueId();
        pageByPlayer.put(uuid, Math.max(0, page));
        tagsManager.getPlayerTag(uuid, selectedTag -> {
            if (selectedTag != null) {
                selectedTagByPlayer.put(uuid, selectedTag.getId());
            } else {
                selectedTagByPlayer.remove(uuid);
            }
            open(player);
        });
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        UUID uuid = player.getUniqueId();
        List<Tag> tags = tagsManager.getTagsSorted();

        int totalPages = Math.max(1, (int) Math.ceil(tags.size() / (double) pageSize()));
        int currentPage = Math.max(0, Math.min(pageByPlayer.getOrDefault(uuid, 0), totalPages - 1));
        pageByPlayer.put(uuid, currentPage);

        Map<Integer, Button> buttons = new HashMap<>();
        int start = currentPage * pageSize();
        int end = Math.min(start + pageSize(), tags.size());
        String selectedId = selectedTagByPlayer.get(uuid);

        for (int index = start; index < end; index++) {
            final Tag tag = tags.get(index);
            final int slot = index - start;
            final boolean selected = selectedId != null && selectedId.equalsIgnoreCase(tag.getId());
            buttons.put(slot, new Button(buildTagItem(tag, selected), event -> {
                tagsManager.setPlayerTag(uuid, tag.getId());
                selectedTagByPlayer.put(uuid, tag.getId());
                event.getWhoClicked().closeInventory();
                player.sendMessage(plugin.getMessageConfig().get("tags.selected",
                        "&aSelected tag: {tag}",
                        "{tag}", TagsManager.colorNameToCode(tag.getColor()) + tag.getName()));
            }));
        }

        if (currentPage > 0) {
            buttons.put(controlSlot("previous", SLOT_PREVIOUS), new Button(
                    buildControlItem("tags-menu.controls.previous", Material.ARROW,
                            "{page}", String.valueOf(currentPage),
                            "{total_pages}", String.valueOf(totalPages)),
                    event -> {
                        pageByPlayer.put(uuid, currentPage - 1);
                        refresh(player);
                    }
            ));
        }

        buttons.put(controlSlot("info", SLOT_INFO), new Button(buildInfoItem(currentPage, totalPages, selectedId, tags)));

        buttons.put(controlSlot("clear", SLOT_CLEAR), new Button(
                buildControlItem("tags-menu.controls.clear", Material.BARRIER),
                event -> {
                    tagsManager.setPlayerTag(uuid, null);
                    selectedTagByPlayer.remove(uuid);
                    event.getWhoClicked().closeInventory();
                    player.sendMessage(plugin.getMessageConfig().get("tags.cleared", "&aTag cleared."));
                }
        ));

        if (currentPage < totalPages - 1) {
            buttons.put(controlSlot("next", SLOT_NEXT), new Button(
                    buildControlItem("tags-menu.controls.next", Material.ARROW,
                            "{page}", String.valueOf(currentPage + 2),
                            "{total_pages}", String.valueOf(totalPages)),
                    event -> {
                        pageByPlayer.put(uuid, currentPage + 1);
                        refresh(player);
                    }
            ));
        }

        return buttons;
    }

    @Override
    public void onClose(Player player) {
        UUID uuid = player.getUniqueId();
        pageByPlayer.remove(uuid);
        selectedTagByPlayer.remove(uuid);
    }

    /**
     * Refreshes the GUI for all players currently viewing it and syncs their
     * selected-tag state. Optionally notifies a specific staff member who
     * triggered the reload — avoids spamming the reload message to every viewer.
     *
     * @param notifyStaff the staff player who ran /tags reload, or null for silent refresh
     */
    public void refresh(Player notifyStaff) {
        for (Player player : net.curxxed.dev.wintercore.utils.Utilities.getOnlinePlayers()) {
            if (!MenuManager.getInstance().hasOpenMenu(player)) continue;
            if (MenuManager.getInstance().getOpenMenu(player) != this) continue;

            UUID uuid = player.getUniqueId();
            tagsManager.getPlayerTag(uuid, selectedTag -> {
                if (selectedTag != null) {
                    selectedTagByPlayer.put(uuid, selectedTag.getId());
                } else {
                    selectedTagByPlayer.remove(uuid);
                }
                refresh(player);
            });
        }

        if (notifyStaff != null && notifyStaff.isOnline()) {
            notifyStaff.sendMessage(plugin.getMessageConfig().get("tags.reloaded",
                    "&eTags were reloaded. Open menus have been refreshed."));
        }
    }

    public void refresh() {
        refresh(null);
    }

    private ItemStack buildTagItem(Tag tag, boolean selected) {
        String colorCode = TagsManager.colorNameToCode(tag.getColor());
        String selectedStatus = selected
                ? plugin.getMenuConfig().getString("tags-menu.selected-status", "&aCurrently selected")
                : plugin.getMenuConfig().getString("tags-menu.unselected-status", "&eClick to select this tag");
        return plugin.getMenuConfig().buildItem(
                "tags-menu.tag-item",
                Material.NAME_TAG,
                "{tag_id}", tag.getId(),
                "{tag_name}", tag.getName(),
                "{tag_prefix}", tag.getPrefix(),
                "{tag_weight}", String.valueOf(tag.getWeight()),
                "{color}", colorCode,
                "{selected_status}", selectedStatus
        );
    }

    private ItemStack buildInfoItem(int currentPage, int totalPages, String selectedId, List<Tag> tags) {
        String selectedName = "None";
        if (selectedId != null) {
            for (Tag tag : tags) {
                if (tag.getId().equalsIgnoreCase(selectedId)) {
                    selectedName = tag.getName();
                    break;
                }
            }
        }
        return buildControlItem("tags-menu.controls.info", Material.BOOK,
                "{page}", String.valueOf(currentPage + 1),
                "{total_pages}", String.valueOf(totalPages),
                "{selected_tag}", selectedName);
    }

    private ItemStack buildControlItem(String path, Material material, String... placeholders) {
        return plugin.getMenuConfig().buildItem(path, material, placeholders);
    }

    private int pageSize() {
        return Math.max(1, plugin.getMenuConfig().getInt("tags-menu.page-size", PAGE_SIZE));
    }

    private int controlSlot(String key, int fallback) {
        return plugin.getMenuConfig().getSlot("tags-menu.controls." + key, fallback);
    }
}