package com.itsfriendly.designcore.manager;

import com.itsfriendly.designcore.DesignCore;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ChatManager {

    private final DesignCore plugin;
    private boolean muted;
    private final Set<UUID> spyEnabled = new HashSet<>();

    public ChatManager(DesignCore plugin) {
        this.plugin = plugin;
        this.muted = plugin.getConfig().getBoolean("settings.chat-muted", false);
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        plugin.getConfig().set("settings.chat-muted", muted);
        plugin.saveConfig();
    }

    public boolean toggleSpy(UUID uuid) {
        if (spyEnabled.contains(uuid)) {
            spyEnabled.remove(uuid);
            return false;
        } else {
            spyEnabled.add(uuid);
            return true;
        }
    }

    public boolean hasSpy(UUID uuid) {
        return spyEnabled.contains(uuid);
    }

    public Set<UUID> getSpyEnabled() {
        return spyEnabled;
    }
}
