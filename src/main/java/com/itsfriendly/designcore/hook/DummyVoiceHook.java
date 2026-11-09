package com.itsfriendly.designcore.hook;

import java.util.UUID;

public class DummyVoiceHook implements VoiceHook {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void mutePlayer(UUID playerUuid) {
        // No-op fallback
    }

    @Override
    public void unmutePlayer(UUID playerUuid) {
        // No-op fallback
    }

    @Override
    public boolean isMuted(UUID playerUuid) {
        return false;
    }
}