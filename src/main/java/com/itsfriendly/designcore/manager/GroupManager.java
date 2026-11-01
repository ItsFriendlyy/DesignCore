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

        for (Player online : Bukkit.getOnlinePlayers()) {
            applyDisplay(online);
        }
        return true;
    }

    public boolean deleteGroupAttribute(String name, String type) {
        return editGroup(name, type, "");
    }

    /**
     * Resolves the player group without getting fooled by wildcard '*'.
     * Checks:
     * 1. LuckPerms direct primary group or explicit permission nodes
     * 2. Non-wildcard Bukkit permissions
     * 3. OP / Default fallbacks
     */
    public GroupData getPlayerGroup(Player player) {
        // Step 1: Check LuckPerms explicitly if running
        GroupData lpMatch = getLuckPermsGroup(player);
        if (lpMatch != null) {
            return lpMatch;
        }

        // Step 2: Fallback for setups without LuckPerms
        for (GroupData data : groups.values()) {
            String groupKey = data.name().toLowerCase();
            if (groupKey.equals("default") || groupKey.equals("op")) continue;

            if (player.hasPermission("designcore.group." + groupKey)) {
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

    /**
     * Reads directly from LuckPerms via reflection to avoid adding a hard Maven dependency.
     */
    private GroupData getLuckPermsGroup(Player player) {
        try {
            org.bukkit.plugin.RegisteredServiceProvider<?> provider = Bukkit.getServicesManager().getRegistration(Class.forName("net.luckperms.api.LuckPerms"));
            if (provider == null) return null;

            net.luckperms.api.LuckPerms lp = (net.luckperms.api.LuckPerms) provider.getProvider();
            net.luckperms.api.model.user.User user = lp.getUserManager().getUser(player.getUniqueId());
            if (user == null) return null;

            // 1. Check if user's primary LuckPerms group matches a DesignCore group
            String primaryGroup = user.getPrimaryGroup().toLowerCase();
            if (groups.containsKey(primaryGroup)) {
                return groups.get(primaryGroup);
            }

            // 2. Scan nodes explicitly assigned to the user or their parent groups (ignoring '*')
            for (net.luckperms.api.node.Node node : user.getNodes()) {
                if (!node.getValue()) continue;
                String key = node.getKey().toLowerCase();

                // If assigned via permission: designcore.group.<name>
                if (key.startsWith("designcore.group.")) {
                    String groupName = key.substring("designcore.group.".length());
                    if (groups.containsKey(groupName)) {
                        return groups.get(groupName);
                    }
                }

                // If assigned via group inheritance: group.<name>
                if (key.startsWith("group.")) {
                    String groupName = key.substring("group.".length());
                    if (groups.containsKey(groupName)) {
                        return groups.get(groupName);
                    }
                }
            }
        } catch (Throwable ignored) {
            // LuckPerms not present or API unavailable
        }
        return null;
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