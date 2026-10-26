package com.itsfriendly.designcore.manager;

import com.itsfriendly.designcore.DesignCore;
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

    private final Map<UUID, Set<UUID>> friends = new HashMap<>();
    private final Map<UUID, Set<UUID>> mutedSenders = new HashMap<>();
    private final Map<UUID, PrivacyMode> privacyModes = new HashMap<>();
    private final Map<UUID, UUID> lastMessaged = new HashMap<>();

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
        for (String key : dataConfig.getKeys(false)) {
            UUID id = UUID.fromString(key);
            List<String> rawFriends = dataConfig.getStringList(key + ".friends");
            Set<UUID> friendList = new HashSet<>();
            for (String f : rawFriends) friendList.add(UUID.fromString(f));
            friends.put(id, friendList);

            List<String> rawMutes = dataConfig.getStringList(key + ".muted");
            Set<UUID> muteList = new HashSet<>();
            for (String m : rawMutes) muteList.add(UUID.fromString(m));
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
        for (Map.Entry<UUID, Set<UUID>> entry : friends.entrySet()) {
            List<String> list = new ArrayList<>();
            for (UUID uid : entry.getValue()) list.add(uid.toString());
            dataConfig.set(entry.getKey() + ".friends", list);
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

    public boolean addFriend(UUID player, UUID target) {
        friends.putIfAbsent(player, new HashSet<>());
        return friends.get(player).add(target);
    }

    public Set<UUID> getFriends(UUID player) {
        return friends.getOrDefault(player, Collections.emptySet());
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