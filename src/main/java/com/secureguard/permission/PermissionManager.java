package com.secureguard.permission;

import com.secureguard.SecureGuard;
import com.secureguard.storage.UuidStore;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

public final class PermissionManager {
    private final UuidStore admins;
    private final UuidStore whitelist;

    public PermissionManager(UuidStore admins, UuidStore whitelist) {
        this.admins = admins;
        this.whitelist = whitelist;
    }

    public boolean isConsole(CommandSourceStack source) {
        return source.getEntity() == null;
    }

    public boolean isAdmin(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        return player != null && admins.contains(player.getUUID());
    }

    public boolean isWhitelist(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        return player != null && whitelist.contains(player.getUUID());
    }

    public boolean isAuthorized(CommandSourceStack source) {
        return isConsole(source) || isAdmin(source) || isWhitelist(source);
    }

    public boolean canSeeAdminCommands(CommandSourceStack source) {
        return isConsole(source) || isAdmin(source) || isWhitelist(source);
    }

    public boolean canSeeWhitelistCommands(CommandSourceStack source) {
        var visibility = SecureGuard.CONFIG.get();
        if (isAdmin(source)) return visibility.whitelistVisibleToAdmins;
        if (isConsole(source)) return !visibility.whitelistHiddenFromConsole;
        if (isWhitelist(source)) return !visibility.whitelistHiddenFromWhitelistUsers;
        return false;
    }

    public boolean isConsoleOrWhitelist(CommandSourceStack source) {
        return isConsole(source) || isWhitelist(source);
    }
}