package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.commands.staff.VanishCommand;
import net.curxxed.dev.wintercore.database.redis.RedisSocials;
import net.curxxed.dev.wintercore.listeners.FreezeListener;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.rank.RankManager;
import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.utils.ItemBuilder;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.*;

@CommandInfo(
        name = "profile",
        description = "View your or another player's profile.",
        usage = "/profile [player]",
        inGameOnly = true,
        async = true,
        permission = {}
)
public class ProfileCommand extends BaseCommand implements Listener {

    private final WinterCore plugin;
    private final RankManager rankManager;
    private final RedisSocials redis;
    private final Set<UUID> clickCooldown = new HashSet<>();
    private final Map<UUID, ProfileView> openProfiles = new HashMap<>();

    public ProfileCommand(WinterCore plugin, RedisSocials redis) {
        super(plugin);
        this.plugin = plugin;
        this.rankManager = plugin.getRankManager();
        this.redis = redis;
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        runSync(() -> resolveAndOpenProfile(commandArgs));
    }

    private void resolveAndOpenProfile(CommandArguments commandArgs) {
        Player viewer = commandArgs.getPlayer();
        Player target = viewer;
        if (commandArgs.length() > 0) {
            Player found = Bukkit.getPlayer(commandArgs.getArgs()[0]);
            if (found == null) {
                send(viewer, "general.player-not-found", "&cPlayer not found.");
                return;
            }
            target = found;
        }

        Player finalTarget = target;
        UUID targetUuid = finalTarget.getUniqueId();
        runAsync(() -> {
            Map<String, String> socials = redis.getAllSocialLinks(targetUuid);
            runSync(() -> openProfileInventory(viewer, finalTarget, socials));
        });
    }

