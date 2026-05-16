package net.curxxed.dev.wintercore.menus;

import net.curxxed.dev.wintercore.database.redis.packet.packets.RankTagSyncPacket;
import net.curxxed.dev.wintercore.events.network.RankChangeEvent;
import net.curxxed.dev.wintercore.menu.Button;
import net.curxxed.dev.wintercore.menu.Menu;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.utils.ItemBuilder;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RankMenu extends Menu {

    private enum View { RANK_SELECT, DURATION }

    static class GrantState {
        public final UUID targetUUID;
        public final String rank;
        public long durationMillis = 0L;
        public boolean permanent = false;

        GrantState(UUID targetUUID, String rank) {
            this.targetUUID = targetUUID;
            this.rank = rank;
        }
    }

    private static final long HOUR = 3_600_000L;
    private static final long DAY = 86_400_000L;
    private static final long WEEK = 604_800_000L;
    private static final long MONTH = 2_592_000_000L;

    static final Map<UUID, GrantState> pendingGrants = new ConcurrentHashMap<>();

    private final WinterCore plugin;
    private final UUID targetUUID;
    private final String targetName;

    private View view = View.RANK_SELECT;
    private GrantState state = null;

    public RankMenu(WinterCore plugin, UUID targetUUID, String targetName) {
        this.plugin = plugin;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
    }

    @Override
    public String getTitle() {
        return view == View.RANK_SELECT
                ? plugin.getMenuConfig().getString("rank-menu.title", "&8Set rank for {target}", "{target}", targetName)
                : plugin.getMenuConfig().getString("rank-menu.duration-title", "&8Set duration for {target}", "{target}", targetName);
    }

    @Override
    public int getSize() {
        if (view == View.DURATION) {
            return plugin.getMenuConfig().getInventorySize("rank-menu.duration.size", 27);
        }

        int count = plugin.getRankManager().getSortedRanks().size() + 1;
        int dynamicSize = Math.min(54, ((count - 1) / 9 + 1) * 9);
        return plugin.getMenuConfig().getInventorySize("rank-menu.size", dynamicSize);
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        return view == View.RANK_SELECT ? buildRankButtons(player) : buildDurationButtons(player);
    }

    private Map<Integer, Button> buildRankButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        List<String> sortedRanks = plugin.getRankManager().getSortedRanks();
        ConfigurationSection ranksSection = plugin.getRankManager().getRanksSection();

        int slot = 0;
        for (String rank : sortedRanks) {
            if (rank == null || rank.isEmpty()) {
                continue;
            }

            String colorCode = ranksSection.getString(rank + ".name-color", "&f");
            String translatedColor = CC.translate(colorCode);
            String prefix = ranksSection.getString(rank + ".prefix", "");
            String translatedPrefix = CC.translate(prefix);
            final String finalRank = rank;

            char colorChar = normalizeColorChar(colorCode);

            ItemStack item;
            if (colorChar == '4') {
                item = new ItemBuilder(Material.STAINED_CLAY, 1, (byte) 14).toItemStack();
            } else {
                org.bukkit.DyeColor dye = dyeColor(colorChar);
                item = new ItemBuilder(Material.WOOL, 1, dye.getWoolData()).toItemStack();
            }

            item = plugin.getMenuConfig().buildItem(
                    "rank-menu.rank-item",
                    item.getType(),
                    item.getDurability(),
                    "{rank}", rank,
                    "{rank_color}", colorCode,
                    "{rank_prefix}", prefix,
                    "{target}", targetName,
                    "{preview}", translatedPrefix + " " + translatedColor + targetName
                            + CC.translate("&f") + ": " + CC.translate("&7")
                            + "Hi! This is what your message would look like."
            );

            buttons.put(slot++, new Button(item, e -> {
                if (!ranksSection.contains(finalRank)) {
                    player.sendMessage(plugin.getMessageConfig().get("rank-menu.invalid-rank", "&cInvalid rank selected."));
                    return;
                }
                state = new GrantState(targetUUID, finalRank);
                pendingGrants.put(player.getUniqueId(), state);
                view = View.DURATION;
                open(player);
            }));
        }

        buttons.put(plugin.getMenuConfig().getSlot("rank-menu.cancel-button", getSize() - 1),
                cancelButton(player, "rank-menu.cancel-button", "&cRank selection cancelled."));
        return buttons;
    }

    private static char normalizeColorChar(String colorCode) {
        if (colorCode == null || colorCode.isEmpty()) {
            return 'f';
        }
        String normalized = colorCode.replace("&", "").replace("§", "").trim();
        return normalized.isEmpty() ? 'f' : Character.toLowerCase(normalized.charAt(0));
    }

    private static org.bukkit.DyeColor dyeColor(char colorChar) {
        switch (colorChar) {
            case '0':
                return org.bukkit.DyeColor.BLACK;
            case '1':
                return org.bukkit.DyeColor.BLUE;
            case '2':
                return org.bukkit.DyeColor.GREEN;
            case '3':
                return org.bukkit.DyeColor.CYAN;
            case '4':
                return org.bukkit.DyeColor.RED;
            case '5':
                return org.bukkit.DyeColor.PURPLE;
            case '6':
                return org.bukkit.DyeColor.ORANGE;
            case '7':
                return org.bukkit.DyeColor.SILVER;
            case '8':
                return org.bukkit.DyeColor.GRAY;
            case '9':
                return org.bukkit.DyeColor.LIGHT_BLUE;
            case 'a':
                return org.bukkit.DyeColor.LIME;
            case 'b':
                return org.bukkit.DyeColor.LIGHT_BLUE;
            case 'c':
                return org.bukkit.DyeColor.RED;
            case 'd':
                return org.bukkit.DyeColor.PINK;
            case 'e':
                return org.bukkit.DyeColor.YELLOW;
            default:
                return org.bukkit.DyeColor.WHITE;
        }
    }

    private Map<Integer, Button> buildDurationButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        buttons.put(plugin.getMenuConfig().getSlot("rank-menu.duration.adjustments.add-hour", 0),
                adjustButton(player, "rank-menu.duration.adjustments.add-hour", HOUR, true));
        buttons.put(plugin.getMenuConfig().getSlot("rank-menu.duration.adjustments.add-day", 1),
                adjustButton(player, "rank-menu.duration.adjustments.add-day", DAY, true));
        buttons.put(plugin.getMenuConfig().getSlot("rank-menu.duration.adjustments.add-week", 2),
                adjustButton(player, "rank-menu.duration.adjustments.add-week", WEEK, true));
        buttons.put(plugin.getMenuConfig().getSlot("rank-menu.duration.adjustments.add-month", 3),
                adjustButton(player, "rank-menu.duration.adjustments.add-month", MONTH, true));
        buttons.put(plugin.getMenuConfig().getSlot("rank-menu.duration.adjustments.remove-hour", 5),
                adjustButton(player, "rank-menu.duration.adjustments.remove-hour", HOUR, false));
        buttons.put(plugin.getMenuConfig().getSlot("rank-menu.duration.adjustments.remove-day", 6),
                adjustButton(player, "rank-menu.duration.adjustments.remove-day", DAY, false));
        buttons.put(plugin.getMenuConfig().getSlot("rank-menu.duration.adjustments.remove-week", 7),
                adjustButton(player, "rank-menu.duration.adjustments.remove-week", WEEK, false));
        buttons.put(plugin.getMenuConfig().getSlot("rank-menu.duration.adjustments.remove-month", 8),
                adjustButton(player, "rank-menu.duration.adjustments.remove-month", MONTH, false));

        buttons.put(plugin.getMenuConfig().getSlot("rank-menu.duration.permanent-button", 13),
                new Button(plugin.getMenuConfig().buildItem("rank-menu.duration.permanent-button", Material.BEDROCK), e -> {
                    state.permanent = true;
                    state.durationMillis = 0L;
                    refresh(player);
                }));

        String durationLabel = state != null && state.permanent
                ? "&6Permanent"
                : "&e" + formatDuration(state != null ? state.durationMillis : 0L);
        buttons.put(plugin.getMenuConfig().getSlot("rank-menu.duration.current-duration", 22),
                new Button(plugin.getMenuConfig().buildItem("rank-menu.duration.current-duration",
                        Material.PAPER,
                        "{duration}", durationLabel)));

        buttons.put(plugin.getMenuConfig().getSlot("rank-menu.duration.continue-button", 26),
                new Button(plugin.getMenuConfig().buildItem("rank-menu.duration.continue-button", Material.WOOL, (short) 5), e -> {
                    player.closeInventory();
                    player.sendMessage(plugin.getMenuConfig().getString("rank-menu.messages.reason-prompt",
                            "&ePlease type the reason for granting this rank in chat. Type &ccancel &eto abort."));
                }));

        buttons.put(plugin.getMenuConfig().getSlot("rank-menu.duration.cancel-button", 18),
                cancelButton(player, "rank-menu.duration.cancel-button", "&cRank grant cancelled."));
        return buttons;
    }

    private Button adjustButton(Player player, String path, long amount, boolean add) {
        return new Button(plugin.getMenuConfig().buildItem(path, Material.WATCH), e -> {
            if (add) {
                state.durationMillis += amount;
            } else {
                state.durationMillis = Math.max(0, state.durationMillis - amount);
            }
            state.permanent = false;
            refresh(player);
        });
    }

    private Button cancelButton(Player player, String path, String messageFallback) {
        Material mat = Utilities.IS_1_7 ? Material.STAINED_GLASS_PANE : Material.BARRIER;
        short data = Utilities.IS_1_7 ? (short) 14 : 0;
        return new Button(plugin.getMenuConfig().buildItem(path, mat, data), e -> {
            pendingGrants.remove(player.getUniqueId());
            player.closeInventory();
            player.sendMessage(plugin.getMenuConfig().getString(path + ".message", messageFallback));
        });
    }

    private String formatDuration(long millis) {
        if (millis <= 0) {
            return "0";
        }

        long months = millis / MONTH;
        millis %= MONTH;
        long weeks = millis / WEEK;
        millis %= WEEK;
        long days = millis / DAY;
        millis %= DAY;
        long hours = millis / HOUR;

        StringBuilder sb = new StringBuilder();
        if (months > 0) {
            sb.append(months).append("mo ");
        }
        if (weeks > 0) {
            sb.append(weeks).append("w ");
        }
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h");
        }
        return sb.toString().trim();
    }

    public static boolean isPendingGrant(UUID staffUUID) {
        return pendingGrants.containsKey(staffUUID);
    }

    public static class ChatListener implements Listener {

        private final WinterCore plugin;

        public ChatListener(WinterCore plugin) {
            this.plugin = plugin;
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void onPlayerChat(AsyncPlayerChatEvent event) {
            Player player = event.getPlayer();
            GrantState gs = pendingGrants.get(player.getUniqueId());
            if (gs == null) {
                return;
            }

            event.setCancelled(true);
            String message = event.getMessage();

            if (message.equalsIgnoreCase("cancel")) {
                pendingGrants.remove(player.getUniqueId());
                player.sendMessage(plugin.getMenuConfig().getString("rank-menu.messages.cancelled",
                        "&cRank grant cancelled."));
                return;
            }

            pendingGrants.remove(player.getUniqueId());

            long now = System.currentTimeMillis();
            Long expiresAt = gs.permanent ? null : (gs.durationMillis > 0 ? now + gs.durationMillis : null);

            plugin.getDatabaseManager().getProfileService().setRankWithMeta(gs.targetUUID, gs.rank, player.getUniqueId(), now, expiresAt, message);
            plugin.getDatabaseManager().getModerationService().addRankGrant(gs.targetUUID, gs.rank, player.getUniqueId(), now, expiresAt, message);
            plugin.getRedisManager().publish(new RankTagSyncPacket(
                    plugin.getConfig().getString("server-name", "Unknown"),
                    System.currentTimeMillis(),
                    gs.targetUUID,
                    gs.rank
            ));

            String grantedName = Bukkit.getOfflinePlayer(gs.targetUUID).getName();
            player.sendMessage(plugin.getMenuConfig().getString("rank-menu.messages.granted",
                    "&aGranted rank &e{rank} &ato &b{target}&a.",
                    "{rank}", gs.rank,
                    "{target}", grantedName == null ? gs.targetUUID.toString() : grantedName));

            Bukkit.getScheduler().runTask(plugin, () -> {
                Player target = Bukkit.getPlayer(gs.targetUUID);
                if (target != null) {
                    plugin.getRankManager().cachePlayerRank(target, gs.rank);
                    plugin.getRankManager().refreshPlayerDisplay(target);
                    plugin.getRankManager().refreshPlayerDisplayForAll(target);
                    Bukkit.getPluginManager().callEvent(new RankChangeEvent(
                            target,
                            gs.rank,
                            plugin.getRankManager().getRankSync(target)
                    ));
                }
            });
        }
    }
}
