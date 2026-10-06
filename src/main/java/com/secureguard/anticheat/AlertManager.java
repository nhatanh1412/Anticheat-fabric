package com.secureguard.anticheat;

import com.secureguard.SecureGuard;
import com.secureguard.anticheat.check.Check;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class AlertManager {
    public void alert(MinecraftServer server, Check check, MovementFrame frame, double violationLevel) {
        var settings = SecureGuard.CONFIG.get();
        if (!settings.alertsEnabled) return;
        String message = "[SecureGuard] " + frame.playerName() + " failed " + check.name() + " A VL="
                + String.format(java.util.Locale.ROOT, "%.1f", violationLevel) + " Ping=" + frame.ping();
        if (settings.consoleAlerts && SecureGuard.consoleAlerts) {
            SecureGuard.LOGGER.log(System.Logger.Level.WARNING, message);
        }
        if (settings.adminAlerts) {
            for (ServerPlayer recipient : server.getPlayerList().getPlayers()) {
                if ((SecureGuard.ADMINS.contains(recipient.getUUID()) || SecureGuard.WHITELIST.contains(recipient.getUUID()))
                        && SecureGuard.ALERT_PREFERENCES.isEnabled(recipient.getUUID())) {
                    recipient.sendSystemMessage(Component.literal(message));
                }
            }
        }
    }
}