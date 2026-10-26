package com.itsfriendly.designcore.manager;

import com.itsfriendly.designcore.DesignCore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupManager {

    private final DesignCore plugin;
    private final Map<String, GroupData> groups = new HashMap<>();

    public record GroupData(String name, String prefix, String suffix) {}

    public GroupManager(DesignCore plugin) {
        this.plugin = plugin;
        loadGroups();
    }

    public void loadGroups() {
        groups.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("groups");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String prefix = section.getString(key + ".prefix", "");
                String suffix = section.getString(key + ".suffix", "");
                groups.put(key.toLowerCase(), new GroupData(key, prefix, suffix));
                registerGroupPermission(key.toLowerCase());
            }
        }
    }

    private void registerGroupPermission(String groupName) {
        String permName = "designcore.group." + groupName.toLowerCase();
        Permission perm = Bukkit.getPluginManager().getPermission(permName);
        if (perm == null) {
            perm = new Permission(permName, "Access to " + groupName + " group", PermissionDefault.FALSE);
            Bukkit.getPluginManager().addPermission(perm);
        } else {
            perm.setDefault(PermissionDefault.FALSE);
        }
    }

    public List<String> getGroupNames() {
        return new ArrayList<>(groups.keySet());
    }

    public boolean createGroup(String name) {
        String key = name.toLowerCase();
        if (groups.containsKey(key)) return false;

        plugin.getConfig().set("groups." + key + ".prefix", "");
        plugin.getConfig().set("groups." + key + ".suffix", "");
        plugin.saveConfig();

        groups.put(key, new GroupData(key, "", ""));
        registerGroupPermission(key);
        return true;
    }

    public boolean editGroup(String name, String type, String value) {
        String key = name.toLowerCase();
        if (!groups.containsKey(key)) return false;

        GroupData existing = groups.get(key);
        String prefix = existing.prefix();
        String suffix = existing.suffix();

        if (type.equalsIgnoreCase("prefix")) {
            prefix = value;
            plugin.getConfig().set("groups." + key + ".prefix", value);
        } else if (type.equalsIgnoreCase("suffix")) {
            suffix = value;
            plugin.getConfig().set("groups." + key + ".suffix", value);
        } else {
            return false;
        }

        plugin.saveConfig();
        groups.put(key, new GroupData(key, prefix, suffix));

        // Instantly re-apply display for all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyDisplay(player);
        }
        return true;
    }

    public boolean deleteGroupAttribute(String name, String type) {
        return editGroup(name, type, "");
    }

    public GroupData getPlayerGroup(Player player) {
        for (GroupData data : groups.values()) {
            String groupKey = data.name().toLowerCase();
            if (groupKey.equals("default") || groupKey.equals("op")) {
                continue;
            }

            String perm = "designcore.group." + groupKey;
            if (player.isPermissionSet(perm) && player.hasPermission(perm)) {
                return data;
            }
        }

        if (player.isOp()) {
            if (groups.containsKey("op")) {
                return groups.get("op");
            }
            return new GroupData("op", "&4&lOP - ", "");
        }

        return groups.getOrDefault("default", new GroupData("default", "&7[Member] ", ""));
    }

    public void applyDisplay(Player player) {
        GroupData group = getPlayerGroup(player);
        String rawPrefix = group.prefix();
        String rawSuffix = group.suffix();

        String lastColors = ChatColor.getLastColors(rawPrefix.replace('&', '§'));
        if (lastColors.isEmpty()) {
            lastColors = "§f";
        }

        boolean prefixHasTrailingSpace = rawPrefix.endsWith(" ");
        String spacer = (rawPrefix.isEmpty() || prefixHasTrailingSpace) ? "" : " ";

        Component tabPrefix = DesignCore.color(rawPrefix);
        Component tabName = DesignCore.color(lastColors + player.getName());

        Component suffixComponent = Component.empty();
        if (!rawSuffix.isEmpty()) {
            boolean suffixHasLeadingSpace = rawSuffix.startsWith(" ");
            suffixComponent = DesignCore.color((suffixHasLeadingSpace ? "" : " ") + rawSuffix);
        }

        // 1. Tab list update
        player.playerListName(
            Component.empty()
                .append(tabPrefix)
                .append(spacer.isEmpty() ? Component.empty() : Component.text(spacer))
                .append(tabName)
                .append(suffixComponent)
        );

        // 2. Nametag & Scoreboard Teams update
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team oldTeam = board.getEntryTeam(player.getName());
        if (oldTeam != null) {
            oldTeam.removeEntry(player.getName());
        }

        String teamName = "dc_" + group.name();
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }

        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }

        Component teamPrefix = rawPrefix.isEmpty() ? Component.empty() : tabPrefix.append(spacer.isEmpty() ? Component.empty() : Component.text(spacer));
        team.prefix(teamPrefix);
        team.suffix(suffixComponent);

        ChatColor teamColor = null;
        for (int i = lastColors.length() - 2; i >= 0; i -= 2) {
            if (lastColors.charAt(i) == '§') {
                ChatColor c = ChatColor.getByChar(lastColors.charAt(i + 1));
                if (c != null && c.isColor()) {
                    teamColor = c;
                    break;
                }
            }
        }
        if (teamColor != null) {
            try {
                team.setColor(teamColor);
            } catch (Exception ignored) {}
        } else {
            team.setColor(ChatColor.WHITE);
        }

        team.addEntry(player.getName());

        // Re-send player scoreboard to enforce instant client-side nametag update
        player.setScoreboard(board);
    }
}