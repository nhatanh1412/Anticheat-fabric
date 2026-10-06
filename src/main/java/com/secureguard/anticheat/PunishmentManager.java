package com.secureguard.anticheat;

import com.secureguard.SecureGuard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PunishmentManager {
    private final Map<UUID, Map<String, Long>> lastPunishment = new HashMap<>();

    public void punishIfConfigured(MinecraftServer server, ServerPlayer player, String checkId, double violationLevel, long tick, double tps) {
        var settings = SecureGuard.CONFIG.get();
        var check = settings.checks.get(checkId);
        if (!settings.punishmentsEnabled || check == null || violationLevel < check.punishThreshold()
                || tps < settings.lowTpsThreshold) return;
        Map<String, Long> perCheck = lastPunishment.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
        long previous = perCheck.getOrDefault(checkId, Long.MIN_VALUE / 2);
        if (tick - previous < 1200) return;
        String command = settings.punishmentCommands.get(checkId);
        if (command == null || command.isBlank()) return;
        command = command.replace("%player%", player.getPlainTextName()).replace("%uuid%", player.getUUID().toString());
        try {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS), command);
            perCheck.put(checkId, tick);
            SecureGuard.AUDIT.record("SecureGuard", null, "ANTICHEAT_" + checkId.toUpperCase(java.util.Locale.ROOT), player.getUUID().toString(), "DISPATCHED");
        } catch (RuntimeException | IOException exception) {
            SecureGuard.LOGGER.log(System.Logger.Level.ERROR, "Could not dispatch SecureGuard punishment for " + player.getPlainTextName(), exception);
        }
    }

    public void remove(UUID uuid) {
        lastPunishment.remove(uuid);
    }
}