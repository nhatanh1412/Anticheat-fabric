package com.secureguard.anticheat;

import com.secureguard.SecureGuard;
import com.secureguard.anticheat.check.FlyCheck;
import com.secureguard.anticheat.check.NoFallCheck;
import com.secureguard.anticheat.check.PhaseCheck;
import com.secureguard.anticheat.check.SpeedCheck;
import com.secureguard.anticheat.check.StepCheck;
import com.secureguard.anticheat.check.TimerCheck;
import com.secureguard.anticheat.check.VelocityCheck;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class AntiCheatManager {
    private final MovementTracker tracker = new MovementTracker();
    private final ViolationManager violations = new ViolationManager(
            java.util.List.of(new SpeedCheck(), new FlyCheck(), new NoFallCheck(), new PhaseCheck(),
                    new VelocityCheck(), new TimerCheck(), new StepCheck()),
            new AlertManager(), new PunishmentManager());
    private final ConcurrentMap<UUID, PacketWindow> packetWindows = new ConcurrentHashMap<>();
    private long tick;

    public void register() {
        ServerTickEvents.END_SERVER_TICK.register(this::onEndTick);
        ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> onJoin(listener.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((listener, server) -> onDisconnect(listener.getPlayer()));
    }

    public void onMovementPacket(ServerPlayer player) {
        if (player == null) return;
        packetWindows.computeIfAbsent(player.getUUID(), ignored -> new PacketWindow()).record(System.nanoTime());
    }

    private void onJoin(ServerPlayer player) {
        tracker.start(player);
        packetWindows.put(player.getUUID(), new PacketWindow());
    }

    private void onDisconnect(ServerPlayer player) {
        UUID uuid = player.getUUID();
        tracker.remove(uuid);
        violations.remove(uuid);
        packetWindows.remove(uuid);
        SecureGuard.DEBUG_TARGETS.disable(uuid);
    }

    private void onEndTick(MinecraftServer server) {
        tick++;
        var settings = SecureGuard.CONFIG.get();
        if (!settings.enabled || !SecureGuard.antiCheatEnabled) return;
        double tickMillis = server.getCurrentSmoothedTickTime();
        double tps = tickMillis <= 0.0 ? 20.0 : Math.min(20.0, 1000.0 / Math.max(50.0, tickMillis));
        long now = System.nanoTime();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketWindow window = packetWindows.computeIfAbsent(player.getUUID(), ignored -> new PacketWindow());
            int packetRate = window.countRecent(now);
            MovementFrame frame = tracker.sample(player, tick, packetRate, tps);
            PlayerData data = tracker.get(player.getUUID());
            violations.process(server, player, data, frame, tick);
            if (SecureGuard.DEBUG_TARGETS.isEnabled(player.getUUID()) && tick % 10 == 0) {
                String debug = "[SecureGuard Debug] movement=(" + format(frame.dx()) + "," + format(frame.dy()) + "," + format(frame.dz())
                        + ") velocity=(" + format(frame.velocityX()) + "," + format(frame.velocityY()) + "," + format(frame.velocityZ())
                        + ") ping=" + frame.ping() + " packets/s=" + frame.movementPacketsPerSecond() + " tps=" + format(frame.tps())
                        + " prediction=" + (frame.inGrace() ? "grace" : "server movement envelope") + " setback=" + (frame.teleportGrace() ? "grace" : "ready")
                        + " " + violations.describe(player.getUUID());
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(debug));
            }
        }
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }

    private static final class PacketWindow {
        private final long[] timestamps = new long[64];
        private int next;
        private int size;

        private synchronized void record(long now) {
            timestamps[next] = now;
            next = (next + 1) % timestamps.length;
            if (size < timestamps.length) size++;
        }

        private synchronized int countRecent(long now) {
            int count = 0;
            long cutoff = now - 1_000_000_000L;
            for (int i = 0; i < size; i++) {
                if (timestamps[i] >= cutoff && timestamps[i] <= now) count++;
            }
            return count;
        }
    }
}