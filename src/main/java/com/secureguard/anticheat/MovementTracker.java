package com.secureguard.anticheat;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MovementTracker {
    private final Map<UUID, PlayerData> players = new ConcurrentHashMap<>();

    public void start(ServerPlayer player) {
        players.put(player.getUUID(), new PlayerData(player.getUUID()));
    }

    public MovementFrame sample(ServerPlayer player, long tick, int packetRate, double tps) {
        PlayerData data = players.computeIfAbsent(player.getUUID(), PlayerData::new);
        return data.sample(player, tick, packetRate, tps);
    }

    public PlayerData get(UUID uuid) {
        return players.get(uuid);
    }

    public void remove(UUID uuid) {
        players.remove(uuid);
    }

    public void clear() {
        players.clear();
    }
}