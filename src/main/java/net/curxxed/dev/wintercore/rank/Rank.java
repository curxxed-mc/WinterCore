package net.curxxed.dev.wintercore.rank;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Rank {
    @Getter
    private final String name;
    private final ConfigurationSection rankSection;
    private final WinterCore plugin;

    public Rank(String name, WinterCore plugin) {
        this.name = name;
        this.plugin = plugin;
        this.rankSection = plugin.getRankManager().getRanksSection().getConfigurationSection(name);
        if (rankSection == null) {
            throw new IllegalArgumentException("rank does not exist: " + name);
        }
    }

    public String getPrefix() {
        return rankSection.getString("prefix", "");
    }

    public void setPrefix(String prefix) {
        rankSection.set("prefix", prefix);
        plugin.getRankManager().saveRanksConfig();
    }

    public String getColor() {
        return rankSection.getString("name-color", "&f");
    }

    public void setColor(String color) {
        rankSection.set("name-color", color);
        plugin.getRankManager().saveRanksConfig();
    }

    public int getWeight() {
        return rankSection.getInt("weight", 0);
    }

    public void setWeight(int weight) {
        rankSection.set("weight", weight);
        plugin.getRankManager().saveRanksConfig();
    }

    public List<Map<String, String>> getPermissions() {
        List<?> rawList = rankSection.getList("permissions", new ArrayList<>());
        List<Map<String, String>> result = new ArrayList<>();
        for (Object obj : rawList) {
            if (obj instanceof Map) {
                Map<String, String> permission = new HashMap<>();
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        permission.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                    }
                }
                result.add(permission);
            } else if (obj instanceof String) {
                Map<String, String> map = new HashMap<>();
                map.put("permission", (String) obj);
                result.add(map);
            }
        }
        return result;
    }

    public void addPermission(String permission) {
        List<Map<String, String>> permissions = getPermissions();
        if (permissions.stream().noneMatch(p -> p.get("permission").equals(permission))) {
            Map<String, String> permissionMap = new HashMap<>();
            permissionMap.put("permission", permission);
            permissions.add(permissionMap);
            rankSection.set("permissions", permissions);
            plugin.getRankManager().saveRanksConfig();
        }
    }

    public void removePermission(String permission) {
        List<Map<String, String>> permissions = getPermissions();
        permissions.removeIf(p -> p.get("permission").equals(permission));
        rankSection.set("permissions", permissions);
        plugin.getRankManager().saveRanksConfig();
    }

    public List<String> getInheritance() {
        return rankSection.getStringList("inheritance");
    }

    public void addInheritance(String inheritedRank) {
        List<String> inheritance = getInheritance();
        if (!inheritance.contains(inheritedRank)) {
            inheritance.add(inheritedRank);
            rankSection.set("inheritance", inheritance);
            plugin.getRankManager().saveRanksConfig();
        }
    }

    public void removeInheritance(String inheritedRank) {
        List<String> inheritance = getInheritance();
        inheritance.remove(inheritedRank);
        rankSection.set("inheritance", inheritance);
        plugin.getRankManager().saveRanksConfig();
    }

    public void inheritPermissionsFrom(String inheritedRank) {
        List<Map<String, String>> inheritedPermissions = new Rank(inheritedRank, plugin).getPermissions();
        List<Map<String, String>> currentPermissions = getPermissions();
        for (Map<String, String> permission : inheritedPermissions) {
            if (currentPermissions.stream().noneMatch(p -> p.get("permission").equals(permission.get("permission")))) {
                currentPermissions.add(permission);
            }
        }
        rankSection.set("permissions", currentPermissions);
        plugin.getRankManager().saveRanksConfig();
    }

    public void delete() {
        plugin.getRankManager().getRanksSection().set(name, null);
        plugin.getRankManager().saveRanksConfig();
    }

    public boolean isValid() {
        return rankSection != null;
    }

    public static List<String> getLowerRanks(String playerRank, WinterCore plugin) {
        ConfigurationSection ranksSection = plugin.getRankManager().getRanksSection();
        if (ranksSection == null) {
            plugin.getLogger().warning("Ranks section is missing in ranks.yml");
            return new ArrayList<>();
        }

        int playerWeight = new Rank(playerRank, plugin).getWeight();
        List<String> lowerRanks = new ArrayList<>();
        for (String rank : ranksSection.getKeys(false)) {
            Rank currentRank = new Rank(rank, plugin);
            if (currentRank.getWeight() < playerWeight) {
                lowerRanks.add(rank);
            }
        }
        return lowerRanks;
    }
}
