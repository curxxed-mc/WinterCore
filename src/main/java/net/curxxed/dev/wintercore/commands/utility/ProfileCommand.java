package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.commands.staff.VanishCommand;
import net.curxxed.dev.wintercore.database.redis.RedisSocials;
import net.curxxed.dev.wintercore.listeners.FreezeListener;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.rank.RankManager;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
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
                viewer.sendMessage(CC.RED + "Player not found.");
                return;
            }
            target = found;
        }

        Player finalTarget = target;
        runAsync(() -> openProfileInventory(viewer, finalTarget));
    }

    private void openProfileInventory(Player viewer, Player target) {
        Map<String, String> socials = redis.getAllSocialLinks(target.getUniqueId());

        rankManager.getRank(target, rank ->
                rankManager.getRankPrefix(target, prefix ->
                        rankManager.getColorPreference(rank, color ->
                                runSync(() -> {
                                    Inventory inv = Bukkit.createInventory(null, 54,
                                            CC.DARK_GRAY + (target.equals(viewer) ? "Your Profile" : target.getName() + "'s Profile"));
                                    String coloredName = CC.translateAlternateColorCodes('&', color) + target.getName();

                                    ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
                                    SkullMeta meta = (SkullMeta) skull.getItemMeta();
                                    meta.setOwner(target.getName());
                                    meta.setDisplayName(coloredName);
                                    meta.setLore(Arrays.asList(
                                            CC.GRAY + "Rank: " + CC.translateAlternateColorCodes('&', color) + rank,
                                            CC.GRAY + "Game Mode: " + CC.AQUA + target.getGameMode(),
                                            CC.GRAY + "Frozen: " + (FreezeListener.getInstance().isPlayerFrozen(target) ? CC.GREEN + "Yes" : CC.RED + "No"),
                                            CC.GRAY + "Vanished: " + (VanishCommand.vanishedPlayers.contains(target.getUniqueId()) ? CC.GREEN + "Yes" : CC.RED + "No"),
                                            CC.GRAY + "Staff: " + (target.hasPermission("WinterCore.staff") || target.hasPermission("WinterCore.Admin")
                                                    || target.isOp() || target.hasPermission("WinterCore.Manager") ? CC.GREEN + "Yes" : CC.RED + "No")
                                    ));
                                    skull.setItemMeta(meta);
                                    inv.setItem(13, skull);

                                    boolean isSelf = target.getUniqueId().equals(viewer.getUniqueId());

                                    inv.setItem(29, createCustomHead("Discord", "Discord", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzg3M2MxMmJmZmI1MjUxYTBiODhkNWFlNzVjNzI0N2NiMzlhNzVmZjFhODFjYmU0YzhhMzliMzExZGRlZGEifX19", socials.get("discord"), isSelf));
                                    inv.setItem(31, createCustomHead("YouTube", "YouTube", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDJmNmMwN2EzMjZkZWY5ODRlNzJmNzcyZWQ2NDU0NDlmNWVjOTZjNmNhMjU2NDk5YjVkMmI4NGE4ZGNlIn19fQ==", socials.get("youtube"), isSelf));
                                    inv.setItem(33, createCustomHead("Twitter", "Twitter", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTFiN2EwYzIxMGU2Y2RmNWEzNWZkODE5N2U2ZTI0YTAzODMxNWJiZTNiZGNkMWJjYzM2MzBiZjI2ZjU5ZWM1YyJ9fX0==", socials.get("twitter"), isSelf));

                                    viewer.openInventory(inv);
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

        String title = CC.stripColor(e.getView().getTitle());
        if (!title.endsWith("Profile")) return;

        int slot = e.getRawSlot();
        if (slot >= e.getInventory().getSize()) return;

        e.setCancelled(true);

        if (clickCooldown.contains(uuid)) return;
        clickCooldown.add(uuid);
        Bukkit.getScheduler().runTaskLater(plugin, () -> clickCooldown.remove(uuid), 10L);

        boolean isSelf = title.equalsIgnoreCase("Your Profile");

        if (e.getClick() != ClickType.LEFT && e.getClick() != ClickType.RIGHT) return;

        if (isSelf) {
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
                    p.sendMessage(CC.YELLOW + "Click this to open the link: " + CC.UNDERLINE + link);
                } else {
                    p.sendMessage(CC.RED + "No link set.");
                }
            }
        } else {
            if (e.getClick() == ClickType.LEFT) {
                String link;
                String playerName = title.replace("'s Profile", "");
                UUID targetUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
                switch (slot) {
                    case 29:
                        link = redis.getSocialLink(targetUUID, "Discord");
                        break;
                    case 31:
                        link = redis.getSocialLink(targetUUID, "YouTube");
                        break;
                    case 33:
                        link = redis.getSocialLink(targetUUID, "Twitter");
                        break;
                    default:
                        return;
                }
                if (link != null && !link.isEmpty()) {
                    p.sendMessage(CC.YELLOW + "Click this to open the link: " + CC.UNDERLINE + link);
                } else {
                    p.sendMessage(CC.RED + "No link set.");
                }
            }
        }
    }

    private ItemStack createCustomHead(String name, String platform, String texture, String value, boolean isSelf) {
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

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

        meta.setDisplayName(CC.AQUA + name);

        List<String> lore = new ArrayList<>();
        lore.add(CC.GRAY + (value != null ? value : "Not set"));
        if (isSelf) {
            lore.add(CC.YELLOW + "Right-click to edit");
            lore.add(CC.GREEN + "Left-click to open");
        }
        meta.setLore(lore);

        head.setItemMeta(meta);
        return head;
    }
}





