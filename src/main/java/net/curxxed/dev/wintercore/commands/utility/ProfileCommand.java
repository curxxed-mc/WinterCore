package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.staff.VanishCommand;
import net.curxxed.dev.wintercore.database.RedisManager;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.listeners.FreezeListener;
import net.curxxed.dev.wintercore.rank.RankManager;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.*;

@CommandInfo(
        name = "profile",
            description = "View your or another player's profile.",
            usage = "/profile [player]",
            inGameOnly = true
    
    )
public class ProfileCommand extends BaseCommand implements Listener {

    private final WinterCore plugin;
    private final RankManager rankManager;
    private final RedisManager redis;
    private final Set<UUID> clickCooldown = new HashSet<>();

    public ProfileCommand(WinterCore plugin, RedisManager redis) {
        super(plugin);
        this.plugin = plugin;
        this.rankManager = plugin.getRankManager();
        this.redis = redis;
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();
        String[] args = commandArgs.getArgs();

        Player target = player;
        if (args.length > 0) {
            Player found = Bukkit.getPlayer(args[0]);
            if (found != null) {
                target = found;
            } else {
                player.sendMessage(ChatColor.RED + "Player not found.");
                return;
            }
        }

        Player finalTarget = target;

        rankManager.getRank(finalTarget, rank ->
                rankManager.getRankPrefix(finalTarget, prefix ->
                        rankManager.getColorPreference(rank, color ->
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    Inventory inv = Bukkit.createInventory(null, 54,
                                            ChatColor.DARK_GRAY + (finalTarget.equals(player) ? "Your Profile" : finalTarget.getName() + "'s Profile"));
                                    String coloredName = ChatColor.translateAlternateColorCodes('&', color) + finalTarget.getName();

                                    // Player Skull
                                    ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
                                    SkullMeta meta = (SkullMeta) skull.getItemMeta();
                                    meta.setOwner(finalTarget.getName());
                                    meta.setDisplayName(coloredName);
                                    meta.setLore(Arrays.asList(
                                            ChatColor.GRAY + "Rank: " + ChatColor.translateAlternateColorCodes('&', color) + rank,
                                            ChatColor.GRAY + "Game Mode: " + ChatColor.AQUA + finalTarget.getGameMode(),
                                            ChatColor.GRAY + "Frozen: " + (FreezeListener.getInstance().isPlayerFrozen(finalTarget) ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"),
                                            ChatColor.GRAY + "Vanished: " + (VanishCommand.vanishedPlayers.contains(finalTarget.getUniqueId()) ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"),
                                            ChatColor.GRAY + "Staff: " + (finalTarget.hasPermission("WinterCore.staff") || finalTarget.hasPermission("WinterCore.Admin")
                                                    || finalTarget.isOp() || finalTarget.hasPermission("WinterCore.Manager") ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No")
                                    ));
                                    skull.setItemMeta(meta);
                                    inv.setItem(13, skull);

                                    boolean isSelf = finalTarget.getUniqueId().equals(player.getUniqueId());

                                    inv.setItem(29, createCustomHead("Discord", "Discord", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzg3M2MxMmJmZmI1MjUxYTBiODhkNWFlNzVjNzI0N2NiMzlhNzVmZjFhODFjYmU0YzhhMzliMzExZGRlZGEifX19", redis.getSocialLink(finalTarget.getUniqueId(), "Discord"), isSelf));
                                    inv.setItem(31, createCustomHead("YouTube", "YouTube", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDJmNmMwN2EzMjZkZWY5ODRlNzJmNzcyZWQ2NDU0NDlmNWVjOTZjNmNhMjU2NDk5YjVkMmI4NGE4ZGNlIn19fQ==", redis.getSocialLink(finalTarget.getUniqueId(), "YouTube"), isSelf));
                                    inv.setItem(33, createCustomHead("Twitter", "Twitter", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTFiN2EwYzIxMGU2Y2RmNWEzNWZkODE5N2U2ZTI0YTAzODMxNWJiZTNiZGNkMWJjYzM2MzBiZjI2ZjU5ZWM1YyJ9fX0==", redis.getSocialLink(finalTarget.getUniqueId(), "Twitter"), isSelf));

                                    player.openInventory(inv);
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

        String title = ChatColor.stripColor(e.getView().getTitle());
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
                    p.sendMessage(ChatColor.YELLOW + "Click this to open the link: " + ChatColor.UNDERLINE + link);
                } else {
                    p.sendMessage(ChatColor.RED + "No link set.");
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
                    p.sendMessage(ChatColor.YELLOW + "Click this to open the link: " + ChatColor.UNDERLINE + link);
                } else {
                    p.sendMessage(ChatColor.RED + "No link set.");
                }
            }
        }
    }

    private ItemStack createCustomHead(String name, String platform, String texture, String value, boolean isSelf) {
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);
            profile.getProperties().put("textures", new Property("textures", texture));
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        meta.setDisplayName(ChatColor.AQUA + name);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + (value != null ? value : "Not set"));
        if (isSelf) {
            lore.add(ChatColor.YELLOW + "Right-click to edit");
            lore.add(ChatColor.GREEN + "Left-click to open");
        }
        meta.setLore(lore);

        head.setItemMeta(meta);
        return head;
    }
}