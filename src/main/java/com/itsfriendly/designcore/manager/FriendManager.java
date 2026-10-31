package com.itsfriendly.designcore.manager;

import com.itsfriendly.designcore.DesignCore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FriendManager {

    public enum PrivacyMode {
        EVERYONE,
        FRIENDS,
        NO_ONE
    }

    private final DesignCore plugin;
    private final File dataFile;
    private final YamlConfiguration dataConfig;

    // UUID -> (Friend UUID -> Timestamp since friendship started in ms)
    private final Map<UUID, Map<UUID, Long>> friendsWithTime = new HashMap<>();
    private final Map<UUID, Set<UUID>> mutedSenders = new HashMap<>();
    private final Map<UUID, PrivacyMode> privacyModes = new HashMap<>();
    private final Map<UUID, UUID> lastMessaged = new HashMap<>();

    // Tracks incoming pending requests: Target UUID -> Set of Sender UUIDs
    private final Map<UUID, Set<UUID>> incomingRequests = new HashMap<>();

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
        friendsWithTime.clear();
        mutedSenders.clear();
        privacyModes.clear();

        for (String key : dataConfig.getKeys(false)) {
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }

            Map<UUID, Long> friendMap = new HashMap<>();
            ConfigurationSection friendSec = dataConfig.getConfigurationSection(key + ".friends-data");
            if (friendSec != null) {
                for (String fKey : friendSec.getKeys(false)) {
                    try {
                        friendMap.put(UUID.fromString(fKey), friendSec.getLong(fKey));
                    } catch (IllegalArgumentException ignored) {}
                }
            } else {
                // Backwards compatibility for older list structure
                List<String> rawFriends = dataConfig.getStringList(key + ".friends");
                for (String f : rawFriends) {
                    try {
                        friendMap.put(UUID.fromString(f), System.currentTimeMillis());
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            friendsWithTime.put(id, friendMap);

            List<String> rawMutes = dataConfig.getStringList(key + ".muted");
            Set<UUID> muteList = new HashSet<>();
            for (String m : rawMutes) {
                try {
                    muteList.add(UUID.fromString(m));
                } catch (IllegalArgumentException ignored) {}
            }
            mutedSenders.put(id, muteList);

            String modeStr = dataConfig.getString(key + ".privacy-mode", "EVERYONE");
            try {
                privacyModes.put(id, PrivacyMode.valueOf(modeStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                privacyModes.put(id, PrivacyMode.EVERYONE);
            }
        }
    }

    public void saveData() {
        for (Map.Entry<UUID, Map<UUID, Long>> entry : friendsWithTime.entrySet()) {
            String path = entry.getKey().toString() + ".friends-data";
            dataConfig.set(entry.getKey() + ".friends", null); // clear legacy key
            for (Map.Entry<UUID, Long> fEntry : entry.getValue().entrySet()) {
                dataConfig.set(path + "." + fEntry.getKey().toString(), fEntry.getValue());
            }
        }

        for (Map.Entry<UUID, Set<UUID>> entry : mutedSenders.entrySet()) {
            List<String> list = new ArrayList<>();
            for (UUID uid : entry.getValue()) list.add(uid.toString());
            dataConfig.set(entry.getKey() + ".muted", list);
        }

        for (Map.Entry<UUID, PrivacyMode> entry : privacyModes.entrySet()) {
            dataConfig.set(entry.getKey() + ".privacy-mode", entry.getValue().name());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save userdata.yml!");
        }
    }

    public boolean areFriends(UUID p1, UUID p2) {
        return friendsWithTime.getOrDefault(p1, Collections.emptyMap()).containsKey(p2);
    }

    public void sendRequest(UUID sender, UUID target) {
        incomingRequests.computeIfAbsent(target, k -> new HashSet<>()).add(sender);
    }

    public boolean hasRequestFrom(UUID target, UUID sender) {
        return incomingRequests.getOrDefault(target, Collections.emptySet()).contains(sender);
    }

    public void removeRequest(UUID target, UUID sender) {
        Set<UUID> requests = incomingRequests.get(target);
        if (requests != null) {
            requests.remove(sender);
            if (requests.isEmpty()) {
                incomingRequests.remove(target);
            }
        }
    }

    public void makeFriends(UUID p1, UUID p2) {
        long now = System.currentTimeMillis();
        friendsWithTime.computeIfAbsent(p1, k -> new HashMap<>()).put(p2, now);
        friendsWithTime.computeIfAbsent(p2, k -> new HashMap<>()).put(p1, now);
        removeRequest(p1, p2);
        removeRequest(p2, p1);
        saveData();
    }

    public boolean unfriend(UUID p1, UUID p2) {
        boolean removed1 = false;
        boolean removed2 = false;

        Map<UUID, Long> map1 = friendsWithTime.get(p1);
        if (map1 != null && map1.containsKey(p2)) {
            map1.remove(p2);
            dataConfig.set(p1.toString() + ".friends-data." + p2.toString(), null);
            removed1 = true;
        }

        Map<UUID, Long> map2 = friendsWithTime.get(p2);
        if (map2 != null && map2.containsKey(p1)) {
            map2.remove(p1);
            dataConfig.set(p2.toString() + ".friends-data." + p1.toString(), null);
            removed2 = true;
        }

        if (removed1 || removed2) {
            saveData();
            return true;
        }
        return false;
    }

    public long getFriendshipTimestamp(UUID p1, UUID p2) {
        return friendsWithTime.getOrDefault(p1, Collections.emptyMap()).getOrDefault(p2, System.currentTimeMillis());
    }

    public String formatFriendshipDuration(long sinceMillis) {
        long diff = Math.max(0, System.currentTimeMillis() - sinceMillis);
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + (days == 1 ? " day" : " days");
        } else if (hours > 0) {
            return hours + (hours == 1 ? " hour" : " hours");
        } else if (minutes > 0) {
            return minutes + (minutes == 1 ? " minute" : " minutes");
        } else {
            return "Just now";
        }
    }

    public Set<UUID> getFriends(UUID player) {
        return friendsWithTime.getOrDefault(player, Collections.emptyMap()).keySet();
    }

    public PrivacyMode getPrivacyMode(UUID player) {
        return privacyModes.getOrDefault(player, PrivacyMode.EVERYONE);
    }

    public PrivacyMode cyclePrivacyMode(UUID player) {
        PrivacyMode current = getPrivacyMode(player);
        PrivacyMode next = switch (current) {
            case EVERYONE -> PrivacyMode.FRIENDS;
            case FRIENDS -> PrivacyMode.NO_ONE;
            case NO_ONE -> PrivacyMode.EVERYONE;
        };
        privacyModes.put(player, next);
        return next;
    }

    public boolean isMuted(UUID receiver, UUID sender) {
        return mutedSenders.getOrDefault(receiver, Collections.emptySet()).contains(sender);
    }

    public boolean toggleMute(UUID receiver, UUID sender) {
        mutedSenders.putIfAbsent(receiver, new HashSet<>());
        if (mutedSenders.get(receiver).contains(sender)) {
            mutedSenders.get(receiver).remove(sender);
            return false;
        } else {
            mutedSenders.get(receiver).add(sender);
            return true;
        }
    }

    public void setLastMessaged(UUID p1, UUID p2) {
        lastMessaged.put(p1, p2);
    }

    public UUID getLastMessaged(UUID player) {
        return lastMessaged.get(player);
    }
}