    private void openProfileInventory(Player viewer, Player target, Map<String, String> socials) {
        if (!viewer.isOnline() || !target.isOnline()) {
            return;
        }

        rankManager.getRank(target, rank ->
                rankManager.getRankPrefix(target, prefix ->
                        rankManager.getColorPreference(rank, color ->
                                runSync(() -> {
                                    boolean isSelf = target.getUniqueId().equals(viewer.getUniqueId());
                                    Inventory inv = Bukkit.createInventory(null, 54,
                                            isSelf
                                                    ? msg("profile.title.self", "&8Your Profile")
                                                    : msg("profile.title.other", "&8{target}'s Profile", "{target}", target.getName()));
                                    String coloredName = CC.translate(color) + target.getName();

                                    ItemStack skull = new ItemBuilder(Material.SKULL_ITEM, 1, (byte) 3).toItemStack();
                                    SkullMeta meta = (SkullMeta) skull.getItemMeta();
                                    meta.setOwner(target.getName());
                                    meta.setDisplayName(coloredName);
                                    meta.setLore(msgList("profile.skull-lore", Arrays.asList(
                                            "&7Rank: {rank_color}{rank}",
                                            "&7Game Mode: &b{gamemode}",
                                            "&7Frozen: {frozen}",
                                            "&7Vanished: {vanished}",
                                            "&7Staff: {staff}"
                                    ), "{rank}", rank,
                                            "{rank_color}", color,
                                            "{gamemode}", String.valueOf(target.getGameMode()),
                                            "{frozen}", booleanLabel(FreezeListener.getInstance().isPlayerFrozen(target)),
                                            "{vanished}", booleanLabel(VanishCommand.vanishedPlayers.contains(target.getUniqueId())),
                                            "{staff}", booleanLabel(target.hasPermission("WinterCore.staff") || target.hasPermission("WinterCore.Admin")
                                                    || target.isOp() || target.hasPermission("WinterCore.Manager"))));
                                    skull.setItemMeta(meta);
                                    inv.setItem(13, skull);

                                    inv.setItem(29, createCustomHead("Discord", "Discord", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzg3M2MxMmJmZmI1MjUxYTBiODhkNWFlNzVjNzI0N2NiMzlhNzVmZjFhODFjYmU0YzhhMzliMzExZGRlZGEifX19", socials.get("discord"), isSelf));
                                    inv.setItem(31, createCustomHead("YouTube", "YouTube", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDJmNmMwN2EzMjZkZWY5ODRlNzJmNzcyZWQ2NDU0NDlmNWVjOTZjNmNhMjU2NDk5YjVkMmI4NGE4ZGNlIn19fQ==", socials.get("youtube"), isSelf));
                                    inv.setItem(33, createCustomHead("Twitter", "Twitter", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTFiN2EwYzIxMGU2Y2RmNWEzNWZkODE5N2U2ZTI0YTAzODMxNWJiZTNiZGNkMWJjYzM2MzBiZjI2ZjU5ZWM1YyJ9fX0==", socials.get("twitter"), isSelf));

                                    viewer.openInventory(inv);
                                    openProfiles.put(viewer.getUniqueId(), new ProfileView(target.getUniqueId(), isSelf));
                                })
                        )
                )
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;

        Player p = (Player) e.getWhoClicked();
        UUID uuid = p.getUniqueId();

        ProfileView view = openProfiles.get(uuid);
        if (view == null) return;

        int slot = e.getRawSlot();
        if (slot >= e.getInventory().getSize()) return;

        e.setCancelled(true);

        if (clickCooldown.contains(uuid)) return;
        clickCooldown.add(uuid);
        plugin.getTasks().later(() -> clickCooldown.remove(uuid), 10L);

        if (e.getClick() != ClickType.LEFT && e.getClick() != ClickType.RIGHT) return;

        if (view.self) {
            if (e.getClick() == ClickType.LEFT) {
                String link;
                switch (slot) {
                    case 29:
                        link = redis.getSocialLink(uuid, "Discord");
                        break;
                    case 31:
                        link = redis.getSocialLink(uuid, "YouTube");
                        break;
                    case 33:
                        link = redis.getSocialLink(uuid, "Twitter");
                        break;
                    default:
                        return;
                }
                if (link != null && !link.isEmpty()) {
                    send(p, "profile.link-open", "&eClick this to open the link: &n{link}",
                            "{link}", link);
                } else {
                    send(p, "profile.no-link", "&cNo link set.");
                }
            }
        } else {
            if (e.getClick() == ClickType.LEFT) {
                String link;
                switch (slot) {
                    case 29:
                        link = redis.getSocialLink(view.targetUuid, "Discord");
                        break;
                    case 31:
                        link = redis.getSocialLink(view.targetUuid, "YouTube");
                        break;
                    case 33:
                        link = redis.getSocialLink(view.targetUuid, "Twitter");
                        break;
                    default:
                        return;
                }
                if (link != null && !link.isEmpty()) {
                    send(p, "profile.link-open", "&eClick this to open the link: &n{link}",
                            "{link}", link);
                } else {
                    send(p, "profile.no-link", "&cNo link set.");
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            openProfiles.remove(event.getPlayer().getUniqueId());
        }
    }

    private ItemStack createCustomHead(String name, String platform, String texture, String value, boolean isSelf) {
        ItemBuilder headBuilder = new ItemBuilder(Material.SKULL_ITEM, 1, (byte) 3);
        SkullMeta meta = (SkullMeta) headBuilder.toItemStack().getItemMeta();

        try {
            Class<?> gameProfileClass = Utilities.resolveAuthlibClass("GameProfile");
            Class<?> propertyClass = Utilities.resolveAuthlibClass("properties.Property");

            Object profile = gameProfileClass
                    .getConstructor(UUID.class, String.class)
                    .newInstance(UUID.randomUUID(), null);

            Object property = propertyClass
                    .getConstructor(String.class, String.class)
                    .newInstance("textures", texture);

            Object properties = gameProfileClass.getMethod("getProperties").invoke(profile);

            properties.getClass()
                    .getMethod("put", Object.class, Object.class)
                    .invoke(properties, "textures", property);

            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);

        } catch (Exception e) {
            e.printStackTrace();
        }

        meta.setDisplayName(msg("profile.social-item.name", "&b{platform}", "{platform}", name));

        List<String> lore = new ArrayList<>(msgList("profile.social-item.lore", Collections.singletonList(
                "&7{value}"
        ), "{value}", value != null ? value : msg("profile.social-item.not-set", "Not set")));
        if (isSelf) {
            lore.addAll(msgList("profile.social-item.self-lore", Arrays.asList(
                    "&eRight-click to edit",
                    "&aLeft-click to open"
            )));
        }
        meta.setLore(lore);

        headBuilder.toItemStack().setItemMeta(meta);
        return headBuilder.toItemStack();
    }

    private String booleanLabel(boolean value) {
        return value
                ? msg("profile.boolean.yes", "&aYes")
                : msg("profile.boolean.no", "&cNo");
    }

    private static final class ProfileView {
        private final UUID targetUuid;
        private final boolean self;

        private ProfileView(UUID targetUuid, boolean self) {
            this.targetUuid = targetUuid;
            this.self = self;
        }
    }
}
