package net.curxxed.dev.wintercore.menus;

import net.curxxed.dev.wintercore.events.network.RankChangeEvent;
import net.curxxed.dev.wintercore.database.redis.packet.packets.RankTagSyncPacket;
import net.curxxed.dev.wintercore.menu.Button;
import net.curxxed.dev.wintercore.menu.ButtonBuilder;
import net.curxxed.dev.wintercore.menu.Menu;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
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

    private static final long HOUR  = 3_600_000L;
    private static final long DAY   = 86_400_000L;
    private static final long WEEK  = 604_800_000L;
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
                ? "Set rank for " + targetName
                : "Set duration for " + targetName;
    }

    @Override
    public int getSize() {
        if (view == View.DURATION) return 27;
        int count = plugin.getRankManager().getSortedRanks().size() + 1;
        return Math.min(54, ((count - 1) / 9 + 1) * 9);
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
            if (rank == null || rank.isEmpty()) continue;

            String colorCode          = ranksSection.getString(rank + ".name-color", "&f");
            String translatedColor    = CC.translate(colorCode);
            String prefix             = ranksSection.getString(rank + ".prefix", "");
            String translatedPrefix   = CC.translate(prefix);
            final String finalRank    = rank;

            org.bukkit.ChatColor chatColor = org.bukkit.ChatColor.getByChar(
                    colorCode.replace("&", "").replace("§", "").charAt(0));
            if (chatColor == null) chatColor = org.bukkit.ChatColor.WHITE;

            ItemStack item;
            if (chatColor == org.bukkit.ChatColor.DARK_RED) {
                item = new ItemStack(Material.STAINED_CLAY, 1, (short) 14);
            } else {
                org.bukkit.DyeColor dye = dyeColor(chatColor);
                item = new ItemStack(Material.WOOL, 1, dye.getWoolData());
            }

            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(translatedColor + rank);

            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add(CC.translate("&7&m------------------------"));
            lore.add(CC.translate("&6Preview:"));
            lore.add(translatedPrefix + " " + translatedColor + targetName
                    + CC.translate("&f") + ": " + CC.translate("&7")
                    + "Hi! This is what your message would look like.");
            lore.add(CC.translate("&7&m------------------------"));
            lore.add(CC.translate("&aClick to grant this rank to &b" + targetName + "&a."));
            meta.setLore(lore);
            item.setItemMeta(meta);

            buttons.put(slot++, new Button(item, e -> {
                if (!ranksSection.contains(finalRank)) {
                    player.sendMessage(CC.translate("&cInvalid rank selected."));
                    return;
                }
                state = new GrantState(targetUUID, finalRank);
                pendingGrants.put(player.getUniqueId(), state);
                view = View.DURATION;
                open(player);
            }));
        }

        buttons.put(getSize() - 1, cancelButton(player, "&cRank selection cancelled."));
        return buttons;
    }

    private static org.bukkit.DyeColor dyeColor(org.bukkit.ChatColor color) {
        switch (color) {
            case BLACK:        return org.bukkit.DyeColor.BLACK;
            case DARK_BLUE:    return org.bukkit.DyeColor.BLUE;
            case DARK_GREEN:   return org.bukkit.DyeColor.GREEN;
            case DARK_AQUA:    return org.bukkit.DyeColor.CYAN;
            case DARK_RED:     return org.bukkit.DyeColor.RED;
            case DARK_PURPLE:  return org.bukkit.DyeColor.PURPLE;
            case GOLD:         return org.bukkit.DyeColor.ORANGE;
            case GRAY:         return org.bukkit.DyeColor.SILVER;
            case DARK_GRAY:    return org.bukkit.DyeColor.GRAY;
            case BLUE:         return org.bukkit.DyeColor.LIGHT_BLUE;
            case GREEN:        return org.bukkit.DyeColor.LIME;
            case AQUA:         return org.bukkit.DyeColor.LIGHT_BLUE;
            case RED:          return org.bukkit.DyeColor.RED;
            case LIGHT_PURPLE: return org.bukkit.DyeColor.PINK;
            case YELLOW:       return org.bukkit.DyeColor.YELLOW;
            default:           return org.bukkit.DyeColor.WHITE;
        }
    }

    private Map<Integer, Button> buildDurationButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        buttons.put(0, adjustButton(player, "&a+1 Hour",  HOUR,  true));
        buttons.put(1, adjustButton(player, "&a+1 Day",   DAY,   true));
        buttons.put(2, adjustButton(player, "&a+1 Week",  WEEK,  true));
        buttons.put(3, adjustButton(player, "&a+1 Month", MONTH, true));
        buttons.put(5, adjustButton(player, "&c-1 Hour",  HOUR,  false));
        buttons.put(6, adjustButton(player, "&c-1 Day",   DAY,   false));
        buttons.put(7, adjustButton(player, "&c-1 Week",  WEEK,  false));
        buttons.put(8, adjustButton(player, "&c-1 Month", MONTH, false));

        buttons.put(13, ButtonBuilder.of(Material.BEDROCK)
                .name("&6Permanent")
                .action(e -> {
                    state.permanent = true;
                    state.durationMillis = 0L;
                    refresh(player);
                }).build());

        String durationLabel = state != null && state.permanent
                ? "&6Permanent"
                : "&e" + formatDuration(state != null ? state.durationMillis : 0L);
        buttons.put(22, ButtonBuilder.of(Material.PAPER)
                .name("&bCurrent Duration: " + durationLabel)
                .build());

        buttons.put(26, ButtonBuilder.of(Material.WOOL).data((short) 5)
                .name("&aContinue")
                .action(e -> {
                    player.closeInventory();
                    player.sendMessage(CC.translate(
                            "&ePlease type the reason for granting this rank in chat. Type &ccancel &eto abort."));
                }).build());

        buttons.put(18, cancelButton(player, "&cRank grant cancelled."));
        return buttons;
    }

    private Button adjustButton(Player player, String label, long amount, boolean add) {
        return ButtonBuilder.of(Material.WATCH)
                .name(label)
                .action(e -> {
                    if (add) {
                        state.durationMillis += amount;
                    } else {
                        state.durationMillis = Math.max(0, state.durationMillis - amount);
                    }
                    state.permanent = false;
                    refresh(player);
                }).build();
    }

    private Button cancelButton(Player player, String message) {
        Material mat = Utilities.IS_1_7 ? Material.STAINED_GLASS_PANE : Material.BARRIER;
        short data = Utilities.IS_1_7 ? (short) 14 : 0;
        return ButtonBuilder.of(mat).data(data)
                .name("&cCancel")
                .action(e -> {
                    pendingGrants.remove(player.getUniqueId());
                    player.closeInventory();
                    player.sendMessage(CC.translate(message));
                }).build();
    }

    private String formatDuration(long millis) {
        if (millis <= 0) return "0";
        long months = millis / MONTH; millis %= MONTH;
        long weeks  = millis / WEEK;  millis %= WEEK;
        long days   = millis / DAY;   millis %= DAY;
        long hours  = millis / HOUR;
        StringBuilder sb = new StringBuilder();
        if (months > 0) sb.append(months).append("mo ");
        if (weeks  > 0) sb.append(weeks).append("w ");
        if (days   > 0) sb.append(days).append("d ");
        if (hours  > 0) sb.append(hours).append("h");
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
            if (gs == null) return;

            event.setCancelled(true);
            String message = event.getMessage();

            if (message.equalsIgnoreCase("cancel")) {
                pendingGrants.remove(player.getUniqueId());
                player.sendMessage(CC.translate("&cRank grant cancelled."));
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
            player.sendMessage(CC.translate("&aGranted rank &e" + gs.rank + " &ato &b" + grantedName + "&a."));

            Bukkit.getScheduler().runTask(plugin, () -> {
                Player target = Bukkit.getPlayer(gs.targetUUID);
                if (target != null) {
                    plugin.getRankManager().cachePlayerRank(target, gs.rank);
                    plugin.getRankManager().refreshPlayerDisplay(target);
                    plugin.getRankManager().refreshPlayerDisplayForAll(target);
                    Bukkit.getPluginManager().callEvent(
                            new RankChangeEvent(
                                    target, gs.rank, plugin.getRankManager().getRankSync(target)));
                }
            });
        }
    }
}
