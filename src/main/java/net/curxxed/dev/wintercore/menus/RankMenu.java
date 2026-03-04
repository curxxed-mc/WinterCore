package net.curxxed.dev.wintercore.menus;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RankMenu extends Menu implements Listener {

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

    private static final Map<UUID, GrantState> pendingGrants = new ConcurrentHashMap<>();

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
            String colorCode = ranksSection.getString(rank + ".name-color", "&f");
            final String finalRank = rank;
            buttons.put(slot++, ButtonBuilder.of(Material.PAPER)
                    .name(colorCode + rank)
                    .lore("&7Click to grant &b" + rank + " &7to &e" + targetName)
                    .action(e -> {
                        if (!ranksSection.contains(finalRank)) {
                            player.sendMessage(CC.translate("&cInvalid rank selected."));
                            return;
                        }
                        state = new GrantState(targetUUID, finalRank);
                        pendingGrants.put(player.getUniqueId(), state);
                        view = View.DURATION;
                        open(player);
                    })
                    .build());
        }

        buttons.put(getSize() - 1, cancelButton(player, "&cRank selection cancelled."));
        return buttons;
    }

    private Map<Integer, Button> buildDurationButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        buttons.put(0, adjustButton(player, "&a+1 Hour",   HOUR,  true));
        buttons.put(1, adjustButton(player, "&a+1 Day",    DAY,   true));
        buttons.put(2, adjustButton(player, "&a+1 Week",   WEEK,  true));
        buttons.put(3, adjustButton(player, "&a+1 Month",  MONTH, true));
        buttons.put(5, adjustButton(player, "&c-1 Hour",   HOUR,  false));
        buttons.put(6, adjustButton(player, "&c-1 Day",    DAY,   false));
        buttons.put(7, adjustButton(player, "&c-1 Week",   WEEK,  false));
        buttons.put(8, adjustButton(player, "&c-1 Month",  MONTH, false));

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
        if (weeks  > 0) sb.append(weeks) .append("w ");
        if (days   > 0) sb.append(days)  .append("d ");
        if (hours  > 0) sb.append(hours) .append("h");
        return sb.toString().trim();
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

        plugin.getDatabaseManager().setRankWithMeta(gs.targetUUID, gs.rank, player.getUniqueId(), now, expiresAt, message);
        plugin.getDatabaseManager().addRankGrant(gs.targetUUID, gs.rank, player.getUniqueId(), now, expiresAt, message);

        String grantedName = Bukkit.getOfflinePlayer(gs.targetUUID).getName();
        player.sendMessage(CC.translate("&aGranted rank &e" + gs.rank + " &ato &b" + grantedName + "&a."));

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player target = Bukkit.getPlayer(gs.targetUUID);
            if (target != null) {
                plugin.getRankManager().cachePlayerRank(target, gs.rank);
                plugin.getRankManager().refreshPlayerDisplay(target);
                plugin.getRankManager().refreshPlayerDisplayForAll(target);
                Bukkit.getPluginManager().callEvent(
                        new net.curxxed.dev.wintercore.rank.RankChangeEvent(target, gs.rank, plugin.getRankManager().getRankSync(target)));
            }
        });
    }

    public static boolean isPendingGrant(UUID staffUUID) {
        return pendingGrants.containsKey(staffUUID);
    }
}