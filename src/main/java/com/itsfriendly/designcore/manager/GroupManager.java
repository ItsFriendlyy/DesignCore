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
            // PermissionDefault.FALSE prevents OP players from getting this permission by default
            perm = new Permission(permName, "Access to " + groupName + " design group", PermissionDefault.FALSE);
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

        for (Player player : Bukkit.getOnlinePlayers()) {
            applyDisplay(player);
        }
        return true;
    }

    public GroupData getPlayerGroup(Player player) {
        // 1. Check if the player has an explicit group permission assigned to them
        for (GroupData data : groups.values()) {
            if (data.name().equalsIgnoreCase("default") || data.name().equalsIgnoreCase("admin")) {
                continue;
            }
            if (player.hasPermission("designcore.group." + data.name().toLowerCase())) {
                return data;
            }
        }

        // 2. If no custom group was explicitly assigned, give OP the admin group
        if (player.isOp() && groups.containsKey("admin")) {
            return groups.get("admin");
        }

        // 3. Fallback to default group
        return groups.getOrDefault("default", new GroupData("default", "", ""));
    }

    public void applyDisplay(Player player) {
        GroupData group = getPlayerGroup(player);
        String rawPrefix = group.prefix();
        String rawSuffix = group.suffix();

        // Extract last color code in the prefix to continue onto the player's name
        String lastColors = ChatColor.getLastColors(rawPrefix.replace('&', '§'));
        if (lastColors.isEmpty()) {
            lastColors = "§f";
        }

        // Determine spacing
        boolean prefixHasTrailingSpace = rawPrefix.endsWith(" ");
        String spacer = (rawPrefix.isEmpty() || prefixHasTrailingSpace) ? "" : " ";

        Component tabPrefix = DesignCore.color(rawPrefix);
        Component tabName = DesignCore.color(lastColors + player.getName());

        Component suffixComponent = Component.empty();
        if (!rawSuffix.isEmpty()) {
            boolean suffixHasLeadingSpace = rawSuffix.startsWith(" ");
            suffixComponent = DesignCore.color((suffixHasLeadingSpace ? "" : " ") + rawSuffix);
        }

        // 1. Tab list display: [PREFIX] [PLAYERNAME] [SUFFIX]
        player.playerListName(
            Component.empty()
                .append(tabPrefix)
                .append(spacer.isEmpty() ? Component.empty() : Component.text(spacer))
                .append(tabName)
                .append(suffixComponent)
        );

        // 2. Nametag display via Scoreboard Teams
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "dc_" + group.name();
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }

        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }

        team.prefix(tabPrefix.append(spacer.isEmpty() ? Component.empty() : Component.text(spacer)));
        team.suffix(suffixComponent);

        // Nametag color propagation via Bukkit ChatColor
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
        }

        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
    }
}