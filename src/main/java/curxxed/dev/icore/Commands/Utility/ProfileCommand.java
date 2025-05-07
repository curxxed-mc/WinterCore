package curxxed.dev.icore.Commands.Utility;

import curxxed.dev.icore.Commands.Staff.VanishCommand;
import curxxed.dev.icore.Database.RedisManager;
import curxxed.dev.icore.iCore;
import curxxed.dev.icore.listeners.FreezeListener;
import curxxed.dev.icore.utils.NMSUtils;
import curxxed.dev.icore.utils.RankManager;
import curxxed.dev.icore.utils.SocialInput;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class ProfileCommand implements CommandExecutor, Listener {

    private final iCore plugin;
    private final RankManager rankManager;
    private final RedisManager redis;
    private final Set<UUID> clickCooldown = new HashSet<>();

    public ProfileCommand(iCore plugin, RedisManager redis) {
        this.plugin = plugin;
        this.rankManager = plugin.getRankManager();
        this.redis = redis;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        Player target = player;
        if (args.length > 0) {
            Player found = Bukkit.getPlayer(args[0]);
            if (found != null) {
                target = found;
            } else {
                player.sendMessage(ChatColor.RED + "Player not found.");
                return true;
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
                                            ChatColor.GRAY + "Staff: " + (finalTarget.hasPermission("iCore.staff") || finalTarget.hasPermission("iCore.Admin")
                                                    || finalTarget.isOp() || finalTarget.hasPermission("iCore.Manager") ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No")
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

        return true;
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
        boolean success = false;

        if (e.getClick() != ClickType.LEFT && e.getClick() != ClickType.RIGHT) return;

        if (isSelf) {
            if (e.getClick() == ClickType.RIGHT) {
                switch (slot) {
                    case 29:
                        success = SocialInput.waitForInput(p, "Discord", redis);
                        break;
                    case 31:
                        success = SocialInput.waitForInput(p, "YouTube", redis);
                        break;
                    case 33:
                        success = SocialInput.waitForInput(p, "Twitter", redis);
                        break;
                }
                if (success) p.closeInventory();
            } else if (e.getClick() == ClickType.LEFT) {
                String link = null;
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
                }
                if (link != null && !link.isEmpty()) {
                    p.sendMessage(ChatColor.YELLOW + "Click this to open the link: " + ChatColor.UNDERLINE + link);
                } else if ((slot != 13)) {
                    p.sendMessage(ChatColor.RED + "No link set.");
                }
            }
        } else {
            if (e.getClick() == ClickType.LEFT) {
                String link = null;
                switch (slot) {
                    case 29:
                        link = redis.getSocialLink(Bukkit.getOfflinePlayer(title.replace("'s Profile", "")).getUniqueId(), "Discord");
                        break;
                    case 31:
                        link = redis.getSocialLink(Bukkit.getOfflinePlayer(title.replace("'s Profile", "")).getUniqueId(), "YouTube");
                        break;
                    case 33:
                        link = redis.getSocialLink(Bukkit.getOfflinePlayer(title.replace("'s Profile", "")).getUniqueId(), "Twitter");
                        break;
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
            // Dynamically load GameProfile and Property classes
            Class<?> gameProfileClass = NMSUtils.getNMSClass("GameProfile");
            Class<?> propertyClass = NMSUtils.getNMSClass("Property");

            // Create a new GameProfile instance
            Constructor<?> gameProfileConstructor = gameProfileClass.getConstructor(UUID.class, String.class);
            Object gameProfile = gameProfileConstructor.newInstance(UUID.randomUUID(), null);

            // Create a new Property instance
            Constructor<?> propertyConstructor = propertyClass.getConstructor(String.class, String.class);
            Object property = propertyConstructor.newInstance("textures", texture);

            // Add the Property to the GameProfile
            Method getPropertiesMethod = gameProfileClass.getMethod("getProperties");
            Object properties = getPropertiesMethod.invoke(gameProfile);
            Method putMethod = properties.getClass().getMethod("put", Object.class, Object.class);
            putMethod.invoke(properties, "textures", property);

            // Set the GameProfile in the SkullMeta
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, gameProfile);
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