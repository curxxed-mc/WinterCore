package net.curxxed.dev.wintercore.tags;

import net.curxxed.dev.wintercore.menu.Button;
import net.curxxed.dev.wintercore.menu.Menu;
import net.curxxed.dev.wintercore.menu.MenuManager;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TagsGUI extends Menu {

    private static final int SIZE = 54;
    private static final int PAGE_SIZE = 45;

    private static final int SLOT_PREVIOUS = 45;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_CLEAR = 50;
    private static final int SLOT_NEXT = 53;

    private final TagsManager tagsManager;
    private final Map<UUID, Integer> pageByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, String> selectedTagByPlayer = new ConcurrentHashMap<>();

    public TagsGUI(TagsManager tagsManager) {
        this.tagsManager = tagsManager;
    }

    @Override
    public String getTitle() {
        return CC.translate("&8Select a Tag");
    }

    @Override
    public int getSize() {
        return SIZE;
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

        int totalPages = Math.max(1, (int) Math.ceil(tags.size() / (double) PAGE_SIZE));
        int currentPage = Math.max(0, Math.min(pageByPlayer.getOrDefault(uuid, 0), totalPages - 1));
        pageByPlayer.put(uuid, currentPage);

        Map<Integer, Button> buttons = new HashMap<>();
        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, tags.size());
        String selectedId = selectedTagByPlayer.get(uuid);

        for (int index = start; index < end; index++) {
            final Tag tag = tags.get(index);
            final int slot = index - start;
            final boolean selected = selectedId != null && selectedId.equalsIgnoreCase(tag.getId());
            buttons.put(slot, new Button(buildTagItem(tag, selected), event -> {
                tagsManager.setPlayerTag(uuid, tag.getId());
                selectedTagByPlayer.put(uuid, tag.getId());
                event.getWhoClicked().closeInventory();
                player.sendMessage(CC.translate("&aSelected tag: " + TagsManager.colorNameToCode(tag.getColor()) + tag.getName()));
            }));
        }

        if (currentPage > 0) {
            buttons.put(SLOT_PREVIOUS, new Button(
                    buildControlItem(Material.ARROW, "&aPrevious Page", "&7Go to page " + currentPage + "."),
                    event -> {
                        pageByPlayer.put(uuid, currentPage - 1);
                        refresh(player);
                    }
            ));
        }

        buttons.put(SLOT_INFO, new Button(buildInfoItem(currentPage, totalPages, selectedId, tags)));

        buttons.put(SLOT_CLEAR, new Button(
                buildControlItem(Material.BARRIER, "&cClear Tag", "&7Remove your active tag."),
                event -> {
                    tagsManager.setPlayerTag(uuid, null);
                    selectedTagByPlayer.remove(uuid);
                    event.getWhoClicked().closeInventory();
                    player.sendMessage(CC.translate("&aTag cleared."));
                }
        ));

        if (currentPage < totalPages - 1) {
            buttons.put(SLOT_NEXT, new Button(
                    buildControlItem(Material.ARROW, "&aNext Page", "&7Go to page " + (currentPage + 2) + "."),
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

    public void refresh() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!MenuManager.getInstance().hasOpenMenu(player)) {
                continue;
            }
            if (MenuManager.getInstance().getOpenMenu(player) != this) {
                continue;
            }

            UUID uuid = player.getUniqueId();
            tagsManager.getPlayerTag(uuid, selectedTag -> {
                if (selectedTag != null) {
                    selectedTagByPlayer.put(uuid, selectedTag.getId());
                } else {
                    selectedTagByPlayer.remove(uuid);
                }
                refresh(player);
                player.sendMessage(CC.translate("&eTags were reloaded. Your menu has been refreshed."));
            });
        }
    }

    private ItemStack buildTagItem(Tag tag, boolean selected) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        String colorCode = TagsManager.colorNameToCode(tag.getColor());
        meta.setDisplayName(CC.translate(colorCode + tag.getName()));

        List<String> lore = new ArrayList<>();
        if (!tag.getPrefix().isEmpty()) {
            lore.add(CC.translate("&7Prefix: " + colorCode + tag.getPrefix()));
        }
        lore.add(CC.translate("&7Weight: &f" + tag.getWeight()));
        lore.add(CC.translate(selected ? "&aCurrently selected" : "&eClick to select this tag"));
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
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
        return buildControlItem(
                Material.BOOK,
                "&bPage " + (currentPage + 1) + "/" + totalPages,
                "&7Current tag: &f" + selectedName
        );
    }

    private ItemStack buildControlItem(Material material, String name, String loreLine) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(CC.translate(name));
        List<String> lore = new ArrayList<>();
        lore.add(CC.translate(loreLine));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
