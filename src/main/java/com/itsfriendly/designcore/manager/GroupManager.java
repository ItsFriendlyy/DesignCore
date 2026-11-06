package com.itsfriendly.designcore.manager;

import com.itsfriendly.designcore.DesignCore;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GroupManager {

    private final DesignCore plugin;
    private final Map<String, GroupData> groups = new HashMap<>();
    private final Map<UUID, PlayerDisplayOverride> playerOverrides = new HashMap<>();
    private final File dataFile;
    private final YamlConfiguration dataConfig;

    public record GroupData(String name, String prefix, String suffix, int priority) {}
    public record PlayerDisplayOverride(String prefix, String suffix) {}

    public GroupManager(DesignCore plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "userdata.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException ignored) {}
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadGroups();
        loadPlayerOverrides();
    }

    public void loadGroups() {
        groups.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("groups");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String prefix = section.getString(key + ".prefix", "");
                String suffix = section.getString(key + ".suffix", "");
                // Default priority to the current size if not set
                int priority = section.getInt(key + ".priority", groups.size() + 1);
                groups.put(key.toLowerCase(), new GroupData(key, prefix, suffix, priority));
                registerGroupPermission(key.toLowerCase());
            }
        }
    }

    public void loadPlayerOverrides() {
        playerOverrides.clear();
        for (String key : dataConfig.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                String prefix = dataConfig.getString(key + ".custom-prefix", null);
                String suffix = dataConfig.getString(key + ".custom-suffix", null);
                if (prefix != null || suffix != null) {
                    playerOverrides.put(id, new PlayerDisplayOverride(prefix, suffix));
                }
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public boolean setGroupPriority(String groupName, int priority) {
        String key = groupName.toLowerCase();
        if (!groups.containsKey(key)) return false;

        GroupData existing = groups.get(key);
        plugin.getConfig().set("groups." + key + ".priority", priority);
        plugin.saveConfig();

        groups.put(key, new GroupData(existing.name(), existing.prefix(), existing.suffix(), priority));

        for (Player online : Bukkit.getOnlinePlayers()) {
            applyDisplay(online);
        }
        return true;
    }

    public int getGroupCount() {
        return groups.size();
    }

    public boolean createGroup(String name) {
        String key = name.toLowerCase();
        if (groups.containsKey(key)) return false;

        // Automatically set priority to the lowest (total count of groups)
        int newPriority = groups.size() + 1;
        plugin.getConfig().set("groups." + key + ".prefix", "");
        plugin.getConfig().set("groups." + key + ".suffix", "");
        plugin.getConfig().set("groups." + key + ".priority", newPriority);
        plugin.saveConfig();

        groups.put(key, new GroupData(key, "", "", newPriority));
        registerGroupPermission(key);
        return true;
    }

    public boolean editGroup(String name, String type, String value) {
        String key = name.toLowerCase();
        if (!groups.containsKey(key)) return false;

        GroupData existing = groups.get(key);
        String prefix = existing.prefix();
        String suffix = existing.suffix();
        int priority = existing.priority();

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
        groups.put(key, new GroupData(key, prefix, suffix, priority));

        for (Player online : Bukkit.getOnlinePlayers()) {
            applyDisplay(online);
        }
        return true;
    }

    public boolean deleteGroupAttribute(String name, String type) {
        return editGroup(name, type, "");
    }

    public boolean setPlayerOverride(UUID uuid, String type, String value) {
        PlayerDisplayOverride current = playerOverrides.getOrDefault(uuid, new PlayerDisplayOverride(null, null));
        String newPrefix = current.prefix();
        String newSuffix = current.suffix();

        if (type.equalsIgnoreCase("prefix")) {
            newPrefix = value;
        } else if (type.equalsIgnoreCase("suffix")) {
            newSuffix = value;
        } else {
            return false;
        }

        playerOverrides.put(uuid, new PlayerDisplayOverride(newPrefix, newSuffix));
        dataConfig.set(uuid.toString() + ".custom-" + type.toLowerCase(), value);
        try {
            dataConfig.save(dataFile);
        } catch (IOException ignored) {}

        Player target = Bukkit.getPlayer(uuid);
        if (target != null && target.isOnline()) {
            applyDisplay(target);
        }
        return true;
    }

    public boolean deletePlayerOverride(UUID uuid, String type) {
        PlayerDisplayOverride current = playerOverrides.get(uuid);
        if (current == null) return false;

        String newPrefix = current.prefix();
        String newSuffix = current.suffix();

        if (type.equalsIgnoreCase("prefix")) {
            newPrefix = null;
        } else if (type.equalsIgnoreCase("suffix")) {
            newSuffix = null;
        } else {
            return false;
        }

        if (newPrefix == null && newSuffix == null) {
            playerOverrides.remove(uuid);
        } else {
            playerOverrides.put(uuid, new PlayerDisplayOverride(newPrefix, newSuffix));
        }

        dataConfig.set(uuid.toString() + ".custom-" + type.toLowerCase(), null);
        try {
            dataConfig.save(dataFile);
        } catch (IOException ignored) {}

        Player target = Bukkit.getPlayer(uuid);
        if (target != null && target.isOnline()) {
            applyDisplay(target);
        }
        return true;
    }

    public boolean hasPlayerOverride(Player player) {
        PlayerDisplayOverride override = playerOverrides.get(player.getUniqueId());
        return override != null && (override.prefix() != null || override.suffix() != null);
    }

    public String getEffectivePrefix(Player player) {
        PlayerDisplayOverride override = playerOverrides.get(player.getUniqueId());
        if (override != null && override.prefix() != null) {
            return override.prefix();
        }
        return getPlayerGroup(player).prefix();
    }

    public String getEffectiveSuffix(Player player) {
        PlayerDisplayOverride override = playerOverrides.get(player.getUniqueId());
        if (override != null && override.suffix() != null) {
            return override.suffix();
        }
        return getPlayerGroup(player).suffix();
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

    public GroupData getPlayerGroup(Player player) {
        if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            try {
                LuckPerms lp = LuckPermsProvider.get();
                User user = lp.getUserManager().getUser(player.getUniqueId());

                if (user != null) {
                    for (Node node : user.getNodes()) {
                        if (!node.getValue()) continue;
                        String key = node.getKey().toLowerCase();

                        if (key.startsWith("designcore.group.")) {
                            String groupName = key.substring("designcore.group.".length());
                            if (groups.containsKey(groupName) && !groupName.equals("default") && !groupName.equals("op")) {
                                return groups.get(groupName);
                            }
                        }

                        if (key.startsWith("group.")) {
                            String groupName = key.substring("group.".length());
                            if (groups.containsKey(groupName) && !groupName.equals("default") && !groupName.equals("op")) {
                                return groups.get(groupName);
                            }
                        }
                    }

                    String primary = user.getPrimaryGroup().toLowerCase();
                    if (groups.containsKey(primary) && !primary.equals("default") && !primary.equals("op")) {
                        return groups.get(primary);
                    }
                }
            } catch (Exception ignored) {}
        }

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
            return new GroupData("op", "&4&lOP - ", "", 1);
        }

        // Lowest priority + 2 for the fallback default group
        int defaultPriority = groups.size() + 2;
        return groups.getOrDefault("default", new GroupData("default", "&7[Member] ", "", defaultPriority));
    }

    public void setupPlayerScoreboard(Player player) {
        if (player.getScoreboard().equals(Bukkit.getScoreboardManager().getMainScoreboard())) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
    }

    public void applyDisplay(Player player) {
        setupPlayerScoreboard(player);

        GroupData targetGroup = getPlayerGroup(player);
        String rawPrefix = getEffectivePrefix(player);
        String rawSuffix = getEffectiveSuffix(player);

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

        // 1. Tab List Display
        player.playerListName(
            Component.empty()
                .append(tabPrefix)
                .append(spacer.isEmpty() ? Component.empty() : Component.text(spacer))
                .append(tabName)
                .append(suffixComponent)
        );

        // 2. Tab Sort Order Calculation:
        // - Official group priorities: 1 to N
        // - Player custom override: (Group count + 1) -> places them under group prefixes
        // - Default group: (Group count + 2) -> places normal users at the very bottom
        int sortPriority;
        if (hasPlayerOverride(player)) {
            sortPriority = groups.size() + 1;
        } else if (targetGroup.name().equalsIgnoreCase("default")) {
            sortPriority = groups.size() + 2;
        } else {
            sortPriority = targetGroup.priority();
        }

        // Format team name: "dc_001_Player" (16 char limit safe)
        String prefixOrder = String.format("%03d", Math.min(999, Math.max(1, sortPriority)));
        String pName = player.getName();
        if (pName.length() > 8) {
            pName = pName.substring(0, 8);
        }
        String teamName = "d" + prefixOrder + "_" + pName;

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            setupPlayerScoreboard(viewer);
            Scoreboard board = viewer.getScoreboard();

            Team oldTeam = board.getEntryTeam(player.getName());
            if (oldTeam != null) {
                oldTeam.removeEntry(player.getName());
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

            if (!team.hasEntry(player.getName())) {
                team.addEntry(player.getName());
            }
        }
    }

    public void cleanupMainScoreboard() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : new ArrayList<>(board.getTeams())) {
            if (team.getName().startsWith("d") || team.getName().startsWith("dc_")) {
                team.unregister();
            }
        }
    }
}