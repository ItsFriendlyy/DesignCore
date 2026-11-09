package com.itsfriendly.designcore.hook;

import java.util.UUID;

public interface VoiceHook {
    boolean isAvailable();
    void mutePlayer(UUID playerUuid);
    void unmutePlayer(UUID playerUuid);
    boolean isMuted(UUID playerUuid);
}