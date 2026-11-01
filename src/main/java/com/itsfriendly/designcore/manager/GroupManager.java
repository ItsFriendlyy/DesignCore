package com.itsfriendly.designcore.manager;

import com.itsfriendly.designcore.DesignCore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachmentInfo;
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

        for (Player online : Bukkit.getOnlinePlayers()) {
            applyDisplay(online);
        }
        return true;
    }

    public boolean deleteGroupAttribute(String name, String type) {
        return editGroup(name, type, "");
    }

    /**
     * Finds the player's group by inspecting explicit permission entries first.
     * This ensures that assigning a specific node like 'designcore.group.admin' in LuckPerms
     * takes precedence over the wildcard '*' permission.
     */
    public GroupData getPlayerGroup(Player player) {
        // First Pass: Scan for an explicitly set permission string on the player
        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            if (!pai.getValue()) continue;

            String perm = pai.getPermission().toLowerCase();
            if (perm.startsWith("designcore.group.")) {
                String groupKey = perm.substring("designcore.group.".length());
                if (groups.containsKey(groupKey) && !groupKey.equals("default") && !groupKey.equals("op")) {
                    return groups.get(groupKey);
                }
            }
        }

        // Second Pass: Standard permission check (fallback for non-wildcard setups)
        for (GroupData data : groups.values()) {
            String groupKey = data.name().toLowerCase();
            if (groupKey.equals("default") || groupKey.equals("op")) {
                continue;
            }

            String perm = "designcore.group." + groupKey;
            if (player.hasPermission(perm)) {
                return data;
            }
        }

        // Fallback for OP players
        if (player.isOp()) {
            if (groups.containsKey("op")) {
                return groups.get("op");
            }
            return new GroupData("op", "&4&lOP - ", "");
        }

        // Default group fallback
        return groups.getOrDefault("default", new GroupData("default", "&7[Member] ", ""));
    }

    public void setupPlayerScoreboard(Player player) {
        if (player.getScoreboard().equals(Bukkit.getScoreboardManager().getMainScoreboard())) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
    }

    public void applyDisplay(Player player) {
        setupPlayerScoreboard(player);

        GroupData targetGroup = getPlayerGroup(player);
        String rawPrefix = targetGroup.prefix();
        String rawSuffix = targetGroup.suffix();

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

        // 1. Tab list display
        player.playerListName(
            Component.empty()
                .append(tabPrefix)
                .append(spacer.isEmpty() ? Component.empty() : Component.text(spacer))
                .append(tabName)
                .append(suffixComponent)
        );

        // 2. Nametag display on isolated scoreboards across all online players
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            setupPlayerScoreboard(viewer);
            updateTeamOnScoreboard(viewer.getScoreboard(), player, targetGroup, tabPrefix, suffixComponent, lastColors, spacer);
        }
    }

    private void updateTeamOnScoreboard(Scoreboard board, Player subject, GroupData group, Component tabPrefix, Component suffixComponent, String lastColors, String spacer) {
        Team oldTeam = board.getEntryTeam(subject.getName());
        if (oldTeam != null) {
            oldTeam.removeEntry(subject.getName());
        }

        String teamName = "dc_" + group.name();
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }

        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }

        Component teamPrefix = group.prefix().isEmpty() ? Component.empty() : tabPrefix.append(spacer.isEmpty() ? Component.empty() : Component.text(spacer));
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

        if (!team.hasEntry(subject.getName())) {
            team.addEntry(subject.getName());
        }
    }

    public void cleanupMainScoreboard() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : new ArrayList<>(board.getTeams())) {
            if (team.getName().startsWith("dc_")) {
                team.unregister();
            }
        }
    }
}