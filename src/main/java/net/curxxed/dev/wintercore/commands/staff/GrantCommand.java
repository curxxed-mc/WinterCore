package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.wintercore.menus.RankMenu;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Curxxed
 * @since iCore (WinterCore) 0.5
 * @revision 0.9
 *  <p>
 *  <b>This command allows staff members to set a player's rank by opening a GUI.</b>
 *  </p>
 *  @see RankMenu
 */
public class GrantCommand extends BaseCommand {

    private final WinterCore plugin;

    public GrantCommand(WinterCore plugin) {
        this.plugin = plugin;
    }

    @Command(
            name = "grant",
            permission = "WinterCore.commands.grant",
            description = "Open the rank selection GUI for target player.",
            aliases = {"setrank"},
            usage = "/grant <player>",
            inGameOnly = true
    )
    public void onCommand(CommandArgs commandArgs) {
        Player sender = commandArgs.getPlayer();
        String targetName = commandArgs.getArgs(0);
        UUID targetUUID = Bukkit.getOfflinePlayer(targetName).getUniqueId();
        plugin.getRankManager().setTargetPlayerUUID(sender.getUniqueId(), targetUUID);
        openRankGUI(sender, targetName);
    }

    private void openRankGUI(Player player, String targetName) {
        Inventory rankGUI = Bukkit.createInventory(null, 27, "Set rank for " + targetName);

        ConfigurationSection ranksSection = plugin.getRankManager().getRanksSection();
        if (ranksSection == null) {
            player.sendMessage(CC.translate("&cNo ranks found in the configuration."));
            return;
        }

        List<String> sortedRanks = plugin.getRankManager().getSortedRanks();

        int slot = 0;
        for (String rank : sortedRanks) {
            if (rank == null || rank.isEmpty()) continue;

            ItemStack rankItem = createRankItem(rank, ranksSection, targetName);
            rankGUI.setItem(slot++, rankItem);
        }

        ItemStack cancelItem = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(CC.translate("&cCancel"));
        cancelItem.setItemMeta(cancelMeta);
        rankGUI.setItem(26, cancelItem);

        player.openInventory(rankGUI);
    }

    private ItemStack createRankItem(String rank, ConfigurationSection ranksSection, String targetName) {
        String colorCode = ranksSection.getString(rank + ".name-color", "&f");
        String translatedColorCode = CC.translate(colorCode);
        String prefix = ranksSection.getString(rank + ".prefix", "");
        DyeColor dyeColor = getDyeColorFromChatColor(ChatColor.getByChar(colorCode.replace("&", "").charAt(0)));
        ItemStack rankItem;

        if (ChatColor.getByChar(colorCode.replace("&", "").charAt(0)) == ChatColor.DARK_RED){
            rankItem = new ItemStack(Material.STAINED_CLAY , 1, (short) 14);
        } else {
            rankItem = new ItemStack(Material.WOOL, 1, dyeColor.getWoolData());
        }

        ItemMeta meta = rankItem.getItemMeta();
        meta.setDisplayName(translatedColorCode + rank);
        List<String> lore = new ArrayList<>();
        lore.add(CC.Gray + "§m------------------------");
        lore.add(CC.Gold + "Preview:");
        String translatedPrefix = CC.translate(prefix);
        lore.add(translatedPrefix + translatedColorCode + targetName + CC.White + ": " + CC.Gray + "Hi! This is what your message would look like.");
        lore.add(CC.Gray + "§m------------------------");
        lore.add(CC.Green + "Click to Grant this Rank to " + CC.Aqua + targetName + CC.Green + ".");
        meta.setLore(lore);
        rankItem.setItemMeta(meta);

        return rankItem;
    }

    private DyeColor getDyeColorFromChatColor(ChatColor chatColor) {
        switch (chatColor) {
            case BLACK: return DyeColor.BLACK;
            case DARK_BLUE: return DyeColor.BLUE;
            case DARK_GREEN: return DyeColor.GREEN;
            case DARK_AQUA: return DyeColor.CYAN;
            case DARK_RED: return DyeColor.RED;
            case DARK_PURPLE: return DyeColor.PURPLE;
            case GOLD: return DyeColor.ORANGE;
            case GRAY: return DyeColor.SILVER;
            case DARK_GRAY: return DyeColor.GRAY;
            case BLUE: return DyeColor.LIGHT_BLUE;
            case GREEN: return DyeColor.LIME;
            case AQUA: return DyeColor.LIGHT_BLUE;
            case RED: return DyeColor.RED;
            case LIGHT_PURPLE: return DyeColor.PINK;
            case YELLOW: return DyeColor.YELLOW;
            case WHITE: return DyeColor.WHITE;
            default: return DyeColor.WHITE;
        }
    }
}