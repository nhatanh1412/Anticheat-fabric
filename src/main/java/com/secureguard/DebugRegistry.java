package com.secureguard;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DebugRegistry {
    private final Set<UUID> enabled = ConcurrentHashMap.newKeySet();

    public boolean enable(UUID uuid) {
        return enabled.add(uuid);
    }

    public boolean disable(UUID uuid) {
        return enabled.remove(uuid);
    }

    public boolean isEnabled(UUID uuid) {
        return enabled.contains(uuid);
    }

    public void clear() {
        enabled.clear();
    }
}