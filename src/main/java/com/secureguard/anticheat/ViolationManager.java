package com.secureguard.anticheat;

import com.secureguard.SecureGuard;
import com.secureguard.anticheat.check.Check;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ViolationManager {
    private final List<Check> checks;
    private final AlertManager alerts;
    private final PunishmentManager punishments;
    private final Map<UUID, Map<String, CheckState>> players = new HashMap<>();

    public ViolationManager(List<Check> checks, AlertManager alerts, PunishmentManager punishments) {
        this.checks = List.copyOf(checks);
        this.alerts = alerts;
        this.punishments = punishments;
    }

    public void process(MinecraftServer server, ServerPlayer player, PlayerData data, MovementFrame frame, long tick) {
        var settings = SecureGuard.CONFIG.get();
        Map<String, CheckState> playerStates = players.computeIfAbsent(frame.uuid(), ignored -> new HashMap<>());
        for (Check check : checks) {
            CheckState state = playerStates.computeIfAbsent(check.id(), ignored -> new CheckState());
            var configured = settings.checks.get(check.id());
            if (configured == null || !configured.enabled()) {
                state.violationLevel = Math.max(0.0, state.violationLevel - (configured == null ? 0.0 : configured.decay() / 20.0));
                continue;
            }
            double evidence = check.evaluate(frame);
            if (frame.tps() < settings.lowTpsThreshold) evidence *= Math.max(0.0, settings.lowTpsPunishmentMultiplier);
            if (evidence > 0.0) {
                state.buffer = Math.min(100.0, state.buffer + evidence);
                double required = Math.max(1.0, 1.0 + configured.buffer());
                while (state.buffer >= required) {
                    state.buffer -= required;
                    state.violationLevel += 1.0;
                }
            } else {
                state.buffer = Math.max(0.0, state.buffer - configured.decay() / 20.0);
                state.violationLevel = Math.max(0.0, state.violationLevel - configured.decay() / 20.0);
            }
            if (state.violationLevel >= configured.alertThreshold() && tick - state.lastAlertTick >= 20) {
                alerts.alert(server, check, frame, state.violationLevel);
                state.lastAlertTick = tick;
            }
            punishments.punishIfConfigured(server, player, check.id(), state.violationLevel, tick, frame.tps());
            if (settings.setbackEnabled && state.violationLevel >= Math.max(4.0, configured.alertThreshold() * 2.0)
                    && (check.id().equals("speed") || check.id().equals("fly") || check.id().equals("phase"))) {
                data.setback(player, tick, settings.setbackCooldownTicks);
            }
        }
    }

    public String describe(UUID uuid) {
        Map<String, CheckState> states = players.get(uuid);
        if (states == null || states.isEmpty()) return "VL=none";
        StringBuilder result = new StringBuilder("VL=");
        boolean first = true;
        for (Map.Entry<String, CheckState> entry : states.entrySet()) {
            if (!first) result.append(',');
            result.append(entry.getKey()).append(':').append(String.format(java.util.Locale.ROOT, "%.1f", entry.getValue().violationLevel));
            first = false;
        }
        return result.toString();
    }

    public void remove(UUID uuid) {
        players.remove(uuid);
        punishments.remove(uuid);
    }

    public void clear() {
        players.clear();
    }

    private static final class CheckState {
        private double violationLevel;
        private double buffer;
        private long lastAlertTick = Long.MIN_VALUE / 2;
    }
}