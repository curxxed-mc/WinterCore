package hyp.ilfov.i.icore.Commands.Staff.StaffList;

import hyp.ilfov.i.icore.Main;
import hyp.ilfov.i.icore.utils.Staff.StaffData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StaffList implements CommandExecutor {

    private final Map<UUID, StaffData> staffMap;
    private final StaffListGUI staffListGUI;

    public StaffList(Main plugin, Map<UUID, StaffData> staffMap) {
        this.staffMap = staffMap;
        this.staffListGUI = new StaffListGUI(plugin);
    }



    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player p = Bukkit.getPlayer(sender.getName());

        List<StaffData> staffList = new ArrayList<>(staffMap.values());
        staffListGUI.open(p, staffList);
        return true;
    }
}
