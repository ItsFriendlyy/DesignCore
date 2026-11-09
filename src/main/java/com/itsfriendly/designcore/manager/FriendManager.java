package com.itsfriendly.designcore.manager;

import com.itsfriendly.designcore.DesignCore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class FriendManager {

    private final DesignCore plugin;
    private final File dataFile;
    private final YamlConfiguration dataConfig;

    public enum PrivacyMode {
        EVERYONE, FRIENDS, NO_ONE
    }

    // player UUID -> Set of friend UUIDs
    private final Map<UUID, Set<UUID>> friends = new HashMap<>();
    // player UUID -> (friend UUID -> timestamp in ms)
    private final Map<UUID, Map<UUID, Long>> friendshipTimestamps = new HashMap<>();
    // player UUID -> Set of muted player UUIDs
    private final Map<UUID, Set<UUID>> mutedPlayers = new HashMap<>();
    // player UUID -> PrivacyMode
    private final Map<UUID, PrivacyMode> privacyModes = new HashMap<>();
    // receiver UUID -> sender UUID (pending friend request)
    private final Map<UUID, UUID> pendingRequests = new HashMap<>();
    // player UUID -> last direct messaged UUID (for /reply)
    private final Map<UUID, UUID> lastMessaged = new HashMap<>();

    // Persistent cache: UUID -> Last known Name
    private final Map<UUID, String> nameCache = new HashMap<>();
    // Name (lowercase) -> UUID
    private final Map<String, UUID> uuidCache = new HashMap<>();

    public FriendManager(DesignCore plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "userdata.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException ignored) {}
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadData();
    }

    public void loadData() {
        friends.clear();
        friendshipTimestamps.clear();
        mutedPlayers.clear();
        privacyModes.clear();
        nameCache.clear();
        uuidCache.clear();

        for (String key : dataConfig.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);

                // Cache saved player name
                String savedName = dataConfig.getString(key + ".last-name");
                if (savedName != null && !savedName.isBlank()) {
                    nameCache.put(id, savedName);
                    uuidCache.put(savedName.toLowerCase(), id);
                }

                // Friends & Timestamps
                List<String> rawFriends = dataConfig.getStringList(key + ".friends");
                Set<UUID> friendSet = new HashSet<>();
                Map<UUID, Long> timeMap = new HashMap<>();

                for (String raw : rawFriends) {
                    try {
                        UUID fId = UUID.fromString(raw);
                        friendSet.add(fId);
                        long ts = dataConfig.getLong(key + ".timestamps." + raw, System.currentTimeMillis());
                        timeMap.put(fId, ts);
                    } catch (IllegalArgumentException ignored) {}
                }
                friends.put(id, friendSet);
                friendshipTimestamps.put(id, timeMap);

                // Mutes
                List<String> rawMutes = dataConfig.getStringList(key + ".mutes");
                Set<UUID> muteSet = new HashSet<>();
                for (String raw : rawMutes) {
                    try {
                        muteSet.add(UUID.fromString(raw));
                    } catch (IllegalArgumentException ignored) {}
                }
                mutedPlayers.put(id, muteSet);

                // Privacy
                String modeStr = dataConfig.getString(key + ".privacy", "EVERYONE");
                try {
                    privacyModes.put(id, PrivacyMode.valueOf(modeStr.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    privacyModes.put(id, PrivacyMode.EVERYONE);
                }

            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void cachePlayer(Player player) {
        nameCache.put(player.getUniqueId(), player.getName());
        uuidCache.put(player.getName().toLowerCase(), player.getUniqueId());
        dataConfig.set(player.getUniqueId().toString() + ".last-name", player.getName());
        try {
            dataConfig.save(dataFile);
        } catch (IOException ignored) {}
    }

    public String resolvePlayerName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            cachePlayer(online);
            return online.getName();
        }

        if (nameCache.containsKey(uuid)) {
            return nameCache.get(uuid);
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        if (offline.getName() != null) {
            nameCache.put(uuid, offline.getName());
            uuidCache.put(offline.getName().toLowerCase(), uuid);
            return offline.getName();
        }

        return uuid.toString().substring(0, 8);
    }

    public UUID resolvePlayerUuid(String input) {
        Player target = Bukkit.getPlayer(input);
        if (target != null) {
            return target.getUniqueId();
        }

        if (uuidCache.containsKey(input.toLowerCase())) {
            return uuidCache.get(input.toLowerCase());
        }

        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException ignored) {}

        OfflinePlayer offline = Bukkit.getOfflinePlayer(input);
        return offline.getUniqueId();
    }

    public void saveData() {
        for (Map.Entry<UUID, Set<UUID>> entry : friends.entrySet()) {
            List<String> raw = new ArrayList<>();
            for (UUID u : entry.getValue()) {
                raw.add(u.toString());
            }
            dataConfig.set(entry.getKey() + ".friends", raw);
        }

        for (Map.Entry<UUID, Map<UUID, Long>> entry : friendshipTimestamps.entrySet()) {
            for (Map.Entry<UUID, Long> timeEntry : entry.getValue().entrySet()) {
                dataConfig.set(entry.getKey() + ".timestamps." + timeEntry.getKey().toString(), timeEntry.getValue());
            }
        }

        for (Map.Entry<UUID, Set<UUID>> entry : mutedPlayers.entrySet()) {
            List<String> raw = new ArrayList<>();
            for (UUID u : entry.getValue()) {
                raw.add(u.toString());
            }
            dataConfig.set(entry.getKey() + ".mutes", raw);
        }

        for (Map.Entry<UUID, PrivacyMode> entry : privacyModes.entrySet()) {
            dataConfig.set(entry.getKey() + ".privacy", entry.getValue().name());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save friend data to userdata.yml!");
        }
    }

    public boolean areFriends(UUID u1, UUID u2) {
        return friends.getOrDefault(u1, Collections.emptySet()).contains(u2);
    }

    public boolean addFriend(UUID u1, UUID u2) {
        long now = System.currentTimeMillis();
        friends.computeIfAbsent(u1, k -> new HashSet<>()).add(u2);
        friends.computeIfAbsent(u2, k -> new HashSet<>()).add(u1);

        friendshipTimestamps.computeIfAbsent(u1, k -> new HashMap<>()).put(u2, now);
        friendshipTimestamps.computeIfAbsent(u2, k -> new HashMap<>()).put(u1, now);

        saveData();
        return true;
    }

    public boolean removeFriend(UUID u1, UUID u2) {
        boolean r1 = friends.getOrDefault(u1, Collections.emptySet()).remove(u2);
        boolean r2 = friends.getOrDefault(u2, Collections.emptySet()).remove(u1);

        if (friendshipTimestamps.containsKey(u1)) friendshipTimestamps.get(u1).remove(u2);
        if (friendshipTimestamps.containsKey(u2)) friendshipTimestamps.get(u2).remove(u1);

        dataConfig.set(u1.toString() + ".timestamps." + u2.toString(), null);
        dataConfig.set(u2.toString() + ".timestamps." + u1.toString(), null);

        if (r1 || r2) {
            saveData();
            return true;
        }
        return false;
    }

    // Alias for GUIListener
    public boolean unfriend(UUID u1, UUID u2) {
        return removeFriend(u1, u2);
    }

    public long getFriendshipTimestamp(UUID u1, UUID u2) {
        Map<UUID, Long> times = friendshipTimestamps.get(u1);
        if (times != null && times.containsKey(u2)) {
            return times.get(u2);
        }
        return System.currentTimeMillis();
    }

    public static String formatFriendshipDuration(long timestamp) {
        long diff = Math.max(0, System.currentTimeMillis() - timestamp);
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;

        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return Math.max(1, minutes) + "m";
    }

    public Set<UUID> getFriends(UUID uuid) {
        return friends.getOrDefault(uuid, Collections.emptySet());
    }

    public void sendRequest(UUID from, UUID to) {
        pendingRequests.put(to, from);
    }

    public UUID getPendingRequest(UUID to) {
        return pendingRequests.get(to);
    }

    public void clearPendingRequest(UUID to) {
        pendingRequests.remove(to);
    }

    public boolean isMuted(UUID receiver, UUID sender) {
        return mutedPlayers.getOrDefault(receiver, Collections.emptySet()).contains(sender);
    }

    public boolean toggleMute(UUID receiver, UUID target) {
        Set<UUID> set = mutedPlayers.computeIfAbsent(receiver, k -> new HashSet<>());
        boolean mutedNow;
        if (set.contains(target)) {
            set.remove(target);
            mutedNow = false;
        } else {
            set.add(target);
            mutedNow = true;
        }
        saveData();
        return mutedNow;
    }

    public PrivacyMode getPrivacyMode(UUID uuid) {
        return privacyModes.getOrDefault(uuid, PrivacyMode.EVERYONE);
    }

    public void setPrivacyMode(UUID uuid, PrivacyMode mode) {
        privacyModes.put(uuid, mode);
        saveData();
    }

    public PrivacyMode cyclePrivacyMode(UUID uuid) {
        PrivacyMode current = getPrivacyMode(uuid);
        PrivacyMode next = switch (current) {
            case EVERYONE -> PrivacyMode.FRIENDS;
            case FRIENDS -> PrivacyMode.NO_ONE;
            case NO_ONE -> PrivacyMode.EVERYONE;
        };
        setPrivacyMode(uuid, next);
        return next;
    }

    public UUID getLastMessaged(UUID uuid) {
        return lastMessaged.get(uuid);
    }

    public void setLastMessaged(UUID player, UUID target) {
        lastMessaged.put(player, target);
    }

    public void openSettingsGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Direct Message Settings"));

        PrivacyMode mode = getPrivacyMode(player.getUniqueId());
        ItemStack privacyItem = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = privacyItem.getItemMeta();
        if (meta != null) {
            meta.displayName(DesignCore.color("&e&lMessage Privacy"));
            meta.lore(List.of(
                    DesignCore.color("&7Current: &a" + mode.name()),
                    DesignCore.color("&8Click to cycle (Everyone / Friends / No One)")
            ));
            privacyItem.setItemMeta(meta);
        }
        inv.setItem(13, privacyItem);

        player.openInventory(inv);
    }
}