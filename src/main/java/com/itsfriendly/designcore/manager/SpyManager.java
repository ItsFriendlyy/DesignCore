package com.itsfriendly.designcore.manager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SpyManager {

    private final Set<UUID> activeSpies = new HashSet<>();

    public boolean isSpying(UUID playerUuid) {
        return activeSpies.contains(playerUuid);
    }

    public boolean toggleSpy(UUID playerUuid) {
        if (activeSpies.contains(playerUuid)) {
            activeSpies.remove(playerUuid);
            return false;
        } else {
            activeSpies.add(playerUuid);
            return true;
        }
    }
}